package lk.sampath.leaderboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImportResponse {
    private boolean success;
    private String message;
    private int importedCount;
    private List<String> errors;
}