"""Prepara infraestructura temporal y ejecuta únicamente JUnit de communities-service."""
from pathlib import Path
import base64
import os
import secrets
import shutil
import socket
import subprocess
import sys
import tempfile
import time
import uuid

service = Path(__file__).resolve().parents[1]
pg = Path('/Applications/Postgres.app/Contents/Versions/latest/bin')
if not (pg / 'initdb').exists():
    binary = shutil.which('initdb')
    if not binary:
        raise SystemExit('Se necesita PostgreSQL con initdb y pg_ctl en PATH.')
    pg = Path(binary).parent
with socket.socket() as sock:
    sock.bind(('127.0.0.1', 0))
    port = sock.getsockname()[1]
temp = Path(tempfile.mkdtemp(prefix='musa-communities-junit-', dir='/private/tmp'))
password = secrets.token_urlsafe(32)
broker_password = secrets.token_hex(24)
pw = temp / 'password'
pw.write_text(password)
pw.chmod(0o600)
container = 'musa-communities-junit-' + uuid.uuid4().hex[:12]
env = os.environ.copy()
env.update(PGPASSWORD=password, RABBITMQ_DEFAULT_USER='integration',
           RABBITMQ_DEFAULT_PASS=broker_password, RABBITMQ_DEFAULT_VHOST='musa')
pg_started = False
docker_started = False
try:
    subprocess.run([str(pg/'initdb'), '-D', str(temp/'data'), '-U', 'integration_owner',
                    '-A', 'scram-sha-256', '--pwfile', str(pw)],
                   check=True, stdout=subprocess.DEVNULL)
    subprocess.run([str(pg/'pg_ctl'), '-D', str(temp/'data'), '-l', str(temp/'postgres.log'),
                    '-o', f'-p {port} -h 127.0.0.1 -k {temp}', '-w', 'start'],
                   check=True, stdout=subprocess.DEVNULL)
    pg_started = True
    args = ['-h', '127.0.0.1', '-p', str(port), '-U', 'integration_owner']
    subprocess.run([str(pg/'createdb'), *args, 'communities_db'], env=env, check=True)
    scripts = [service/'database/schema.sql', *sorted((service/'database/migrations').glob('*.sql'))]
    for script in scripts:
        subprocess.run([str(pg/'psql'), *args, '-d', 'communities_db',
                        '-v', 'ON_ERROR_STOP=1', '-f', str(script)],
                       env=env, check=True, stdout=subprocess.DEVNULL)
    print('Preparando PostgreSQL y RabbitMQ exclusivos de JUnit de comunidades...', flush=True)
    subprocess.run(['docker', 'run', '--pull=never', '-d', '--name', container,
                    '-p', '127.0.0.1::5672', '--tmpfs', '/var/lib/rabbitmq:rw,mode=0777',
                    '-e', 'RABBITMQ_DEFAULT_USER', '-e', 'RABBITMQ_DEFAULT_PASS',
                    '-e', 'RABBITMQ_DEFAULT_VHOST',
                    '-e', 'RABBITMQ_SERVER_ADDITIONAL_ERL_ARGS=+S 2:2 +SDcpu 1 +SDio 1',
                    '-e', 'RABBITMQ_CTL_ERL_ARGS=+S 1:1 +SDcpu 1 +SDio 1',
                    'rabbitmq:4-management'], env=env, check=True, stdout=subprocess.DEVNULL)
    docker_started = True
    for attempt in range(60):
        check = subprocess.run(['docker', 'exec', '--user', 'rabbitmq', container,
                                'rabbitmq-diagnostics', '-q', 'check_running'],
                               stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        if check.returncode == 0:
            break
        time.sleep(1)
    else:
        raise RuntimeError('RabbitMQ de prueba no inició a tiempo.')
    broker_port = subprocess.check_output(['docker', 'port', container, '5672/tcp'],
                                          text=True).strip().rsplit(':', 1)[1]
    url = f'jdbc:postgresql://127.0.0.1:{port}/communities_db'
    env.update(POLLS_TEST_ISOLATED='true', POLLS_TEST_DB_URL=url,
               POLLS_TEST_DB_PASSWORD=password, POLLS_TEST_RABBIT_PORT=broker_port,
               POLLS_TEST_RABBIT_PASSWORD=broker_password, DB_PASSWORD=password,
               JWT_SECRET=base64.b64encode(bytes(64)).decode(),
               SPRING_DATASOURCE_URL=url, SPRING_DATASOURCE_USERNAME='integration_owner',
               SPRING_DATASOURCE_PASSWORD=password, SPRING_JPA_HIBERNATE_DDL_AUTO='validate',
               SPRING_JPA_SHOW_SQL='false', SPRING_RABBITMQ_HOST='127.0.0.1',
               SPRING_RABBITMQ_PORT=broker_port, SPRING_RABBITMQ_USERNAME='integration',
               SPRING_RABBITMQ_PASSWORD=broker_password, SPRING_RABBITMQ_VIRTUAL_HOST='musa',
               POLL_EVENTS_ENABLED='false')
    subprocess.run(['mvn', '-f', str(service/'pom.xml'), *sys.argv[1:], 'test'],
                   env=env, check=True)
finally:
    try:
        if docker_started:
            subprocess.run(['docker', 'stop', '-t', '5', container],
                           stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            subprocess.run(['docker', 'rm', '-v', container],
                           stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    finally:
        if pg_started:
            subprocess.run([str(pg/'pg_ctl'), '-D', str(temp/'data'), '-m', 'fast', '-w', 'stop'],
                           stdout=subprocess.DEVNULL, check=True)
        shutil.rmtree(temp)
