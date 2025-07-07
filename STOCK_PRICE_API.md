# 주식 시세 조회 API 문서

## 개요
한국투자증권 OpenAPI를 기반으로 한 주식 시세 조회 API입니다. 현재가 조회, 일자별 시세 조회 등의 기능을 제공합니다.

## API 엔드포인트

### 1. 주식 현재가 조회 (POST)
**POST** `/api/stock/price/current`

주식의 현재가 정보를 조회합니다.

#### 요청 본문
```json
{
  "stockCode": "005930"
}
```

#### 요청 필드 설명
- `stockCode` (필수): 종목코드 (6자리 숫자)

#### 응답 예시
```json
{
  "status": "success",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "stockCode": "005930",
    "stockName": "삼성전자",
    "currentPrice": 70000,
    "previousClose": 69500,
    "openPrice": 69800,
    "highPrice": 70500,
    "lowPrice": 69500,
    "tradingVolume": 1000000,
    "tradingValue": 70000000000,
    "changeRate": 0.72,
    "changeAmount": 500,
    "marketStatus": "정상",
    "timestamp": "2023-12-01T10:30:00",
    "message": "현재가 조회가 완료되었습니다.",
    "errorCode": null,
    "errorMessage": null
  }
}
```

### 2. 주식 현재가 조회 (GET)
**GET** `/api/stock/price/current/{stockCode}`

URL 경로로 주식 현재가를 조회합니다.

#### 경로 변수
- `stockCode`: 종목코드 (6자리 숫자)

#### 응답 예시
```json
{
  "status": "success",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "stockCode": "005930",
    "stockName": "삼성전자",
    "currentPrice": 70000,
    "previousClose": 69500,
    "openPrice": 69800,
    "highPrice": 70500,
    "lowPrice": 69500,
    "tradingVolume": 1000000,
    "tradingValue": 70000000000,
    "changeRate": 0.72,
    "changeAmount": 500,
    "marketStatus": "정상",
    "timestamp": "2023-12-01T10:30:00",
    "message": "현재가 조회가 완료되었습니다.",
    "errorCode": null,
    "errorMessage": null
  }
}
```

### 3. 주식 일자별 시세 조회
**POST** `/api/stock/price/daily`

주식의 일자별 시세 정보를 조회합니다.

#### 요청 본문
```json
{
  "stockCode": "005930",
  "startDate": "20231201",
  "endDate": "20231207"
}
```

#### 요청 필드 설명
- `stockCode` (필수): 종목코드 (6자리 숫자)
- `startDate` (필수): 시작일자 (YYYYMMDD 형식)
- `endDate` (필수): 종료일자 (YYYYMMDD 형식)

#### 응답 예시
```json
{
  "status": "success",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "stockCode": "005930",
    "stockName": "삼성전자",
    "startDate": "20231201",
    "endDate": "20231207",
    "dailyPrices": [
      {
        "date": "2023-12-01",
        "openPrice": 70000,
        "highPrice": 70500,
        "lowPrice": 69500,
        "closePrice": 70200,
        "tradingVolume": 1000000,
        "tradingValue": 70000000000,
        "changeRate": 1.0,
        "changeAmount": 200
      },
      {
        "date": "2023-12-04",
        "openPrice": 70200,
        "highPrice": 71000,
        "lowPrice": 70000,
        "closePrice": 70800,
        "tradingVolume": 1200000,
        "tradingValue": 84000000000,
        "changeRate": 0.85,
        "changeAmount": 600
      }
    ],
    "message": "일자별 시세 조회가 완료되었습니다.",
    "errorCode": null,
    "errorMessage": null
  }
}
```

### 4. 테스트 API

#### 4.1 삼성전자 현재가 조회 테스트
**GET** `/api/stock/price/test/samsung`

삼성전자(005930) 현재가 조회 테스트를 실행합니다.

#### 4.2 SK하이닉스 현재가 조회 테스트
**GET** `/api/stock/price/test/skhynix`

SK하이닉스(000660) 현재가 조회 테스트를 실행합니다.

#### 4.3 NAVER 현재가 조회 테스트
**GET** `/api/stock/price/test/naver`

NAVER(035420) 현재가 조회 테스트를 실행합니다.

#### 4.4 삼성전자 일자별 시세 조회 테스트
**GET** `/api/stock/price/test/samsung/daily`

삼성전자 일자별 시세 조회 테스트를 실행합니다.

## 응답 필드 설명

### 현재가 조회 응답 필드
- `stockCode`: 종목코드
- `stockName`: 종목명
- `currentPrice`: 현재가
- `previousClose`: 전일종가
- `openPrice`: 시가
- `highPrice`: 고가
- `lowPrice`: 저가
- `tradingVolume`: 거래량
- `tradingValue`: 거래대금
- `changeRate`: 등락률 (%)
- `changeAmount`: 등락폭
- `marketStatus`: 시장상태
- `timestamp`: 조회시간
- `message`: 응답메시지
- `errorCode`: 에러코드
- `errorMessage`: 에러메시지

### 일자별 시세 조회 응답 필드
- `stockCode`: 종목코드
- `stockName`: 종목명
- `startDate`: 시작일자
- `endDate`: 종료일자
- `dailyPrices`: 일자별 시세 데이터 배열
  - `date`: 날짜
  - `openPrice`: 시가
  - `highPrice`: 고가
  - `lowPrice`: 저가
  - `closePrice`: 종가
  - `tradingVolume`: 거래량
  - `tradingValue`: 거래대금
  - `changeRate`: 등락률 (%)
  - `changeAmount`: 등락폭
- `message`: 응답메시지
- `errorCode`: 에러코드
- `errorMessage`: 에러메시지

## 주요 종목코드

| 종목명 | 종목코드 |
|--------|----------|
| 삼성전자 | 005930 |
| SK하이닉스 | 000660 |
| NAVER | 035420 |
| 카카오 | 035720 |
| LG에너지솔루션 | 373220 |
| 현대차 | 005380 |
| 기아 | 000270 |
| POSCO홀딩스 | 005490 |
| KB금융 | 105560 |
| 신한지주 | 055550 |

## 에러 응답

### 유효성 검사 오류
```json
{
  "status": "error",
  "message": "요청 처리 중 오류가 발생했습니다.",
  "data": null,
  "error_code": "VALIDATION_ERROR",
  "error_message": "종목코드는 필수입니다"
}
```

### API 오류
```json
{
  "status": "error",
  "message": "요청 처리 중 오류가 발생했습니다.",
  "data": null,
  "error_code": "PRICE_QUERY_ERROR",
  "error_message": "주식 현재가 조회 실패: 종목코드를 찾을 수 없습니다"
}
```

## 시장 상태 코드

- `정상`: 정상 거래
- `거래정지`: 거래 정지
- `관리종목`: 관리종목
- `투자주의`: 투자주의종목
- `투자경고`: 투자경고종목
- `투자위험`: 투자위험종목

## 사용 예시

### cURL 예시

#### 현재가 조회 (POST)
```bash
curl -X POST http://localhost:8080/api/stock/price/current \
  -H "Content-Type: application/json" \
  -d '{"stockCode": "005930"}'
```

#### 현재가 조회 (GET)
```bash
curl -X GET http://localhost:8080/api/stock/price/current/005930
```

#### 일자별 시세 조회
```bash
curl -X POST http://localhost:8080/api/stock/price/daily \
  -H "Content-Type: application/json" \
  -d '{
    "stockCode": "005930",
    "startDate": "20231201",
    "endDate": "20231207"
  }'
```

#### 테스트 API 호출
```bash
curl -X GET http://localhost:8080/api/stock/price/test/samsung
```

### JavaScript 예시

#### 현재가 조회
```javascript
const response = await fetch('/api/stock/price/current', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    stockCode: '005930'
  })
});

const data = await response.json();
console.log(data.data.currentPrice);
```

#### 일자별 시세 조회
```javascript
const response = await fetch('/api/stock/price/daily', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    stockCode: '005930',
    startDate: '20231201',
    endDate: '20231207'
  })
});

const data = await response.json();
console.log(data.data.dailyPrices);
```

## 주의사항

1. **API 호출 제한**: 한국투자증권 API는 호출 횟수 제한이 있습니다.
2. **실시간 데이터**: 현재가 조회는 실시간 데이터를 제공합니다.
3. **거래 시간**: 주식 시장 운영 시간에만 정확한 데이터를 제공합니다.
4. **일자 범위**: 일자별 시세 조회는 최대 1년 범위로 제한됩니다.
5. **종목코드**: 정확한 6자리 종목코드를 사용해야 합니다.

## 설정

### application.yml 설정
```yaml
kis:
  api:
    base-url: https://openapi.koreainvestment.com:9443
    app-key: YOUR_APP_KEY
    app-secret: YOUR_APP_SECRET
```

## 테스트

### 단위 테스트 실행
```bash
./gradlew test --tests "trade.project.api.controller.StockPriceControllerTest"
./gradlew test --tests "trade.project.api.service.StockPriceServiceTest"
```

### 통합 테스트 실행
```bash
./gradlew test
```

## 로그

시세 조회 관련 로그는 다음과 같이 출력됩니다:
- 시세 조회 요청: `INFO` 레벨
- 시세 조회 성공: `INFO` 레벨
- 시세 조회 실패: `ERROR` 레벨
- API 오류: `ERROR` 레벨 