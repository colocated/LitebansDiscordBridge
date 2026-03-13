package dev.colocated.litebansdiscordbridge.discord;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.colocated.litebansdiscordbridge.platform.PlatformAdapter;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class DiscordWebhookSender {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final PlatformAdapter platform;
    private final OkHttpClient httpClient;

    public DiscordWebhookSender(PlatformAdapter platform) {
        this.platform = platform;
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build();
    }

    public void sendEmbed(String webhookUrl, DiscordEmbed embed) {
        sendEmbed(webhookUrl, embed, null);
    }

    public void sendEmbed(String webhookUrl, DiscordEmbed embed, String content) {
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            platform.logWarn("Webhook URL is not configured, skipping Discord notification");
            return;
        }

        JsonObject payload = new JsonObject();

        if (content != null && !content.isEmpty()) {
            payload.addProperty("content", content);
        }

        JsonArray embeds = new JsonArray();
        embeds.add(embed.toJson());
        payload.add("embeds", embeds);

        sendWebhook(webhookUrl, payload.toString());
    }

    public void sendMessage(String webhookUrl, String content) {
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            platform.logWarn("Webhook URL is not configured, skipping Discord notification");
            return;
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("content", content);

        sendWebhook(webhookUrl, payload.toString());
    }

    private void sendWebhook(final String webhookUrl, String jsonPayload) {
        RequestBody body = RequestBody.create(jsonPayload, JSON);
        Request request = new Request.Builder()
            .url(webhookUrl)
            .post(body)
            .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                platform.logError("Failed to send Discord webhook to " + webhookUrl, e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String bodyStr = response.body() != null ? response.body().string() : "No body";
                    platform.logWarn("Discord webhook returned non-successful response: "
                        + response.code() + " - " + bodyStr);
                }
                response.close();
            }
        });
    }

    public void shutdown() {
        httpClient.dispatcher().executorService().shutdown();
        httpClient.connectionPool().evictAll();
    }
}
