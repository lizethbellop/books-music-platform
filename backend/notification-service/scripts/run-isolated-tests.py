"""Aislamiento completo: cluster PG y contenedor RabbitMQ nuevos, sin leer .env."""
from pathlib import Path
import os,secrets,shutil,socket,subprocess,tempfile,time,uuid
root=Path(__file__).resolve().parents[3]
pg=Path('/Applications/Postgres.app/Contents/Versions/latest/bin')
if not (pg/'initdb').exists():
    binary=shutil.which('initdb')
    if not binary:raise SystemExit('Se necesita PostgreSQL (initdb/pg_ctl) en PATH.')
    pg=Path(binary).parent
with socket.socket() as sock:sock.bind(('127.0.0.1',0));port=sock.getsockname()[1]
temp=Path(tempfile.mkdtemp(prefix='musa-notifications-test-',dir='/private/tmp'))
password=secrets.token_urlsafe(32);broker_password=secrets.token_hex(24)
pw=temp/'password';pw.write_text(password);pw.chmod(0o600)
container='musa-notifications-test-'+uuid.uuid4().hex[:12]
env=os.environ.copy();env.update(PGPASSWORD=password,RABBITMQ_DEFAULT_USER='integration',RABBITMQ_DEFAULT_PASS=broker_password,RABBITMQ_DEFAULT_VHOST='musa')
pg_started=False;docker_started=False
try:
    subprocess.run([str(pg/'initdb'),'-D',str(temp/'data'),'-U','integration_owner','-A','scram-sha-256','--pwfile',str(pw)],check=True,stdout=subprocess.DEVNULL)
    subprocess.run([str(pg/'pg_ctl'),'-D',str(temp/'data'),'-l',str(temp/'postgres.log'),'-o',f'-p {port} -h 127.0.0.1 -k {temp}','-w','start'],check=True,stdout=subprocess.DEVNULL);pg_started=True
    args=['-h','127.0.0.1','-p',str(port),'-U','integration_owner']
    for db in ['notifications_db','chatdb','social_db']:
        subprocess.run([str(pg/'createdb'),*args,db],env=env,check=True)
    def sql(db,script=None,command=None):
        params=['-f',str(script)] if script else ['-c',command]
        subprocess.run([str(pg/'psql'),*args,'-d',db,'-v','ON_ERROR_STOP=1',*params],env=env,check=True,stdout=subprocess.DEVNULL)
    sql('notifications_db',root/'backend/notification-service/database/schema.sql')
    sql('chatdb',root/'backend/chat-service/database/schema.sql')
    sql('chatdb',root/'backend/chat-service/database/migrations/002_groups.sql')
    sql('notifications_db',root/'backend/notification-service/database/migrations/002_group_notifications.sql')
    sql('chatdb',root/'backend/chat-service/database/migrations/001_notification_outbox.sql')
    sql('social_db',root/'backend/social-service/database/migrations/001_notification_outbox.sql')
    sql('social_db',command='CREATE TABLE follows (id UUID PRIMARY KEY, follower_id UUID NOT NULL, followed_id UUID NOT NULL, created_at TIMESTAMPTZ NOT NULL, CONSTRAINT uq_follow UNIQUE(follower_id, followed_id));')
    print('Iniciando RabbitMQ aislado...',flush=True)
    subprocess.run(['docker','run','--pull=never','-d','--name',container,'-p','127.0.0.1::5672','--tmpfs','/var/lib/rabbitmq:rw,mode=0777','-e','RABBITMQ_DEFAULT_USER','-e','RABBITMQ_DEFAULT_PASS','-e','RABBITMQ_DEFAULT_VHOST','-e','RABBITMQ_SERVER_ADDITIONAL_ERL_ARGS=+S 2:2 +SDcpu 1 +SDio 1','-e','RABBITMQ_CTL_ERL_ARGS=+S 1:1 +SDcpu 1 +SDio 1','rabbitmq:4-management'],env=env,check=True,stdout=subprocess.DEVNULL);docker_started=True
    ready=False
    for _ in range(60):
        result=subprocess.run(['docker','exec','--user','rabbitmq',container,'rabbitmq-diagnostics','-q','check_running'],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
        if result.returncode==0:ready=True;break
        state = subprocess.check_output(['docker','inspect','--format','{{.State.Running}}',container],text=True).strip()
        if state != 'true': break
        time.sleep(1)
    if not ready:
        subprocess.run(['docker','logs','--tail','25',container])
        raise RuntimeError('RabbitMQ de prueba no inició a tiempo.')
    broker_port=subprocess.check_output(['docker','port',container,'5672/tcp'],text=True).strip().rsplit(':',1)[1]
    subprocess.run(['docker','exec','--user','rabbitmq',container,'rabbitmqctl','set_policy','-p','musa','--apply-to','queues','--priority','10','notifications-dlx',r'^musa\.notifications$','{"dead-letter-exchange":"musa.notifications.dlx","dead-letter-routing-key":"notifications.failed"}'],check=True,stdout=subprocess.DEVNULL)
    print('PostgreSQL y RabbitMQ aislados listos.',flush=True)
    env.update(NOTIFICATIONS_TEST_ISOLATED='true',NOTIFICATIONS_TEST_DB_URL=f'jdbc:postgresql://127.0.0.1:{port}/notifications_db',
        NOTIFICATIONS_TEST_DB_PASSWORD=password,NOTIFICATIONS_TEST_RABBIT_PORT=broker_port,NOTIFICATIONS_TEST_RABBIT_PASSWORD=broker_password,
        CHAT_TEST_ISOLATED='true',CHAT_TEST_DB_USERNAME='integration_owner',CHAT_TEST_DB_URL=f'jdbc:postgresql://127.0.0.1:{port}/chatdb',CHAT_TEST_DB_PASSWORD=password,CHAT_OUTBOX_TEST_DB_URL=f'jdbc:postgresql://127.0.0.1:{port}/chatdb',SOCIAL_OUTBOX_TEST_DB_URL=f'jdbc:postgresql://127.0.0.1:{port}/social_db')
    for service,tests,db in [('chat-service','*BlockTest,NotificationOutboxTest,ChatOutboxIntegrationTest,GroupChatFlowIntegrationTest,ChatFlowIntegrationTest','chatdb'),('social-service','NotificationOutboxTest,SocialOutboxIntegrationTest','social_db'),('notification-service','NotificationFlowIntegrationTest','notifications_db')]:
        env['OUTBOX_TEST_DB_URL']=f'jdbc:postgresql://127.0.0.1:{port}/{db}'
        subprocess.run(['mvn','-f',str(root/'backend'/service/'pom.xml'),'-Dtest='+tests,'test'],env=env,check=True)
finally:
    if docker_started:
        subprocess.run(['docker','stop','-t','5',container],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
        subprocess.run(['docker','rm','-v',container],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
    if pg_started:subprocess.run([str(pg/'pg_ctl'),'-D',str(temp/'data'),'-m','fast','-w','stop'],stdout=subprocess.DEVNULL,check=True)
    shutil.rmtree(temp)
