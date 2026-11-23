"""Run only against a new disposable PostgreSQL cluster; never use the user's server."""
from pathlib import Path
import os, secrets, shutil, socket, subprocess, tempfile
root=Path(__file__).resolve().parents[3]
pgbin=Path('/Applications/Postgres.app/Contents/Versions/latest/bin')
if not (pgbin/'initdb').exists():
    found=shutil.which('initdb')
    if not found: raise SystemExit('Se necesita initdb/pg_ctl de PostgreSQL para esta prueba aislada.')
    pgbin=Path(found).parent
with socket.socket() as sock:
    sock.bind(('127.0.0.1',0)); port=sock.getsockname()[1]
temp=Path(tempfile.mkdtemp(prefix='musa-chat-isolated-',dir='/private/tmp'))
password=secrets.token_urlsafe(32)
pw=temp/'password';pw.write_text(password);pw.chmod(0o600)
env=os.environ.copy();env['PGPASSWORD']=password
running=False
try:
    subprocess.run([str(pgbin/'initdb'),'-D',str(temp/'data'),'-U','chat_test_owner','-A','scram-sha-256','--pwfile',str(pw)],check=True,stdout=subprocess.DEVNULL)
    subprocess.run([str(pgbin/'pg_ctl'),'-D',str(temp/'data'),'-l',str(temp/'postgres.log'),'-o',f'-p {port} -h 127.0.0.1 -k {temp}','-w','start'],check=True,stdout=subprocess.DEVNULL)
    running=True
    args=['-h','127.0.0.1','-p',str(port),'-U','chat_test_owner']
    subprocess.run([str(pgbin/'createdb'),*args,'chatdb'],env=env,check=True)
    subprocess.run([str(pgbin/'psql'),*args,'-d','chatdb','-v','ON_ERROR_STOP=1','-f',str(root/'backend/chat-service/database/schema.sql')],env=env,check=True,stdout=subprocess.DEVNULL)
    env.update(CHAT_TEST_ISOLATED='true',CHAT_TEST_DB_URL=f'jdbc:postgresql://127.0.0.1:{port}/chatdb',CHAT_TEST_DB_PASSWORD=password)
    result=subprocess.run(['mvn','-f',str(root/'backend/chat-service/pom.xml'),'-Dtest=*BlockTest,ChatFlowIntegrationTest','test'],env=env)
    raise SystemExit(result.returncode)
finally:
    if running:
        subprocess.run([str(pgbin/'pg_ctl'),'-D',str(temp/'data'),'-m','fast','-w','stop'],stdout=subprocess.DEVNULL,check=True)
    shutil.rmtree(temp)
