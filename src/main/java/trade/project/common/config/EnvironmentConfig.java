package trade.project.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import jakarta.annotation.PostConstruct;

@Slf4j
@Configuration
@Profile("dev")
public class EnvironmentConfig {

    @Value("${DB_HOST:localhost}")
    private String dbHost;

    @Value("${DB_PORT:3306}")
    private String dbPort;

    @Value("${DB_NAME:finance}")
    private String dbName;

    @Value("${DB_USERNAME:}")
    private String dbUsername;

    @Value("${DB_PASSWORD:}")
    private String dbPassword;

    @Value("${KIS_APP_KEY:}")
    private String kisAppKey;

    @Value("${KIS_APP_SECRET:}")
    private String kisAppSecret;

    @Value("${USER_NAME:}")
    private String userName;

    @Value("${PASSWORD:}")
    private String password;

    @PostConstruct
    public void logEnvironmentVariables() {
        log.info("=== Environment Variables ===");
        log.info("DB_HOST: {}", dbHost);
        log.info("DB_PORT: {}", dbPort);
        log.info("DB_NAME: {}", dbName);
        log.info("DB_USERNAME: {}", dbUsername.isEmpty() ? "NOT_SET" : "SET");
        log.info("DB_PASSWORD: {}", dbPassword.isEmpty() ? "NOT_SET" : "SET");
        log.info("KIS_APP_KEY: {}", kisAppKey.isEmpty() ? "NOT_SET" : "SET");
        log.info("KIS_APP_SECRET: {}", kisAppSecret.isEmpty() ? "NOT_SET" : "SET");
        log.info("USER_NAME: {}", userName.isEmpty() ? "NOT_SET" : "SET");
        log.info("PASSWORD: {}", password.isEmpty() ? "NOT_SET" : "SET");
        log.info("=============================");
    }
} 