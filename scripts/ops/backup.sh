#!/usr/bin/env sh
set -eu

OUTPUT_DIR="${1:-./backups}"
mkdir -p "$OUTPUT_DIR"

TS="$(date +%Y%m%d-%H%M%S)"
BACKUP_FILE="$OUTPUT_DIR/novadepot-$TS.sql.gz"

echo "[backup] writing compressed dump to $BACKUP_FILE"
docker compose exec -T mysql mysqldump -uroot -proot --single-transaction --set-gtid-purged=OFF novadepot | gzip > "$BACKUP_FILE"

if [ ! -s "$BACKUP_FILE" ]; then
  echo "[backup] failed: backup file is empty" >&2
  exit 1
fi

find "$OUTPUT_DIR" -name 'novadepot-*.sql.gz' -type f -mtime +35 -delete
echo "[backup] done: $BACKUP_FILE"
