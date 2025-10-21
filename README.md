# HR Service

팀 관리 및 인사 관련 기능을 담당하는 마이크로서비스입니다.

## 기능

- 팀 생성 및 관리
- 팀장 권한 관리
- 팀 멤버 관리
- 연차 수 설정
- 팀 설명 관리

## 설정

### 1. 데이터베이스 생성

```sql
CREATE DATABASE coresync_hr_db;
```

### 2. 환경 변수 설정

`.env` 파일을 생성하고 다음 내용을 설정하세요:

```bash
SERVER_PORT=8003
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/coresync_hr_db?allowpublickeyretrieval=true&usessl=false&serverTimezone=UTC
SPRING_DATASOURCE_USERNAME=your_username
SPRING_DATASOURCE_PASSWORD=your_password
SPRING_DATA_REDIS_HOST=127.0.0.1
SPRING_DATA_REDIS_PORT=6379
SPRING_DATA_REDIS_PASSWORD=your_redis_password
```

### 3. 실행

```bash
./gradlew bootRun
```

## API 엔드포인트

### 팀 관리

- `POST /api/team/create` - 팀 생성
- `GET /api/team/list` - 팀 목록 조회
- `GET /api/team/{teamId}` - 팀 상세 조회
- `PUT /api/team/{teamId}` - 팀 정보 수정 (팀장만)
- `DELETE /api/team/{teamId}` - 팀 삭제 (팀장만)

### 팀 멤버 관리

- `POST /api/team/{teamId}/invite` - 팀 멤버 초대
- `DELETE /api/team/{teamId}/member/{memberId}` - 팀 멤버 제거 (팀장만)
- `GET /api/team/{teamId}/members` - 팀 멤버 목록 조회

### 권한 확인

- `GET /api/team/{teamId}/is-leader` - 팀장 여부 확인
- `GET /api/team/{teamId}/validate-member` - 팀 멤버 여부 확인

## 아키텍처

이 서비스는 MSA 아키텍처의 일부로, agile_service와 RestTemplate을 통해 통신합니다.

```
Frontend → agile_service (8002) → hr_service (8003) → MySQL
```

## 주의사항

- 팀 생성 시 자동으로 생성자가 팀장(LEADER)으로 지정됩니다.
- 팀장만 팀 정보 수정, 팀 삭제, 멤버 제거가 가능합니다.
- Redis가 실행 중이어야 합니다 (토큰 검증용).
