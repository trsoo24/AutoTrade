package trade.project.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import trade.project.api.client.NewsApiClient;

@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsApiClient newsApiClient;

    @GetMapping
    public ResponseEntity<String> getNews() {
        String newsJson = newsApiClient.searchNews();
        return ResponseEntity.ok(newsJson);
    }

    // Toss 에서 주요 뉴스 긁어와서 없을 시 DB에 저장
    @Scheduled(cron = "0 10 * * * *")
    public void getNewsToss() {
        newsApiClient.searchTosNewsToss();
    }
} 