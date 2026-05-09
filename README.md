# Zoopick Server

- Redis 서비스는 로컬에서 실행 중이어야 합니다.

### 환경변수

```bash .env
# Optional ==============================
# default: jdbc:postgresql://mir.lalaalal.com:5432/zoopick
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/zoopick
# default: false
SPRING_JPA_SHOW_SQL=false

# Mandatory =============================
SPRING_DATASOURCE_USERNAME=username
SPRING_DATASOURCE_PASSWORD=password
SPRING_MAIL_USERNAME=example@example.com
SPRING_MAIL_PASSWORD=password
FIREBASE_ACCOUNT_KEY_PATH=/path/to/firebase-adminsdk.json
# 32 바이트 이상
JWT_SECRET=secret
```

### 빌드

```bash
./mvnw clean package
```

### 실행

```bash
# setup env first
cd target
java -jar zoopick-server-x.x.x.jar
```

## 데이터베이스 세팅 및 복원 가이드 (PostgreSQL)

본 프로젝트는 PostgresQL(18.3)을 사용합니다. 새로운 로컬 환경에서 서버를 띄우기 전에 아래 절차에 따라 데이터베이스를 세팅해 주세요.

### 1. DB 초기 복원 (Restore)
프로젝트에 포함된 `zoopick_dump.sql` 파일을 이용하여 스키마와 초기 시드 데이터를 한 번에 세팅할 수 있습니다. 터미널을 열고 아래 명령어를 순서대로 실행하세요.
```bash
# 1. zoopick 데이터베이스 생성
createdb -U postgres(DB 사용자계정명) zoopick 

#2. 덤프 파일을 이용하여 스키마 및 데이터 복원
psql -U postgres(DB 사용자계정명) -d zoopick -f zoopick_dump.sql
```
```
bash
pg_dump -h localhost -U postgres -f zoopick_dump.sql -d zoopick 
```
시에 인코딩 오류 발생할 수도 있으니 주의(utf- 8 기준)

### 2. Redis 서버 실행(Redis 버전: 3.0.504)
이메일 인증번호 등 휘발성 데이터 처리를 위해 Redis가 반드시 켜져 있어야 합니다.

Windows/Mac: Docker를 이용하거나 로컬에 설치된 Redis 서버를 실행해 주세요. (기본 포트: 6379)

정상 실행 확인 명령어: redis-cli ping 입력 시 PONG 응답이 와야 합니다
