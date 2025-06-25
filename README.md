# OpenAPI 연동 및 백트래킹 시스템

이 프로젝트는 한국투자증권 OpenAPI와의 연동 및 주식 거래 전략 백트래킹을 위한 Spring Boot 애플리케이션입니다.

## 주요 기능

### 1. OpenAPI 연동
- **BaseRestClient**: 모든 HTTP 요청을 처리하는 기본 REST 클라이언트
- **KisApiClient**: 한국투자증권 OpenAPI 전용 클라이언트
- **WebClient 설정**: 로깅, 타임아웃, 에러 처리 포함
- **공통 응답 DTO**: 일관된 API 응답 형식
- **예외 처리**: API 호출 중 발생하는 예외 처리

### 2. 백트래킹 시스템
- **다양한 거래 전략**: SMA, RSI, MACD 전략 지원
- **기술적 지표 계산**: 이동평균, RSI, MACD, 볼린저 밴드 등
- **성과 분석**: 수익률, 샤프 비율, 최대 낙폭, 승률 등
- **리스크 관리**: 손절/익절, 포지션 크기 제한
- **상세한 거래 내역**: 매수/매도 시점, 수수료, 수익률 등

## 프로젝트 구조

```
src/main/java/trade/project/
├── common/
│   ├── client/
│   │   └── BaseRestClient.java          # 기본 REST 클라이언트
│   ├── config/
│   │   └── WebClientConfig.java         # WebClient 설정
│   ├── dto/
│   │   └── ApiResponse.java             # 공통 응답 DTO
│   └── exception/
│       └── ApiException.java            # API 예외 클래스
├── api/
│   ├── client/
│   │   └── KisApiClient.java            # 한국투자증권 API 클라이언트
│   └── controller/
│       └── ApiTestController.java       # API 테스트 컨트롤러
├── backtest/
│   ├── dto/
│   │   ├── BacktestRequest.java         # 백트래킹 요청 DTO
│   │   ├── BacktestResult.java          # 백트래킹 결과 DTO
│   │   └── StockData.java               # 주식 데이터 DTO
│   ├── strategy/
│   │   ├── TradingStrategy.java         # 거래 전략 인터페이스
│   │   ├── SMAStrategy.java             # 이동평균선 전략
│   │   ├── RSIStrategy.java             # RSI 전략
│   │   ├── MACDStrategy.java            # MACD 전략
│   │   └── StrategyFactory.java         # 전략 팩토리
│   ├── engine/
│   │   └── BacktestEngine.java          # 백트래킹 엔진
│   ├── service/
│   │   └── BacktestService.java         # 백트래킹 서비스
│   └── controller/
│       └── BacktestController.java      # 백트래킹 컨트롤러
```

## 설정

### 1. 환경 변수 설정

한국투자증권 OpenAPI 사용을 위해 다음 환경 변수를 설정하세요:

```bash
export KIS_APP_KEY="your-app-key-here"
export KIS_APP_SECRET="your-app-secret-here"
export KIS_ACCESS_TOKEN="your-access-token-here"
```

### 2. application.yml 설정

```yaml
kis:
  api:
    base-url: https://openapi.koreainvestment.com:9443
    app-key: ${KIS_APP_KEY:your-app-key-here}
    app-secret: ${KIS_APP_SECRET:your-app-secret-here}
    access-token: ${KIS_ACCESS_TOKEN:your-access-token-here}

logging:
  level:
    trade.project.common.client: DEBUG
    trade.project.api.client: DEBUG
    trade.project.backtest: DEBUG
    org.springframework.web.reactive.function.client: DEBUG
```

## 사용법

### 1. OpenAPI 연동

```java
@Autowired
private BaseRestClient baseRestClient;

// GET 요청
Map<String, Object> response = baseRestClient.get("https://api.example.com/data", Map.class);

// POST 요청
Map<String, String> requestBody = new HashMap<>();
requestBody.put("key", "value");
Map<String, Object> response = baseRestClient.post("https://api.example.com/data", requestBody, Map.class);
```

### 2. 백트래킹 실행

#### REST API 사용

```bash
# 간단한 백트래킹 실행
curl "http://localhost:8080/api/backtest/run?stockCode=005930&startDate=2024-01-01&endDate=2024-01-31&strategy=SMA&initialCapital=10000000"

# 상세한 백트래킹 실행 (POST)
curl -X POST "http://localhost:8080/api/backtest/run" \
  -H "Content-Type: application/json" \
  -d '{
    "stockCode": "005930",
    "startDate": "2024-01-01",
    "endDate": "2024-01-31",
    "strategy": "SMA",
    "initialCapital": 10000000,
    "shortPeriod": 5,
    "longPeriod": 20,
    "commission": 0.001
  }'
```

#### Java 코드 사용

```java
@Autowired
private BacktestService backtestService;

BacktestRequest request = BacktestRequest.builder()
    .stockCode("005930")
    .startDate(LocalDate.of(2024, 1, 1))
    .endDate(LocalDate.of(2024, 1, 31))
    .strategy("SMA")
    .initialCapital(new BigDecimal("10000000"))
    .shortPeriod(5)
    .longPeriod(20)
    .commission(new BigDecimal("0.001"))
    .build();

BacktestResult result = backtestService.runBacktest(request);
```

## API 엔드포인트

### OpenAPI 테스트
- `GET /api/test/health` - API 서버 상태 확인
- `GET /api/test/token` - 토큰 발급 테스트
- `GET /api/test/stock/price/{stockCode}` - 주식 현재가 조회
- `GET /api/test/stock/daily/{stockCode}?startDate=20240101&endDate=20240131` - 일자별 시세 조회

### 백트래킹
- `GET /api/backtest/health` - 백트래킹 서비스 상태 확인
- `GET /api/backtest/strategies` - 사용 가능한 전략 목록
- `GET /api/backtest/default-config` - 기본 설정 조회
- `GET /api/backtest/run` - 간단한 백트래킹 실행
- `POST /api/backtest/run` - 상세한 백트래킹 실행
- `POST /api/backtest/validate` - 백트래킹 요청 유효성 검사

## 지원하는 거래 전략

### 1. SMA (Simple Moving Average) 전략
- **설명**: 단기 이동평균선이 장기 이동평균선을 상향 돌파할 때 매수, 하향 돌파할 때 매도
- **매개변수**: 
  - `shortPeriod`: 단기 이동평균 기간 (기본값: 5)
  - `longPeriod`: 장기 이동평균 기간 (기본값: 20)

### 2. RSI (Relative Strength Index) 전략
- **설명**: RSI가 과매도 구간에서 상승 반전할 때 매수, 과매수 구간에서 하락 반전할 때 매도
- **매개변수**:
  - `rsiPeriod`: RSI 계산 기간 (기본값: 14)
  - `rsiOverbought`: 과매수 기준 (기본값: 70)
  - `rsiOversold`: 과매도 기준 (기본값: 30)

### 3. MACD (Moving Average Convergence Divergence) 전략
- **설명**: MACD가 시그널선을 상향 돌파할 때 매수, 하향 돌파할 때 매도
- **매개변수**:
  - `macdFastPeriod`: MACD 빠른선 기간 (기본값: 12)
  - `macdSlowPeriod`: MACD 느린선 기간 (기본값: 26)
  - `macdSignalPeriod`: MACD 시그널 기간 (기본값: 9)

## 백트래킹 결과 분석

백트래킹 결과에는 다음과 같은 정보가 포함됩니다:

### 기본 정보
- 주식 코드, 기간, 사용 전략
- 초기 자본금, 최종 자본금, 총 수익률

### 성과 지표
- **연간 수익률**: 연간화된 수익률
- **샤프 비율**: 위험 대비 수익률
- **최대 낙폭**: 최대 손실 구간
- **변동성**: 수익률의 표준편차

### 거래 통계
- **총 거래 횟수**: 매수/매도 총 횟수
- **승률**: 수익 거래 비율
- **평균 수익/손실**: 거래당 평균 수익/손실
- **수익 팩터**: 총 수익 / 총 손실

### 포트폴리오 정보
- **피크 자본**: 최고 자본금과 날짜
- **최대 낙폭**: 최대 손실과 날짜
- **거래 내역**: 상세한 매수/매도 기록
- **포트폴리오 히스토리**: 일별 포트폴리오 가치 변화

## 실행 방법

### 1. 프로젝트 빌드

```bash
./gradlew build -x test
```

### 2. 애플리케이션 실행

```bash
./gradlew bootRun
```

### 3. 테스트

```bash
# API 서버 상태 확인
curl http://localhost:8080/api/test/health

# 백트래킹 서비스 상태 확인
curl http://localhost:8080/api/backtest/health

# 사용 가능한 전략 목록
curl http://localhost:8080/api/backtest/strategies

# 삼성전자 백트래킹 실행 (SMA 전략)
curl "http://localhost:8080/api/backtest/run?stockCode=005930&startDate=2024-01-01&endDate=2024-01-31&strategy=SMA&initialCapital=10000000"
```

## 주요 특징

### 1. 확장성
- 새로운 거래 전략을 쉽게 추가할 수 있는 인터페이스 구조
- 다양한 기술적 지표 계산 지원
- 모듈화된 백트래킹 엔진

### 2. 정확성
- 실제 거래 수수료 반영
- 정확한 기술적 지표 계산
- 상세한 거래 내역 추적

### 3. 사용 편의성
- REST API를 통한 쉬운 접근
- 기본값이 설정된 간편한 사용법
- 상세한 결과 분석 제공

### 4. 안정성
- 예외 처리 및 에러 핸들링
- 입력 데이터 유효성 검사
- 로깅을 통한 디버깅 지원

## 주의사항

1. **보안**: API 키와 시크릿은 환경 변수로 관리하세요.
2. **Rate Limiting**: API 호출 제한을 확인하고 준수하세요.
3. **백테스팅 한계**: 과거 데이터 기반이므로 미래 성과를 보장하지 않습니다.
4. **리스크 관리**: 실제 거래 시에는 적절한 리스크 관리가 필요합니다.
5. **데이터 품질**: 백트래킹 결과의 정확성은 입력 데이터의 품질에 의존합니다.

## 향후 개선 계획

- [ ] 추가 거래 전략 (볼린저 밴드, 스토캐스틱 등)
- [ ] 포트폴리오 백트래킹 (여러 주식 동시 거래)
- [ ] 실시간 백트래킹 (실시간 데이터 기반)
- [ ] 웹 UI (백트래킹 결과 시각화)
- [ ] 성과 비교 기능 (여러 전략 동시 비교)
- [ ] 최적화 기능 (매개변수 자동 최적화) 