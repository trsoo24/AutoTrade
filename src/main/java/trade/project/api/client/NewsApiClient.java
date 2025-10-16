package trade.project.api.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import trade.project.api.dto.NewsResponseDto;
import trade.project.api.entity.NewsData;
import trade.project.api.repository.NewsRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class NewsApiClient {
    @Value("${newsapi.api.key}")
    private String apiKey;

    private static final String NEWS_API_URL = "https://newsapi.org/v2/everything";
    private static final String TOSS_NEWS_API_URL = "https://wts-info-api.tossinvest.com/api/v2/forum/news/headline";

    private final NewsRepository newsRepository;

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

    // 토스 뉴스
    public void searchTosNewsToss() throws JsonProcessingException {
        RestTemplate restTemplate = new RestTemplate();

        String url = UriComponentsBuilder.fromHttpUrl(TOSS_NEWS_API_URL)
                .toUriString();

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                String.class
        );

        ObjectMapper mapper = new ObjectMapper();

        JsonNode newsArray = mapper.readTree(response.getBody())
                .path("result")
                .path("news");

        List<NewsResponseDto> responseDtoList = mapper.convertValue(newsArray, new TypeReference<>() {});
        List<NewsData> dataList = new ArrayList<>();

        for (NewsResponseDto newsDto : responseDtoList) {
            if (newsDto.getNewsId() != null && !existNews(newsDto.getNewsId())) {
                NewsData newsData = NewsData.toEntity(newsDto);
                dataList.add(newsData);
            }
        }

        newsRepository.saveAll(dataList);
    }

    private boolean existNews(String id) {
        return newsRepository.existsByNewsId(id);
    }
}