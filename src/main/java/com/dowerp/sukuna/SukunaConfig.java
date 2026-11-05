package com.dowerp.sukuna;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public final class SukunaConfig {
    public boolean blockDamage = true;
    public boolean breakHardBlocks = false;
    public boolean cutBlockEntities = false;
    public boolean damagePlayers = true;
    public boolean domainBuildsShrine = true;
    public boolean domainRestoresTerrain = true;
    public boolean domainThunder = true;
    public boolean freeCursedEnergy = false;
    public int domainRadius = 28;
    public int domainClosedRadius = 18;
    public int domainDurationTicks = 480;
    public int worldSlashLength = 96;
    public int worldSlashHeight = 56;
    public int worldSlashChargeTicks = 40;
    public int mahoragaLifetimeTicks = 2400;
    public int adaptationSteps = 8;
    public int adaptationDelayTicks = 12;
    public double maxHardness = 6.0;
    public double damageMultiplier = 1.0;
    public double ceRegenMultiplier = 1.0;
    public boolean legacyBindings = false;
    public boolean domainMeterEnabled = true;
    public boolean lowHpAwaken = true;
    public int purpleChargeTicks = 20;
    public double bluePullRadius = 6.0;
    public int awakenDurationTicks = 500;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static SukunaConfig load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("sukuna.json");
        SukunaConfig cfg = new SukunaConfig();
        try {
            if (Files.exists(path)) {
                SukunaConfig read = GSON.fromJson(Files.readString(path), SukunaConfig.class);
                if (read != null) {
                    cfg = read;
                }
            }
            Files.writeString(path, GSON.toJson(cfg));
        } catch (Exception e) {
            SukunaMod.LOGGER.warn("Config error: {}", e.toString());
        }
        cfg.adaptationSteps = Math.max(2, cfg.adaptationSteps);
        cfg.domainRadius = Math.max(8, Math.min(64, cfg.domainRadius));
        cfg.purpleChargeTicks = Math.max(5, Math.min(100, cfg.purpleChargeTicks));
        cfg.bluePullRadius = Math.max(2.0, Math.min(16.0, cfg.bluePullRadius));
        cfg.awakenDurationTicks = Math.max(100, Math.min(2400, cfg.awakenDurationTicks));
        cfg.domainClosedRadius = Math.max(12, Math.min(24, cfg.domainClosedRadius));
        return cfg;
    }
}
