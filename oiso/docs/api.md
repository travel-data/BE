# OISO 백엔드 API

수정일: 2026-07-22

## 기본 URL과 경로 정책

- 배포 기본 URL: `https://oiso.duckdns.org`
- 일반 REST API는 `/api` 아래에 둔다.
- OAuth2 표준 진입점과 콜백은 Spring Security 기본 경로를 유지한다.
  - `/oauth2/authorization/{provider}`
  - `/login/oauth2/code/{provider}`
- 기존 `GET /users`는 사용하지 않는다. `GET /api/users`로 변경되었다.

Nginx는 `/api/`, `/oauth2/`, `/login/oauth2/`를 `oiso-backend:8080`으로 전달해야 한다. 현재 서버의 `/api/` 프록시 설정을 그대로 사용할 수 있으므로 `/users` 전용 location은 필요하지 않다.

## 인증 방식

OAuth2 로그인 성공 시 백엔드가 다음 HttpOnly 쿠키를 발급한다.

- `accessToken`: 보호 API 인증
- `refreshToken`: access token 재발급

프론트엔드는 토큰을 직접 읽지 않고 모든 인증 API 요청에 credentials를 포함한다.

```ts
await fetch(`${backendBaseUrl}/api/auth/me`, {
  method: "GET",
  credentials: "include",
});
```

로컬 프론트와 배포 백엔드처럼 서로 다른 site에서 호출할 수 있도록 인증 쿠키는 `HttpOnly; Secure; SameSite=None; Path=/`로 발급한다.

## API 목록

### 소셜 로그인 시작

```http
GET /oauth2/authorization/kakao
GET /oauth2/authorization/google
```

AJAX가 아니라 브라우저 전체 이동으로 호출한다.

```ts
window.location.href = `${backendBaseUrl}/oauth2/authorization/kakao`;
```

로그인 성공 후 JWT 쿠키를 발급하고 `PROD_OAUTH2_REDIRECT_URI`로 리다이렉트한다.

### 로그인 사용자 확인

```http
GET /api/auth/me
Cookie: accessToken=...
```

성공:

```http
200 OK
```

```json
{
  "email": "user@example.com",
  "nickname": "홍길동"
}
```

access token이 없거나 유효하지 않으면:

```http
401 Unauthorized
```

프론트 로그인 상태는 이 API의 `200` 여부로 판단한다. 전체 회원 목록 API를 로그인 확인 용도로 사용하지 않는다.

### Access token 재발급

```http
POST /api/auth/refresh
Cookie: refreshToken=...
```

성공 시 새로운 `accessToken` 쿠키와 `200 OK`를 반환한다.

### 로그아웃

```http
POST /api/auth/logout
Cookie: accessToken=...
```

`accessToken`과 `refreshToken` 쿠키를 만료시킨다.

### 전체 회원 조회

```http
GET /api/users
Cookie: accessToken=...
```

인증이 필요한 관리성 API다. 로그인 여부 확인 용도로 사용하지 않는다. 기존 `/users` 경로는 제거되었다.

## CORS

허용 origin은 서버 표준인 `PROD_CORS_ALLOWED_ORIGIN` 환경변수로 관리한다.

```dotenv
PROD_OAUTH2_REDIRECT_URI=http://localhost:5173
PROD_CORS_ALLOWED_ORIGIN=http://localhost:5173
```

credential 요청을 허용하므로 `*` origin은 사용하지 않는다. 정상 preflight 응답에는 다음 값이 포함된다.

```http
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Credentials: true
Access-Control-Allow-Methods: GET,POST,PUT,DELETE,OPTIONS
```

## 배포 확인

```bash
curl -i -X OPTIONS 'https://oiso.duckdns.org/api/auth/me' \
  -H 'Origin: http://localhost:5173' \
  -H 'Access-Control-Request-Method: GET'
```

```bash
curl -i 'https://oiso.duckdns.org/api/auth/me' \
  -H 'Origin: http://localhost:5173'
```

첫 번째 요청은 `200` 또는 `204`, 두 번째 비인증 요청은 리다이렉트가 아닌 `401`이어야 한다.
