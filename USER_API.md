# 사용자 관리 API 문서

## 개요
사용자 회원가입, 로그인, 회원 탈퇴 기능을 제공하는 API입니다.

## 기본 URL
```
http://localhost:8080/api/users
```

## API 엔드포인트

### 1. 회원가입
**POST** `/api/users/signup`

#### 요청 본문
```json
{
  "username": "testuser",
  "email": "test@example.com",
  "password": "TestPass123!",
  "confirmPassword": "TestPass123!",
  "realName": "테스트 사용자",
  "phone": "010-1234-5678"
}
```

#### 응답 (성공)
```json
{
  "success": true,
  "data": {
    "userId": 1,
    "username": "testuser",
    "email": "test@example.com",
    "realName": "테스트 사용자",
    "phone": "010-1234-5678",
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T10:30:00",
    "isActive": true,
    "lastLoginAt": null
  }
}
```

#### 응답 (실패)
```json
{
  "success": false,
  "error_message": "이미 사용 중인 사용자명입니다."
}
```

### 2. 로그인
**POST** `/api/users/login`

#### 요청 본문
```json
{
  "username": "testuser",
  "password": "TestPass123!"
}
```

#### 응답 (성공)
```json
{
  "success": true,
  "data": {
    "userId": 1,
    "username": "testuser",
    "email": "test@example.com",
    "realName": "테스트 사용자",
    "phone": "010-1234-5678",
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T10:30:00",
    "isActive": true,
    "lastLoginAt": "2024-01-15T11:00:00"
  }
}
```

#### 응답 (실패)
```json
{
  "success": false,
  "error_message": "비밀번호가 일치하지 않습니다."
}
```

### 3. 사용자 정보 조회
**GET** `/api/users/{userId}`

#### 응답 (성공)
```json
{
  "success": true,
  "data": {
    "userId": 1,
    "username": "testuser",
    "email": "test@example.com",
    "realName": "테스트 사용자",
    "phone": "010-1234-5678",
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T10:30:00",
    "isActive": true,
    "lastLoginAt": "2024-01-15T11:00:00"
  }
}
```

### 4. 회원 탈퇴
**DELETE** `/api/users/{userId}`

#### 응답 (성공)
```json
{
  "success": true,
  "data": null
}
```

### 5. 사용자명 중복 확인
**GET** `/api/users/check-username?username=testuser`

#### 응답 (사용 가능)
```json
{
  "success": true,
  "data": true
}
```

#### 응답 (사용 불가)
```json
{
  "success": true,
  "data": false
}
```

### 6. 이메일 중복 확인
**GET** `/api/users/check-email?email=test@example.com`

#### 응답 (사용 가능)
```json
{
  "success": true,
  "data": true
}
```

#### 응답 (사용 불가)
```json
{
  "success": true,
  "data": false
}
```

## 유효성 검사 규칙

### 회원가입 요청
- **username**: 3-50자, 영문/숫자/언더스코어만 허용
- **email**: 올바른 이메일 형식, 최대 100자
- **password**: 8-100자, 영문 대소문자/숫자/특수문자 포함
- **confirmPassword**: password와 일치해야 함
- **realName**: 필수, 최대 100자
- **phone**: 전화번호 형식 (010-1234-5678)

### 로그인 요청
- **username**: 필수 (사용자명 또는 이메일)
- **password**: 필수

## 에러 코드

| 에러 메시지 | 설명 |
|------------|------|
| 비밀번호가 일치하지 않습니다. | 회원가입 시 비밀번호 확인 불일치 |
| 이미 사용 중인 사용자명입니다. | 중복된 사용자명 |
| 이미 사용 중인 이메일입니다. | 중복된 이메일 |
| 존재하지 않는 사용자이거나 비활성화된 계정입니다. | 로그인 실패 |
| 비밀번호가 일치하지 않습니다. | 로그인 시 비밀번호 오류 |
| 존재하지 않는 사용자입니다. | 사용자 조회 실패 |
| 이미 탈퇴된 계정입니다. | 중복 탈퇴 시도 |

## 보안 고려사항

1. **비밀번호 암호화**: BCrypt를 사용하여 비밀번호를 해시화
2. **중복 가입 방지**: 사용자명과 이메일의 유니크 제약조건
3. **계정 비활성화**: 탈퇴 시 실제 삭제가 아닌 비활성화 처리
4. **입력 검증**: 모든 입력값에 대한 유효성 검사
5. **로그 기록**: 중요한 작업에 대한 로그 기록

## 사용 예시

### cURL 예시

#### 회원가입
```bash
curl -X POST http://localhost:8080/api/users/signup \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "TestPass123!",
    "confirmPassword": "TestPass123!",
    "realName": "테스트 사용자",
    "phone": "010-1234-5678"
  }'
```

#### 로그인
```bash
curl -X POST http://localhost:8080/api/users/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "TestPass123!"
  }'
```

#### 사용자명 중복 확인
```bash
curl -X GET "http://localhost:8080/api/users/check-username?username=testuser"
```

#### 회원 탈퇴
```bash
curl -X DELETE http://localhost:8080/api/users/1
``` 