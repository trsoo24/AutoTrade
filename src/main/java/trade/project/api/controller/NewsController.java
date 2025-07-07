package trade.project.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
} 