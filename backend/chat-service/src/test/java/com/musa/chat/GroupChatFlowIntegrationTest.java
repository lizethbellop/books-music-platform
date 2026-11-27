package com.musa.chat;

import com.musa.chat.messaging.NotificationOutboxPublisher;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.*;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnabledIfEnvironmentVariable(named = "NOTIFICATIONS_TEST_ISOLATED", matches = "true")
class GroupChatFlowIntegrationTest {
    private static final Set<String> ACTIVE = ConcurrentHashMap.newKeySet();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static HttpServer users;
    private static ExecutorService userExecutor;
    @Value("${local.server.port}") int port;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean NotificationOutboxPublisher publisher;
    @DynamicPropertySource static void props(DynamicPropertyRegistry registry) throws Exception {
        users = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        userExecutor = Executors.newCachedThreadPool(); users.setExecutor(userExecutor);
        users.createContext("/api/v1/users/usernames", exchange -> {
            Map<String,String> result = new LinkedHashMap<>();
            for (String part : exchange.getRequestURI().getRawQuery().split("&")) {
                String id = URLDecoder.decode(part.split("=", 2)[1], StandardCharsets.UTF_8);
                if (ACTIVE.contains(id)) result.put(id, "test-user");
            }
            byte[] body = JSON.writeValueAsBytes(result); exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (var out = exchange.getResponseBody()) { out.write(body); }
        }); users.start();
        registry.add("spring.datasource.url", () -> System.getenv("CHAT_OUTBOX_TEST_DB_URL"));
        registry.add("spring.datasource.username", () -> "integration_owner");
        registry.add("spring.datasource.password", () -> System.getenv("NOTIFICATIONS_TEST_DB_PASSWORD"));
        registry.add("musa.security.jwt.secret", () -> Base64.getEncoder().encodeToString(new byte[64]));
        registry.add("musa.services.users.base-url", () -> "http://127.0.0.1:"+users.getAddress().getPort());
        registry.add("notifications.events.enabled", () -> "true");
        registry.add("spring.config.import", () -> "");
    }
    @AfterAll static void cleanup() { if(users!=null)users.stop(0); if(userExecutor!=null)userExecutor.shutdownNow(); }
    String user() { String id=UUID.randomUUID().toString(); ACTIVE.add(id); return id; }
    String token(String user) throws Exception {
        var claims = new JWTClaimsSet.Builder().subject(user).issuer("musa-auth").audience("musa-api")
                .expirationTime(Date.from(Instant.now().plusSeconds(600))).build();
        var token = new SignedJWT(new JWSHeader(JWSAlgorithm.HS512), claims);
        token.sign(new MACSigner(new byte[64])); return token.serialize();
    }
    HttpResponse<String> req(String method,String path,String user,Object body) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/v1/chat"+path))
                .timeout(Duration.ofSeconds(15)).header("Authorization","Bearer "+token(user));
        if(body==null)request.method(method,HttpRequest.BodyPublishers.noBody());
        else request.header("Content-Type","application/json").method(method,HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body)));
        return HTTP.send(request.build(),HttpResponse.BodyHandlers.ofString());
    }
    JsonNode ok(HttpResponse<String> response) { assertEquals(200,response.statusCode(),response.body());return JSON.readTree(response.body()); }
    String create(String owner,List<String> members) throws Exception {
        return ok(req("POST","/conversations/groups",owner,Map.of("name","  Lecturas  ","participantIds",members))).path("id").asString();
    }
    String messages(String group){return "/conversations/"+group+"/messages";}
    @Test void createListsMetadataAndEnforcesAdminAndMembership() throws Exception {
        String owner=user(),member=user(),other=user(),outsider=user();String group=create(owner,List.of(member,other));
        var details=ok(req("GET","/conversations/"+group,member,null));
        assertEquals("GROUP",details.path("type").asString());assertEquals("Lecturas",details.path("name").asString());
        assertEquals(owner,details.path("ownerId").asString());assertEquals(3,details.path("participantIds").size());
        assertEquals(403,req("POST","/conversations/"+group+"/participants",member,Map.of("userId",outsider)).statusCode());
        assertEquals(403,req("PATCH","/conversations/"+group+"/group",member,Map.of("name","Cambio")).statusCode());
        assertEquals(404,req("GET",messages(group),outsider,null).statusCode());
        assertEquals(404,req("POST",messages(group),outsider,Map.of("content","intruso")).statusCode());
        assertEquals(404,req("GET","/conversations/"+group,outsider,null).statusCode());
        var renamed=ok(req("PATCH","/conversations/"+group+"/group",owner,Map.of("name","Música")));
        assertEquals("Música",renamed.path("name").asString());
        assertTrue(req("GET","/conversations",member,null).body().contains(group));
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM notification_outbox WHERE routing_key='chat.group.member.added' AND payload LIKE ?",Integer.class,"%"+group+"%"));
    }
    @Test void joinRemoveAndRejoinPreserveAuthorsButRestrictHistory() throws Exception {
        String owner=user(),member=user(),newcomer=user();String group=create(owner,List.of(member));
        ok(req("POST",messages(group),member,Map.of("content","Antes de ingresar")));
        ok(req("POST","/conversations/"+group+"/participants",owner,Map.of("userId",newcomer)));
        String visible=ok(req("POST",messages(group),member,Map.of("content","Después de ingresar"))).path("id").asString();
        var history=ok(req("GET",messages(group),newcomer,null)).path("items");assertEquals(1,history.size());
        assertEquals(visible,history.get(0).path("id").asString());
        ok(req("DELETE","/conversations/"+group+"/participants/"+member,owner,null));
        assertEquals(404,req("GET",messages(group),member,null).statusCode());
        assertEquals(404,req("POST",messages(group),member,Map.of("content","No permitido")).statusCode());
        assertEquals(2,ok(req("GET",messages(group),owner,null)).path("items").size());
        ok(req("POST","/conversations/"+group+"/participants",owner,Map.of("userId",member)));
        assertEquals(0,ok(req("GET",messages(group),member,null)).path("items").size());
        ok(req("POST",messages(group),owner,Map.of("content","Nueva etapa")));
        assertEquals(1,ok(req("GET",messages(group),member,null)).path("items").size());
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM conversation_participants WHERE conversation_id=? AND user_id=?",Integer.class,UUID.fromString(group),UUID.fromString(member)));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM notification_outbox WHERE routing_key='chat.group.member.removed' AND payload LIKE ?",Integer.class,"%"+group+"%"));
    }
    @Test void transferLeaveAndEmptyGroupClosure() throws Exception {
        String owner=user(),member=user();String group=create(owner,List.of(member));
        ok(req("POST","/conversations/"+group+"/admin",owner,Map.of("userId",member)));
        assertEquals(403,req("PATCH","/conversations/"+group+"/group",owner,Map.of("name","No")).statusCode());
        // Sale el administrador; la administración pasa al otro miembro disponible.
        assertEquals(204,req("POST","/conversations/"+group+"/leave",member,null).statusCode());
        assertEquals(owner,ok(req("GET","/conversations/"+group,owner,null)).path("ownerId").asString());
        assertEquals(204,req("POST","/conversations/"+group+"/leave",owner,null).statusCode());
        assertEquals(404,req("GET","/conversations/"+group,owner,null).statusCode());
        assertNotNull(jdbc.queryForObject("SELECT closed_at FROM conversations WHERE id=?",Object.class,UUID.fromString(group)));
        assertFalse(req("GET","/conversations",owner,null).body().contains(group));
    }
    @Test void simultaneousAddsCannotExceedFifty() throws Exception {
        String owner=user();List<String> members=new ArrayList<>();for(int i=0;i<48;i++)members.add(user());
        String group=create(owner,members),one=user(),two=user();var pool=Executors.newFixedThreadPool(2);
        try{
            var a=pool.submit(()->req("POST","/conversations/"+group+"/participants",owner,Map.of("userId",one)).statusCode());
            var b=pool.submit(()->req("POST","/conversations/"+group+"/participants",owner,Map.of("userId",two)).statusCode());
            assertEquals(Set.of(200,400),Set.of(a.get(15,TimeUnit.SECONDS),b.get(15,TimeUnit.SECONDS)));
            assertEquals(50,ok(req("GET","/conversations/"+group,owner,null)).path("participantIds").size());
        }finally{pool.shutdownNow();}
    }
    @Test void inactiveRecipientDoesNotBlockGroupAndGetsNoMessageEvent() throws Exception {
        String owner=user(),active=user(),inactive=user();String group=create(owner,List.of(active,inactive));ACTIVE.remove(inactive);
        ok(req("POST",messages(group),owner,Map.of("content","Para participantes activos")));
        var payloads=jdbc.queryForList("SELECT payload FROM notification_outbox WHERE routing_key='chat.message.created' AND payload LIKE ?",String.class,"%"+group+"%");
        assertEquals(1,payloads.size());assertEquals(active,JSON.readTree(payloads.getFirst()).path("recipientId").asString());
    }
    @Test void rejectsInvalidGroupsAndPrivateMembershipOperations() throws Exception {
        String owner=user(),member=user();
        assertEquals(400,req("POST","/conversations/groups",owner,Map.of("name"," ","participantIds",List.of(member))).statusCode());
        assertEquals(400,req("POST","/conversations/groups",owner,Map.of("name","Grupo","participantIds",List.of(member,member))).statusCode());
        assertEquals(400,req("POST","/conversations/groups",owner,Map.of("name","Grupo","participantIds",List.of(owner))).statusCode());
        assertEquals(404,req("POST","/conversations/groups",owner,Map.of("name","Grupo","participantIds",List.of(UUID.randomUUID().toString()))).statusCode());
        String privateId=ok(req("POST","/conversations/private",owner,Map.of("recipientId",member))).path("id").asString();
        assertEquals(400,req("POST","/conversations/"+privateId+"/leave",owner,null).statusCode());
    }
    static class Listener implements WebSocket.Listener {
        final BlockingQueue<String> frames=new LinkedBlockingQueue<>();final StringBuilder partial=new StringBuilder();
        @Override public void onOpen(WebSocket socket){socket.request(1);}
        @Override public CompletionStage<?> onText(WebSocket socket,CharSequence data,boolean last){partial.append(data);if(last){frames.add(partial.toString());partial.setLength(0);}socket.request(1);return null;}
        String next(String type,long millis)throws Exception{long deadline=System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(millis);while(System.nanoTime()<deadline){String frame=frames.poll(50,TimeUnit.MILLISECONDS);if(frame!=null&&type.equals(JSON.readTree(frame).path("type").asString()))return frame;}return null;}
    }
    WebSocket socket(String user,Listener listener)throws Exception{
        String ticket=ok(req("POST","/realtime/tickets",user,null)).path("ticket").asString();
        var socket=HTTP.newWebSocketBuilder().buildAsync(URI.create("ws://127.0.0.1:"+port+"/api/v1/chat/ws"),listener).get(5,TimeUnit.SECONDS);
        socket.sendText(JSON.writeValueAsString(Map.of("ticket",ticket)),true).join();assertNotNull(listener.next("ready",5000));return socket;
    }
    @Test void groupRealtimeExcludesOutsidersAndFutureDeliveryToRemovedMember()throws Exception{
        String owner=user(),member=user(),removed=user(),outsider=user();String group=create(owner,List.of(member,removed));
        var a=new Listener();var b=new Listener();var c=new Listener();var d=new Listener();
        var sa=socket(owner,a);var sb=socket(member,b);var sc=socket(removed,c);var sd=socket(outsider,d);
        try{
            ok(req("POST",messages(group),owner,Map.of("content","Todos")));
            assertNotNull(a.next("message.created",5000));assertNotNull(b.next("message.created",5000));assertNotNull(c.next("message.created",5000));
            assertNull(d.next("message.created",200));
            ok(req("DELETE","/conversations/"+group+"/participants/"+removed,owner,null));
            assertNotNull(c.next("conversation.updated",5000));
            ok(req("POST",messages(group),owner,Map.of("content","Solo activos")));
            assertNotNull(a.next("message.created",5000));assertNotNull(b.next("message.created",5000));assertNull(c.next("message.created",250));
        }finally{sa.abort();sb.abort();sc.abort();sd.abort();}
    }
}
