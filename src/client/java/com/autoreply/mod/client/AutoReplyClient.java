package com.autoreply.mod.client;

import com.autoreply.mod.config.AutoReplyConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutoReplyClient implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("autoreplymod");
    public static AutoReplyConfig CONFIG;

    private boolean wasPressed = false;
    private boolean isReplying = false;

    private static final Pattern CHAT_PATTERN = Pattern.compile("^<([^>]+)>\\s+(.+)$");
    private static final String TRIGGER = "-rimuIA";

    @Override
    public void onInitializeClient() {
        CONFIG = AutoReplyConfig.load();

        // ;キーでオン/オフ
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            if (client.currentScreen != null) return;

            boolean isPressed = GLFW.glfwGetKey(client.getWindow().getHandle(), GLFW.GLFW_KEY_SEMICOLON) == GLFW.GLFW_PRESS;

            if (isPressed && !wasPressed) {
                CONFIG.autoReplyEnabled = !CONFIG.autoReplyEnabled;
                CONFIG.save();
                String status = CONFIG.autoReplyEnabled
                        ? "§a[rimuIA] 自動返信: ON (;キーで切替)"
                        : "§c[rimuIA] 自動返信: OFF (;キーで切替)";
                client.player.sendMessage(Text.literal(status), true);
            }
            wasPressed = isPressed;
        });

        // チャット受信時
        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
            if (!CONFIG.autoReplyEnabled) return;
            if (isReplying) return;

            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null) return;

            String rawMessage = message.getString();
            String myName = client.player.getName().getString();

            if (rawMessage.contains("<" + myName + ">")) return;

            Matcher matcher = CHAT_PATTERN.matcher(rawMessage);
            if (!matcher.matches()) return;

            String senderName = matcher.group(1);
            String chatContent = matcher.group(2).trim();

            if (senderName.equals(myName)) return;

            // -rimuIA で始まるメッセージのみ処理
            if (!chatContent.startsWith(TRIGGER)) return;

            String actualMessage = chatContent.substring(TRIGGER.length()).trim();

            LOGGER.info("[rimuIA] Triggered by {}: {}", senderName, actualMessage);

            isReplying = true;
            client.player.sendMessage(Text.literal("§7[rimuIA] 返信を考えています..."), true);

            String prompt = String.format(
                "%sというプレイヤーから「%s」というメッセージが来ました。返信してください。",
                senderName, actualMessage.isEmpty() ? "（メッセージなし）" : actualMessage
            );

            GeminiClient.generateReply(CONFIG.geminiApiKey, CONFIG.systemPrompt, prompt)
                    .orTimeout(20, TimeUnit.SECONDS)
                    .whenComplete((reply, error) -> {
                        isReplying = false;
                        if (reply == null || error != null) {
                            client.execute(() -> {
                                if (client.player != null)
                                    client.player.sendMessage(Text.literal("§c[rimuIA] 返信の生成に失敗しました"), true);
                            });
                            return;
                        }

                        try { Thread.sleep(CONFIG.replyDelaySeconds * 1000L); }
                        catch (InterruptedException ignored) {}

                        client.execute(() -> {
                            if (client.player != null && client.getNetworkHandler() != null) {
                                client.getNetworkHandler().sendChatMessage(reply);
                                client.player.sendMessage(Text.literal("§a[rimuIA] 送信: " + reply), true);
                            }
                        });
                    });
        });

        LOGGER.info("[rimuIA] Auto Reply Mod initialized! Press ; to toggle.");
    }
}
