package lk.sampath.leaderboard.controller;

import lk.sampath.leaderboard.dto.ImportResponse;
import lk.sampath.leaderboard.services.ImportFromJsonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

@RestController
@RequestMapping("/api/import")
@CrossOrigin(origins = "*")
public class ImportFromJsonController {

    @Autowired
    private ImportFromJsonService importFromJsonService;


    @PostMapping("/issues")
    public ResponseEntity<ImportResponse> importIssues(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "files", required = false) MultipartFile[] files,
            @RequestParam(value = "url", required = false) String url,
            @RequestParam(value = "urls", required = false) List<String> urls
    ) {
        return processImport("issues", file, files, url, urls);
    }

    @PostMapping("/developers")
    public ResponseEntity<ImportResponse> importDevelopers(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "files", required = false) MultipartFile[] files,
            @RequestParam(value = "url", required = false) String url,
            @RequestParam(value = "urls", required = false) List<String> urls
    ) {
        return processImport("developers", file, files, url, urls);
    }

    @PostMapping("/projects")
    public ResponseEntity<ImportResponse> importProjects(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "files", required = false) MultipartFile[] files,
            @RequestParam(value = "url", required = false) String url,
            @RequestParam(value = "urls", required = false) List<String> urls
    ) {
        return processImport("projects", file, files, url, urls);
    }



    private ResponseEntity<ImportResponse> processImport(
            String type,
            MultipartFile file,
            MultipartFile[] files,
            String url,
            List<String> urls
    ) {
        List<String> errors = new ArrayList<>();
        int totalImported = 0;

        try {
            List<MultipartFile> fileList = new ArrayList<>();
            if (files != null) fileList.addAll(Arrays.asList(files));
            if (file != null) fileList.add(file);

            // Normalize URL list
            List<String> urlList = new ArrayList<>();
            if (urls != null) urlList.addAll(urls);
            if (url != null && !url.isBlank()) urlList.add(url);


            for (MultipartFile f : fileList) {
                if (!isValidJsonFile(f)) {
                    errors.add("Invalid file: " + (f == null ? "(null)" : f.getOriginalFilename()));
                    continue;
                }

                ImportResponse resp = importJson(type, f);
                if (resp != null) {
                    totalImported += resp.getImportedCount();
                    if (resp.getErrors() != null) errors.addAll(resp.getErrors());
                }
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
                    new ImportResponse(success, msg, totalImported, errors.isEmpty() ? null : errors)
            );

        } catch (Exception ex) {
            return ResponseEntity.status(500)
                    .body(new ImportResponse(false,
                            "Import failed: " + ex.getMessage(),
                            0,
                            List.of(ex.getMessage())));
        }
    }

    private ImportResponse importJson(String type, MultipartFile file) throws Exception {
        return switch (type) {
            case "issues" -> importFromJsonService.importIssuesFromJson(file);
            case "developers" -> importFromJsonService.importDevelopersFromJson(file);
            case "projects" -> importFromJsonService.importProjectsFromJson(file);
            default -> throw new IllegalArgumentException("Unknown type: " + type);
        };
    }

    private ImportResponse importJson(String type, InputStream input) throws Exception {
        return switch (type) {
            case "issues" -> importFromJsonService.importIssuesFromJson(input);
            case "developers" -> importFromJsonService.importDevelopersFromJson(input);
            case "projects" -> importFromJsonService.importProjectsFromJson(input);
            default -> throw new IllegalArgumentException("Unknown type: " + type);
        };
    }

    // -------------------------------------------------------------------
    // VALIDATION HELPERS
    // -------------------------------------------------------------------

    private boolean isValidJsonFile(MultipartFile file) {
        if (file == null || file.isEmpty()) return false;
        String name = file.getOriginalFilename();
        return name != null && name.toLowerCase().endsWith(".json");
    }

    private InputStream resolveInputStream(String pathOrUrl) throws Exception {
        if (pathOrUrl == null || pathOrUrl.isBlank())
            throw new IllegalArgumentException("Path or URL is empty");

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
        if (file.exists() && file.isFile())
            return new FileInputStream(file);

        throw new FileNotFoundException("Not a valid URL or file path: " + pathOrUrl);
    }
}
