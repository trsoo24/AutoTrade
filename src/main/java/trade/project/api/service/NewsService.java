package trade.project.api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import trade.project.api.entity.NewsData;
import trade.project.api.repository.NewsRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NewsService {
    private final NewsRepository newsRepository;

    public List<NewsData> findKoreaNewsData() {
        return newsRepository.findNewsDataByNationOrderByCreateDt("KR");
    }

    public List<NewsData> findOtherNewsData() {
        return newsRepository.findNewsDataByNationOrderByCreateDt("US");
    }
}
