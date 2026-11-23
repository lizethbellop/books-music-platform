package com.musa.communities_service.polls;

import com.musa.communities_service.CommunitiesServiceApplication;
import com.musa.communities_service.polls.dto.*;
import com.musa.communities_service.polls.messaging.*;
import com.musa.communities_service.polls.service.PollService;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.connection.*;
import org.springframework.amqp.core.Message;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(classes=CommunitiesServiceApplication.class,webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnabledIfEnvironmentVariable(named="POLLS_TEST_ISOLATED",matches="true")
class PollBackendIntegrationTest {
    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    @Autowired PollService service;
    @Autowired PollChangePublisher publisher;
    @Autowired PollChanges changes;
    @Autowired PlatformTransactionManager manager;
    @Autowired @Qualifier("pollRabbitTemplate") RabbitTemplate rabbit;
    private static final JsonMapper JSON=JsonMapper.builder().build();
    private static final HttpClient HTTP=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    UUID community,admin,member,outsider;
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",()->System.getenv("POLLS_TEST_DB_URL"));
        r.add("spring.datasource.username",()->"integration_owner");
        r.add("spring.datasource.password",()->System.getenv("POLLS_TEST_DB_PASSWORD"));
        r.add("spring.jpa.hibernate.ddl-auto",()->"validate");
        r.add("spring.jpa.show-sql",()->"false");
        r.add("spring.jpa.open-in-view",()->"false");
        r.add("spring.datasource.hikari.maximum-pool-size",()->10);
        r.add("spring.datasource.hikari.connection-timeout",()->2000);
        r.add("spring.datasource.hikari.data-source-properties.socketTimeout",()->10);
        r.add("musa.security.jwt.secret",()->Base64.getEncoder().encodeToString(new byte[64]));
        r.add("polls.events.enabled",()->true);
        r.add("polls.events.interval-ms",()->3600000);
        r.add("spring.rabbitmq.host",()->"127.0.0.1");
        r.add("spring.rabbitmq.port",()->System.getenv("POLLS_TEST_RABBIT_PORT"));
        r.add("spring.rabbitmq.username",()->"integration");
        r.add("spring.rabbitmq.password",()->System.getenv("POLLS_TEST_RABBIT_PASSWORD"));
        r.add("spring.rabbitmq.virtual-host",()->"musa");
        r.add("spring.rabbitmq.connection-timeout",()->"1s");
    }
    @BeforeEach void setup() {
        assertEquals("integration_owner",jdbc.queryForObject("SELECT current_user",String.class));
        assertNotEquals(5432,jdbc.queryForObject("SELECT inet_server_port()",Integer.class));
        jdbc.execute("TRUNCATE community_poll_changes,community_poll_votes,community_poll_options,community_polls,activity_participants,community_activities,community_members,communities CASCADE");
        community=UUID.randomUUID();admin=UUID.randomUUID();member=UUID.randomUUID();outsider=UUID.randomUUID();
        jdbc.update("INSERT INTO communities(id,name,type,visibility,created_by) VALUES (?,'Pruebas','LIBROS','PRIVADA',?)",community,admin);
        addMember(admin,"ADMINISTRADOR");addMember(member,"MIEMBRO");
    }
    void addMember(UUID user,String role) {
        jdbc.update("INSERT INTO community_members(id,community_id,user_id,role) VALUES (?,?,?,?)",UUID.randomUUID(),community,user,role);
    }
    String root(){return "/api/v1/communities/"+community+"/polls";}
    String token(UUID user)throws Exception{return token(user,"musa-auth","musa-api",Instant.now().plusSeconds(600),new byte[64]);}
    String token(UUID user,String issuer,String audience,Instant exp,byte[] key)throws Exception {
        var claims=new JWTClaimsSet.Builder().subject(user.toString()).issuer(issuer).audience(audience)
                .expirationTime(Date.from(exp)).build();
        var jwt=new SignedJWT(new JWSHeader(JWSAlgorithm.HS512),claims);jwt.sign(new MACSigner(key));return jwt.serialize();
    }
    HttpResponse<String> raw(String method,String path,String token,String body)throws Exception {
        var b=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).timeout(Duration.ofSeconds(15));
        if(token!=null)b.header("Authorization","Bearer "+token);
        if(body!=null)b.header("Content-Type","application/json");
        return HTTP.send(b.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
    }
    HttpResponse<String> req(String method,String path,UUID user,Object body)throws Exception {
        return raw(method,path,token(user),body==null?null:JSON.writeValueAsString(body));
    }
    JsonNode expect(int status,HttpResponse<String> response) {
        assertEquals(status,response.statusCode(),response.body());return JSON.readTree(response.body());
    }
    Map<String,Object> createBody(List<String> labels){return Map.of("question","¿Qué leemos?","options",labels,"closesAt",OffsetDateTime.now(ZoneOffset.UTC).plusHours(1).toString());}
    PollResponse poll(){return service.create(community,admin,new CreatePollRequest("¿Qué leemos?",List.of("A","B"),OffsetDateTime.now(ZoneOffset.UTC).plusHours(1)));}
    String path(PollResponse p){return root()+"/"+p.id();}
    int count(String table){return jdbc.queryForObject("SELECT count(*) FROM "+table,Integer.class);}

    @Test void fullHttpFlowAndIdempotentRetryAfterClose()throws Exception {
        var created=expect(201,req("POST",root(),admin,createBody(List.of(" A ","B"))));
        String p=root()+"/"+created.path("id").asString();String option=created.path("options").get(0).path("id").asString();
        assertEquals("A",created.path("options").get(0).path("label").asString());
        var body=Map.of("optionId",option);
        var result=expect(200,req("POST",p+"/votes",member,body));assertEquals(1,result.path("totalVotes").asInt());
        assertEquals(option,result.path("selectedOptionId").asString());
        expect(200,req("POST",p+"/votes",member,body));assertEquals(1,count("community_poll_votes"));
        String other=created.path("options").get(1).path("id").asString();
        expect(409,req("POST",p+"/votes",member,Map.of("optionId",other)));
        assertFalse(expect(200,req("POST",p+"/close",admin,null)).path("open").asBoolean());
        expect(200,req("POST",p+"/votes",member,body));
        expect(409,req("POST",p+"/votes",admin,body));
        expect(200,req("POST",p+"/close",admin,null));assertEquals(1,count("community_poll_changes"));
    }
    @Test void jwtRejectsMissingExpiredWrongIssuerAudienceSignatureAndMalformedToken()throws Exception {
        assertEquals(401,raw("GET",root(),null,null).statusCode());
        for(String bad:List.of("invalid",token(admin,"bad","musa-api",Instant.now().plusSeconds(60),new byte[64]),
                token(admin,"musa-auth","bad",Instant.now().plusSeconds(60),new byte[64]),
                token(admin,"musa-auth","musa-api",Instant.now().minusSeconds(1),new byte[64]),
                token(admin,"musa-auth","musa-api",Instant.now().plusSeconds(60),"different-test-signing-key-".repeat(4).getBytes(java.nio.charset.StandardCharsets.UTF_8))))
            assertEquals(401,raw("GET",root(),bad,null).statusCode());
        assertEquals(200,req("GET",root(),admin,null).statusCode());
    }
    @Test void rolesMembershipAndCrossCommunityAccessAreEnforced()throws Exception {
        var p=poll();expect(403,req("POST",root(),member,createBody(List.of("A","B"))));
        expect(403,req("POST",path(p)+"/close",member,null));
        expect(404,req("GET",path(p),outsider,null));
        expect(404,req("POST",path(p)+"/realtime/tickets",outsider,null));
        UUID other=UUID.randomUUID();jdbc.update("INSERT INTO communities(id,name,type,visibility,created_by) VALUES (?,'Otra','LIBROS','PRIVADA',?)",other,admin);
        jdbc.update("INSERT INTO community_members(id,community_id,user_id,role) VALUES (?,?,?,'ADMINISTRADOR')",UUID.randomUUID(),other,admin);
        expect(404,req("GET","/api/v1/communities/"+other+"/polls/"+p.id(),admin,null));
        jdbc.update("UPDATE community_members SET status='RETIRADO' WHERE user_id=?",member);
        expect(404,req("GET",path(p)+"/results",member,null));
        var communities=expect(200,req("GET","/api/v1/polls/communities",admin,null));assertEquals(2,communities.path("items").size());
    }
    @Test void rejectsInvalidOptionsDatesQuestionAndPaginationWithoutPartialWrites()throws Exception {
        for(var labels:List.of(List.of("A"),List.of("A"," a "),List.of("A"," "),Collections.nCopies(11,"A")))
            expect(400,req("POST",root(),admin,createBody(labels)));
        var body=new HashMap<>(createBody(List.of("A","B")));body.put("question"," ");expect(400,req("POST",root(),admin,body));
        body.put("question","x".repeat(301));expect(400,req("POST",root(),admin,body));
        body.put("question","Válida");body.put("closesAt",OffsetDateTime.now().minusHours(1).toString());expect(400,req("POST",root(),admin,body));
        assertEquals(0,count("community_polls"));assertEquals(0,count("community_poll_changes"));
        expect(400,req("GET",root()+"?size=51",admin,null));
        expect(400,req("GET",root()+"?page=-1",admin,null));
        expect(400,raw("POST",root(),token(admin),"{"));
    }
    @Test void optionMustBelongToPollAndDatabaseAlsoRejectsIt()throws Exception {
        var a=poll();var b=poll();UUID foreign=b.options().getFirst().id();
        expect(400,req("POST",path(a)+"/votes",member,Map.of("optionId",foreign)));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->jdbc.update(
                "INSERT INTO community_poll_votes(id,poll_id,option_id,user_id) VALUES (?,?,?,?)",UUID.randomUUID(),a.id(),foreign,member));
        assertEquals(0,count("community_poll_votes"));
    }
    @Test @Tag("concurrency") @Timeout(40) void sameUserConcurrentRequestsCreateOneVote()throws Exception {
        var p=poll();var body=Map.of("optionId",p.options().getFirst().id());
        var pool=Executors.newFixedThreadPool(12);var start=new CountDownLatch(1);
        try {
            var tasks=new ArrayList<Future<Integer>>();
            for(int i=0;i<12;i++)tasks.add(pool.submit(()->{start.await();return req("POST",path(p)+"/votes",member,body).statusCode();}));
            start.countDown();for(var task:tasks)assertEquals(200,task.get(20,TimeUnit.SECONDS));
        } finally {stopWorkers(pool);}
        assertEquals(1,count("community_poll_votes"));assertEquals(1,service.results(community,p.id(),admin).totalVotes());
    }
    @Test @Tag("concurrency") @Timeout(40) void differentUsersConcurrentRequestsCountEveryCommittedVote()throws Exception {
        var p=poll();var pool=Executors.newFixedThreadPool(8);var start=new CountDownLatch(1);
        var users=new ArrayList<UUID>();for(int i=0;i<24;i++){var u=UUID.randomUUID();addMember(u,"MIEMBRO");users.add(u);}
        try {
            var tasks=new ArrayList<Future<Integer>>();
            for(var u:users)tasks.add(pool.submit(()->{start.await();return req("POST",path(p)+"/votes",u,Map.of("optionId",p.options().getFirst().id())).statusCode();}));
            start.countDown();for(var task:tasks)assertEquals(200,task.get(30,TimeUnit.SECONDS));
        } finally {stopWorkers(pool);}
        assertEquals(24,service.results(community,p.id(),admin).totalVotes());assertEquals(1,count("community_poll_changes"));
    }
    @Test @Tag("concurrency") @Timeout(40) void closingWaitsForVotingLockAndNewVotesAreRejected()throws Exception {
        var p=poll();var pool=Executors.newSingleThreadExecutor();
        try(var connection=jdbc.getDataSource().getConnection()) {
            connection.setAutoCommit(false);
            try(var statement=connection.prepareStatement("SELECT id FROM community_polls WHERE id=? FOR SHARE")) {
                statement.setObject(1,p.id());statement.executeQuery().close();
                var closing=pool.submit(()->service.close(community,p.id(),admin));
                awaitLockWait();assertFalse(closing.isDone());
                connection.commit();assertFalse(closing.get(5,TimeUnit.SECONDS).open());
            }
        } finally {stopWorkers(pool);}
        expect(409,req("POST",path(p)+"/votes",member,Map.of("optionId",p.options().getFirst().id())));
    }
    @Test @Tag("concurrency") @Timeout(40) void lockTimeoutReturnsControlled503AndLeavesNoVote()throws Exception {
        var p=poll();
        try(var connection=jdbc.getDataSource().getConnection()) {
            connection.setAutoCommit(false);
            try(var statement=connection.prepareStatement("SELECT id FROM community_polls WHERE id=? FOR UPDATE")) {
                statement.setObject(1,p.id());statement.executeQuery().close();
                long start=System.nanoTime();var response=req("POST",path(p)+"/votes",member,Map.of("optionId",p.options().getFirst().id()));
                expect(503,response);assertTrue(Duration.ofNanos(System.nanoTime()-start).toSeconds()<6);
                assertFalse(response.body().contains("SELECT"));assertFalse(response.body().contains("SQLException"));
            } finally {connection.rollback();}
        }
        assertEquals(0,count("community_poll_votes"));
    }
    @Test void expirationUsesDatabaseTime()throws Exception {
        var p=poll();jdbc.update("UPDATE community_polls SET created_at=clock_timestamp()-interval '2 hours',closes_at=clock_timestamp()-interval '1 hour' WHERE id=?",p.id());
        expect(409,req("POST",path(p)+"/votes",member,Map.of("optionId",p.options().getFirst().id())));
        assertFalse(service.get(community,p.id(),admin).open());
    }
    @Test void voteAndOutboxRollbackTogether() {
        var p=poll();UUID revision=jdbc.queryForObject("SELECT revision FROM community_poll_changes",UUID.class);
        assertThrows(IllegalStateException.class,()->new TransactionTemplate(manager).executeWithoutResult(status->{
            service.vote(community,p.id(),member,new VoteRequest(p.options().getFirst().id()));throw new IllegalStateException("simulated failure");
        }));
        assertEquals(0,count("community_poll_votes"));assertEquals(revision,jdbc.queryForObject("SELECT revision FROM community_poll_changes",UUID.class));
    }
    @Test void brokerFailureRetainsChangeAndRecoveryPublishesIt() {
        var p=poll();var dead=new CachingConnectionFactory("127.0.0.1",1);dead.setConnectionTimeout(200);
        try {
            new PollChangePublisher(jdbc,new RabbitTemplate(dead)).publish();
            assertEquals(1,count("community_poll_changes"));assertEquals(1,jdbc.queryForObject("SELECT attempts FROM community_poll_changes",Integer.class));
        } finally {dead.destroy();}
        jdbc.update("UPDATE community_poll_changes SET next_attempt_at=clock_timestamp()");
        publisher.publish();assertEquals(0,count("community_poll_changes"));assertEquals(p.id(),service.get(community,p.id(),admin).id());
    }
    @Test void newerRevisionSurvivesConfirmationOfOlderEvent() {
        var p=poll();RabbitTemplate stub=mock(RabbitTemplate.class);
        doAnswer(call->{
            new TransactionTemplate(manager).executeWithoutResult(status->changes.record(p.id()));
            CorrelationData correlation=call.getArgument(3);correlation.getFuture().complete(new CorrelationData.Confirm(true,null));return null;
        }).when(stub).send(eq("musa.polls"),eq(""),any(Message.class),any(CorrelationData.class));
        new PollChangePublisher(jdbc,stub).publish();assertEquals(1,count("community_poll_changes"));
        publisher.publish();assertEquals(0,count("community_poll_changes"));
    }
    @Test void bodyAndRequestRateAreBounded()throws Exception {
        assertEquals(413,raw("POST",root(),token(admin),"x".repeat(8193)).statusCode());
        boolean limited=false;
        for(int i=0;i<75;i++) {var response=req("GET",root(),admin,null);if(response.statusCode()==429){limited=true;assertTrue(response.headers().firstValue("Retry-After").isPresent());break;}}
        assertTrue(limited);
    }
    private static class SocketListener implements WebSocket.Listener {
        final BlockingQueue<String> frames=new LinkedBlockingQueue<>(32);
        final CompletableFuture<Integer> closed=new CompletableFuture<>();
        public void onOpen(WebSocket socket){socket.request(1);}
        public CompletionStage<?> onText(WebSocket socket,CharSequence data,boolean last){frames.offer(data.toString());socket.request(1);return null;}
        public CompletionStage<?> onClose(WebSocket socket,int status,String reason){closed.complete(status);return null;}
        String waitFor(String type)throws Exception {
            long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(6);
            while(System.nanoTime()<deadline){String frame=frames.poll(500,TimeUnit.MILLISECONDS);if(frame!=null&&type.equals(JSON.readTree(frame).path("type").asString()))return frame;}
            fail("No se recibió "+type);return "";
        }
    }
    @Test void websocketUsesSingleUseScopedTicketAndReceivesBrokerUpdates()throws Exception {
        var p=poll();var issued=expect(200,req("POST",path(p)+"/realtime/tickets",member,null));
        var listener=new SocketListener();var socket=HTTP.newWebSocketBuilder().buildAsync(URI.create("ws://127.0.0.1:"+port+"/api/v1/polls/ws"),listener).get(5,TimeUnit.SECONDS);
        try {
            String ticket=issued.path("ticket").asString();socket.sendText(JSON.writeValueAsString(Map.of("ticket",ticket)),true).join();
            listener.waitFor("ready");listener.waitFor("poll.updated");
            socket.sendText("{\"type\":\"ping\"}",true).join();listener.waitFor("pong");
            publisher.publish();listener.waitFor("poll.updated");
            var reused=new SocketListener();var other=HTTP.newWebSocketBuilder().buildAsync(URI.create("ws://127.0.0.1:"+port+"/api/v1/polls/ws"),reused).get(5,TimeUnit.SECONDS);
            try {other.sendText(JSON.writeValueAsString(Map.of("ticket",ticket)),true).join();assertEquals(1008,reused.closed.get(5,TimeUnit.SECONDS));}finally{other.abort();}
        } finally {socket.abort();}
    }
    @Test @Tag("concurrency") @Timeout(40) void exhaustedPoolReturns503AndRecoversAfterConnectionsAreReleased()throws Exception {
        var held=new ArrayList<java.sql.Connection>();
        try {
            for(int i=0;i<10;i++)held.add(jdbc.getDataSource().getConnection());
            var response=req("GET",root(),admin,null);expect(503,response);
            assertFalse(response.body().contains("HikariPool"));
        }finally{for(var c:held)c.close();}
        expect(200,req("GET",root(),admin,null));
    }
    @Test void jwtRequiresExpirationAndCanonicalUuidSubject()throws Exception {
        for(var claims:List.of(
                new JWTClaimsSet.Builder().subject(admin.toString()).issuer("musa-auth").audience("musa-api").build(),
                new JWTClaimsSet.Builder().subject("not-a-uuid").issuer("musa-auth").audience("musa-api").expirationTime(Date.from(Instant.now().plusSeconds(60))).build())) {
            var jwt=new SignedJWT(new JWSHeader(JWSAlgorithm.HS512),claims);jwt.sign(new MACSigner(new byte[64]));
            assertEquals(401,raw("GET",root(),jwt.serialize(),null).statusCode());
        }
    }
    @Test void ticketsAreBoundedPerUserAndRequireActiveMembership()throws Exception {
        var p=poll();for(int i=0;i<3;i++)expect(200,req("POST",path(p)+"/realtime/tickets",member,null));
        expect(429,req("POST",path(p)+"/realtime/tickets",member,null));
        jdbc.update("UPDATE community_members SET status='RETIRADO' WHERE user_id=?",member);
        expect(404,req("POST",path(p)+"/realtime/tickets",member,null));
    }
    @Test @Tag("concurrency") @Timeout(30)
    @DisplayName("Dos opciones simultáneas del mismo usuario producen un único voto y un conflicto")
    void concurrentDifferentOptionsKeepExactlyOneChoice()throws Exception {
        var p=poll();var workers=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);
        try {
            var first=workers.submit(()->{start.await();return req("POST",path(p)+"/votes",member,Map.of("optionId",p.options().get(0).id())).statusCode();});
            var second=workers.submit(()->{start.await();return req("POST",path(p)+"/votes",member,Map.of("optionId",p.options().get(1).id())).statusCode();});
            start.countDown();var statuses=new ArrayList<>(List.of(first.get(10,TimeUnit.SECONDS),second.get(10,TimeUnit.SECONDS)));Collections.sort(statuses);
            assertEquals(List.of(200,409),statuses);assertEquals(1,count("community_poll_votes"));
            assertEquals(1,service.results(community,p.id(),member).totalVotes());
        }finally{stopWorkers(workers);}
    }

    @Test @Tag("concurrency") @Timeout(30)
    @DisplayName("El bloqueo de una encuesta no impide votar en otra")
    void lockedPollDoesNotBlockUnrelatedPoll()throws Exception {
        var blocked=poll();var independent=poll();var workers=Executors.newSingleThreadExecutor();
        try(var connection=jdbc.getDataSource().getConnection()) {
            connection.setAutoCommit(false);
            try(var statement=connection.prepareStatement("SELECT id FROM community_polls WHERE id=? FOR UPDATE")) {
                statement.setObject(1,blocked.id());statement.executeQuery().close();
                var waiting=workers.submit(()->req("POST",path(blocked)+"/votes",member,Map.of("optionId",blocked.options().getFirst().id())));
                awaitLockWait();
                expect(200,req("POST",path(independent)+"/votes",admin,Map.of("optionId",independent.options().getFirst().id())));
                assertFalse(waiting.isDone(),"La encuesta independiente debe responder antes del timeout de la bloqueada");
                connection.commit();expect(200,waiting.get(5,TimeUnit.SECONDS));
                assertEquals(2,count("community_poll_votes"));
            }finally{connection.rollback();}
        }finally{stopWorkers(workers);}
    }

    @Test @Tag("concurrency") @Timeout(30)
    @DisplayName("Cancelar un voto revierte la transacción y libera el bloqueo para cerrar")
    void cancelledVoteReleasesLocksAndRollsBack()throws Exception {
        var p=poll();var workers=Executors.newSingleThreadExecutor();
        var inserted=new CountDownLatch(1);var finished=new CountDownLatch(1);var release=new CountDownLatch(1);
        try {
            var voting=workers.submit(()->{
                try {
                    new TransactionTemplate(manager).executeWithoutResult(status->{
                        service.vote(community,p.id(),member,new VoteRequest(p.options().getFirst().id()));
                        inserted.countDown();
                        try{release.await();}catch(InterruptedException ex){Thread.currentThread().interrupt();throw new IllegalStateException("Voto cancelado",ex);}
                    });
                }finally{finished.countDown();}
            });
            assertTrue(inserted.await(3,TimeUnit.SECONDS));assertTrue(voting.cancel(true));
            assertTrue(finished.await(3,TimeUnit.SECONDS));
            assertEquals(0,count("community_poll_votes"));assertFalse(service.close(community,p.id(),admin).open());
        }finally{release.countDown();stopWorkers(workers);}
    }

    private void awaitLockWait()throws Exception {
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(1);
        do {
            int waiting=jdbc.queryForObject("SELECT count(*) FROM pg_stat_activity WHERE datname=current_database() AND wait_event_type='Lock' AND query ILIKE '%community_polls%'",Integer.class);
            if(waiting>0)return;
            TimeUnit.MILLISECONDS.sleep(10);
        }while(System.nanoTime()<deadline);
        fail("No se observó la espera de bloqueo en PostgreSQL");
    }

    private void stopWorkers(ExecutorService workers)throws InterruptedException {
        workers.shutdownNow();assertTrue(workers.awaitTermination(3,TimeUnit.SECONDS),"Los hilos de prueba deben finalizar");
    }

    @Test void missingSocketAuthenticationIsClosed()throws Exception {
        var listener=new SocketListener();var socket=HTTP.newWebSocketBuilder().buildAsync(URI.create("ws://127.0.0.1:"+port+"/api/v1/polls/ws"),listener).get(5,TimeUnit.SECONDS);
        try {assertEquals(1008,listener.closed.get(8,TimeUnit.SECONDS));}finally{socket.abort();}
    }
}
