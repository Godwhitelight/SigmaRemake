package io.github.sst.remake.util.client.yt;

import com.jfposton.ytdlp.YtDlp;
import com.jfposton.ytdlp.YtDlpException;
import com.jfposton.ytdlp.YtDlpRequest;
import com.jfposton.ytdlp.YtDlpResponse;
import io.github.sst.remake.Client;
import io.github.sst.remake.util.client.ConfigUtils;
import io.github.sst.remake.util.system.io.FileUtils;
import io.github.sst.remake.util.java.RandomUtils;
import io.github.sst.remake.util.java.StringUtils;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class YtDlpUtils {
    private static volatile boolean prepared = false;
    private static volatile String prepareError = null;

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(14))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    private static final HttpClient DOWNLOAD_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public static boolean isPrepared() {
        return prepared;
    }

    public static String getPrepareError() {
        return prepareError;
    }

    public static URL resolveStream(String songUrl) {
        if (songUrl == null) {
            Client.LOGGER.error("Failed to play song, url is null");
            return null;
        }
        return tryResolveStream(songUrl, "18", false);
    }

    public static URL resolveFallbackStream(String songUrl) {
        if (songUrl == null) {
            Client.LOGGER.error("Failed to play song, url is null");
            return null;
        }

        String[] formatCandidates = new String[]{
                "140",
                "bestaudio[ext=m4a]",
                "bestaudio[acodec^=mp4a]",
                "best[ext=mp4][acodec^=mp4a]"
        };

        for (String format : formatCandidates) {
            URL resolved = tryResolveStream(songUrl, format, true);
            if (resolved != null) {
                return resolved;
            }
        }

        return null;
    }

    private static URL tryResolveStream(String songUrl, String format, boolean useExtractorArgs) {
        YtDlpRequest request = new YtDlpRequest(songUrl, ConfigUtils.MUSIC_FOLDER.getAbsolutePath());
        request.addOption("no-check-certificates");
        request.addOption("rm-cache-dir");
        request.addOption("get-url");
        request.addOption("retries", 10);
        if (useExtractorArgs) {
            request.addOption("extractor-args", "youtube:player_client=android,ios,web");
        }
        request.addOption("format", format);

        try {
            YtDlpResponse response = YtDlp.execute(request);
            String output = response.getOut();
            if (output == null || output.trim().isEmpty()) {
                return null;
            }

            String[] lines = output.split("\\R");
            String streamUrl = null;
            for (String line : lines) {
                if (line != null && !line.trim().isEmpty()) {
                    streamUrl = line.trim();
                    break;
                }
            }

            if (streamUrl == null) {
                return null;
            }

            return new URL(streamUrl);
        } catch (YtDlpException | MalformedURLException e) {
            return null;
        }
    }

    public static void prepareExecutable() {
        prepared = false;
        prepareError = null;

        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        String assetName = isWindows ? "yt-dlp.exe" : "yt-dlp";

        File targetFile = new File(ConfigUtils.MUSIC_FOLDER, assetName);
        File versionFile = new File(ConfigUtils.MUSIC_FOLDER, "yt-dlp.version");

        if (targetFile.getParentFile() != null) {
            targetFile.getParentFile().mkdirs();
        }

        String latestBaseUrl = null;
        try {
            latestBaseUrl = resolveLatestAssetBaseUrl(assetName);
        } catch (Exception e) {
            Client.LOGGER.warn("Failed to resolve latest YT-DLP asset URL (will fallback to current file if present)", e);
        }

        boolean needsDownload = !targetFile.exists();

        if (!needsDownload && latestBaseUrl != null) {
            String saved = null;
            if (versionFile.exists()) {
                String content = FileUtils.readFile(versionFile).trim();
                if (!content.isEmpty()) {
                    saved = content;
                }
            }

            if (saved == null || !saved.equals(latestBaseUrl)) {
                Client.LOGGER.info("YT-DLP appears outdated (saved={}, latest={}), downloading...", saved, latestBaseUrl);
                needsDownload = true;
            }
        }

        if (needsDownload) {
            Client.LOGGER.info("Downloading YT-DLP...");
            boolean downloaded = downloadYtDlp(targetFile, assetName);
            if (downloaded && latestBaseUrl != null) {
                FileUtils.writeFile(versionFile, latestBaseUrl);
            } else if (!downloaded && !targetFile.exists()) {
                prepareError = "download_failed";
            }
        }

        if (!isWindows && targetFile.exists()) {
            targetFile.setExecutable(true);
        }

        boolean ready = targetFile.exists() && targetFile.length() > 0;
        if (!isWindows && ready) {
            ready = targetFile.canExecute() || targetFile.setExecutable(true);
        }

        prepared = ready;

        if (prepared) {
            YtDlp.setExecutablePath(targetFile.getAbsolutePath());
        } else {
            if (prepareError == null) {
                prepareError = "missing_executable";
            }
            Client.LOGGER.warn("YT-DLP executable not available; music playback will not work.");
        }
    }

    private static boolean downloadYtDlp(File targetFile, String assetName) {
        String downloadUrl = "https://github.com/yt-dlp/yt-dlp/releases/latest/download/" + assetName;

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(downloadUrl))
                    .timeout(Duration.ofSeconds(60))
                    .GET()
                    .build();

            HttpResponse<InputStream> response = DOWNLOAD_CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                Client.LOGGER.error("Failed to download YT-DLP: HTTP {}", response.statusCode());
                return false;
            }

            try (InputStream body = response.body();
                 FileOutputStream outputStream = new FileOutputStream(targetFile)) {
                body.transferTo(outputStream);
            }
            return true;
        } catch (Exception e) {
            Client.LOGGER.error("Failed to download YT-DLP", e);
            return false;
        }
    }

    private static String resolveLatestAssetBaseUrl(String assetName) throws Exception {
        String url = "https://github.com/yt-dlp/yt-dlp/releases/latest/download/" + assetName;

        for (int i = 0; i < 10; i++) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<Void> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.discarding());
            int code = response.statusCode();

            if (code >= 300 && code < 400) {
                java.util.Optional<String> location = response.headers().firstValue("Location");
                if (location.isEmpty()) {
                    break;
                }
                url = location.get();
                continue;
            }

            return StringUtils.stripQuery(url);
        }

        return StringUtils.stripQuery(url);
    }
}
