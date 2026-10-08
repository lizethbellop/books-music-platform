#!/usr/bin/env python3
"""Create ignored service settings without overwriting existing credentials."""
from pathlib import Path
import base64
import secrets

root = Path(__file__).resolve().parents[1]
for service in ('users', 'music', 'books', 'profile'):
    folder = root / 'backend' / f'{service}-service'
    destination = folder / '.env'
    if destination.exists():
        print(f'{service}: .env existente conservado')
        continue
    content = (folder / '.env.example').read_text()
    if service == 'users':
        key = base64.b64encode(secrets.token_bytes(64)).decode()
        content = content.replace('JWT_SECRET=\n', f'JWT_SECRET={key}\n')
    destination.write_text(content)
    destination.chmod(0o600)
    print(f'{service}: .env creado; completa tus credenciales locales')
