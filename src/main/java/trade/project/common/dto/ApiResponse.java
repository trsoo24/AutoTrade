package trade.project.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private String status;
    private String message;
    private T data;
    
    @JsonProperty("error_code")
    private String errorCode;
    
    @JsonProperty("error_message")
    private String errorMessage;
    
    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setStatus("success");
        response.setMessage("요청이 성공적으로 처리되었습니다.");
        response.setData(data);
        return response;
    }
    
    public static <T> ApiResponse<T> error(String errorCode, String errorMessage) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setStatus("error");
        response.setMessage("요청 처리 중 오류가 발생했습니다.");
        response.setErrorCode(errorCode);
        response.setErrorMessage(errorMessage);
        return response;
    }
} 