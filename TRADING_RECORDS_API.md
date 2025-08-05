# 매매 기록 및 시세 조회 기록 API

## 개요

이 문서는 매매 기록과 시세 조회 기록을 저장하고 조회하는 API에 대한 설명입니다.

- **매매 기록**: MariaDB에 저장되며, 주식 주문 시 자동으로 저장됩니다.
- **시세 조회 기록**: MongoDB에 저장되며, 시세 조회 시 자동으로 저장됩니다.

## 매매 기록 API

### 1. 주문번호로 매매 기록 조회

**GET** `/api/trading/records/order/{orderNumber}`

주문번호로 특정 매매 기록을 조회합니다.

**응답 예시:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "orderNumber": "202312010001",
    "accountNumber": "1234567890",
    "stockCode": "005930",
    "stockName": "삼성전자",
    "orderType": "매수",
    "quantity": 10,
    "price": 75000,
    "priceType": "지정가",
    "orderStatus": "체결",
    "orderCategory": "일반",
    "executedQuantity": 10,
    "executedPrice": 75000,
    "executionTime": "2023-12-01T09:30:00",
    "totalAmount": 750000,
    "commission": 1125,
    "orderDateTime": "2023-12-01T09:25:00",
    "createdAt": "2023-12-01T09:25:00",
    "updatedAt": "2023-12-01T09:30:00",
    "errorCode": null,
    "errorMessage": null,
    "apiResponse": "{...}"
  }
}
```

### 2. 계좌번호로 매매 기록 목록 조회

**GET** `/api/trading/records/account/{accountNumber}`

특정 계좌의 모든 매매 기록을 조회합니다.

### 3. 종목코드로 매매 기록 목록 조회

**GET** `/api/trading/records/stock/{stockCode}`

특정 종목의 모든 매매 기록을 조회합니다.

### 4. 주문구분으로 매매 기록 목록 조회

**GET** `/api/trading/records/type/{orderType}`

주문구분(매수/매도)으로 매매 기록을 조회합니다.

### 5. 주문상태로 매매 기록 목록 조회

**GET** `/api/trading/records/status/{orderStatus}`

주문상태로 매매 기록을 조회합니다.

### 6. 계좌번호와 종목코드로 매매 기록 목록 조회

**GET** `/api/trading/records/account/{accountNumber}/stock/{stockCode}`

특정 계좌의 특정 종목 매매 기록을 조회합니다.

### 7. 기간별 매매 기록 조회

**GET** `/api/trading/records/period?startDateTime=2023-12-01T00:00:00&endDateTime=2023-12-31T23:59:59`

특정 기간의 매매 기록을 조회합니다.

### 8. 계좌번호와 기간으로 매매 기록 조회

**GET** `/api/trading/records/account/{accountNumber}/period?startDateTime=2023-12-01T00:00:00&endDateTime=2023-12-31T23:59:59`

특정 계좌의 특정 기간 매매 기록을 조회합니다.

### 9. 종목코드와 기간으로 매매 기록 조회

**GET** `/api/trading/records/stock/{stockCode}/period?startDateTime=2023-12-01T00:00:00&endDateTime=2023-12-31T23:59:59`

특정 종목의 특정 기간 매매 기록을 조회합니다.

### 10. 에러가 발생한 매매 기록 조회

**GET** `/api/trading/records/errors`

에러가 발생한 매매 기록을 조회합니다.

### 11. 성공한 매매 기록 조회

**GET** `/api/trading/records/success`

성공한 매매 기록을 조회합니다.

### 12. 특정 계좌의 성공한 매매 기록 조회

**GET** `/api/trading/records/account/{accountNumber}/success`

특정 계좌의 성공한 매매 기록을 조회합니다.

### 13. 특정 종목의 성공한 매매 기록 조회

**GET** `/api/trading/records/stock/{stockCode}/success`

특정 종목의 성공한 매매 기록을 조회합니다.

### 14. 매매 기록 통계 조회 (계좌별)

**GET** `/api/trading/records/statistics/account/{accountNumber}`

특정 계좌의 매매 통계를 조회합니다.

**응답 예시:**
```json
{
  "success": true,
  "data": [
    ["매수", 5, 50, 3750000],
    ["매도", 3, 30, 2250000]
  ]
}
```

### 15. 매매 기록 통계 조회 (종목별)

**GET** `/api/trading/records/statistics/stock/{stockCode}`

특정 종목의 매매 통계를 조회합니다.

### 16. 최근 매매 기록 조회

**GET** `/api/trading/records/recent?limit=10`

최근 매매 기록을 조회합니다. (기본값: 10개)

## 시세 조회 기록 API

### 1. 종목코드로 시세 조회 기록 조회

**GET** `/api/price/records/stock/{stockCode}`

특정 종목의 시세 조회 기록을 조회합니다.

**응답 예시:**
```json
{
  "success": true,
  "data": [
    {
      "id": "507f1f77bcf86cd799439011",
      "stockCode": "005930",
      "stockName": "삼성전자",
      "queryType": "current",
      "currentPrice": 75000,
      "previousClose": 74500,
      "openPrice": 74600,
      "highPrice": 75200,
      "lowPrice": 74400,
      "tradingVolume": 15000000,
      "tradingValue": 1125000000000,
      "changeRate": 0.67,
      "changeAmount": 500,
      "marketStatus": "정상",
      "queryDateTime": "2023-12-01T14:30:00",
      "userAgent": "Mozilla/5.0...",
      "clientIp": "192.168.1.100",
      "sessionId": "session123",
      "errorCode": null,
      "errorMessage": null,
      "apiResponse": "{...}",
      "createdAt": "2023-12-01T14:30:00",
      "updatedAt": null
    }
  ]
}
```

### 2. 조회유형으로 시세 조회 기록 조회

**GET** `/api/price/records/type/{queryType}`

조회유형(current/daily)으로 시세 조회 기록을 조회합니다.

### 3. 종목코드와 조회유형으로 시세 조회 기록 조회

**GET** `/api/price/records/stock/{stockCode}/type/{queryType}`

특정 종목의 특정 조회유형 시세 조회 기록을 조회합니다.

### 4. 기간별 시세 조회 기록 조회

**GET** `/api/price/records/period?startDateTime=2023-12-01T00:00:00&endDateTime=2023-12-31T23:59:59`

특정 기간의 시세 조회 기록을 조회합니다.

### 5. 종목코드와 기간으로 시세 조회 기록 조회

**GET** `/api/price/records/stock/{stockCode}/period?startDateTime=2023-12-01T00:00:00&endDateTime=2023-12-31T23:59:59`

특정 종목의 특정 기간 시세 조회 기록을 조회합니다.

### 6. 조회유형과 기간으로 시세 조회 기록 조회

**GET** `/api/price/records/type/{queryType}/period?startDateTime=2023-12-01T00:00:00&endDateTime=2023-12-31T23:59:59`

특정 조회유형의 특정 기간 시세 조회 기록을 조회합니다.

### 7. 에러가 발생한 시세 조회 기록 조회

**GET** `/api/price/records/errors`

에러가 발생한 시세 조회 기록을 조회합니다.

### 8. 성공한 시세 조회 기록 조회

**GET** `/api/price/records/success`

성공한 시세 조회 기록을 조회합니다.

### 9. 특정 종목의 성공한 시세 조회 기록 조회

**GET** `/api/price/records/stock/{stockCode}/success`

특정 종목의 성공한 시세 조회 기록을 조회합니다.

### 10. 특정 조회유형의 성공한 시세 조회 기록 조회

**GET** `/api/price/records/type/{queryType}/success`

특정 조회유형의 성공한 시세 조회 기록을 조회합니다.

### 11. 클라이언트 IP로 시세 조회 기록 조회

**GET** `/api/price/records/client/{clientIp}`

특정 클라이언트 IP의 시세 조회 기록을 조회합니다.

### 12. 세션 ID로 시세 조회 기록 조회

**GET** `/api/price/records/session/{sessionId}`

특정 세션 ID의 시세 조회 기록을 조회합니다.

### 13. 최근 시세 조회 기록 조회

**GET** `/api/price/records/recent`

최근 10개의 시세 조회 기록을 조회합니다.

### 14. 특정 종목의 최근 시세 조회 기록 조회

**GET** `/api/price/records/stock/{stockCode}/recent`

특정 종목의 최근 5개 시세 조회 기록을 조회합니다.

### 15. 특정 종목의 현재가 조회 기록만 조회

**GET** `/api/price/records/stock/{stockCode}/current`

특정 종목의 현재가 조회 기록만 조회합니다.

### 16. 특정 종목의 일자별 시세 조회 기록만 조회

**GET** `/api/price/records/stock/{stockCode}/daily`

특정 종목의 일자별 시세 조회 기록만 조회합니다.

### 17. 시세 조회 통계 조회

**GET** `/api/price/records/statistics`

시세 조회 통계를 조회합니다.

### 18. 특정 기간의 시세 조회 기록 수 조회

**GET** `/api/price/records/count/period?startDateTime=2023-12-01T00:00:00&endDateTime=2023-12-31T23:59:59`

특정 기간의 시세 조회 기록 수를 조회합니다.

### 19. 특정 종목의 시세 조회 기록 수 조회

**GET** `/api/price/records/count/stock/{stockCode}`

특정 종목의 시세 조회 기록 수를 조회합니다.

### 20. 특정 조회유형의 시세 조회 기록 수 조회

**GET** `/api/price/records/count/type/{queryType}`

특정 조회유형의 시세 조회 기록 수를 조회합니다.

### 21. 에러가 발생한 시세 조회 기록 수 조회

**GET** `/api/price/records/count/errors`

에러가 발생한 시세 조회 기록 수를 조회합니다.

### 22. 성공한 시세 조회 기록 수 조회

**GET** `/api/price/records/count/success`

성공한 시세 조회 기록 수를 조회합니다.

## 데이터베이스 스키마

### 매매 기록 (MariaDB)

```sql
CREATE TABLE trading_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_number VARCHAR(20) NOT NULL,
    account_number VARCHAR(20) NOT NULL,
    stock_code VARCHAR(6) NOT NULL,
    stock_name VARCHAR(50),
    order_type VARCHAR(10) NOT NULL,
    quantity INT NOT NULL,
    price INT NOT NULL,
    price_type VARCHAR(10),
    order_status VARCHAR(20),
    order_category VARCHAR(10),
    original_order_number VARCHAR(20),
    executed_quantity INT,
    executed_price INT,
    execution_time DATETIME,
    total_amount BIGINT,
    commission INT,
    order_datetime DATETIME NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    error_code VARCHAR(10),
    error_message VARCHAR(500),
    api_response TEXT,
    INDEX idx_order_number (order_number),
    INDEX idx_account_number (account_number),
    INDEX idx_stock_code (stock_code),
    INDEX idx_order_datetime (order_datetime)
);
```

### 시세 조회 기록 (MongoDB)

```json
{
  "_id": "ObjectId",
  "stockCode": "String",
  "stockName": "String",
  "queryType": "String",
  "currentPrice": "Number",
  "previousClose": "Number",
  "openPrice": "Number",
  "highPrice": "Number",
  "lowPrice": "Number",
  "tradingVolume": "Number",
  "tradingValue": "Number",
  "changeRate": "Number",
  "changeAmount": "Number",
  "marketStatus": "String",
  "startDate": "String",
  "endDate": "String",
  "dailyCount": "Number",
  "queryDateTime": "Date",
  "userAgent": "String",
  "clientIp": "String",
  "sessionId": "String",
  "errorCode": "String",
  "errorMessage": "String",
  "apiResponse": "String",
  "createdAt": "Date",
  "updatedAt": "Date"
}
```

## 자동 저장 기능

### 매매 기록 자동 저장

주식 주문 API 호출 시 자동으로 매매 기록이 MariaDB에 저장됩니다:

1. **주문 실행 시**: `StockOrderService.executeOrder()` 메서드에서 자동 저장
2. **저장 정보**: 주문 요청, 응답, API 응답 전체 JSON
3. **에러 처리**: 매매 기록 저장 실패는 주문 실행에 영향을 주지 않음

### 시세 조회 기록 자동 저장

시세 조회 API 호출 시 자동으로 시세 조회 기록이 MongoDB에 저장됩니다:

1. **현재가 조회 시**: `StockPriceService.getCurrentPrice()` 메서드에서 자동 저장
2. **일자별 시세 조회 시**: `StockPriceService.getDailyPrices()` 메서드에서 자동 저장
3. **저장 정보**: 조회 요청, 응답, 클라이언트 정보, API 응답 전체 JSON
4. **에러 처리**: 시세 조회 기록 저장 실패는 API 응답에 영향을 주지 않음

## 에러 코드

### 매매 기록 관련 에러

- `TRADING_RECORD_QUERY_ERROR`: 매매 기록 조회 실패
- `TRADING_STATISTICS_ERROR`: 매매 기록 통계 조회 실패

### 시세 조회 기록 관련 에러

- `PRICE_QUERY_RECORD_ERROR`: 시세 조회 기록 조회 실패
- `PRICE_QUERY_STATISTICS_ERROR`: 시세 조회 통계 조회 실패
- `PRICE_QUERY_COUNT_ERROR`: 시세 조회 기록 수 조회 실패

## 사용 예시

### 매매 기록 조회

```bash
# 특정 계좌의 매매 기록 조회
curl -X GET "http://localhost:8080/api/trading/records/account/1234567890"

# 특정 종목의 매매 기록 조회
curl -X GET "http://localhost:8080/api/trading/records/stock/005930"

# 기간별 매매 기록 조회
curl -X GET "http://localhost:8080/api/trading/records/period?startDateTime=2023-12-01T00:00:00&endDateTime=2023-12-31T23:59:59"
```

### 시세 조회 기록 조회

```bash
# 특정 종목의 시세 조회 기록 조회
curl -X GET "http://localhost:8080/api/price/records/stock/005930"

# 현재가 조회 기록만 조회
curl -X GET "http://localhost:8080/api/price/records/stock/005930/current"

# 일자별 시세 조회 기록만 조회
curl -X GET "http://localhost:8080/api/price/records/stock/005930/daily"

# 기간별 시세 조회 기록 조회
curl -X GET "http://localhost:8080/api/price/records/period?startDateTime=2023-12-01T00:00:00&endDateTime=2023-12-31T23:59:59"
```

## 주의사항

1. **데이터베이스 연결**: MariaDB와 MongoDB가 정상적으로 연결되어야 합니다.
2. **인덱스**: 자주 조회하는 필드에 인덱스가 생성되어 있습니다.
3. **에러 처리**: 기록 저장 실패는 원본 API 호출에 영향을 주지 않습니다.
4. **데이터 보존**: API 응답 전체 JSON이 저장되어 추후 분석에 활용할 수 있습니다.
5. **클라이언트 정보**: 시세 조회 시 클라이언트 IP, 세션 ID, User-Agent가 저장됩니다. 