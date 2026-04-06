package io.github.thebusybiscuit.slimefun4.core.services;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;

import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.utils.NumberUtils;

/**
 * Embedded bStats service for Slimefun.
 * <p>
 * Compatibility note: this replaces the old runtime-downloaded MetricsModule flow
 * so modern custom builds can ship a single self-contained jar.
 */
public class MetricsService {

    private static final int BSTATS_PLUGIN_ID = 30622;

    private final Slimefun plugin;

    private @Nullable Metrics metrics;
    private @Nullable String metricVersion;

    public MetricsService(@Nonnull Slimefun plugin) {
        this.plugin = plugin;
    }

    /**
     * Starts embedded bStats metrics for this Slimefun build.
     */
    public void start() {
        if (plugin.isUnitTest()) {
            metricVersion = "disabled";
            return;
        }

        if (metrics != null) {
            return;
        }

        try {
            metrics = new Metrics(plugin, BSTATS_PLUGIN_ID);

            // Compatibility note: keep charts lightweight and safe so metrics never block startup.
            metrics.addCustomChart(new SimplePie("distribution", () -> "SlimefunCoreV4.0"));
            metrics.addCustomChart(new SimplePie("build_channel", () -> "4.0-UNOFFICIAL"));
            metrics.addCustomChart(new SimplePie("minecraft_version", () -> Slimefun.getMinecraftVersion().getName()));
            metrics.addCustomChart(new SimplePie("slimefun_version", Slimefun::getVersion));
            metrics.addCustomChart(new SimplePie("server_software", Bukkit::getName));
            metrics.addCustomChart(new SimplePie("java_version", () -> String.valueOf(NumberUtils.getJavaVersion())));

            metricVersion = String.valueOf(BSTATS_PLUGIN_ID);
            plugin.getLogger().info("Embedded bStats started. Plugin ID: " + BSTATS_PLUGIN_ID);
        } catch (LinkageError | RuntimeException x) {
            metrics = null;
            metricVersion = null;
            plugin.getLogger().warning("Failed to start embedded bStats metrics: " + x.getMessage());
        }
    }

    /**
     * Cleans up the in-memory Metrics reference.
     */
    public void cleanUp() {
        metrics = null;
    }

    /**
     * Legacy compatibility shim. Embedded bStats does not download separate updates.
     *
     * @return always false
     */
    public boolean checkForUpdate(@Nullable String currentVersion) {
        return false;
    }

    /**
     * Returns the active bStats plugin id when metrics are running.
     */
    public @Nullable String getVersion() {
        return metricVersion;
    }

    /**
     * Legacy compatibility shim. Embedded bStats has no separate auto-update module.
     *
     * @return always false
     */
    public boolean hasAutoUpdates() {
        return false;
    }
}
