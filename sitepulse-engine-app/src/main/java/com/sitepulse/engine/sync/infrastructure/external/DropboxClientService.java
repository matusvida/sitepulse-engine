package com.sitepulse.engine.sync.infrastructure.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sitepulse.engine.common.exception.ConfigurationException;
import com.sitepulse.engine.common.exception.ExternalServiceException;
import com.sitepulse.engine.config.SitePulseProperties;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DropboxClientService {

    private static final String API_BASE = "https://api.dropboxapi.com/2";
    private static final String CONTENT_BASE = "https://content.dropboxapi.com/2";
    private static final String OAUTH_BASE = "https://api.dropboxapi.com/oauth2/token";

    private final SitePulseProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public DropboxClientService(SitePulseProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
    }

    public List<String> listSubfolders(String dropboxUrl) {
        ParsedDropboxUrl parsed = parseSharedLinkUrl(dropboxUrl);
        log.info("Listing Dropbox subfolders baseUrl={} subfolder={}", parsed.baseUrl(), parsed.subfolder());
        JsonNode response = callApi("/files/list_folder", Map.of(
                "path", parsed.subfolder(),
                "shared_link", Map.of("url", parsed.baseUrl())
        ));
        List<String> folders = new ArrayList<>();
        response.path("entries").forEach(entry -> {
            if ("folder".equals(entry.path(".tag").asText())) {
                folders.add(entry.path("name").asText());
            }
        });
        folders.sort(String::compareTo);
        log.info("Listed {} Dropbox subfolders for baseUrl={}", folders.size(), parsed.baseUrl());
        return folders;
    }

    public List<DropboxFileEntry> listFiles(String dropboxUrl, String subfolderName) {
        ParsedDropboxUrl parsed = parseSharedLinkUrl(dropboxUrl);
        String folderPath = parsed.subfolder().isBlank() ? "/" + subfolderName : parsed.subfolder() + "/" + subfolderName;
        log.info("Listing Dropbox files baseUrl={} folderPath={}", parsed.baseUrl(), folderPath);
        JsonNode response = callApi("/files/list_folder", Map.of(
                "path", folderPath,
                "shared_link", Map.of("url", parsed.baseUrl())
        ));
        List<DropboxFileEntry> files = new ArrayList<>();
        response.path("entries").forEach(entry -> {
            String name = entry.path("name").asText();
            if ("file".equals(entry.path(".tag").asText()) && isImage(name)) {
                files.add(new DropboxFileEntry(name, folderPath + "/" + name, entry.path("size").asLong()));
            }
        });
        log.info("Listed {} Dropbox image files for folderPath={}", files.size(), folderPath);
        return files;
    }

    public byte[] downloadFile(String dropboxUrl, String relativePath) {
        try {
            log.info("Downloading Dropbox file url={} path={}", dropboxUrl, relativePath);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(CONTENT_BASE + "/sharing/get_shared_link_file"))
                    .timeout(Duration.ofSeconds(120))
                    .header("Authorization", "Bearer " + resolveAccessToken())
                    .header("Dropbox-API-Arg", objectMapper.writeValueAsString(Map.of("url", parseSharedLinkUrl(dropboxUrl).baseUrl(), "path", relativePath)))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() >= 400) {
                log.warn("Dropbox file download failed url={} path={} status={}", dropboxUrl, relativePath, response.statusCode());
                throw new ExternalServiceException("Failed to download Dropbox file: HTTP " + response.statusCode());
            }
            log.info("Downloaded Dropbox file url={} path={} bytes={}", dropboxUrl, relativePath, response.body().length);
            return response.body();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.error("Dropbox file download interrupted url={} path={}", dropboxUrl, relativePath, ex);
            throw new ExternalServiceException("Failed to download Dropbox file", ex);
        } catch (IOException ex) {
            log.error("Dropbox file download IO failure url={} path={} reason={}", dropboxUrl, relativePath, ex.getMessage(), ex);
            throw new ExternalServiceException("Failed to download Dropbox file", ex);
        }
    }

    private JsonNode callApi(String path, Object body) {
        try {
            log.info("Calling Dropbox API path={}", path);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE + path))
                    .timeout(Duration.ofSeconds(60))
                    .header("Authorization", "Bearer " + resolveAccessToken())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                log.warn("Dropbox API call failed path={} status={} body={}", path, response.statusCode(), summarizeBody(response.body()));
                throw new ExternalServiceException("Dropbox API call failed: HTTP " + response.statusCode() + " - " + summarizeBody(response.body()));
            }
            log.info("Dropbox API call completed path={} status={}", path, response.statusCode());
            return objectMapper.readTree(response.body());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.error("Dropbox API call interrupted path={}", path, ex);
            throw new ExternalServiceException("Dropbox API call failed", ex);
        } catch (IOException ex) {
            log.error("Dropbox API call IO failure path={} reason={}", path, ex.getMessage(), ex);
            throw new ExternalServiceException("Dropbox API call failed", ex);
        }
    }

    private String resolveAccessToken() {
        if (properties.dropboxRefreshToken() != null && !properties.dropboxRefreshToken().isBlank()) {
            return refreshAccessToken();
        }
        if (properties.dropboxToken() == null || properties.dropboxToken().isBlank()) {
            throw new ConfigurationException("Dropbox credentials are not configured");
        }
        return properties.dropboxToken();
    }

    private String refreshAccessToken() {
        if (properties.dropboxAppKey() == null || properties.dropboxAppKey().isBlank()
                || properties.dropboxAppSecret() == null || properties.dropboxAppSecret().isBlank()) {
            throw new ConfigurationException("DROPBOX_APP_KEY and DROPBOX_APP_SECRET are required with DROPBOX_REFRESH_TOKEN");
        }
        log.info("Refreshing Dropbox access token using refresh token flow");
        String form = "grant_type=refresh_token"
                + "&refresh_token=" + encode(properties.dropboxRefreshToken())
                + "&client_id=" + encode(properties.dropboxAppKey())
                + "&client_secret=" + encode(properties.dropboxAppSecret());
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OAUTH_BASE))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                log.warn("Dropbox token refresh failed status={} body={}", response.statusCode(), summarizeBody(response.body()));
                throw new ExternalServiceException("Dropbox token refresh failed: HTTP " + response.statusCode() + " - " + summarizeBody(response.body()));
            }
            JsonNode payload = objectMapper.readTree(response.body());
            String accessToken = payload.path("access_token").asText("");
            if (accessToken.isBlank()) {
                throw new ExternalServiceException("Dropbox token refresh failed: access_token missing");
            }
            log.info("Dropbox access token refresh completed successfully");
            return accessToken;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.error("Dropbox token refresh interrupted", ex);
            throw new ExternalServiceException("Dropbox token refresh failed", ex);
        } catch (IOException ex) {
            log.error("Dropbox token refresh IO failure reason={}", ex.getMessage(), ex);
            throw new ExternalServiceException("Dropbox token refresh failed", ex);
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String summarizeBody(String body) {
        if (body == null || body.isBlank()) {
            return "<empty>";
        }
        String normalized = body.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 300 ? normalized : normalized.substring(0, 300) + "...";
    }

    private boolean isImage(String name) {
        String lower = name.toLowerCase();
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png");
    }

    private ParsedDropboxUrl parseSharedLinkUrl(String url) {
        String base = url;
        String subfolder = "";
        int queryIndex = url.indexOf('?');
        String beforeQuery = queryIndex >= 0 ? url.substring(0, queryIndex) : url;
        String query = queryIndex >= 0 ? url.substring(queryIndex) : "";
        String[] parts = beforeQuery.split("/");
        if (parts.length >= 7 && "scl".equals(parts[3]) && "fo".equals(parts[4])) {
            base = String.join("/", parts[0], "", parts[2], parts[3], parts[4], parts[5], parts[6]) + query;
            if (parts.length > 7) {
                StringBuilder builder = new StringBuilder();
                for (int i = 7; i < parts.length; i++) {
                    builder.append('/').append(parts[i]);
                }
                subfolder = builder.toString();
            }
        }
        return new ParsedDropboxUrl(base, subfolder);
    }

    private record ParsedDropboxUrl(String baseUrl, String subfolder) {
    }

    public record DropboxFileEntry(String name, String path, long size) {
    }
}
