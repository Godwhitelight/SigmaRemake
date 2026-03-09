package io.github.sst.remake.manager.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import io.github.sst.remake.Client;
import io.github.sst.remake.data.profile.Profile;
import io.github.sst.remake.util.http.NetUtils;
import io.github.sst.remake.util.java.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class OnlineProfileManager {
    public final List<String> cachedProfileNames = new ArrayList<>();

    public void getOnlineProfileNames(ProfileNamesListener listener) {
        new Thread(() -> {
            if (this.cachedProfileNames.isEmpty())
                this.fetchOnlineProfileNames();

            listener.onProfileNamesReceived(cachedProfileNames);
        }).start();
    }

    public void fetchOnlineProfileNames() {
        try {
            HttpClient client = NetUtils.getHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://jelloconnect.sigmaclient.cloud/profiles.php?v=" + Client.VERSION + "remake"))
                    .timeout(Duration.ofSeconds(14))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String content = response.body();
            if (content != null && !content.isEmpty()) {
                JsonArray jsonArray = JsonParser.parseString(content).getAsJsonArray();
                for (int i = 0; i < jsonArray.size(); i++) {
                    cachedProfileNames.add(jsonArray.get(i).getAsString());
                }
            }
        } catch (Exception e) {
            Client.LOGGER.error("Failed to fetch online profiles", e);
        }
    }

    public JsonObject fetchOnlineProfileConfig(String profileName) {
        try {
            HttpClient client = NetUtils.getHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://jelloconnect.sigmaclient.cloud/profiles/" + StringUtils.encode(profileName) + ".profile?v=" + Client.VERSION + "remake"))
                    .timeout(Duration.ofSeconds(14))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String content = response.body();
            if (content != null && !content.isEmpty()) {
                return JsonParser.parseString(content).getAsJsonObject();
            }
        } catch (Exception e) {
            Client.LOGGER.error("Failed to fetch online profile by the name {}", profileName, e);
        }
        return new JsonObject();
    }

    public Profile downloadOnlineProfile(String name) {
        try {
            JsonObject config = fetchOnlineProfileConfig(name);
            if (config.size() != 0) {
                return new Profile(name, config);
            }
        } catch (JsonParseException e) {
            Client.LOGGER.error("Failed to parse profile configuration for {}", name, e);
        }
        return null;
    }

    public interface ProfileNamesListener {
        void onProfileNamesReceived(List<String> profileNames);
    }
}
