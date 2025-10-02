package trade.project.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import trade.project.api.dto.NewsResponseDto;
import trade.project.api.entity.NewsData;

import java.util.List;

@Repository
public interface NewsRepository extends JpaRepository<NewsData, Long> {
    void insertNews(NewsResponseDto newsResponseDto);
    List<NewsData> findNewsDataByNationOrderByCreateDt(String nation);
}
