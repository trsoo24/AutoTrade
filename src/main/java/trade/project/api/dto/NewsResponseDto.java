package trade.project.api.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsResponseDto {
    private LocalDateTime createDt;
    private String newsId;
    private String title;
    private String contentText;
    private String imageUrl;
    private String agencyName;
    private String nation;
}
