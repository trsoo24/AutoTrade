package trade.project.api.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import trade.project.api.client.NewsApiClient;
import trade.project.api.entity.NewsData;
import trade.project.api.service.NewsService;

import java.util.List;

@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsApiClient newsApiClient;
    private final NewsService newsService;

    @GetMapping
    public ResponseEntity<String> getNews() {
        String newsJson = newsApiClient.searchNews();
        return ResponseEntity.ok(newsJson);
    }

    // Toss 에서 주요 뉴스 긁어와서 없을 시 DB에 저장
    @Scheduled(cron = "0 10 * * * *")
    public void getNewsToss() throws JsonProcessingException {
        newsApiClient.searchTosNewsToss();
    }

    @GetMapping("/kr")
    public ResponseEntity<List<NewsData>> getKRNewsData() {
        return ResponseEntity.ok(newsService.findKoreaNewsData());
    }

    @GetMapping("/us")
    public ResponseEntity<List<NewsData>> getUSNewsData() {
        return ResponseEntity.ok(newsService.findOtherNewsData());
    }
} 