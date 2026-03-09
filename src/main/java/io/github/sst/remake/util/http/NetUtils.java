package io.github.sst.remake.util.http;

import com.sun.net.httpserver.HttpExchange;
import io.github.sst.remake.Client;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class NetUtils {
    public static final String USER_AGENT = "SigmaRemake/" + Client.VERSION;

    private static final HttpClient SHARED_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(14))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public static HttpClient getHttpClient() {
        return SHARED_CLIENT;
    }

    public static InputStream getInputStreamFromURL(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("User-Agent", USER_AGENT);
        connection.setRequestMethod("GET");
        connection.setDoInput(true);
        connection.connect();

        if (connection.getResponseCode() != 200) {
            throw new IOException("Failed to load image, HTTP response code: " + connection.getResponseCode());
        }

        return new BufferedInputStream(connection.getInputStream());
    }

    public static String getStringFromURL(String urlString) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(urlString))
                    .timeout(Duration.ofSeconds(14))
                    .header("User-Agent", "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)")
                    .GET()
                    .build();

            HttpResponse<String> response = SHARED_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body() != null ? response.body() : "";
        } catch (Exception e) {
            return "";
        }
    }

    public static URLConnection getConnection(URL url) throws IOException {
        URLConnection connection = url.openConnection();
        connection.setConnectTimeout(14000);
        connection.setReadTimeout(14000);
        connection.setUseCaches(true);

        connection.setRequestProperty("Connection", "Keep-Alive");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");

        return connection;
    }

    public static boolean isSafePath(String path, String prefix) {
        return path != null && path.startsWith(prefix) && !path.contains("..");
    }

    public static void sendNotFound(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(404, -1);
        exchange.close();
    }

    public static void sendClasspathResource(HttpExchange exchange, String path, Class<?> resourceClass) throws IOException {
        try (InputStream stream = resourceClass.getResourceAsStream(path)) {
            if (stream == null) {
                sendNotFound(exchange);
                return;
            }

            byte[] response = stream.readAllBytes();
            exchange.getResponseHeaders().add("Content-Type", getContentType(path));
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.getResponseBody().close();
        }
    }

    public static String getContentType(String path) {
        String lower = path.toLowerCase();
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (lower.endsWith(".gif")) {
            return "image/gif";
        }
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        if (lower.endsWith(".svg")) {
            return "image/svg+xml";
        }
        if (lower.endsWith(".ttf")) {
            return "font/ttf";
        }
        if (lower.endsWith(".otf")) {
            return "font/otf";
        }
        if (lower.endsWith(".woff")) {
            return "font/woff";
        }
        if (lower.endsWith(".woff2")) {
            return "font/woff2";
        }
        if (lower.endsWith(".css")) {
            return "text/css; charset=utf-8";
        }
        if (lower.endsWith(".js")) {
            return "application/javascript; charset=utf-8";
        }
        if (lower.endsWith(".html")) {
            return "text/html; charset=utf-8";
        }
        return "application/octet-stream";
    }
}
