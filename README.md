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
./gradlew :muse-api:bootRun --args='--spring.profiles.active=dev'
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

Health와 `dev`의 Swagger 문서에 익명 접근을 허용하고 나머지는 차단합니다. 인증 API, 기본 사용자,
폼 로그인, HTTP Basic 로그인은 제공하지 않습니다. 인증 방식이 결정되면
`SecurityConfig`의 접근 규칙과 CSRF·CORS 정책을 함께 조정합니다.
현재 CSRF 보호는 유지하며 인증 쿠키를 사용하는 cross-origin 요청은 허용하지 않습니다.

## Swagger UI

`dev` 프로필로 API를 실행한 뒤 아래 주소에 접속합니다.

- Swagger UI: http://localhost:8080/swagger-ui/index.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

현재 공개된 Health API도 문서에 표시됩니다. 이후 추가하는 MVC 컨트롤러는 자동으로
문서에 반영됩니다. 기본 프로필에서는 Swagger UI와 OpenAPI 생성을 비활성화합니다.
API 요청에는 기존 보안 정책이 그대로 적용됩니다.

## 모듈 구조

| 모듈 | 역할 | 산출물 |
| --- | --- | --- |
| `muse-api` | 실행 진입점, HTTP API, 환경설정, DB 마이그레이션 | 실행 가능한 Boot jar |
| `muse-auth` | 독립 인증 서버, 보안 설정, 향후 인증 기능 | 실행 가능한 Boot jar |
| `muse-domain` | 도메인 모델, JPA 엔티티·저장소 설정 | 라이브러리 jar |

모듈 의존성은 `muse-api → muse-domain`입니다. `muse-auth`는 독립 실행 서버이며,
API가 auth의 jar나 실행 설정을 불러오지 않습니다. API와 auth는 각자의 보안 설정을 사용합니다.
`muse-common`과 `muse-global`은 아직 연결하지 않았습니다.

```text
muse-api/src/main/
├── java/muse/api/MuseApiApplication.java
└── resources/                      # application*.yml, db/migration
muse-auth/src/
├── main/
│   ├── java/muse/auth/
│   │   ├── MuseAuthApplication.java
│   │   └── config/SecurityConfig.java
│   └── resources/
│       ├── application.yml
│       └── application-dev.yml
└── test/java/muse/auth/             # 보안 및 서버 기동 테스트
muse-domain/src/main/java/muse/domain/
├── config/DomainJpaConfig.java
├── user/
├── project/
├── track/
├── sample/
└── share/

muse-domain/src/test/java/muse/domain/         # 실행할 PostgreSQL 저장·조회 테스트
muse-domain/src/testFixtures/java/muse/domain/ # 재사용할 테스트 엔티티·저장소
```

세 모듈 모두 Java 21과 Gradle 표준 소스 디렉터리 구조를 사용합니다.
API는 `muse.api`·`muse.domain`을, auth 서버는 `muse.auth`를 스캔합니다.
`DomainJpaConfig`는 `muse.domain` 아래의 엔티티와 Spring Data 저장소를 탐색합니다.
도메인 패키지는 경계만 선언했으며, 로그인·JWT·엔티티·CRUD는 아직 구현하지 않았습니다.

루트에서 `:muse-api:bootRun`을 실행하면 루트 `.env`를 읽습니다.
루트 `build`와 `clean`은 세 모듈 모두에 적용됩니다. API와 auth는 각각
실행 클래스와 프로필 설정을 갖습니다. domain은 서버가 아닌 JPA 공용 라이브러리이므로
실행 클래스나 `application.yml`을 두지 않습니다. 테스트 전용 엔티티는 배포 jar에 포함되지 않습니다.

domain은 `java-test-fixtures` 플러그인을 사용합니다. 보조 코드는 `src/testFixtures`,
실행할 테스트는 `src/test`에 둡니다. domain 자체 테스트는 fixtures를 자동으로 사용합니다.
다른 모듈에서 필요하면 `testImplementation(testFixtures(project(':muse-domain')))`으로
테스트 의존성에만 추가합니다.

API와 auth도 `java-test-fixtures`를 사용합니다. API의 `src/testFixtures`에는
Spring이 수명을 관리하는 PostgreSQL 컨테이너 설정(`ApiPostgresFixture`), auth에는
테스트 사용자 생성 코드(`AuthFixtures`)가 있습니다. 세 모듈 모두 실행할 테스트는
`src/test`에 유지하고, fixtures는 별도 test-fixtures jar로 생성하여 배포 jar에서 제외합니다.

### auth 서버 실행

```bash
./gradlew :muse-auth:bootRun --args='--spring.profiles.active=dev'
```

기본 포트는 `8081`이며 `AUTH_SERVER_PORT`로 변경할 수 있습니다. 루트 `.env`는
`dev`에서 읽고, API의 `SERVER_PORT`는 auth 포트에 영향을 주지 않습니다.
Health는 http://localhost:8081/api/health, 개발용 Swagger는
http://localhost:8081/swagger-ui/index.html 입니다.

auth는 현재 DB를 사용하지 않으며 Health·보안 정책·개발용 API 문서만 준비되어 있습니다.
로그인·회원가입·JWT 발급 및 API와의 인증 연동은 아직 구현하지 않았습니다.

## DB 변경 및 오류 응답

스키마 변경은 `muse-api/src/main/resources/db/migration/V{번호}__{설명}.sql`에 추가합니다.
적용된 migration을 수정하지 말고 새 버전을 추가하세요. 초기 V1은 도메인 테이블
없이 Flyway 이력을 시작합니다. Hibernate는 기본 프로필에서 `ddl-auto: validate`,
`dev`에서 `ddl-auto: update`로 동작합니다. 개발 중 Hibernate가 자동 반영한 스키마 변경도
공유·배포 환경에 적용하려면 Flyway 마이그레이션으로 작성해야 합니다.

인증·접근 권한 오류는 각 서버의 Security filter에서
`ProblemDetail`(`application/problem+json`) 형식의 401/403으로 응답합니다.

## 검증

```bash
./gradlew :muse-api:test          # API 웹/보안 테스트
./gradlew :muse-auth:test         # auth 보안 테스트
./gradlew :muse-auth:integrationTest # 실제 HTTP 서버 기동·Health·Swagger 테스트
./gradlew :muse-domain:integrationTest # Docker 필수: 엔티티·저장소 탐색과 실제 PostgreSQL 저장·조회
./gradlew build         # 전체 모듈 빌드와 기본 테스트, API bootJar 생성
./gradlew :muse-api:integrationTest  # Docker 필수: 실제 PostgreSQL, Flyway, JPA 초기화, Health 검증
./gradlew :muse-api:jacocoTestReport # Docker 필수: 기본·통합 테스트 실행 및 합산 커버리지 생성
```

`build`에는 Docker 통합 테스트가 포함되지 않습니다. DB 관련 변경 시
`integrationTest`도 실행하세요. Docker가 없으면 통합 테스트는 실패하며
자동 건너뛰지 않습니다. Testcontainers는 별도 임시 DB를 사용합니다.
리포트는 `muse-api/build/reports/tests/test/index.html`과
`muse-api/build/reports/tests/integrationTest/index.html`에 생성됩니다.

JaCoCo HTML 리포트는 `muse-api/build/reports/jacoco/test/html/index.html`, XML 리포트는
`muse-api/build/reports/jacoco/test/jacocoTestReport.xml`에 생성됩니다.
`jacocoTestReport`는 세 모듈의 기본·통합 테스트를 실행하고 결과를 합산합니다.
각 모듈의 테스트 결과는 해당 모듈의 `build/reports/tests/`에 생성됩니다.

## CI

GitHub Actions는 `main`·`develop` push, 모든 PR 및 수동 실행을 지원합니다.

- `Java Build`: Java 21에서 `clean build`로 전체 모듈 빌드와 기본 테스트를 검증합니다.
- `Test Coverage`: `clean :muse-api:jacocoTestReport`로 세 모듈의 기본·통합 테스트를 실행하고
  커버리지를 합산합니다. API와 domain 통합 테스트에는 PostgreSQL Testcontainers를 사용합니다.

테스트 결과와 HTML/XML 커버리지 리포트는 Actions의 Artifacts에서 다운로드할 수
있으며 14일간 보관합니다. 커버리지 최소 비율에 따른 실패 기준은 설정하지 않았습니다.
별도 DB 서비스나 저장소 secrets는 필요하지 않으며, 통합 테스트의 임시 PostgreSQL은
GitHub 호스팅 Ubuntu 러너의 Docker에서 실행됩니다.

## 빌드 결과 실행

```bash
java -jar muse-api/build/libs/muse-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

개발용 jar 실행도 `.env`가 있는 작업 디렉터리 또는 필수 환경변수가 필요합니다.
배포 시에는 `--spring.profiles.active=dev`를 제외하고 필수 환경변수를 주입합니다.
