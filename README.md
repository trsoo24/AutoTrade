# Trade Project

모의 투자 시스템을 위한 Spring Boot 프로젝트입니다.

## 🚀 주요 기능

### 1. 사용자 관리 시스템
- **회원가입**: 사용자명, 이메일, 비밀번호 등록
- **로그인**: 사용자명 또는 이메일로 로그인
- **회원 탈퇴**: 계정 비활성화 처리
- **중복 확인**: 사용자명/이메일 중복 확인

### 2. 백테스팅 시스템
- **기술적 지표**: MACD, RSI, SMA 전략
- **백테스팅 엔진**: 과거 데이터 기반 전략 검증
- **결과 분석**: 수익률, 최대 낙폭 등 분석

### 3. API 연동
- **한국투자증권 API**: 실시간 시세 데이터 수집
- **RESTful API**: 표준화된 API 인터페이스

## 🏗️ 아키텍처

### 패키지 구조
```
src/main/java/trade/project/
├── api/                    # API 관련
│   ├── client/            # 외부 API 클라이언트
│   └── controller/        # REST API 컨트롤러
├── backtest/              # 백테스팅 관련
│   ├── controller/        # 백테스팅 API 컨트롤러
│   ├── dto/              # 데이터 전송 객체
│   ├── engine/           # 백테스팅 엔진
│   ├── service/          # 백테스팅 서비스
│   ├── strategy/         # 거래 전략
│   └── util/             # 유틸리티 (기술적 지표 계산)
└── common/               # 공통 모듈
    ├── client/           # 기본 REST 클라이언트
    ├── config/           # 설정
    ├── dto/              # 공통 DTO
    └── exception/        # 예외 처리
└── user/                 # 사용자 관리
    ├── controller/       # 사용자 컨트롤러
    ├── dto/             # 사용자 DTO
    ├── entity/          # 사용자 엔티티
    ├── repository/      # 사용자 리포지토리
    ├── service/         # 사용자 서비스
    └── exception/       # 사용자 예외
```

## 🔧 기술 스택

- **Spring Boot 2.7+**
- **Spring WebFlux** (WebClient)
- **Lombok**
- **MySQL**
- **MyBatis**
- **Spring Security**
- **Spring Data JPA**

## 📋 환경 설정

### 1. 환경변수 설정
```bash
# 한국투자증권 API 키
export KIS_APP_KEY="your_app_key"
export KIS_APP_SECRET="your_app_secret"

# 데이터베이스 설정 (운영환경)
export DB_HOST="localhost"
export DB_PORT="3306"
export DB_NAME="trade_prod"
export DB_USERNAME="your_username"
export DB_PASSWORD="your_password"

# MongoDB 설정
export MONGODB_HOST="localhost"
export MONGODB_PORT="27017"
export MONGODB_DATABASE="trade_project"
```

### 2. 프로파일 설정
- **개발환경**: `application-dev.yml`
- **운영환경**: `application-prod.yml`

### 3. 애플리케이션 실행
```bash
# 개발환경
./gradlew bootRun --args='--spring.profiles.active=dev'

# 운영환경
./gradlew bootRun --args='--spring.profiles.active=prod'
```

## 📊 API 엔드포인트

### 사용자 관리 API

#### 회원가입
```bash
POST /api/users/signup
Content-Type: application/json

{
  "username": "testuser",
  "email": "test@example.com",
  "password": "TestPass123!",
  "confirmPassword": "TestPass123!",
  "realName": "테스트 사용자",
  "phone": "010-1234-5678"
}
```

#### 로그인
```bash
POST /api/users/login
Content-Type: application/json

{
  "username": "testuser",
  "password": "TestPass123!"
}
```

#### 사용자 정보 조회
```bash
GET /api/users/{userId}
```

#### 회원 탈퇴
```bash
DELETE /api/users/{userId}
```

#### 사용자명 중복 확인
```bash
GET /api/users/check-username?username=testuser
```

#### 이메일 중복 확인
```bash
GET /api/users/check-email?email=test@example.com
```

### 백테스팅 API
- `POST /api/backtest/run` - 백테스팅 실행
- `GET /api/backtest/run` - 간단한 백테스팅 실행
- `GET /api/backtest/strategies` - 사용 가능한 전략 목록
- `POST /api/backtest/validate` - 요청 유효성 검사
- `GET /api/backtest/default-config` - 기본 설정 조회
- `GET /api/backtest/health` - 서비스 상태 확인

### API 테스트
- `GET /api/test/token` - 토큰 발급 테스트
- `GET /api/test/stock/price/{stockCode}` - 주식 현재가 조회
- `GET /api/test/stock/daily/{stockCode}` - 주식 일자별 시세 조회
- `GET /api/test/stock/history/{stockCode}` - 주식 체결 내역 조회
- `GET /api/test/account/balance/{accountNumber}` - 계좌 잔고 조회

## 🔄 리팩토링 개선사항

### 1. 토큰 관리 개선
- ✅ 토큰 캐싱 및 자동 갱신
- ✅ 만료 시간 관리
- ✅ API 엔드포인트 상수화

### 2. 컨트롤러 개선
- ✅ 서비스 메서드만 호출하도록 단순화
- ✅ 비즈니스 로직 제거
- ✅ 예외 메시지 일반화

### 3. 책임 분리
- ✅ 기술적 지표 계산 로직을 별도 유틸리티 클래스로 분리
- ✅ StockData를 순수 데이터 클래스로 변경
- ✅ 기본값 설정 로직을 DTO로 이동

### 4. 예외 처리 개선
- ✅ 민감한 정보 마스킹
- ✅ 예외 메시지 일반화
- ✅ 보안 강화

### 5. 설정 관리 개선
- ✅ 환경별 설정 파일 분리
- ✅ 환경변수 활용 강화
- ✅ 로깅 레벨 환경별 조정

### 6. 성능 최적화
- ✅ StrategyFactory 스레드 안전성 개선
- ✅ @PostConstruct를 활용한 초기화
- ✅ 지연 초기화 제거

## 🧪 테스트

```bash
# 단위 테스트 실행
./gradlew test

# 통합 테스트 실행
./gradlew integrationTest
```

## 📈 성과 지표

백테스팅 결과에서 제공하는 주요 지표:
- **총 수익률**: 전체 투자 기간의 수익률
- **연간 수익률**: 연간화된 수익률
- **최대 낙폭**: 최대 손실 구간
- **샤프 비율**: 위험 대비 수익률
- **승률**: 수익 거래 비율
- **수익 팩터**: 총 수익 / 총 손실

## 🤝 기여하기

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 라이선스

이 프로젝트는 MIT 라이선스 하에 배포됩니다. 