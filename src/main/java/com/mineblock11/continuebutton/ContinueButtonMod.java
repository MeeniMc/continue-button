package com.mineblock11.continuebutton;

import com.google.common.io.Files;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.util.WorldSavePath;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;

public class ContinueButtonMod implements ClientModInitializer {
    private static final boolean isInServerList = false;
    public static boolean lastLocal = true;
    public static String serverName = "";
    public static String serverAddress = "";

    static {
        File configDir = new File(FabricLoader.getInstance().getConfigDir().toFile(), "continuebutton");

        if (!configDir.exists()) {
            if (!configDir.mkdir()) {
            }
        }

        File configFile = new File(configDir, "config.properties");
        Properties properties = new Properties();

        if (configFile.exists()) {
            try (FileInputStream stream = new FileInputStream(configFile)) {
                properties.load(stream);
            } catch (IOException e) {
            }
        }

        lastLocal = (properties.computeIfAbsent("last-local", (a) -> "true")).equals("true");
        serverName = ((String) properties.computeIfAbsent("server-name", (a) -> ""));
        serverAddress = ((String) properties.computeIfAbsent("server-address", (a) -> ""));

        try (FileOutputStream stream = new FileOutputStream(configFile)) {
            properties.store(stream, "ContinueButton config");
        } catch (IOException e) {
        }
    }

    public static void saveConfig() {
        File configDir = new File(FabricLoader.getInstance().getConfigDir().toFile(), "continuebutton");
        File configFile = new File(configDir, "config.properties");
        Properties properties = new Properties();
        try (FileInputStream stream = new FileInputStream(configFile)) {
            properties.load(stream);
        } catch (IOException e) {
        }
        properties.setProperty("last-local", lastLocal ? "true" : "false");
        properties.setProperty("server-name", serverName);
        properties.setProperty("server-address", serverAddress);
        try (FileOutputStream stream = new FileOutputStream(configFile)) {
            properties.store(stream, "ContinueButton config");
        } catch (IOException e) {
        }
    }

    @Override
    public void onInitializeClient() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (client.isIntegratedServerRunning()) {
                // Singleplayer
                lastLocal = true;
                String levelName = client.getServer().getSaveProperties().getLevelName();
                Path pathtoSave = Path.of(Files.simplifyPath(client.getServer().getSavePath(WorldSavePath.ROOT).toString()));
                String folderName = pathtoSave.normalize().toFile().getName();
                serverName = levelName;
                serverAddress = folderName;
            } else {
                ServerInfo serverInfo = client.getCurrentServerEntry();
                lastLocal = false;
                serverName = serverInfo.name;
                serverAddress = serverInfo.address;
            }
            saveConfig();
        });
    }
}
