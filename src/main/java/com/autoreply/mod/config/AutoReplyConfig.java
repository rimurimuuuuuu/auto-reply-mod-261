package com.autoreply.mod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Path;

public class AutoReplyConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("auto-reply.json");

    public String geminiApiKey = "YOUR_GEMINI_API_KEY_HERE";
    public String systemPrompt = "あなたはMinecraftプレイヤーです。チャットで来たメッセージに対して、短く自然な日本語で返信してください。返信は1-2文以内にしてください。";
    public boolean autoReplyEnabled = false;
    public int replyDelaySeconds = 2;

    public static AutoReplyConfig load() {
        if (CONFIG_PATH.toFile().exists()) {
            try (Reader reader = new FileReader(CONFIG_PATH.toFile())) {
                AutoReplyConfig config = GSON.fromJson(reader, AutoReplyConfig.class);
                return config != null ? config : new AutoReplyConfig();
            } catch (Exception e) {
                return new AutoReplyConfig();
            }
        }
        AutoReplyConfig config = new AutoReplyConfig();
        config.save();
        return config;
    }

    public void save() {
        try (Writer writer = new FileWriter(CONFIG_PATH.toFile())) {
            GSON.toJson(this, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
