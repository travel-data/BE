# 소셜 로그인 인수인계 문서

작성일: 2026-07-09  
최근 구현 반영: 2026-07-22  
대상 코드: `oiso` Spring Boot 멀티모듈 프로젝트

## 1. 현재 구현 요약

현재 소셜 로그인은 Spring Security OAuth2 Client 기반으로 카카오와 구글 로그인을 받는 구조까지 만들어져 있다. 로그인 성공 시 OAuth 제공자에서 받은 사용자 정보를 읽고, 자체 JWT access token과 refresh token을 만들어 쿠키에 넣은 뒤 프론트엔드로 리다이렉트한다.

2026-07-22에 JWT 쿠키 인증 필터, `/api/auth/me`, API 401 응답, 환경변수 기반 CORS 및 cross-site 쿠키 설정을 구현했다. 일반 REST API 경로는 `/api` 아래로 통일했으며 회원 목록은 `GET /api/users`를 사용한다. 남아 있는 주요 작업은 다음과 같다.

1. OAuth 로그인 성공 후 회원을 DB에 저장하거나 기존 회원과 매칭하는 로직이 주석 처리되어 있다.
2. JWT subject가 내부 사용자 ID가 아니라 email이다.
3. refresh token을 서버 저장소에 보관하거나 폐기하는 로직이 없다.
4. 카카오 로그인에서 이메일을 사용하려 하지만 현재 카카오 scope에는 이메일 권한이 없다.
5. `.env.example`에 실제 값처럼 보이는 OAuth client secret과 JWT secret이 들어 있다. 저장소에 올라간 값이라면 폐기하고 재발급해야 한다.

현재 API 계약은 [`docs/api.md`](./api.md)를 기준으로 한다.

## 2. 코드 위치

### 백엔드 모듈 구조

- `app`: 실행 애플리케이션 모듈
- `security`: Spring Security, OAuth2, JWT, 인증 API
- `user`: 회원 도메인, 회원 조회 API
- `common`: 공통 상수, 예외, 에러 응답

### 소셜 로그인 관련 주요 파일

- `security/src/main/java/com/nigahae/security/config/SecurityConfig.java`
  - CORS, 인증/인가 경로, OAuth2 로그인 성공 핸들러 연결
- `security/src/main/java/com/nigahae/security/oauth/OAuth2LoginSuccessHandler.java`
  - OAuth 로그인 성공 후 provider별 사용자 정보 파싱, JWT 생성, 쿠키 발급, 프론트 리다이렉트
- `security/src/main/java/com/nigahae/security/oauth/OAuth2UserService.java`
  - provider user-info API 호출 후 `DefaultOAuth2User` 반환
- `security/src/main/java/com/nigahae/security/jwt/JwtTokenProvider.java`
  - JWT 생성, 검증, claim 추출
- `security/src/main/java/com/nigahae/security/controller/AuthController.java`
  - access token 재발급, 로그아웃 API
- `security/src/main/java/com/nigahae/security/util/CookieUtil.java`
  - 쿠키 생성 및 조회 유틸
- `user/src/main/java/com/nigahae/user/domain/User.java`
  - 현재 회원 엔티티
- `user/src/main/java/com/nigahae/user/service/UserService.java`
  - 회원 조회 서비스, 로그인/회원가입 로직 주석 상태
- `user/src/main/java/com/nigahae/user/repository/UserRepository.java`
  - 현재 `JpaRepository<User, Long>`만 있음
- `app/src/main/resources/application.properties`
  - OAuth2/JWT 설정 placeholder
- `.env.example`
  - 로컬 환경변수 예시

## 3. 현재 로그인 흐름

### 3.1 프론트엔드가 로그인 시작

프론트엔드는 백엔드의 Spring Security 기본 OAuth2 authorization endpoint로 브라우저를 이동시켜야 한다.

```text
GET {BACKEND_BASE_URL}/oauth2/authorization/kakao
GET {BACKEND_BASE_URL}/oauth2/authorization/google
```

예시:

```text
http://localhost:8080/oauth2/authorization/kakao
http://localhost:8080/oauth2/authorization/google
```

이 요청은 AJAX가 아니라 `window.location.href` 또는 `<a href>`처럼 브라우저 전체 이동으로 처리해야 한다.

### 3.2 OAuth 제공자 콜백

카카오/구글 개발자 콘솔에는 백엔드 콜백 URL을 등록해야 한다.

```text
{BACKEND_BASE_URL}/login/oauth2/code/kakao
{BACKEND_BASE_URL}/login/oauth2/code/google
```

현재 `.env.example`에는 다음 형태로 잡혀 있다.

```text
KAKAO_REDIRECT_URI={baseUrl}/login/oauth2/code/kakao
GOOGLE_REDIRECT_URI={baseUrl}/login/oauth2/code/google
```

Spring Security가 `{baseUrl}`을 현재 백엔드 주소로 치환한다.

### 3.3 로그인 성공 처리

`OAuth2LoginSuccessHandler`가 실행된다.

현재 처리 내용:

1. 인증 provider 확인: `kakao` 또는 `google`
2. provider별 사용자 정보에서 `email`, `nickname`, `profileImageUrl` 추출
3. 로그 출력
4. access token 생성
5. refresh token 생성
6. 쿠키 발급
7. `PROD_OAUTH2_REDIRECT_URI`로 리다이렉트

현재 쿠키 이름:

```text
accessToken
refreshToken
```

현재 만료 시간:

```text
accessToken: JWT_ACCESS_TOKEN_VALIDITY_SEC, 현재 예시 900초
refreshToken: JWT_REFRESH_TOKEN_VALIDITY_SEC, 현재 예시 604800초
```

현재 로그인 성공 시 쿠키 옵션:

```text
Path=/
Secure=true
HttpOnly=true
SameSite=None
```

`localhost:5173` 프론트에서 HTTPS 배포 백엔드를 호출하는 cross-site 구조를 위해 `SameSite=None`을 사용한다.

### 3.4 프론트엔드로 리다이렉트

로그인 성공 후 백엔드는 `PROD_OAUTH2_REDIRECT_URI`로 이동시킨다.

현재 개발 환경 기준:

```text
PROD_OAUTH2_REDIRECT_URI=http://localhost:5173
PROD_CORS_ALLOWED_ORIGIN=http://localhost:5173
```

두 값은 실제 프론트 origin과 callback 경로에 맞춰 함께 관리한다.

## 4. 현재 인증 API

### 4.1 Access token 재발급

```text
POST /api/auth/refresh
```

현재 동작:

1. 요청 쿠키에서 `refreshToken` 조회
2. refresh token 검증
3. token subject에서 email 추출
4. token claim에서 nickname 추출
5. 새 access token 생성
6. `accessToken` 쿠키 재발급
7. 빈 200 응답 반환

주의할 점:

- refresh token rotation은 없다.
- refresh token을 DB/Redis에 저장하거나 폐기 목록으로 관리하지 않는다.
- 재발급된 access token 쿠키는 현재 `HttpOnly=false`로 발급된다. 로그인 성공 시 `HttpOnly=true`인 것과 정책이 다르다.

### 4.2 로그아웃

```text
POST /api/auth/logout
```

현재 동작:

1. `accessToken` 쿠키 maxAge를 0으로 설정
2. `refreshToken` 쿠키 maxAge를 0으로 설정
3. 로그아웃 메시지 반환

주의할 점:

- `SecurityConfig`에서 permitAll 경로는 `/api/auth/refresh`, `/login/**`, `/oauth2/**`뿐이다.
- `/api/auth/logout`은 현재 인증이 필요한 경로다.
- `JwtAuthenticationFilter`가 access token 쿠키를 인증 객체로 변환하므로 유효한 access token이 있으면 호출할 수 있다.
- 서버 측 refresh token 저장소가 없기 때문에 쿠키만 지우는 로그아웃이다. 탈취된 refresh token을 서버에서 무효화할 수 없다.

## 5. 현재 코드에서 확인한 문제와 위험 요소

### 5.1 회원 저장 로직 없음

`OAuth2LoginSuccessHandler`에는 회원가입/로그인 처리가 주석으로만 남아 있다.

```java
// userModuleService.loginOrRegister(email, nickname, profileImageUrl);
```

`UserService`에도 `loginOrRegister`가 주석 처리되어 있다. 게다가 주석 안의 코드가 `findByEmail`을 호출하지만 `UserRepository`에는 `findByEmail` 메서드가 없다.

현재 `User` 엔티티는 다음 필드만 있다.

```text
id
name
email
```

소셜 로그인을 안정적으로 운영하려면 최소한 다음 필드가 필요하다.

```text
id
email
nickname
profileImageUrl
provider
providerUserId
role
createdAt
updatedAt
lastLoginAt
```

권장 unique 조건:

```text
unique(provider, providerUserId)
unique(email) 또는 정책에 따라 nullable/중복 허용 여부 결정
```

이메일은 provider나 사용자 동의 상태에 따라 없을 수 있으므로, provider user id를 주 식별자로 삼는 것이 안전하다.

### 5.2 JWT 인증 필터 구현됨

`JwtAuthenticationFilter`가 요청 쿠키의 `accessToken`을 검증하고 `AuthenticatedUser`를 Spring Security 인증 객체로 등록한다.

현재 처리 순서:

1. `OncePerRequestFilter` 구현
2. 요청 쿠키에서 `accessToken` 조회
3. JWT 검증
4. token의 email과 nickname claim으로 `AuthenticatedUser` 생성
5. `Authentication` 생성
6. `SecurityContextHolder`에 인증 저장
7. `SecurityConfig`에서 `UsernamePasswordAuthenticationFilter` 앞에 필터 실행

회원 DB 연동이 완료되면 token의 email 대신 내부 user id로 회원을 조회하도록 변경해야 한다.

### 5.3 JWT subject가 email이라 불안정함

현재 JWT subject는 email이다.

```java
.setSubject(email)
```

문제:

- 카카오 이메일 권한이 없거나 사용자가 이메일 제공에 동의하지 않으면 null일 수 있다.
- 사용자가 이메일을 변경할 수 있다.
- provider별로 같은 이메일을 주는 경우 계정 병합 정책이 필요하다.

권장:

- JWT subject는 내부 user id로 한다.
- claim에는 `email`, `nickname`, `role` 정도만 보조 정보로 넣는다.
- refresh token은 가능하면 서버 저장소에 token id 또는 hash를 저장한다.

### 5.4 카카오 이메일 scope 누락

현재 `.env.example`의 카카오 scope:

```text
KAKAO_SCOPE=profile_nickname, profile_image
```

그런데 성공 핸들러는 다음 값을 사용한다.

```java
email = (String) kakaoAccount.get(EMAIL);
```

카카오 이메일이 필요하다면 scope에 `account_email`이 필요하다. 단, 카카오 콘솔에서 해당 동의항목 설정도 같이 필요하다.

권장:

```text
KAKAO_SCOPE=profile_nickname,profile_image,account_email
```

이메일 없이도 가입 가능한 정책이면 `provider + providerUserId`로 가입시키고 email은 nullable로 둔다.

### 5.5 OAuth access token 로그 출력

`OAuth2UserService`에서 provider access token을 로그로 찍는다.

```java
log.info("... {}", userRequest.getAccessToken().getTokenValue());
```

이 값은 외부 provider API를 호출할 수 있는 민감정보다. 운영/개발 모두 로그 출력하면 안 된다.

즉시 제거해야 한다.

### 5.6 CORS origin과 리다이렉트 URI

현재 개발 CORS:

```text
http://localhost:5173
```

현재 로그인 성공 리다이렉트:

```text
http://localhost:5173
```

프론트엔드 실제 개발 서버 포트는 Vite 기본값인 `5173`으로 통일한다.

```text
PROD_OAUTH2_REDIRECT_URI=http://localhost:5173
PROD_CORS_ALLOWED_ORIGIN=http://localhost:5173
```

CORS origin은 서버 표준인 `PROD_CORS_ALLOWED_ORIGIN` 환경변수로 주입된다.

### 5.7 쿠키 정책 미정

현재 쿠키에는 `Secure`와 `HttpOnly`만 있다.

결정해야 할 것:

- 개발 환경에서 HTTPS를 쓸 것인지
- 배포 환경에서 프론트와 백엔드가 같은 site인지
- cross-site 요청이면 `SameSite=None; Secure`가 필요한지
- access token을 JavaScript에서 읽게 할지, HttpOnly 쿠키로만 쓸지

권장 정책:

- access token, refresh token 모두 `HttpOnly=true`
- refresh token은 반드시 `HttpOnly=true`
- 프론트엔드는 토큰 문자열을 직접 읽지 않고 `credentials: "include"`로 쿠키를 보내도록 한다.
- 배포가 프론트/백엔드 다른 도메인이라면 `SameSite=None; Secure`를 명시한다.
- 로컬 개발에서는 쿠키 정책을 profile별로 분리한다.

### 5.8 refresh token 저장소 없음

현재 refresh token은 JWT 자체만 검증한다. 서버에 저장하지 않기 때문에 다음 기능이 어렵다.

- 로그아웃 시 refresh token 무효화
- 탈취 의심 token 폐기
- 기기별 로그아웃
- refresh token rotation
- 동시 로그인 정책

권장:

- DB 또는 Redis에 refresh token hash 저장
- user id, provider, issuedAt, expiresAt, revokedAt, userAgent 정도 기록
- `/api/auth/refresh` 호출 시 refresh token rotation 적용
- 로그아웃 시 현재 refresh token을 revoked 처리

### 5.9 환경변수 로딩 방식 확인 필요

`.env.example`은 예시 파일일 뿐 Spring Boot가 자동으로 읽지 않는다. 실제 실행 시 다음 중 하나가 필요하다.

- IDE Run Configuration에 환경변수 등록
- Docker Compose `env_file` 사용
- 배포 환경 Secret/ConfigMap 등록
- dotenv 라이브러리 도입
- `application-local.properties` 분리

현재 `application.properties`는 대부분 `${...}` placeholder라서 환경변수가 없으면 애플리케이션 시작에 실패할 수 있다.

### 5.10 민감정보 관리

`.env.example`에 실제 client secret처럼 보이는 값들이 들어 있다. 이 파일이 Git에 커밋되어 외부에 공유되었거나 팀원 다수가 접근 가능한 상태라면 다음 조치가 필요하다.

1. 카카오 client secret 재발급
2. 구글 client secret 재발급
3. JWT secret 교체
4. `.env.example`에는 더미 값만 남기기
5. 실제 값은 `.env`, GitHub Actions Secrets, 배포 플랫폼 Secret 등에만 저장

## 6. 다음에 해야 할 백엔드 작업

### 6.1 1순위: 회원 모델과 가입/로그인 정책 확정

먼저 정책을 정해야 한다.

결정 질문:

1. 카카오/구글 계정을 같은 이메일이면 같은 계정으로 병합할 것인가?
2. 이메일이 없는 카카오 사용자를 허용할 것인가?
3. 최초 로그인 시 추가 회원가입 화면이 필요한가?
4. nickname/profile image는 매 로그인마다 갱신할 것인가, 최초 가입 때만 저장할 것인가?
5. 탈퇴 회원이 다시 소셜 로그인하면 복구할 것인가, 신규 가입을 막을 것인가?

권장 기본 정책:

- 내부 식별자는 `User.id`
- 소셜 식별자는 `provider + providerUserId`
- 이메일은 있으면 저장하되 로그인 식별자로 과신하지 않는다.
- 최초 로그인 시 기본 회원 생성
- 필수 추가 정보가 필요하면 `needsSignup=true` 상태를 프론트에 알려 추가 입력 화면으로 보낸다.

### 6.2 User 엔티티 확장

권장 필드:

```java
private Long id;
private String email;
private String nickname;
private String profileImageUrl;
private String provider;
private String providerUserId;
private String role;
private LocalDateTime createdAt;
private LocalDateTime updatedAt;
private LocalDateTime lastLoginAt;
```

권장 사항:

- `provider`, `providerUserId` unique index 추가
- `email` nullable 여부 결정
- JPA Auditing 적용 또는 수동 timestamp 관리
- `name`과 `nickname` 용어 통일

### 6.3 UserRepository 확장

필요 메서드:

```java
Optional<User> findByProviderAndProviderUserId(String provider, String providerUserId);
Optional<User> findByEmail(String email);
boolean existsByProviderAndProviderUserId(String provider, String providerUserId);
```

이메일 병합 정책을 사용하지 않으면 `findByEmail`은 보조 용도로만 둔다.

### 6.4 UserService.loginOrRegister 구현

권장 시그니처:

```java
public User loginOrRegister(OAuthUserProfile profile)
```

또는 단순하게 시작한다면:

```java
public User loginOrRegister(
    String provider,
    String providerUserId,
    String email,
    String nickname,
    String profileImageUrl
)
```

처리 흐름:

1. `provider + providerUserId`로 기존 사용자 조회
2. 있으면 nickname/profileImageUrl/lastLoginAt 갱신 여부 결정
3. 없으면 신규 사용자 생성
4. 저장된 `User` 반환

### 6.5 OAuth2LoginSuccessHandler에서 UserService 연결

현재는 provider profile을 파싱하고 바로 JWT를 발급한다. 다음처럼 바꿔야 한다.

1. provider별 profile을 공통 DTO로 변환
2. `UserService.loginOrRegister(...)` 호출
3. 반환된 내부 `user.id`로 JWT 생성
4. refresh token 저장소에 refresh token 저장
5. 쿠키 발급
6. 프론트엔드 콜백 URL로 리다이렉트

JWT 생성 시 권장 claim:

```text
sub: 내부 user id
email: 이메일
nickname: 닉네임
role: ROLE_USER
```

### 6.6 JWT 인증 필터 추가

필요 작업:

1. `JwtAuthenticationFilter extends OncePerRequestFilter` 생성
2. 쿠키에서 `accessToken` 읽기
3. `JwtTokenProvider.validateToken(...)` 호출
4. subject에서 user id 읽기
5. 사용자 존재 확인
6. `UsernamePasswordAuthenticationToken` 생성
7. `SecurityContextHolder.getContext().setAuthentication(authentication)` 호출
8. `SecurityConfig`에 필터 등록

`SecurityConfig` 예시 방향:

```java
.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
```

### 6.7 AuthController 정책 정리

`/api/auth/refresh`

- refresh token을 쿠키에서 읽는다.
- token 검증뿐 아니라 DB/Redis 저장소에서도 유효 여부를 확인한다.
- access token을 재발급한다.
- 가능하면 refresh token도 rotation한다.

`/api/auth/logout`

- 현재 access token 인증 기반으로 호출할지, refresh token 쿠키만 있으면 허용할지 결정한다.
- 서버 저장 refresh token을 revoked 처리한다.
- 쿠키 삭제 시 발급 때와 같은 path/domain/samesite 정책을 맞춘다.

`/api/auth/me`

- 구현 완료. 프론트엔드가 로그인 성공 후 현재 인증 상태와 token의 사용자 정보를 확인한다.

현재 응답:

```json
{
  "email": "user@example.com",
  "nickname": "홍길동"
}
```

인증 정보가 없으면 로그인 페이지 리다이렉트 대신 `401 Unauthorized`를 반환한다.

### 6.8 쿠키 설정 개선

현재 `jakarta.servlet.http.Cookie`는 `SameSite`를 직접 지원하지 않는다. Spring `ResponseCookie`를 사용하면 설정하기 쉽다.

권장 설정:

개발 환경:

```text
Secure=false 또는 로컬 HTTPS 사용
SameSite=Lax
HttpOnly=true
Path=/
```

배포 환경:

```text
Secure=true
SameSite=None 또는 Lax
HttpOnly=true
Path=/
Domain=.서비스도메인.com, 필요한 경우만
```

프론트와 백엔드가 같은 site면 `Lax`가 단순하다. 완전히 다른 site면 `None`이 필요하다.

### 6.9 설정값 환경변수화

CORS origin은 환경변수로 분리되어 있다. 리다이렉트 URI와 쿠키 옵션도 환경별 설정으로 확장하는 것을 권장한다.

```text
PROD_CORS_ALLOWED_ORIGIN=http://localhost:5173
PROD_OAUTH2_REDIRECT_URI=http://localhost:5173
APP_COOKIE_SECURE=false
APP_COOKIE_SAME_SITE=Lax
```

그리고 `@ConfigurationProperties`로 묶으면 관리가 쉬워진다.

### 6.10 에러 처리와 실패 핸들러 추가

현재 OAuth 로그인 실패 핸들러가 없다. provider 인증 실패, 동의 취소, email 없음, 내부 가입 실패 등을 프론트에 명확히 전달해야 한다.

서버 표준 변수는 성공 리다이렉트의 기준 URL 하나만 사용한다. 실패 핸들러를 추가할 때도 이 URL을 기준으로 프론트 에러 경로를 조합한다.

```text
PROD_OAUTH2_REDIRECT_URI=http://localhost:5173
```

실패 케이스 예시:

```text
/login?error=oauth_cancelled
/login?error=email_required
/login?error=server_error
```

### 6.11 테스트 추가

최소 테스트:

1. `JwtTokenProvider` access/refresh 생성 및 검증
2. 만료/잘못된 token 검증 실패
3. `CookieUtil` 또는 개선된 cookie writer 옵션 검증
4. `UserService.loginOrRegister` 신규 생성/기존 조회
5. `AuthController.refresh` 성공/refresh 없음/invalid refresh
6. `JwtAuthenticationFilter` 정상 인증/토큰 없음/invalid token

통합 테스트:

1. OAuth profile DTO 변환 테스트
2. `/api/auth/me` 인증 필요 테스트
3. CORS credentials 설정 테스트

## 7. 프론트엔드 개발자에게 전달해야 할 내용

### 7.1 로그인 버튼 동작

프론트엔드 로그인 버튼은 백엔드 authorization endpoint로 브라우저를 이동시켜야 한다.

```ts
const backendBaseUrl = import.meta.env.VITE_API_BASE_URL;

function loginWithKakao() {
  window.location.href = `${backendBaseUrl}/oauth2/authorization/kakao`;
}

function loginWithGoogle() {
  window.location.href = `${backendBaseUrl}/oauth2/authorization/google`;
}
```

주의:

- `fetch`나 `axios`로 호출하지 않는다.
- 새 창 정책이 필요한지 UX에서 결정한다. 기본은 현재 창 이동이 가장 단순하다.

### 7.2 로그인 성공 후 이동할 프론트 URL 확정

백엔드가 로그인 성공 후 이동시킬 URL을 프론트에서 정해줘야 한다.

권장:

```text
http://localhost:5173
```

중요:

- 프론트 실제 origin
- 백엔드 `PROD_OAUTH2_REDIRECT_URI`
- 백엔드 CORS allowed origins

이 세 값이 서로 맞아야 한다.

### 7.3 로그인 완료 후 프론트 처리

백엔드는 토큰을 쿠키로 심고 프론트로 리다이렉트한다. 프론트는 URL에 token이 없다고 해서 실패로 보면 안 된다.

현재 설정 기준 프론트 처리:

1. `PROD_OAUTH2_REDIRECT_URI`인 `/` 진입
2. `GET /api/auth/me` 호출
3. 성공하면 전역 사용자 상태 저장
4. 원래 가려던 페이지 또는 홈으로 이동
5. 실패하면 `/api/auth/refresh` 한 번 호출
6. refresh도 실패하면 로그인 화면으로 이동

`/api/auth/me`는 아직 백엔드에 없으므로 백엔드 구현 필요.

### 7.4 API 요청에는 credentials 포함

쿠키 기반 인증이므로 모든 인증 API 요청에 credentials를 포함해야 한다.

fetch:

```ts
fetch(`${backendBaseUrl}/api/auth/me`, {
  method: "GET",
  credentials: "include",
});
```

axios:

```ts
axios.create({
  baseURL: backendBaseUrl,
  withCredentials: true,
});
```

### 7.5 Access token 만료 시 처리

access token은 현재 예시 기준 15분이다. API가 401을 반환하면 프론트는 다음 흐름을 탄다.

1. 기존 요청이 401 응답
2. `POST /api/auth/refresh` 호출
3. refresh 성공 시 기존 요청 1회 재시도
4. refresh 실패 시 로그인 화면으로 이동

동시 요청이 여러 개 401을 받을 수 있으므로 refresh 요청은 한 번만 날아가게 큐잉 처리하는 것이 좋다.

### 7.6 로그아웃 처리

프론트 로그아웃 버튼은 다음 API를 호출한다.

```text
POST /api/auth/logout
```

요청에는 credentials가 필요하다.

```ts
await fetch(`${backendBaseUrl}/api/auth/logout`, {
  method: "POST",
  credentials: "include",
});
```

현재 백엔드에서는 `/api/auth/logout` 호출이 인증 문제로 막힐 수 있다. JWT filter 구현 후 다시 확인해야 한다.

### 7.7 프론트에서 token을 직접 읽지 않는 정책

권장 정책은 token을 JavaScript에서 읽지 않는 것이다.

프론트엔드가 알아야 하는 것:

- `accessToken`, `refreshToken` 쿠키는 백엔드가 관리한다.
- 프론트는 token 값을 localStorage/sessionStorage에 저장하지 않는다.
- 프론트는 로그인 상태 확인을 `/api/auth/me` 응답으로 판단한다.
- 인증 요청에는 항상 `credentials: "include"` 또는 `withCredentials: true`를 사용한다.

### 7.8 프론트 환경변수 예시

```text
VITE_API_BASE_URL=http://localhost:8080
VITE_OAUTH_CALLBACK_PATH=/
```

백엔드와 맞춰야 할 값:

```text
PROD_OAUTH2_REDIRECT_URI=http://localhost:5173
PROD_CORS_ALLOWED_ORIGIN=http://localhost:5173
```

### 7.9 프론트 라우팅 예시

필요 화면:

```text
/login
/
```

`/login`

- 카카오 로그인 버튼
- 구글 로그인 버튼
- 이미 로그인된 사용자가 들어오면 홈으로 이동

`/` 로그인 완료 진입

- 짧은 로딩 UI
- `/api/auth/me` 호출
- 성공 시 홈 이동
- 실패 시 로그인 화면 이동

### 7.10 프론트와 합의해야 할 에러 코드

백엔드에서 OAuth 실패 핸들러를 추가하면 프론트는 query string을 읽어 메시지를 보여줄 수 있다.

예시:

```text
/login?error=oauth_cancelled
/login?error=email_required
/login?error=oauth_failed
/login?error=server_error
```

프론트 표시 문구는 UX에서 다듬되, 백엔드는 error code를 안정적으로 유지해야 한다.

## 8. 환경변수 체크리스트

### 8.1 백엔드 필수값

```text
KAKAO_CLIENT_ID
KAKAO_CLIENT_SECRET
KAKAO_REDIRECT_URI
KAKAO_AUTH_METHOD
KAKAO_GRANT_TYPE
KAKAO_SCOPE
KAKAO_CLIENT_NAME
KAKAO_AUTH_URI
KAKAO_TOKEN_URI
KAKAO_USER_INFO_URI
KAKAO_USER_NAME_ATTR

GOOGLE_CLIENT_ID
GOOGLE_CLIENT_SECRET
GOOGLE_REDIRECT_URI
GOOGLE_SCOPE

JWT_SECRET_KEY
JWT_ACCESS_TOKEN_VALIDITY
JWT_REFRESH_TOKEN_VALIDITY
JWT_ACCESS_TOKEN_VALIDITY_SEC
JWT_REFRESH_TOKEN_VALIDITY_SEC
JWT_COOKIE_DELETE_VALIDITY_SEC

PROD_OAUTH2_REDIRECT_URI
```

### 8.2 추가 권장값

```text
PROD_CORS_ALLOWED_ORIGIN
COOKIE_SECURE
COOKIE_SAME_SITE
COOKIE_DOMAIN
```

### 8.3 Provider 콘솔에 등록해야 할 URL

카카오:

```text
{BACKEND_BASE_URL}/login/oauth2/code/kakao
```

구글:

```text
{BACKEND_BASE_URL}/login/oauth2/code/google
```

프론트 URL이 아니라 백엔드 콜백 URL을 등록해야 한다.

## 9. 권장 구현 순서

1. `.env.example`에서 실제 secret처럼 보이는 값을 제거하고 이미 공유된 secret은 재발급한다.
2. 프론트 개발 서버 origin은 `http://localhost:5173`으로 맞춘다. (완료)
3. `PROD_OAUTH2_REDIRECT_URI`와 `PROD_CORS_ALLOWED_ORIGIN`을 같은 프론트 origin으로 맞춘다. (완료)
4. 카카오 이메일 사용 여부를 결정하고 scope 및 카카오 콘솔 동의항목을 수정한다.
5. `User` 엔티티를 소셜 로그인에 맞게 확장한다.
6. `UserRepository`에 provider 기반 조회 메서드를 추가한다.
7. `UserService.loginOrRegister`를 구현한다.
8. `OAuth2LoginSuccessHandler`에서 `UserService`를 호출하고 내부 user id 기반 JWT를 발급한다.
9. `JwtAuthenticationFilter`를 구현해 API 요청 인증을 가능하게 한다. (완료)
10. `/api/auth/me`를 추가한다. (완료)
11. refresh token 저장소와 logout 무효화 정책을 구현한다.
12. 쿠키 옵션을 환경별로 분리한다.
13. OAuth 실패 핸들러를 추가한다.
14. 단위 테스트와 통합 테스트를 추가한다.
15. 프론트에서 로그인 버튼, callback route, credentials 포함 API client, 401 refresh retry를 구현한다.

## 10. 백엔드-프론트엔드 계약 초안

### 로그인 시작

```text
GET {BACKEND_BASE_URL}/oauth2/authorization/{provider}
```

provider:

```text
kakao
google
```

프론트 호출 방식:

```text
window.location.href
```

### 로그인 성공

백엔드 처리:

```text
Set-Cookie: accessToken=...
Set-Cookie: refreshToken=...
302 Location: {PROD_OAUTH2_REDIRECT_URI}
```

프론트 처리:

```text
GET /api/auth/me
```

### 내 정보 조회

구현 완료. 로그인 상태 확인은 전체 회원 목록이 아니라 이 API를 사용한다.

```text
GET /api/auth/me
```

응답 예시:

```json
{
  "email": "user@example.com",
  "nickname": "홍길동"
}
```

### Access token 재발급

```text
POST /api/auth/refresh
```

요청:

```text
Cookie: refreshToken=...
```

응답:

```text
200 OK
Set-Cookie: accessToken=...
```

실패:

```text
400 NO_REFRESH_TOKEN
401 INVALID_REFRESH_TOKEN
```

### 로그아웃

```text
POST /api/auth/logout
```

요청:

```text
Cookie: accessToken=...
Cookie: refreshToken=...
```

응답:

```text
200 OK
Set-Cookie: accessToken=; Max-Age=0
Set-Cookie: refreshToken=; Max-Age=0
```

## 11. 확인해야 할 로컬 실행 환경

검증 명령:

```powershell
cd C:\kdy\BE\oiso
.\gradlew.bat test
```

현재 확인 결과:

- Gradle wrapper가 Gradle 배포판 다운로드를 시도했다.
- 이후 Java 17 toolchain을 찾지 못해 빌드가 실패했다.

실패 메시지 핵심:

```text
Cannot find a Java installation on your machine matching: {languageVersion=17}
Toolchain download repositories have not been configured.
```

필요 조치:

1. 로컬에 JDK 17 설치
2. `JAVA_HOME`을 JDK 17로 설정
3. `java -version` 확인
4. 다시 `.\gradlew.bat test` 실행

## 12. 바로 확인할 수 있는 수동 테스트 시나리오

### 12.1 카카오 로그인

1. 백엔드 실행
2. 브라우저에서 `{BACKEND_BASE_URL}/oauth2/authorization/kakao` 접속
3. 카카오 동의
4. 백엔드 콜백 성공 여부 확인
5. 프론트 콜백 URL로 이동하는지 확인
6. 브라우저 개발자도구 Application 탭에서 `accessToken`, `refreshToken` 쿠키 확인
7. `/api/auth/me` 구현 후 내 정보 조회 확인

### 12.2 구글 로그인

1. 백엔드 실행
2. 브라우저에서 `{BACKEND_BASE_URL}/oauth2/authorization/google` 접속
3. 구글 동의
4. 프론트 콜백 URL로 이동하는지 확인
5. 쿠키 확인
6. 내 정보 조회 확인

### 12.3 Refresh

1. 로그인 완료
2. `accessToken` 쿠키 삭제 또는 만료 대기
3. `POST /api/auth/refresh` 호출
4. 새 `accessToken` 쿠키 발급 확인
5. 인증 API 재호출 확인

### 12.4 Logout

1. 로그인 완료
2. `POST /api/auth/logout` 호출
3. 두 쿠키 삭제 확인
4. 인증 API가 401을 반환하는지 확인
5. refresh token 저장소를 구현했다면 같은 refresh token 재사용이 실패하는지 확인

## 13. 정리

현재 코드는 “OAuth provider 로그인 성공 → JWT 쿠키 발급 → 프론트 리다이렉트 → JWT 쿠키 기반 보호 API 인증”까지 구현되어 있다. 실제 서비스 로그인을 완성하려면 회원 저장/매칭, 내부 user id 기반 JWT, refresh token 저장/무효화 정책이 추가로 필요하다.

프론트엔드는 token을 직접 다루는 구조가 아니라 쿠키 기반 인증 구조로 맞추는 것이 좋다. 로그인 버튼은 백엔드 OAuth authorization endpoint로 이동시키고, 로그인 완료 후 콜백 화면에서 `/api/auth/me`로 로그인 상태를 확인하는 방식으로 구현하면 된다.
