
- =====================================================
-- 모의 투자 시스템 MySQL 테이블 설계 (상세 주석 포함)
-- =====================================================

-- 1. 사용자 관리 테이블
-- 목적: 시스템 사용자의 기본 정보 및 인증 정보 관리
-- 특징: 이메일과 사용자명은 유니크 제약조건으로 중복 가입 방지
CREATE TABLE users (
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '사용자 고유 식별자 (자동 증가)',
    username VARCHAR(50) UNIQUE NOT NULL COMMENT '로그인용 사용자명 (유니크)',
    email VARCHAR(100) UNIQUE NOT NULL COMMENT '사용자 이메일 주소 (유니크, 비밀번호 재설정용)',
    password_hash VARCHAR(255) NOT NULL COMMENT '암호화된 비밀번호 (BCrypt 등 사용)',
    real_name VARCHAR(100) COMMENT '실명 (법적 이름)',
    phone VARCHAR(20) COMMENT '연락처 (SMS 알림용)',
    birth_date DATE COMMENT '생년월일 (나이 제한, 통계 분석용)',
    gender ENUM('M', 'F', 'OTHER') COMMENT '성별 (통계 분석, 마케팅용)',
    address TEXT COMMENT '주소 (배송, 세무 신고용)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '계정 생성일시 (가입일)',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '정보 수정일시 (최종 업데이트)',
    is_active BOOLEAN DEFAULT TRUE COMMENT '계정 활성화 상태 (탈퇴/정지 관리)',
    last_login_at TIMESTAMP NULL COMMENT '마지막 로그인 시간 (보안 모니터링)',
    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_created_at (created_at)
);

-- 2. 계좌 관리 테이블
-- 목적: 사용자별 거래 계좌 정보 및 잔고 관리
-- 특징: 여러 계좌 타입 지원, 잔고 실시간 추적
CREATE TABLE accounts (
    account_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '계좌 고유 식별자 (자동 증가)',
    user_id BIGINT NOT NULL COMMENT '소유자 사용자 ID (users 테이블 참조)',
    account_number VARCHAR(20) UNIQUE NOT NULL COMMENT '계좌번호 (실제 거래소 계좌번호와 유사)',
    account_type ENUM('STOCK', 'FUTURES', 'OPTIONS', 'MARGIN') DEFAULT 'STOCK' COMMENT '계좌 유형 (주식/선물/옵션/마진)',
    account_name VARCHAR(100) NOT NULL COMMENT '계좌명 (사용자 지정)',
    initial_balance DECIMAL(15,2) DEFAULT 0.00 COMMENT '초기 입금 금액 (수익률 계산 기준)',
    current_balance DECIMAL(15,2) DEFAULT 0.00 COMMENT '현재 현금 잔고 (매수 가능 금액)',
    available_balance DECIMAL(15,2) DEFAULT 0.00 COMMENT '사용 가능 잔고 (동결 금액 제외)',
    frozen_balance DECIMAL(15,2) DEFAULT 0.00 COMMENT '동결된 금액 (미체결 주문 등)',
    total_profit_loss DECIMAL(15,2) DEFAULT 0.00 COMMENT '총 손익 (실현+평가손익)',
    total_commission DECIMAL(15,2) DEFAULT 0.00 COMMENT '총 수수료 (거래 비용 추적)',
    status ENUM('ACTIVE', 'SUSPENDED', 'CLOSED') DEFAULT 'ACTIVE' COMMENT '계좌 상태 (활성/정지/폐쇄)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '계좌 개설일시',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '잔고 업데이트 시간',
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_account_number (account_number),
    INDEX idx_status (status)
);

-- 3. 주식 종목 정보 테이블
-- 목적: 상장 종목의 기본 정보 및 메타데이터 관리
-- 특징: 시장구분, 섹터별 분류, 상장/상폐 이력 관리
CREATE TABLE stocks (
    stock_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '종목 고유 식별자 (자동 증가)',
    stock_code VARCHAR(10) UNIQUE NOT NULL COMMENT '종목코드 (거래소 코드, 6자리)',
    stock_name VARCHAR(100) NOT NULL COMMENT '종목명 (한글)',
    market_type ENUM('KOSPI', 'KOSDAQ', 'KONEX', 'ETF', 'ETN') NOT NULL COMMENT '시장구분 (코스피/코스닥/코넥스/ETF/ETN)',
    sector VARCHAR(100) COMMENT '섹터 (전기전자, 화학, 서비스업 등)',
    industry VARCHAR(100) COMMENT '세부 업종 (반도체, 디스플레이 등)',
    company_name VARCHAR(200) COMMENT '기업명 (법인명)',
    listing_date DATE COMMENT '상장일 (상장 기간 계산용)',
    delisting_date DATE NULL COMMENT '상폐일 (상폐 종목 관리)',
    par_value DECIMAL(10,2) COMMENT '액면가 (주식 분할/병합 계산용)',
    total_shares BIGINT COMMENT '상장주식수 (시가총액 계산용)',
    market_cap DECIMAL(20,2) COMMENT '시가총액 (대형/중형/소형주 분류용)',
    is_active BOOLEAN DEFAULT TRUE COMMENT '거래 가능 여부 (상폐/정지 종목 관리)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '종목 등록일시',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '정보 수정일시',
    INDEX idx_stock_code (stock_code),
    INDEX idx_market_type (market_type),
    INDEX idx_sector (sector),
    INDEX idx_is_active (is_active)
);

-- 4. 주식 보유 현황 테이블
-- 목적: 계좌별 종목 보유 현황 및 수익/손실 관리
-- 특징: 평균단가 계산, 평가손익 실시간 추적
CREATE TABLE stock_positions (
    position_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '보유현황 고유 식별자 (자동 증가)',
    account_id BIGINT NOT NULL COMMENT '계좌 ID (accounts 테이블 참조)',
    stock_id BIGINT NOT NULL COMMENT '종목 ID (stocks 테이블 참조)',
    quantity INT DEFAULT 0 COMMENT '보유 수량 (주식 개수)',
    average_price DECIMAL(10,2) DEFAULT 0.00 COMMENT '평균 매수가 (수량 가중 평균)',
    total_cost DECIMAL(15,2) DEFAULT 0.00 COMMENT '총 매수 금액 (수량 * 평균가)',
    current_price DECIMAL(10,2) DEFAULT 0.00 COMMENT '현재가 (실시간 업데이트)',
    market_value DECIMAL(15,2) DEFAULT 0.00 COMMENT '평가 금액 (수량 * 현재가)',
    unrealized_pnl DECIMAL(15,2) DEFAULT 0.00 COMMENT '평가손익 (미실현 손익)',
    realized_pnl DECIMAL(15,2) DEFAULT 0.00 COMMENT '실현손익 (매도시 발생한 손익)',
    total_commission DECIMAL(15,2) DEFAULT 0.00 COMMENT '총 수수료 (매수/매도 수수료 합계)',
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '최종 업데이트 시간',
    FOREIGN KEY (account_id) REFERENCES accounts(account_id) ON DELETE CASCADE COMMENT '계좌 삭제시 보유현황도 삭제',
    FOREIGN KEY (stock_id) REFERENCES stocks(stock_id) ON DELETE CASCADE COMMENT '종목 삭제시 보유현황도 삭제',
    UNIQUE KEY unique_account_stock (account_id, stock_id),
    INDEX idx_account_id (account_id),
    INDEX idx_stock_id (stock_id),
    INDEX idx_last_updated (last_updated)

);

-- 5. 주문 관리 테이블
-- 목적: 매수/매도 주문 정보 및 상태 관리
-- 특징: 주문 상태 추적, 부분 체결 관리
CREATE TABLE orders (
    order_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '주문 고유 식별자 (자동 증가)',
    account_id BIGINT NOT NULL COMMENT '주문 계좌 ID (accounts 테이블 참조)',
    stock_id BIGINT NOT NULL COMMENT '주문 종목 ID (stocks 테이블 참조)',
    order_type ENUM('BUY', 'SELL') NOT NULL COMMENT '주문 유형 (매수/매도)',
    order_status ENUM('PENDING', 'PARTIAL_FILLED', 'FILLED', 'CANCELLED', 'REJECTED') DEFAULT 'PENDING' COMMENT '주문 상태 (대기/부분체결/체결/취소/거부)',
    quantity INT NOT NULL COMMENT '주문 수량 (주식 개수)',
    filled_quantity INT DEFAULT 0 COMMENT '체결 수량 (실제 거래된 수량)',
    price DECIMAL(10,2) NOT NULL COMMENT '주문 가격 (지정가)',
    order_value DECIMAL(15,2) NOT NULL COMMENT '주문 금액 (수량 * 가격)',
    commission DECIMAL(10,2) DEFAULT 0.00 COMMENT '수수료 (주문시 예상 수수료)',
    total_amount DECIMAL(15,2) NOT NULL COMMENT '총 금액 (주문금액 + 수수료)',
    order_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '주문 접수 시간',
    filled_time TIMESTAMP NULL COMMENT '전체 체결 완료 시간',
    cancelled_time TIMESTAMP NULL COMMENT '주문 취소 시간',
    cancel_reason VARCHAR(200) COMMENT '취소 사유 (사용자 요청/시스템 오류 등)',
    order_source ENUM('WEB', 'MOBILE', 'API', 'SYSTEM') DEFAULT 'WEB' COMMENT '주문 접수 경로 (웹/모바일/API/시스템)',
    FOREIGN KEY (account_id) REFERENCES accounts(account_id) ON DELETE CASCADE,
    FOREIGN KEY (stock_id) REFERENCES stocks(stock_id) ON DELETE CASCADE,
    INDEX idx_account_id (account_id),
    INDEX idx_stock_id (stock_id),
    INDEX idx_order_status (order_status),
    INDEX idx_order_time (order_time),
    INDEX idx_order_type (order_type)
);

-- 6. 체결 내역 테이블
-- 목적: 실제 거래 체결 정보 및 상세 내역 관리
-- 특징: 주문과 1:N 관계, 체결번호 유니크
CREATE TABLE trades (
    trade_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '체결 고유 식별자 (자동 증가)',
    order_id BIGINT NOT NULL COMMENT '연관 주문 ID (orders 테이블 참조)',
    account_id BIGINT NOT NULL COMMENT '체결 계좌 ID (accounts 테이블 참조)',
    stock_id BIGINT NOT NULL COMMENT '체결 종목 ID (stocks 테이블 참조)',
    trade_type ENUM('BUY', 'SELL') NOT NULL COMMENT '체결 유형 (매수/매도)',
    quantity INT NOT NULL COMMENT '체결 수량 (실제 거래된 주식 개수)',
    price DECIMAL(10,2) NOT NULL COMMENT '체결 가격 (실제 거래 가격)',
    trade_value DECIMAL(15,2) NOT NULL COMMENT '체결 금액 (수량 * 체결가)',
    commission DECIMAL(10,2) DEFAULT 0.00 COMMENT '실제 수수료 (거래소 수수료)',
    tax DECIMAL(10,2) DEFAULT 0.00 COMMENT '거래세 (매도시 발생)',
    total_amount DECIMAL(15,2) NOT NULL COMMENT '총 금액 (체결금액 + 수수료 + 세금)',
    trade_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '체결 발생 시간',
    trade_number VARCHAR(50) UNIQUE COMMENT '체결번호 (거래소 체결번호와 유사)',
    FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE,
    FOREIGN KEY (account_id) REFERENCES accounts(account_id) ON DELETE CASCADE,
    FOREIGN KEY (stock_id) REFERENCES stocks(stock_id) ON DELETE CASCADE,
    INDEX idx_order_id (order_id),
    INDEX idx_account_id (account_id),
    INDEX idx_stock_id (stock_id),
    INDEX idx_trade_time (trade_time),
    INDEX idx_trade_type (trade_type),
    INDEX idx_trade_number (trade_number)
);

-- 7. 거래 전략 관리 테이블
-- 목적: 사용자별 거래 전략 저장 및 파라미터 관리
-- 특징: JSON 형태로 유연한 파라미터 저장
CREATE TABLE trading_strategies (
    strategy_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '전략 고유 식별자 (자동 증가)',
    user_id BIGINT NOT NULL COMMENT '전략 소유자 ID (users 테이블 참조)',
    strategy_name VARCHAR(100) NOT NULL COMMENT '전략명 (사용자 지정)',
    strategy_type ENUM('SMA', 'RSI', 'MACD', 'BOLLINGER', 'CUSTOM') NOT NULL COMMENT '전략 유형 (이동평균/RSI/MACD/볼린저/커스텀)',
    description TEXT COMMENT '전략 설명 (사용법, 로직 설명)',
    parameters JSON COMMENT '전략 파라미터 (JSON 형태로 유연한 저장)',
    is_active BOOLEAN DEFAULT TRUE COMMENT '전략 활성화 상태 (사용/비사용)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '전략 생성일시',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '전략 수정일시',
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_strategy_type (strategy_type),
    INDEX idx_is_active (is_active)
);

-- 8. 백트래킹 결과 테이블
-- 목적: 백트래킹 실행 결과 및 성과 지표 저장
-- 특징: 포괄적인 성과 분석 지표 포함
CREATE TABLE backtest_results (
    backtest_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '백트래킹 고유 식별자 (자동 증가)',
    user_id BIGINT NOT NULL COMMENT '백트래킹 실행자 ID (users 테이블 참조)',
    strategy_id BIGINT NULL COMMENT '사용된 전략 ID (trading_strategies 테이블 참조)',
    stock_id BIGINT NOT NULL COMMENT '백트래킹 대상 종목 ID (stocks 테이블 참조)',
    start_date DATE NOT NULL COMMENT '백트래킹 시작일 (기간 설정)',
    end_date DATE NOT NULL COMMENT '백트래킹 종료일 (기간 설정)',
    initial_capital DECIMAL(15,2) NOT NULL COMMENT '초기 자본금 (수익률 계산 기준)',
    final_capital DECIMAL(15,2) NOT NULL COMMENT '최종 자본금 (백트래킹 결과)',
    total_return DECIMAL(10,4) NOT NULL COMMENT '총 수익률 ((최종-초기)/초기)',
    annual_return DECIMAL(10,4) COMMENT '연간 수익률 (연율화된 수익률)',
    total_trades INT DEFAULT 0 COMMENT '총 거래 횟수 (매수+매도)',
    winning_trades INT DEFAULT 0 COMMENT '수익 거래 횟수 (이익 발생 거래)',
    losing_trades INT DEFAULT 0 COMMENT '손실 거래 횟수 (손실 발생 거래)',
    win_rate DECIMAL(5,4) COMMENT '승률 (수익거래/총거래)',
    max_drawdown DECIMAL(10,4) COMMENT '최대 낙폭 (최고점 대비 최대 하락폭)',
    sharpe_ratio DECIMAL(10,4) COMMENT '샤프 비율 (위험 대비 수익률)',
    sortino_ratio DECIMAL(10,4) COMMENT '소르티노 비율 (하방 위험 대비 수익률)',
    profit_factor DECIMAL(10,4) COMMENT '수익 팩터 (총 수익/총 손실)',
    average_trade DECIMAL(15,2) COMMENT '평균 거래 손익 (거래당 평균 손익)',
    largest_win DECIMAL(15,2) COMMENT '최대 수익 거래 (단일 거래 최대 수익)',
    largest_loss DECIMAL(15,2) COMMENT '최대 손실 거래 (단일 거래 최대 손실)',
    total_commission DECIMAL(15,2) DEFAULT 0.00 COMMENT '총 수수료 (백트래킹 중 발생한 수수료)',
    parameters JSON COMMENT '백트래킹 파라미터 (전략 설정값)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '백트래킹 실행일시',
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (strategy_id) REFERENCES trading_strategies(strategy_id) ON DELETE SET NULL,
    FOREIGN KEY (stock_id) REFERENCES stocks(stock_id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_stock_id (stock_id),
    INDEX idx_start_date (start_date),
    INDEX idx_end_date (end_date),
    INDEX idx_total_return (total_return)
);

-- 9. 백트래킹 거래 내역 테이블
-- 목적: 백트래킹 중 발생한 모든 거래 내역 상세 관리
-- 특징: 백트래킹 결과와 1:N 관계, 누적 손익 추적
CREATE TABLE backtest_trades (
    backtest_trade_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '백트래킹 거래 고유 식별자 (자동 증가)',
    backtest_id BIGINT NOT NULL COMMENT '연관 백트래킹 ID (backtest_results 테이블 참조)',
    trade_date DATE NOT NULL COMMENT '거래 발생일 (백트래킹 시뮬레이션 날짜)',
    trade_type ENUM('BUY', 'SELL') NOT NULL COMMENT '거래 유형 (매수/매도)',
    quantity INT NOT NULL COMMENT '거래 수량 (주식 개수)',
    price DECIMAL(10,2) NOT NULL COMMENT '거래 가격 (시뮬레이션 가격)',
    trade_value DECIMAL(15,2) NOT NULL COMMENT '거래 금액 (수량 * 가격)',
    commission DECIMAL(10,2) DEFAULT 0.00 COMMENT '수수료 (시뮬레이션 수수료)',
    cumulative_pnl DECIMAL(15,2) COMMENT '누적 손익 (해당 거래까지의 총 손익)',
    position_size DECIMAL(10,4) COMMENT '포지션 크기 (전체 자본 대비 비율)',
    signal_strength DECIMAL(5,4) COMMENT '신호 강도 (매매 신호의 신뢰도)',
    FOREIGN KEY (backtest_id) REFERENCES backtest_results(backtest_id) ON DELETE CASCADE,
    INDEX idx_backtest_id (backtest_id),
    INDEX idx_trade_date (trade_date),
    INDEX idx_trade_type (trade_type)
);

-- 10. 알림 설정 테이블
-- 목적: 사용자별 알림 관리 및 읽음 상태 추적
-- 특징: 다양한 알림 타입 지원, 읽음 여부 관리
CREATE TABLE notifications (
    notification_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '알림 고유 식별자 (자동 증가)',
    user_id BIGINT NOT NULL COMMENT '알림 수신자 ID (users 테이블 참조)',
    notification_type ENUM('PRICE_ALERT', 'TRADE_ALERT', 'SYSTEM_ALERT', 'STRATEGY_ALERT') NOT NULL COMMENT '알림 유형 (가격/거래/시스템/전략)',
    title VARCHAR(200) NOT NULL COMMENT '알림 제목 (간단한 요약)',
    message TEXT NOT NULL COMMENT '알림 내용 (상세 정보)',
    is_read BOOLEAN DEFAULT FALSE COMMENT '읽음 여부 (읽지 않은 알림 관리)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '알림 생성일시',
    read_at TIMESTAMP NULL COMMENT '읽음 처리 시간',
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_is_read (is_read),
    INDEX idx_created_at (created_at)
);

-- 11. 시스템 로그 테이블
-- 목적: 시스템 활동 및 오류 로그 저장
-- 특징: 사용자 활동 추적, 디버깅, 보안 모니터링
CREATE TABLE system_logs (
    log_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '로그 고유 식별자 (자동 증가)',
    user_id BIGINT NULL COMMENT '관련 사용자 ID (users 테이블 참조, 시스템 로그는 NULL)',
    log_level ENUM('INFO', 'WARN', 'ERROR', 'DEBUG') NOT NULL COMMENT '로그 레벨 (정보/경고/오류/디버그)',
    log_category VARCHAR(50) NOT NULL COMMENT '로그 카테고리 (API/TRADE/SYSTEM/LOGIN 등)',
    message TEXT NOT NULL COMMENT '로그 메시지 (상세 내용)',
    details JSON COMMENT '추가 상세 정보 (JSON 형태로 유연한 저장)',
    ip_address VARCHAR(45) COMMENT 'IP 주소 (보안 추적용)',
    user_agent TEXT COMMENT '사용자 에이전트 (브라우저/앱 정보)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '로그 발생 시간',
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL,
    INDEX idx_user_id (user_id),
    INDEX idx_log_level (log_level),
    INDEX idx_log_category (log_category),
    INDEX idx_created_at (created_at)
);

-- 12. 시장 데이터 캐시 테이블
-- 목적: 최근 시장 데이터 캐싱 (빠른 조회용)
-- 특징: 종목-날짜 조합 유니크, 자주 조회되는 데이터 최적화
CREATE TABLE market_data_cache (
    cache_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '캐시 고유 식별자 (자동 증가)',
    stock_id BIGINT NOT NULL COMMENT '종목 ID (stocks 테이블 참조)',
    data_date DATE NOT NULL COMMENT '데이터 날짜 (일별 데이터)',
    open_price DECIMAL(10,2) COMMENT '시가 (장 시작 가격)',
    high_price DECIMAL(10,2) COMMENT '고가 (당일 최고 가격)',
    low_price DECIMAL(10,2) COMMENT '저가 (당일 최저 가격)',
    close_price DECIMAL(10,2) COMMENT '종가 (장 마감 가격)',
    volume BIGINT COMMENT '거래량 (당일 거래된 주식 수)',
    amount DECIMAL(20,2) COMMENT '거래대금 (당일 거래 금액)',
    change_amount DECIMAL(10,2) COMMENT '변동금액 (전일 대비 변동)',
    change_rate DECIMAL(5,4) COMMENT '변동률 (전일 대비 변동 비율)',
    market_cap DECIMAL(20,2) COMMENT '시가총액 (종가 * 상장주식수)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '캐시 생성일시',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '캐시 업데이트 시간',
    FOREIGN KEY (stock_id) REFERENCES stocks(stock_id) ON DELETE CASCADE,
    UNIQUE KEY unique_stock_date (stock_id, data_date),
    INDEX idx_stock_id (stock_id),
    INDEX idx_data_date (data_date),
    INDEX idx_close_price (close_price)
);

-- =====================================================
-- 샘플 데이터 삽입 (주석 포함)
-- =====================================================

-- 샘플 사용자 (테스트용 계정)
INSERT INTO users (username, email, password_hash, real_name) VALUES
('testuser1', 'test1@example.com', 'hashed_password_1', '테스트 사용자1'),
('testuser2', 'test2@example.com', 'hashed_password_2', '테스트 사용자2');

-- 샘플 계좌 (테스트용 계좌)
INSERT INTO accounts (user_id, account_number, account_name, initial_balance, current_balance, available_balance) VALUES
(1, '1234567890', '주식계좌', 10000000.00, 10000000.00, 10000000.00),
(2, '0987654321', '주식계좌', 5000000.00, 5000000.00, 5000000.00);

-- 샘플 주식 종목 (대표적인 한국 주식)
INSERT INTO stocks (stock_code, stock_name, market_type, sector, company_name) VALUES
('005930', '삼성전자', 'KOSPI', '전기전자', '삼성전자주식회사'),
('000660', 'SK하이닉스', 'KOSPI', '전기전자', 'SK하이닉스주식회사'),
('035420', 'NAVER', 'KOSPI', '서비스업', 'NAVER 주식회사'),
('051910', 'LG화학', 'KOSPI', '화학', 'LG화학주식회사'),
('006400', '삼성SDI', 'KOSPI', '전기전자', '삼성SDI주식회사');

-- 샘플 거래 전략 (기본 전략들)
INSERT INTO trading_strategies (user_id, strategy_name, strategy_type, description, parameters) VALUES
(1, '삼성전자 SMA 전략', 'SMA', '삼성전자 이동평균 전략', '{"shortPeriod": 5, "longPeriod": 20}'),
(1, 'RSI 과매수/과매도 전략', 'RSI', 'RSI 기반 매매 전략', '{"rsiPeriod": 14, "overbought": 70, "oversold": 30}'),
(2, 'MACD 크로스오버 전략', 'MACD', 'MACD 신호선 크로스오버 전략', '{"fastPeriod": 12, "slowPeriod": 26, "signalPeriod": 9}'); 