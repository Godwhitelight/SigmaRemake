package io.github.sst.remake.util.http.ms;

import com.google.gson.*;
import com.sun.net.httpserver.HttpServer;
import io.github.sst.remake.util.http.NetUtils;
import net.minecraft.client.session.Session;
import net.minecraft.util.Util;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class MicrosoftLoginUtils {
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    public static final String CLIENT_ID = "42a60a84-599d-44b2-a7c6-b00cdef1d6a2";
    public static final int PORT = 25575;

    private static String formEncode(Map<String, String> params) {
        return params.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }

    private static Map<String, String> parseQuery(String query) {
        if (query == null || query.isEmpty()) return Collections.emptyMap();
        Map<String, String> map = new LinkedHashMap<>();
        for (String pair : query.split("&")) {
            int idx = pair.indexOf('=');
            if (idx >= 0) {
                String key = java.net.URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                String value = java.net.URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                map.put(key, value);
            }
        }
        return map;
    }

    public static CompletableFuture<String> acquireMSAuthCode(
            final Executor executor
    ) {
        return acquireMSAuthCode(Util.getOperatingSystem()::open, executor);
    }

    public static CompletableFuture<String> acquireMSAuthCode(
            final Consumer<URI> browserAction,
            final Executor executor
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                final String state = RandomStringUtils.randomAlphanumeric(8);

                final HttpServer server = HttpServer.create(
                        new InetSocketAddress(PORT), 0
                );

                final CountDownLatch latch = new CountDownLatch(1);
                final AtomicReference<String> authCode = new AtomicReference<>(null),
                        errorMsg = new AtomicReference<>(null);

                server.createContext("/callback", exchange -> {
                    String rawQuery = exchange.getRequestURI().getQuery();
                    if (rawQuery == null) {
                        String fullUri = exchange.getRequestURI().toString();
                        int qIdx = fullUri.indexOf('?');
                        if (qIdx >= 0) rawQuery = fullUri.substring(qIdx + 1);
                    }
                    final Map<String, String> query = parseQuery(rawQuery);

                    if (!state.equals(query.get("state"))) {
                        errorMsg.set(
                                String.format("State mismatch! Expected '%s' but got '%s'.", state, query.get("state"))
                        );
                    } else if (query.containsKey("code")) {
                        authCode.set(query.get("code"));
                    } else if (query.containsKey("error")) {
                        errorMsg.set(String.format("%s: %s", query.get("error"), query.get("error_description")));
                    }

                    NetUtils.sendClasspathResource(exchange, "/assets/sigma/callback.html", MicrosoftLoginUtils.class);

                    latch.countDown();
                });

                server.createContext("/callback.css", exchange -> {
                    NetUtils.sendClasspathResource(exchange, "/assets/sigma/callback.css", MicrosoftLoginUtils.class);
                });

                server.createContext("/assets/", exchange -> {
                    String path = exchange.getRequestURI().getPath();
                    if (!NetUtils.isSafePath(path, "/assets/")) {
                        NetUtils.sendNotFound(exchange);
                        return;
                    }

                    NetUtils.sendClasspathResource(exchange, path, MicrosoftLoginUtils.class);
                });

                String redirectUri = String.format("http://localhost:%d/callback", server.getAddress().getPort());
                Map<String, String> authParams = new LinkedHashMap<>();
                authParams.put("client_id", CLIENT_ID);
                authParams.put("response_type", "code");
                authParams.put("redirect_uri", redirectUri);
                authParams.put("scope", "XboxLive.signin XboxLive.offline_access");
                authParams.put("state", state);
                authParams.put("prompt", "select_account");
                final URI uri = URI.create("https://login.live.com/oauth20_authorize.srf?" + formEncode(authParams));

                browserAction.accept(uri);

                try {
                    server.start();

                    latch.await();

                    return Optional.ofNullable(authCode.get())
                            .filter(code -> !StringUtils.isBlank(code))
                            .orElseThrow(() -> new Exception(
                                    Optional.ofNullable(errorMsg.get())
                                            .orElse("There was no auth code or error description present.")
                            ));
                } finally {
                    new Thread(() -> {
                        try {
                            Thread.sleep(1500);
                        } catch (InterruptedException ignored) {
                        }
                        server.stop(2);
                    }, "MsAuthCallbackServerStopper").start();
                }
            } catch (InterruptedException e) {
                throw new CancellationException("Microsoft auth code acquisition was cancelled!");
            } catch (Exception e) {
                throw new CompletionException("Unable to acquire Microsoft auth code!", e);
            }
        }, executor);
    }

    public static CompletableFuture<String> acquireMSAccessToken(
            final String authCode,
            final Executor executor
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpClient client = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();

                Map<String, String> form = new LinkedHashMap<>();
                form.put("client_id", CLIENT_ID);
                form.put("grant_type", "authorization_code");
                form.put("code", authCode);
                form.put("redirect_uri", String.format("http://localhost:%d/callback", PORT));

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://login.live.com/oauth20_token.srf"))
                        .timeout(TIMEOUT)
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(formEncode(form)))
                        .build();

                HttpResponse<String> res = client.send(request, HttpResponse.BodyHandlers.ofString());

                final JsonObject json = JsonParser.parseString(res.body()).getAsJsonObject();
                return Optional.ofNullable(json.get("access_token"))
                        .map(JsonElement::getAsString)
                        .filter(token -> !StringUtils.isBlank(token))
                        .orElseThrow(() -> new Exception(
                                json.has("error") ? String.format(
                                        "%s: %s",
                                        json.get("error").getAsString(),
                                        json.get("error_description").getAsString()
                                ) : "There was no access token or error description present."
                        ));
            } catch (InterruptedException e) {
                throw new CancellationException("Microsoft access token acquisition was cancelled!");
            } catch (Exception e) {
                throw new CompletionException("Unable to acquire Microsoft access token!", e);
            }
        }, executor);
    }

    public static CompletableFuture<String> acquireXboxAccessToken(
            final String accessToken,
            final Executor executor
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpClient client = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();

                final JsonObject entity = new JsonObject();
                final JsonObject properties = new JsonObject();
                properties.addProperty("AuthMethod", "RPS");
                properties.addProperty("SiteName", "user.auth.xboxlive.com");
                properties.addProperty("RpsTicket", String.format("d=%s", accessToken));
                entity.add("Properties", properties);
                entity.addProperty("RelyingParty", "http://auth.xboxlive.com");
                entity.addProperty("TokenType", "JWT");

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://user.auth.xboxlive.com/user/authenticate"))
                        .timeout(TIMEOUT)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(entity.toString()))
                        .build();

                HttpResponse<String> res = client.send(request, HttpResponse.BodyHandlers.ofString());

                final JsonObject json = res.statusCode() == 200
                        ? JsonParser.parseString(res.body()).getAsJsonObject()
                        : new JsonObject();
                return Optional.ofNullable(json.get("Token"))
                        .map(JsonElement::getAsString)
                        .filter(token -> !StringUtils.isBlank(token))
                        .orElseThrow(() -> new Exception(
                                json.has("XErr") ? String.format(
                                        "%s: %s", json.get("XErr").getAsString(), json.get("Message").getAsString()
                                ) : "There was no access token or error description present."
                        ));
            } catch (InterruptedException e) {
                throw new CancellationException("Xbox Live access token acquisition was cancelled!");
            } catch (Exception e) {
                throw new CompletionException("Unable to acquire Xbox Live access token!", e);
            }
        }, executor);
    }

    public static CompletableFuture<Map<String, String>> acquireXboxXstsToken(
            final String accessToken,
            final Executor executor
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpClient client = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();

                final JsonObject entity = new JsonObject();
                final JsonObject properties = new JsonObject();
                final JsonArray userTokens = new JsonArray();
                userTokens.add(new JsonPrimitive(accessToken));
                properties.addProperty("SandboxId", "RETAIL");
                properties.add("UserTokens", userTokens);
                entity.add("Properties", properties);
                entity.addProperty("RelyingParty", "rp://api.minecraftservices.com/");
                entity.addProperty("TokenType", "JWT");

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://xsts.auth.xboxlive.com/xsts/authorize"))
                        .timeout(TIMEOUT)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(entity.toString()))
                        .build();

                HttpResponse<String> res = client.send(request, HttpResponse.BodyHandlers.ofString());

                final JsonObject json = res.statusCode() == 200
                        ? JsonParser.parseString(res.body()).getAsJsonObject()
                        : new JsonObject();
                return Optional.ofNullable(json.get("Token"))
                        .map(JsonElement::getAsString)
                        .filter(token -> !StringUtils.isBlank(token))
                        .map(token -> {
                            final String uhs = json.get("DisplayClaims").getAsJsonObject()
                                    .get("xui").getAsJsonArray()
                                    .get(0).getAsJsonObject()
                                    .get("uhs").getAsString();

                            Map<String, String> result = new HashMap<>();
                            result.put("Token", token);
                            result.put("uhs", uhs);
                            return result;
                        })
                        .orElseThrow(() -> new Exception(
                                json.has("XErr") ? String.format(
                                        "%s: %s", json.get("XErr").getAsString(), json.get("Message").getAsString()
                                ) : "There was no access token or error description present."
                        ));
            } catch (InterruptedException e) {
                throw new CancellationException("Xbox Live XSTS token acquisition was cancelled!");
            } catch (Exception e) {
                throw new CompletionException("Unable to acquire Xbox Live XSTS token!", e);
            }
        }, executor);
    }

    public static CompletableFuture<String> acquireMCAccessToken(
            final String xstsToken,
            final String userHash,
            final Executor executor
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpClient client = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.minecraftservices.com/authentication/login_with_xbox"))
                        .timeout(TIMEOUT)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(
                                String.format("{\"identityToken\": \"XBL3.0 x=%s;%s\"}", userHash, xstsToken)
                        ))
                        .build();

                HttpResponse<String> res = client.send(request, HttpResponse.BodyHandlers.ofString());

                final JsonObject json = JsonParser.parseString(res.body()).getAsJsonObject();

                return Optional.ofNullable(json.get("access_token"))
                        .map(JsonElement::getAsString)
                        .filter(token -> !StringUtils.isBlank(token))
                        .orElseThrow(() -> new Exception(
                                json.has("error") ? String.format(
                                        "%s: %s", json.get("error").getAsString(), json.get("errorMessage").getAsString()
                                ) : "There was no access token or error description present."
                        ));
            } catch (InterruptedException e) {
                throw new CancellationException("Minecraft access token acquisition was cancelled!");
            } catch (Exception e) {
                throw new CompletionException("Unable to acquire Minecraft access token!", e);
            }
        }, executor);
    }

    public static CompletableFuture<Session> login(
            final String mcToken,
            final Executor executor
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpClient client = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.minecraftservices.com/minecraft/profile"))
                        .timeout(TIMEOUT)
                        .header("Authorization", "Bearer " + mcToken)
                        .GET()
                        .build();

                HttpResponse<String> res = client.send(request, HttpResponse.BodyHandlers.ofString());

                final JsonObject json = JsonParser.parseString(res.body()).getAsJsonObject();
                return Optional.ofNullable(json.get("id"))
                        .map(JsonElement::getAsString)
                        .filter(uuid -> !StringUtils.isBlank(uuid))
                        .map(uuid -> new Session(
                                json.get("name").getAsString(),
                                uuid,
                                mcToken,
                                Optional.empty(),
                                Optional.empty(),
                                Session.AccountType.MSA
                        ))
                        .orElseThrow(() -> new Exception(
                                json.has("error") ? String.format(
                                        "%s: %s", json.get("error").getAsString(), json.get("errorMessage").getAsString()
                                ) : "There was no profile or error description present."
                        ));
            } catch (InterruptedException e) {
                throw new CancellationException("Minecraft profile fetching was cancelled!");
            } catch (Exception e) {
                throw new CompletionException("Unable to fetch Minecraft profile!", e);
            }
        }, executor);
    }
}
