package lk.sampath.leaderboard.controller;

import lk.sampath.leaderboard.entity.SonarQubeConfig;
import lk.sampath.leaderboard.services.SonarQubeConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class SonarQubeConfigController {

    private final SonarQubeConfigService configService;

    @GetMapping("/admin/sonarqube-config")
    public String getConfigPage(Model model) {
        Optional<SonarQubeConfig> cfgOpt = configService.getLatestConfig();
        if (cfgOpt.isPresent()) {
            SonarQubeConfig cfg = cfgOpt.get();
            model.addAttribute("baseUrl", cfg.getBaseUrl());
            model.addAttribute("createdAt", cfg.getCreatedAt());
            model.addAttribute("updatedAt", cfg.getUpdatedAt());
            // Mask token for display
            String token = cfg.getApiToken();
            if (token != null && !token.isBlank()) {
                model.addAttribute("apiTokenMasked", maskToken(token));
            } else {
                model.addAttribute("apiTokenMasked", "");
            }
        } else {
            model.addAttribute("baseUrl", "");
            model.addAttribute("apiTokenMasked", "");
            model.addAttribute("createdAt", null);
            model.addAttribute("updatedAt", null);
        }
        return "sonar-config";
    }

    @PostMapping("/admin/sonarqube-config")
    public String saveConfig(@RequestParam("baseUrl") String baseUrl,
                             @RequestParam(value = "apiToken", required = false) String apiToken,
                             RedirectAttributes redirectAttributes) {
        try {
            configService.saveConfig(baseUrl, apiToken);
            redirectAttributes.addFlashAttribute("success", "SonarQube configuration saved");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to save configuration: " + e.getMessage());
        }
        return "redirect:/admin/sonarqube-config";
    }

    private String maskToken(String token) {
        if (token == null) return "";
        int len = token.length();
        if (len <= 6) return "******";
        String visible = token.substring(len - 4);
        return "******" + visible;
    }
}

