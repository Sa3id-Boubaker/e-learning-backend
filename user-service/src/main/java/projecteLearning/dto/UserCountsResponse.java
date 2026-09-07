package projecteLearning.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserCountsResponse {
    private long studentsCount;
    private long trainersCount;
    private long adminsCount;
    private long totalCount;
}