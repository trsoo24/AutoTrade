package trade.project.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import trade.project.api.client.NaverNewsApiClient;

@RestController
@RequestMapping("/api/naver/news")
@RequiredArgsConstructor
public class NaverNewsController {

    private final NaverNewsApiClient naverNewsApiClient;

    @GetMapping
    public ResponseEntity<String> getNaverNews() {
        String newsJson = naverNewsApiClient.searchNews();
        return ResponseEntity.ok(newsJson);
    }
} 