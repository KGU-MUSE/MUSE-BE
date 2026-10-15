# MUSE Backend

일상에서 녹음한 소리로 음악을 제작하는 MUSE의 백엔드입니다.

Java 21, Spring Boot 3.5.16, Gradle 8.14.3, Spring Data JPA, PostgreSQL 17,
Flyway, Spring Security를 사용합니다. Java 21 JDK와 Docker Compose v2가 필요합니다.
Java toolchain을 21로 고정했으며 JDK 자동 다운로드는 설정하지 않았습니다.

## 로컬 실행

`MUSE-BE` 디렉터리에서 실행합니다.

```bash
cp .env.example .env
# .env의 DB_PASSWORD를 로컬 비밀번호로 변경
docker compose up -d --wait
./gradlew bootRun --args='--spring.profiles.active=dev'
```

macOS에서 필요한 경우 `export JAVA_HOME=$(/usr/libexec/java_home -v 21)`로
JDK 21을 선택합니다. Spring Boot는 `dev` 프로필에서 `.env`를 읽고,
Docker Compose는 프로필과 관계없이 `.env`를 읽습니다.
Spring Boot는 `spring.config.import`로 properties 형식으로 읽기 때문에
`KEY=value` 형식을 사용하고 값에 따옴표를 붙이지 마세요.
공통 설정은 `application.yml`, 개발 전용 설정은 `application-dev.yml`에 둡니다.
`dev`는 자동 활성화하지 않습니다. 실행 인자 대신 `SPRING_PROFILES_ACTIVE=dev`
환경변수로 활성화할 수도 있습니다. 배포 환경에서는 `dev`를 활성화하지 않고
`.env` 대신 프로세스 환경변수를 주입합니다.
환경변수는 `.env`보다 우선합니다. 기본 프로필은 DB 호스트·이름·사용자·비밀번호를
환경변수로 받습니다. `dev`는 호스트 `localhost`, DB 이름·사용자 `muse`를 기본값으로
사용하고 비밀번호는 필수입니다. Docker Compose의 DB 이름·사용자·비밀번호는
프로필과 관계없이 `.env`에 지정해야 합니다.

`dev`에서는 Hibernate SQL을 DEBUG 레벨로 출력하고 포맷팅합니다.
바인딩된 파라미터 값은 로깅하지 않습니다. 기본 프로필은 SQL 디버그 로그를 켜지 않습니다.

| 변수 | 용도 | 기본값 |
| --- | --- | --- |
| `DB_HOST` | 앱에서 접근할 PostgreSQL 호스트 | 필수 (`dev`: `localhost`) |
| `DB_PORT` | PostgreSQL 호스트 포트 | `5432` |
| `DB_NAME` | 데이터베이스 이름 | 필수 (`dev`: `muse`) |
| `DB_USERNAME` | 데이터베이스 사용자 | 필수 (`dev`: `muse`) |
| `DB_PASSWORD` | 데이터베이스 비밀번호 | 필수 |
| `SERVER_PORT` | HTTP 포트 | `8080` |
| `CORS_ALLOWED_ORIGINS` | 허용 프론트엔드 origin, 쉼표 구분 | `http://localhost:5173` |

DB 포트는 호스트의 `127.0.0.1`에만 공개하며 데이터는 named volume에 보존합니다.
5432 포트를 이미 사용 중이면 `.env`의 `DB_PORT`를 사용 가능한 포트로 변경하세요.
`docker compose stop`으로 중지하고 `docker compose down`으로 컨테이너를 제거합니다.
`docker compose down -v`는 DB 데이터를 삭제하므로 주의하세요.
기존 volume의 DB 이름·계정·비밀번호는 `.env`를 변경해도 자동 변경되지 않습니다.

## Health Check

```bash
curl -i http://localhost:8080/api/health
```

Spring Boot Actuator의 health endpoint로 프로세스와 DB 연결을 확인합니다.
정상이면 HTTP 200과 `{"status":"UP"}`, 실행 중 DB 장애가 발생하면 HTTP 503과
`{"status":"DOWN"}`을 반환합니다. DB 정보 등 상세 내역은 공개하지 않습니다.
DB가 처음부터 연결되지 않으면 Flyway 초기화 실패로 애플리케이션 기동도 실패합니다.

Health만 익명 접근을 허용하고 나머지는 차단합니다. 인증 API, 기본 사용자,
폼 로그인, HTTP Basic 로그인은 제공하지 않습니다. 인증 방식이 결정되면
`SecurityConfig`의 접근 규칙과 CSRF·CORS 정책을 함께 조정합니다.
현재 CSRF 보호는 유지하며 인증 쿠키를 사용하는 cross-origin 요청은 허용하지 않습니다.

## 패키지 구조

```text
com.muse
├── MuseApplication
├── domain
│   ├── user       # 사용자/인증
│   ├── project    # 음악 프로젝트
│   ├── track      # 트랙
│   ├── sample     # 오디오 샘플
│   └── share      # 음원 공유
└── global
    ├── config     # 보안 및 공통 설정
    └── exception  # 공통 예외 처리
```

각 도메인은 `package-info.java`로 경계만 선언했습니다. 기능을 구현할 때 해당
도메인 안에 controller/service/repository/entity/dto를 필요한 만큼 추가합니다.
엔티티·CRUD·JWT·파일 저장소 등 아직 요구하지 않은 기능은 만들지 않았습니다.

## DB 변경 및 오류 응답

스키마 변경은 `src/main/resources/db/migration/V{번호}__{설명}.sql`에 추가합니다.
적용된 migration을 수정하지 말고 새 버전을 추가하세요. 초기 V1은 도메인 테이블
없이 Flyway 이력을 시작합니다. Hibernate는 기본 프로필에서 `ddl-auto: validate`,
`dev`에서 `ddl-auto: update`로 동작합니다. 개발 중 Hibernate가 자동 반영한 스키마 변경도
공유·배포 환경에 적용하려면 Flyway 마이그레이션으로 작성해야 합니다.

오류는 Spring의 `ProblemDetail`(`application/problem+json`) 형식입니다.
공통 advice는 요청 검증·잘못된 JSON·HTTP 예외의 상태를 유지하고,
예상하지 못한 예외는 서버에 기록한 뒤 상세 내용을 숨긴 500으로 응답합니다.
Security filter의 401/403도 같은 형식을 사용합니다.

## 검증

```bash
./gradlew test          # Docker 없이 웹/보안/공통 예외 테스트
./gradlew build         # 기본 테스트 및 실행 가능한 bootJar 생성
./gradlew integrationTest  # Docker 필수: 실제 PostgreSQL, Flyway, JPA 초기화, Health 검증
./gradlew jacocoTestReport # Docker 필수: 기본·통합 테스트 실행 및 합산 커버리지 생성
```

`build`에는 Docker 통합 테스트가 포함되지 않습니다. DB 관련 변경 시
`integrationTest`도 실행하세요. Docker가 없으면 통합 테스트는 실패하며
자동 건너뛰지 않습니다. Testcontainers는 별도 임시 DB를 사용합니다.
리포트는 `build/reports/tests/test/index.html`과
`build/reports/tests/integrationTest/index.html`에 생성됩니다.

JaCoCo HTML 리포트는 `build/reports/jacoco/test/html/index.html`, XML 리포트는
`build/reports/jacoco/test/jacocoTestReport.xml`에 생성됩니다.

## CI

GitHub Actions는 `main`·`develop` push, 모든 PR 및 수동 실행을 지원합니다.

- `Java Build`: Java 21에서 `clean build`로 빌드와 기본 테스트를 검증합니다.
- `Test Coverage`: `clean jacocoTestReport`로 기본 테스트와 PostgreSQL
  Testcontainers 통합 테스트를 실행하고 두 테스트의 커버리지를 합산합니다.

테스트 결과와 HTML/XML 커버리지 리포트는 Actions의 Artifacts에서 다운로드할 수
있으며 14일간 보관합니다. 커버리지 최소 비율에 따른 실패 기준은 설정하지 않았습니다.
별도 DB 서비스나 저장소 secrets는 필요하지 않으며, 통합 테스트의 임시 PostgreSQL은
GitHub 호스팅 Ubuntu 러너의 Docker에서 실행됩니다.

## 빌드 결과 실행

```bash
java -jar build/libs/muse-be-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

개발용 jar 실행도 `.env`가 있는 작업 디렉터리 또는 필수 환경변수가 필요합니다.
배포 시에는 `--spring.profiles.active=dev`를 제외하고 필수 환경변수를 주입합니다.
