#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd -- "$(dirname -- "$0")" && pwd)"
docker compose --env-file "$SCRIPT_DIR/.env.rabbitmq" -f "$SCRIPT_DIR/compose.rabbitmq.yml"   exec -T --user rabbitmq rabbitmq rabbitmqctl set_policy -p musa --apply-to queues --priority 10   notifications-dlx '^musa\.notifications$'   '{"dead-letter-exchange":"musa.notifications.dlx","dead-letter-routing-key":"notifications.failed"}'
