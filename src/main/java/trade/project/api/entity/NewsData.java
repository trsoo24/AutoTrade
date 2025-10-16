package trade.project.api.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;
import trade.project.api.dto.NewsResponseDto;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private LocalDateTime createDt;
    private String newsId;
    private String title;
    private String contentText;
    private String imageUrl;
    private String agencyName;
    private String nation;

    public static NewsData toEntity(NewsResponseDto dto) {
        return NewsData.builder()
                .createDt(dto.getCreateDt())
                .newsId(dto.getNewsId())
                .title(dto.getTitle())
                .contentText(dto.getContentText())
                .imageUrl(dto.getImageUrl())
                .nation(dto.getNation())
                .build();
    }
}
