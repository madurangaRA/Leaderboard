package lk.sampath.leaderboard.services;

import lk.sampath.leaderboard.entity.SonarQubeConfig;
import lk.sampath.leaderboard.repository.SonarQubeConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SonarQubeConfigService {

    private final SonarQubeConfigRepository repository;

    public Optional<SonarQubeConfig> getLatestConfig() {
        return repository.findTopByOrderByUpdatedAtDesc();
    }

    @Transactional
    public SonarQubeConfig saveConfig(String baseUrl, String apiToken) {
        // If apiToken is blank, preserve previous token if present
        String tokenToSave = apiToken;
        if (tokenToSave == null || tokenToSave.isBlank()) {
            Optional<SonarQubeConfig> prev = repository.findTopByOrderByUpdatedAtDesc();
            if (prev.isPresent()) {
                tokenToSave = prev.get().getApiToken();
            } else {
                tokenToSave = null;
            }
        }

        SonarQubeConfig cfg = SonarQubeConfig.builder()
                .baseUrl(baseUrl)
                .apiToken(tokenToSave)
                .build();
        return repository.save(cfg);
    }
}
