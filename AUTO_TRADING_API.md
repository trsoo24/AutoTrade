# 자동매매 API

## 개요

전문적인 트레이딩 관점에서 최적화된 자동매매 시스템입니다. KOSPI 상위 20개 종목을 대상으로 하며, 백테스팅 로직에 기반한 매매 전략을 자동으로 실행합니다.

## 주요 특징

### 1. KOSPI 상위 20개 종목
- 시가총액 기준 상위 종목들로 구성
- 섹터별 분류 (전자, 서비스업, 화학, 운수장비 등)
- Enum 타입으로 관리하여 타입 안정성 보장

### 2. 전문적인 스케줄링
- **장 시작 전 (8:30-9:00)**: 준비 시간
- **장 시작 직후 (9:00-9:30)**: 고빈도 모니터링 (10초 간격)
- **오전 거래 (9:30-11:30)**: 일반 모니터링 (1분 간격)
- **점심시간 (11:30-13:00)**: 거래량 감소로 인한 간격 확대 (90초)
- **오후 거래 (13:00-14:30)**: 일반 모니터링 (1분 간격)
- **장 마감 직전 (14:30-15:00)**: 고빈도 모니터링 (10초 간격)
- **장 마감 후 (15:00-15:30)**: 정리 시간
- **야간 모니터링 (15:30-18:00)**: 긴급 상황 대응
- **긴급 모니터링 (18:00-24:00)**: 글로벌 시장 영향 대응

### 3. 다양한 매매 전략
- **기본 전략**: RSI + MACD + 이동평균 조합
- **보수적 전략**: 낮은 리스크, 안정적인 수익 추구
- **공격적 전략**: 높은 리스크, 높은 수익 추구

## API 엔드포인트

### 1. 자동매매 엔진 관리

#### 엔진 초기화
**POST** `/api/auto-trading/initialize`

자동매매 엔진을 초기화하고 기본 전략을 등록합니다.

**응답 예시:**
```json
{
  "success": true,
  "data": "자동매매 엔진이 성공적으로 초기화되었습니다."
}
```

#### 엔진 종료
**POST** `/api/auto-trading/shutdown`

자동매매 엔진을 종료합니다.

### 2. 전략 관리

#### 전략 등록
**POST** `/api/auto-trading/strategies`

새로운 매매 전략을 등록합니다.

**요청 예시:**
```json
{
  "strategyId": "CUSTOM_SAMSUNG",
  "strategyName": "삼성전자 커스텀 전략",
  "description": "사용자 정의 전략",
  "enabled": true,
  "stockCode": "005930",
  "stockName": "삼성전자",
  "totalInvestment": 10000000,
  "maxPositionSize": 0.1,
  "buyThreshold": 30,
  "sellThreshold": 70,
  "profitTarget": 5,
  "stopLoss": 3
}
```

#### 전략 제거
**DELETE** `/api/auto-trading/strategies/{strategyId}`

등록된 전략을 제거합니다.

#### 기본 전략 등록 (삼성전자)
**POST** `/api/auto-trading/strategies/default/samsung`

삼성전자 기본 전략을 등록합니다.

#### 보수적 전략 등록
**POST** `/api/auto-trading/strategies/conservative/{stockCode}`

특정 종목에 보수적 전략을 등록합니다.

**예시:**
```bash
curl -X POST "http://localhost:8080/api/auto-trading/strategies/conservative/005930"
```

#### 공격적 전략 등록
**POST** `/api/auto-trading/strategies/aggressive/{stockCode}`

특정 종목에 공격적 전략을 등록합니다.

#### 모든 종목 기본 전략 등록
**POST** `/api/auto-trading/strategies/default/all`

KOSPI 상위 20개 종목 모두에 기본 전략을 등록합니다.

### 3. 스케줄 정보

#### 현재 스케줄 조회
**GET** `/api/auto-trading/schedule/current`

현재 시간대의 스케줄 정보를 조회합니다.

**응답 예시:**
```json
{
  "success": true,
  "data": {
    "scheduleName": "MORNING_SESSION",
    "description": "오전 거래",
    "startTime": "09:30:00",
    "endTime": "11:30:00",
    "intervalSeconds": 60,
    "highFrequency": false,
    "isMarketHours": true,
    "isTradingHours": true,
    "isHighFrequencyTime": false
  }
}
```

#### 모든 스케줄 조회
**GET** `/api/auto-trading/schedule/all`

모든 스케줄 정보를 조회합니다.

### 4. 종목 정보

#### KOSPI 상위 종목 목록
**GET** `/api/auto-trading/stocks/top-kospi`

KOSPI 상위 20개 종목 목록을 조회합니다.

**응답 예시:**
```json
{
  "success": true,
  "data": [
    {
      "stockCode": "005930",
      "stockName": "삼성전자",
      "sector": "전자"
    },
    {
      "stockCode": "000660",
      "stockName": "SK하이닉스",
      "sector": "전자"
    }
  ]
}
```

#### 섹터별 종목 목록
**GET** `/api/auto-trading/stocks/sector/{sector}`

특정 섹터의 종목 목록을 조회합니다.

**예시:**
```bash
curl -X GET "http://localhost:8080/api/auto-trading/stocks/sector/전자"
```

#### 섹터 목록
**GET** `/api/auto-trading/stocks/sectors`

모든 섹터 목록을 조회합니다.

### 5. 상태 조회

#### 자동매매 상태
**GET** `/api/auto-trading/status`

자동매매 시스템의 현재 상태를 조회합니다.

**응답 예시:**
```json
{
  "success": true,
  "data": {
    "currentSchedule": "MORNING_SESSION",
    "isMarketHours": true,
    "isTradingHours": true,
    "isHighFrequencyTime": false,
    "currentTime": "10:30:00",
    "engineStatus": "RUNNING"
  }
}
```

## KOSPI 상위 20개 종목

### 1-10위
1. **삼성전자** (005930) - 전자
2. **SK하이닉스** (000660) - 전자
3. **NAVER** (035420) - 서비스업
4. **LG화학** (051910) - 화학
5. **삼성SDI** (006400) - 전기전자
6. **카카오** (035720) - 서비스업
7. **현대차** (005380) - 운수장비
8. **LG에너지솔루션** (373220) - 전기전자
9. **기아** (000270) - 운수장비
10. **POSCO홀딩스** (005490) - 철강금속

### 11-20위
11. **KB금융** (105560) - 은행
12. **신한지주** (055550) - 은행
13. **한화** (000880) - 화학
14. **LG** (003550) - 전자
15. **삼성바이오로직스** (207940) - 의약품
16. **셀트리온** (068270) - 의약품
17. **아모레퍼시픽** (090430) - 화학
18. **롯데쇼핑** (023530) - 유통업
19. **롯데케미칼** (051900) - 화학
20. **현대모비스** (012330) - 운수장비

## 매매 전략 상세

### 기본 전략 (삼성전자)
- **투자금액**: 1천만원
- **최대 포지션**: 10%
- **매수 조건**: RSI 30 이하 + MACD 상승 + 이동평균 상승
- **매도 조건**: RSI 70 이상 + MACD 하락 + 이동평균 하락
- **수익 목표**: 5%
- **손절 기준**: 3%
- **시세 조회 주기**: 1분

### 보수적 전략
- **투자금액**: 500만원
- **최대 포지션**: 5%
- **매수 조건**: RSI 25 이하 (더 엄격)
- **매도 조건**: RSI 75 이상 (더 엄격)
- **수익 목표**: 3%
- **손절 기준**: 2%
- **시세 조회 주기**: 5분

### 공격적 전략
- **투자금액**: 2천만원
- **최대 포지션**: 20%
- **매수 조건**: RSI 35 이하 (더 관대)
- **매도 조건**: RSI 65 이상 (더 관대)
- **수익 목표**: 8%
- **손절 기준**: 5%
- **시세 조회 주기**: 30초

## 기술적 지표

### RSI (Relative Strength Index)
- **기본 기간**: 14일
- **과매수 기준**: 70 (보수적: 75, 공격적: 65)
- **과매도 기준**: 30 (보수적: 25, 공격적: 35)

### MACD (Moving Average Convergence Divergence)
- **빠른 기간**: 12일 (공격적: 8일)
- **느린 기간**: 26일 (공격적: 21일)
- **신호 기간**: 9일 (공격적: 5일)

### 이동평균 (Simple Moving Average)
- **단기 기간**: 5일 (보수적: 10일, 공격적: 3일)
- **장기 기간**: 20일 (보수적: 30일, 공격적: 10일)

## 리스크 관리

### 일일 제한
- **최대 손실**: 2% (보수적: 1%, 공격적: 3%)
- **최대 거래 횟수**: 10회 (보수적: 5회, 공격적: 20회)

### 포지션 관리
- **최대 낙폭**: 10% (보수적: 5%, 공격적: 15%)
- **최대 포지션 크기**: 총 투자금액의 10% (보수적: 5%, 공격적: 20%)

## 사용 예시

### 1. 자동매매 시작
```bash
# 엔진 초기화
curl -X POST "http://localhost:8080/api/auto-trading/initialize"

# 삼성전자 기본 전략 등록
curl -X POST "http://localhost:8080/api/auto-trading/strategies/default/samsung"

# SK하이닉스 보수적 전략 등록
curl -X POST "http://localhost:8080/api/auto-trading/strategies/conservative/000660"

# NAVER 공격적 전략 등록
curl -X POST "http://localhost:8080/api/auto-trading/strategies/aggressive/035420"
```

### 2. 상태 확인
```bash
# 현재 스케줄 확인
curl -X GET "http://localhost:8080/api/auto-trading/schedule/current"

# 자동매매 상태 확인
curl -X GET "http://localhost:8080/api/auto-trading/status"

# KOSPI 상위 종목 목록 확인
curl -X GET "http://localhost:8080/api/auto-trading/stocks/top-kospi"
```

### 3. 전략 관리
```bash
# 모든 종목에 기본 전략 등록
curl -X POST "http://localhost:8080/api/auto-trading/strategies/default/all"

# 특정 전략 제거
curl -X DELETE "http://localhost:8080/api/auto-trading/strategies/DEFAULT_SAMSUNG"
```

## 주의사항

1. **실제 거래**: 이 시스템은 실제 거래를 수행하므로 충분한 테스트 후 사용하세요.
2. **자금 관리**: 투자금액과 리스크 설정을 신중히 결정하세요.
3. **시장 상황**: 급변하는 시장 상황에서는 전략을 조정해야 할 수 있습니다.
4. **API 제한**: 한국투자증권 API 호출 제한을 고려하여 적절한 주기를 설정하세요.
5. **모니터링**: 자동매매 시스템을 지속적으로 모니터링하세요.

## 에러 코드

- `ENGINE_INIT_ERROR`: 엔진 초기화 실패
- `ENGINE_SHUTDOWN_ERROR`: 엔진 종료 실패
- `STRATEGY_REGISTER_ERROR`: 전략 등록 실패
- `STRATEGY_UNREGISTER_ERROR`: 전략 제거 실패
- `DEFAULT_STRATEGY_ERROR`: 기본 전략 등록 실패
- `CONSERVATIVE_STRATEGY_ERROR`: 보수적 전략 등록 실패
- `AGGRESSIVE_STRATEGY_ERROR`: 공격적 전략 등록 실패
- `BULK_STRATEGY_ERROR`: 전체 전략 등록 실패
- `SCHEDULE_INFO_ERROR`: 스케줄 정보 조회 실패
- `SCHEDULE_LIST_ERROR`: 스케줄 목록 조회 실패
- `STOCK_LIST_ERROR`: 종목 목록 조회 실패
- `SECTOR_STOCK_ERROR`: 섹터별 종목 조회 실패
- `SECTOR_LIST_ERROR`: 섹터 목록 조회 실패
- `STATUS_ERROR`: 상태 조회 실패
- `INVALID_STOCK_CODE`: 유효하지 않은 종목코드 