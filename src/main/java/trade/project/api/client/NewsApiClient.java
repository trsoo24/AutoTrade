package trade.project.api.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class NewsApiClient {

    @Value("${newsapi.api.key}")
    private String apiKey;

    private static final String NEWS_API_URL = "https://newsapi.org/v2/everything";

    public String searchNews() {
        RestTemplate restTemplate = new RestTemplate();

        Integer page = 1;
        Integer pageSize = 10;

        String url = UriComponentsBuilder.fromHttpUrl(NEWS_API_URL)
                .queryParam("q", "주식")
                .queryParam("language", "ko")
                .queryParam("apiKey", apiKey)
                .queryParam("page", page)
                .queryParam("pageSize", pageSize)
                .build()
                .toUriString();

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                String.class
        );

        return response.getBody();
    }
} 