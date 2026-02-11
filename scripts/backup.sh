#!/bin/bash

# 1. 환경 설정
# 맥북의 실제 사용자 경로를 기반으로 백업 폴더를 지정합니다.
BACKUP_DIR="/Users/$(whoami)/backups/db"
DATE=$(date +%Y%m%d_%H%M%S)
ENV="prod" 
CONTAINER_NAME="unlock-db-$ENV"

# 2. 백업 디렉토리 생성
mkdir -p $BACKUP_DIR

# 3. DB 백업 실행 (SQL Export)
# [보안 강화]: 도커 내부의 환경변수($POSTGRES_USER, $POSTGRES_DB)를 직접 활용하여 
# 스크립트 내에 비밀번호나 계정명을 남기지 않습니다.
echo "[$DATE] DB 백업을 시작합니다... (Target: $CONTAINER_NAME)"

docker exec $CONTAINER_NAME sh -c 'pg_dump -U $POSTGRES_USER $POSTGRES_DB' > $BACKUP_DIR/backup-$DATE.sql

# 4. 7일 지난 백업 파일 자동 삭제
find $BACKUP_DIR -type f -name "backup-*.sql" -mtime +6 -exec rm -f {} \;

echo "[$DATE] 백업 및 오래된 파일 정리가 완료되었습니다."