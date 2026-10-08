#!/usr/bin/env python3
"""Prepare ignored settings with one shared local JWT key, preserving credentials."""
from pathlib import Path
import base64
import binascii
import re
import secrets


def setup(root):
    services = ('users', 'music', 'books', 'profile')
    settings = []
    existing_keys = set()
    for service in services:
        folder = root / 'backend' / f'{service}-service'
        destination = folder / '.env'
        exists = destination.exists()
        content = (destination if exists else folder / '.env.example').read_text()
        match = re.search(r'^JWT_SECRET=(.*)$', content, re.MULTILINE)
        value = match.group(1).strip() if match else ''
        if exists and value:
            existing_keys.add(value)
        settings.append((service, destination, content, exists, value))

    # Check before writing anything; never replace conflicting private keys.
    if len(existing_keys) > 1:
        raise SystemExit('JWT_SECRET difiere entre servicios. Unifica las claves locales antes de continuar; no se modificó ningún archivo.')
    key = next(iter(existing_keys), base64.b64encode(secrets.token_bytes(64)).decode())
    try:
        if len(base64.b64decode(key, validate=True)) < 64:
            raise ValueError()
    except (ValueError, binascii.Error):
        raise SystemExit('JWT_SECRET debe ser Base64 de al menos 64 bytes; no se modificó ningún archivo.')

    for service, destination, content, exists, value in settings:
        if exists and value:
            print(f'{service}: .env existente conservado')
            continue
        if re.search(r'^JWT_SECRET=.*$', content, re.MULTILINE):
            content = re.sub(r'^JWT_SECRET=.*$', lambda _: 'JWT_SECRET=' + key, content, flags=re.MULTILINE)
        else:
            content = content.rstrip() + '\nJWT_SECRET=' + key + '\n'
        destination.write_text(content)
        destination.chmod(0o600)
        print(f'{service}: JWT local compartido configurado; completa las demás credenciales si faltan')


if __name__ == '__main__':
    setup(Path(__file__).resolve().parents[1])
