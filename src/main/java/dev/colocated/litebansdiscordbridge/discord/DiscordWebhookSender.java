package dev.colocated.litebansdiscordbridge.discord;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.colocated.litebansdiscordbridge.LiteBansDiscordBridge;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class DiscordWebhookSender {

    private final LiteBansDiscordBridge plugin;
    private final OkHttpClient httpClient;
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    public DiscordWebhookSender(LiteBansDiscordBridge plugin) {
        this.plugin = plugin;
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
            plugin.getLogger().warn("Webhook URL is not configured, skipping Discord notification");
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
            plugin.getLogger().warn("Webhook URL is not configured, skipping Discord notification");
            return;
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("content", content);

        sendWebhook(webhookUrl, payload.toString());
    }

    private void sendWebhook(String webhookUrl, String jsonPayload) {
        RequestBody body = RequestBody.create(jsonPayload, JSON);
        Request request = new Request.Builder()
            .url(webhookUrl)
            .post(body)
            .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                plugin.getLogger().error("Failed to send Discord webhook", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    plugin.getLogger().warn("Discord webhook returned non-successful response: {} - {}",
                        response.code(), response.body() != null ? response.body().string() : "No body");
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
