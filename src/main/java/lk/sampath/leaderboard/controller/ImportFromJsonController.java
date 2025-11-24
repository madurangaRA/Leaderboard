package lk.sampath.leaderboard.controller;

import lk.sampath.leaderboard.dto.ImportResponse;
import lk.sampath.leaderboard.services.ImportFromJsonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

@RestController
@RequestMapping("/import")
@CrossOrigin(origins = "*")
public class ImportFromJsonController {

    @Autowired
    private ImportFromJsonService importFromJsonService;

    // ----- IMPORT ISSUES -----
    @PostMapping("/issues")
    public ResponseEntity<ImportResponse> importIssues(
            @RequestParam(value = "url", required = false) String url,
            @RequestParam(value = "urls", required = false) List<String> urls
    ) {
        return processImport("issues", url, urls);
    }

    // ----- IMPORT DEVELOPERS -----
    @PostMapping("/developers")
    public ResponseEntity<ImportResponse> importDevelopers(
            @RequestParam(value = "url", required = false) String url,
            @RequestParam(value = "urls", required = false) List<String> urls
    ) {
        return processImport("developers", url, urls);
    }

    // ----- IMPORT PROJECTS -----
    @PostMapping("/projects")
    public ResponseEntity<ImportResponse> importProjects(
            @RequestParam(value = "url", required = false) String url,
            @RequestParam(value = "urls", required = false) List<String> urls
    ) {
        return processImport("projects", url, urls);
    }


    // ========================================================================================
    //                          MAIN URL-BASED IMPORT HANDLER
    // ========================================================================================
    private ResponseEntity<ImportResponse> processImport(
            String type,
            String url,
            List<String> urls
    ) {
        List<String> errors = new ArrayList<>();
        int totalImported = 0;

        try {
            // Combine url + urls into a single normalized list
            List<String> urlList = new ArrayList<>();
            if (urls != null) urlList.addAll(urls);
            if (url != null && !url.isBlank()) urlList.add(url);

            if (urlList.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new ImportResponse(false, "No URLs provided", 0, List.of("Provide url or urls"))
                );
            }

            for (String source : urlList) {
                try (InputStream in = resolveInputStream(source)) {

                    ImportResponse resp = importJson(type, in);
                    if (resp != null) {
                        totalImported += resp.getImportedCount();
                        if (resp.getErrors() != null) errors.addAll(resp.getErrors());
                    }

                } catch (Exception ex) {
                    errors.add("Failed to load '" + source + "': " + ex.getMessage());
                }
            }

            boolean success = totalImported > 0;
            String msg = totalImported + " " + type + " imported";

            return ResponseEntity.ok(
                    new ImportResponse(
                            success,
                            msg,
                            totalImported,
                            errors.isEmpty() ? null : errors)
            );

        } catch (Exception ex) {
            return ResponseEntity.status(500)
                    .body(new ImportResponse(
                            false,
                            "Import failed: " + ex.getMessage(),
                            0,
                            List.of(ex.getMessage()))
                    );
        }
    }


    // ========================================================================================
    //                                 SERVICE HANDLERS
    // ========================================================================================
    private ImportResponse importJson(String type, InputStream input) throws Exception {
        return switch (type) {
            case "issues" -> importFromJsonService.importIssuesFromJson(input);
            case "developers" -> importFromJsonService.importDevelopersFromJson(input);
            case "projects" -> importFromJsonService.importProjectsFromJson(input);
            default -> throw new IllegalArgumentException("Unknown type: " + type);
        };
    }


    // ========================================================================================
    //                              URL/FILE RESOLUTION
    // ========================================================================================
    private InputStream resolveInputStream(String pathOrUrl) throws Exception {
        if (pathOrUrl == null || pathOrUrl.isBlank()) {
            throw new IllegalArgumentException("URL is empty");
        }

        if (pathOrUrl.startsWith("http://") || pathOrUrl.startsWith("https://")) {
            URLConnection conn = new URL(pathOrUrl).openConnection();
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(60000);
            return conn.getInputStream();
        }

        if (pathOrUrl.startsWith("file:")) {
            return new URL(pathOrUrl).openStream();
        }

        File file = new File(pathOrUrl);
        if (file.exists() && file.isFile()) {
            return new FileInputStream(file);
        }

        throw new FileNotFoundException("Not a valid URL or file path: " + pathOrUrl);
    }
}