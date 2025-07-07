# 주식 주문 API 문서

## 개요
한국투자증권 OpenAPI를 기반으로 한 주식 주문 API입니다. 매수/매도 주문 실행, 주문 상태 조회 등의 기능을 제공합니다.

## API 엔드포인트

### 1. 주식 주문 실행
**POST** `/api/stock/order/execute`

주식 매수/매도 주문을 실행합니다.

#### 요청 본문
```json
{
  "accountNumber": "1234567890",
  "stockCode": "005930",
  "orderType": "매수",
  "quantity": 10,
  "price": 70000,
  "priceType": "지정가",
  "orderCategory": "일반",
  "originalOrderNumber": null
}
```

#### 요청 필드 설명
- `accountNumber` (필수): 계좌번호
- `stockCode` (필수): 종목코드 (6자리 숫자)
- `orderType` (필수): 주문구분 ("매수" 또는 "매도")
- `quantity` (필수): 주문수량 (양수)
- `price` (필수): 주문가격 (양수, 시장가는 0)
- `priceType` (선택): 주문유형 ("지정가" 또는 "시장가", 기본값: "지정가")
- `orderCategory` (선택): 주문구분 ("일반", "정정", "취소", 기본값: "일반")
- `originalOrderNumber` (선택): 정정/취소 시 원주문번호

#### 응답 예시
```json
{
  "status": "success",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "orderNumber": "202312010001",
    "accountNumber": "1234567890",
    "stockCode": "005930",
    "stockName": "삼성전자",
    "orderType": "매수",
    "quantity": 10,
    "price": 70000,
    "priceType": "지정가",
    "orderStatus": "접수완료",
    "orderCategory": "일반",
    "orderDateTime": "2023-12-01T10:30:00",
    "message": "주문이 정상적으로 접수되었습니다.",
    "errorCode": null,
    "errorMessage": null
  }
}
```

### 2. 주문 상태 조회
**POST** `/api/stock/order/status`

주문의 현재 상태를 조회합니다.

#### 요청 본문
```json
{
  "accountNumber": "1234567890",
  "orderNumber": "202312010001"
}
```

#### 요청 필드 설명
- `accountNumber` (필수): 계좌번호
- `orderNumber` (필수): 주문번호

#### 응답 예시
```json
{
  "status": "success",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "orderNumber": "202312010001",
    "accountNumber": "1234567890",
    "stockCode": "005930",
    "stockName": "삼성전자",
    "orderType": "매수",
    "quantity": 10,
    "price": 70000,
    "priceType": "지정가",
    "orderStatus": "체결완료",
    "orderCategory": "일반",
    "orderDateTime": "2023-12-01T10:30:00",
    "message": "주문 상태 조회가 완료되었습니다.",
    "errorCode": null,
    "errorMessage": null
  }
}
```

### 3. 테스트 API

#### 3.1 매수 테스트
**POST** `/api/stock/order/test/buy`

삼성전자 10주 매수 테스트 주문을 실행합니다.

#### 3.2 매도 테스트
**POST** `/api/stock/order/test/sell`

삼성전자 5주 매도 테스트 주문을 실행합니다.

#### 3.3 시장가 매수 테스트
**POST** `/api/stock/order/test/market-buy`

삼성전자 10주 시장가 매수 테스트 주문을 실행합니다.

## 주문 유형별 파라미터

### 1. 일반 주문
```json
{
  "accountNumber": "1234567890",
  "stockCode": "005930",
  "orderType": "매수",
  "quantity": 10,
  "price": 70000,
  "priceType": "지정가",
  "orderCategory": "일반"
}
```

### 2. 시장가 주문
```json
{
  "accountNumber": "1234567890",
  "stockCode": "005930",
  "orderType": "매수",
  "quantity": 10,
  "price": 0,
  "priceType": "시장가",
  "orderCategory": "일반"
}
```

### 3. 정정 주문
```json
{
  "accountNumber": "1234567890",
  "stockCode": "005930",
  "orderType": "매수",
  "quantity": 15,
  "price": 75000,
  "priceType": "지정가",
  "orderCategory": "정정",
  "originalOrderNumber": "202312010001"
}
```

### 4. 취소 주문
```json
{
  "accountNumber": "1234567890",
  "stockCode": "005930",
  "orderType": "매수",
  "quantity": 10,
  "price": 70000,
  "priceType": "지정가",
  "orderCategory": "취소",
  "originalOrderNumber": "202312010001"
}
```

## 에러 응답

### 유효성 검사 오류
```json
{
  "status": "error",
  "message": "요청 처리 중 오류가 발생했습니다.",
  "data": null,
  "error_code": "VALIDATION_ERROR",
  "error_message": "계좌번호는 필수입니다"
}
```

### API 오류
```json
{
  "status": "error",
  "message": "요청 처리 중 오류가 발생했습니다.",
  "data": null,
  "error_code": "ORDER_EXECUTION_ERROR",
  "error_message": "주식 주문 실행 실패: API 오류 발생"
}
```

## 주문 상태 코드

- `접수완료`: 주문이 정상적으로 접수됨
- `체결완료`: 주문이 완전히 체결됨
- `부분체결`: 주문이 일부만 체결됨
- `주문취소`: 주문이 취소됨
- `주문거부`: 주문이 거부됨

## 주의사항

1. **실제 거래**: 이 API는 실제 주식 거래를 수행하므로 신중하게 사용해야 합니다.
2. **계좌 인증**: 한국투자증권 계좌 인증이 필요합니다.
3. **API 키**: 한국투자증권 OpenAPI 앱키와 시크릿이 필요합니다.
4. **거래 시간**: 주식 시장 운영 시간에만 주문이 가능합니다.
5. **잔고 확인**: 매수 시 충분한 자금이 있는지 확인해야 합니다.
6. **보유 주식**: 매도 시 충분한 주식을 보유하고 있는지 확인해야 합니다.

## 설정

### application.yml 설정
```yaml
kis:
  api:
    base-url: https://openapi.koreainvestment.com:9443
    app-key: YOUR_APP_KEY
    app-secret: YOUR_APP_SECRET
```

### 환경별 설정
- 개발환경: `application-dev.yml`
- 운영환경: `application-prod.yml`
- 테스트환경: `application-test.yml`

## 테스트

### 단위 테스트 실행
```bash
./gradlew test --tests "trade.project.api.controller.StockOrderControllerTest"
./gradlew test --tests "trade.project.api.service.StockOrderServiceTest"
```

### 통합 테스트 실행
```bash
./gradlew test
```

## 로그

주문 관련 로그는 다음과 같이 출력됩니다:
- 주문 요청: `INFO` 레벨
- 주문 성공: `INFO` 레벨
- 주문 실패: `ERROR` 레벨
- API 오류: `ERROR` 레벨 