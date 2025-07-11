package trade.project.common.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import trade.project.api.client.KisApiClient;

/**
 * 환경 설정 검증
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EnvironmentConfig implements CommandLineRunner {

    private final KisApiClient kisApiClient;

    @Override
    public void run(String... args) throws Exception {
        log.info("=== 환경 설정 검증 시작 ===");
        
        // KIS API 키 검증
        kisApiClient.validateApiKeys();
        
        log.info("=== 환경 설정 검증 완료 ===");
    }
} 