package io.github.thebusybiscuit.slimefun4.core.services;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import io.github.bakedlibs.dough.updater.BlobBuildUpdater;
import org.bukkit.plugin.Plugin;

import io.github.bakedlibs.dough.config.Config;
import io.github.bakedlibs.dough.updater.PluginUpdater;
import io.github.bakedlibs.dough.versions.PrefixedVersion;
import io.github.thebusybiscuit.slimefun4.api.SlimefunBranch;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;

/**
 * This Class represents our {@link PluginUpdater} Service.
 * Official builds use blob.build for auto-download updates.
 * Unofficial builds only perform a safe GitHub release notification check.
 *
 * @author TheBusyBiscuit
 *
 */
public class UpdaterService {

    private static final String CUSTOM_RELEASE_REPOSITORY = "rutwok-labs/SlimefunCore-V.4.0";
    private static final URI CUSTOM_RELEASES_LATEST = URI.create("https://github.com/" + CUSTOM_RELEASE_REPOSITORY + "/releases/latest");
    private static final String USER_AGENT = "SlimefunCoreV4.0-Updater";
    private static final DateTimeFormatter BUILD_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final Pattern BUILD_TIMESTAMP_PATTERN = Pattern.compile("(\\d{14})(?=\\.jar$)");
    private static final Pattern RELEASE_DATETIME_PATTERN = Pattern.compile("datetime=\"([^\"]+)\"");
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();

    /**
     * Our {@link Slimefun} instance.
     */
    private final Slimefun plugin;

    /**
     * The current plugin jar file so unofficial builds can derive their local build number.
     */
    private final File pluginFile;

    /**
     * Our {@link PluginUpdater} implementation.
     */
    private final PluginUpdater<PrefixedVersion> updater;

    /**
     * The {@link SlimefunBranch} we are currently on.
     * If this is an official {@link SlimefunBranch}, auto updates will be enabled.
     */
    private final SlimefunBranch branch;

    /**
     * This will create a new {@link UpdaterService} for the given {@link Slimefun}.
     * The {@link File} should be the result of the getFile() operation of that {@link Plugin}.
     *
     * @param plugin
     *            The instance of Slimefun
     * @param version
     *            The current version of Slimefun
     * @param file
     *            The {@link File} of this {@link Plugin}
     */
    public UpdaterService(@Nonnull Slimefun plugin, @Nonnull String version, @Nonnull File file) {
        this.plugin = plugin;
        this.pluginFile = file;
        BlobBuildUpdater autoUpdater = null;

        if (version.contains("UNOFFICIAL")) {
            // This Server is using a modified build that is not a public release.
            branch = SlimefunBranch.UNOFFICIAL;
        } else if (version.startsWith("Dev - ")) {
            // If we are using a development build, we want to switch to our custom
            try {
                autoUpdater = new BlobBuildUpdater(plugin, file, "Slimefun4", "Dev");
            } catch (Exception x) {
                plugin.getLogger().log(Level.SEVERE, "Failed to create AutoUpdater", x);
            }

            branch = SlimefunBranch.DEVELOPMENT;
        } else if (version.startsWith("RC - ")) {
            // If we are using a "stable" build, we want to switch to our custom
            try {
                autoUpdater = new BlobBuildUpdater(plugin, file, "Slimefun4", "RC");
            } catch (Exception x) {
                plugin.getLogger().log(Level.SEVERE, "Failed to create AutoUpdater", x);
            }

            branch = SlimefunBranch.STABLE;
        } else {
            branch = SlimefunBranch.UNKNOWN;
        }

        this.updater = autoUpdater;
    }

    /**
     * This method returns the branch the current build of Slimefun is running on.
     * This can be used to determine whether we are dealing with an official build
     * or a build that was unofficially modified.
     *
     * @return The branch this build of Slimefun is on.
     */
    public @Nonnull SlimefunBranch getBranch() {
        return branch;
    }

    /**
     * This method returns the build number that this is running on (or -1 if unofficial).
     * You should combine the usage with {@link #getBranch()} in order to properly see if this is
     * a development or stable build number.
     *
     * @return The build number of this Slimefun.
     */
    public int getBuildNumber() {
        if (updater != null) {
            PrefixedVersion version = updater.getCurrentVersion();
            return version.getVersionNumber();
        }

        return -1;
    }

    public int getLatestVersion() {
        if (updater != null && updater.getLatestVersion().isDone()) {
            PrefixedVersion version;
            try {
                version = updater.getLatestVersion().get();
                return version.getVersionNumber();
            } catch (InterruptedException | ExecutionException e) {
                return -1;
            }
        }

        return -1;
    }

    public boolean isLatestVersion() {
        if (getBuildNumber() == -1 || getLatestVersion() == -1) {
            // We don't know if we're latest so just report we are
            return true;
        }
        
        return getBuildNumber() == getLatestVersion();
    }

    /**
     * This will start the {@link UpdaterService} and check for updates.
     * Official builds auto-download updates, unofficial builds only log newer releases.
     */
    public void start() {
        if (updater != null) {
            updater.start();
        } else {
            printBorder();
            plugin.getLogger().log(Level.WARNING, "It looks like you are using an unofficially modified build of Slimefun!");
            plugin.getLogger().log(Level.WARNING, "Auto-Downloads have been disabled, this build is not considered safe.");
            plugin.getLogger().log(Level.WARNING, "GitHub release notifications remain enabled for unofficial builds.");
            plugin.getLogger().log(Level.WARNING, "Do not report bugs encountered in this Version of Slimefun to any official sources.");
            printBorder();
            checkCustomReleaseAsync();
        }
    }

    /**
     * This returns whether the {@link PluginUpdater} is enabled or not.
     * This includes the {@link Config} setting but also whether or not we are running an
     * official or unofficial build.
     *
     * @return Whether the {@link PluginUpdater} is enabled
     */
    public boolean isEnabled() {
        return Slimefun.getCfg().getBoolean("options.auto-update") && updater != null;
    }

    /**
     * This method is called when the {@link UpdaterService} was disabled.
     */
    public void disable() {
        printBorder();
        plugin.getLogger().log(Level.WARNING, "It looks like you have disabled auto-updates for Slimefun!");
        plugin.getLogger().log(Level.WARNING, "Auto-Updates keep your server safe, performant and bug-free.");
        plugin.getLogger().log(Level.WARNING, "We respect your decision.");

        if (branch != SlimefunBranch.STABLE) {
            plugin.getLogger().log(Level.WARNING, "If you are just scared of Slimefun breaking, then please consider using a \"stable\" build instead of disabling auto-updates.");
        }

        printBorder();
    }

    private void printBorder() {
        plugin.getLogger().log(Level.WARNING, "#######################################################");
    }

    /**
     * Compatibility note: unofficial builds now perform a release-notification check
     * against the custom GitHub repository instead of attempting an auto-download.
     */
    private void checkCustomReleaseAsync() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, this::checkCustomRelease);
    }

    private void checkCustomRelease() {
        try {
            ReleaseInfo latestRelease = fetchLatestCustomRelease();
            Instant currentBuild = parseBuildInstant(pluginFile.getName());

            if (currentBuild == null || latestRelease == null || !latestRelease.publishedAt().isAfter(currentBuild)) {
                return;
            }

            printBorder();
            plugin.getLogger().log(Level.WARNING, "A newer SlimefunCore release is available on GitHub.");
            plugin.getLogger().log(Level.WARNING, "Current build: {0}", pluginFile.getName());
            plugin.getLogger().log(Level.WARNING, "Latest tag: {0}", latestRelease.tag());
            plugin.getLogger().log(Level.WARNING, "Published: {0}", LocalDateTime.ofInstant(latestRelease.publishedAt(), ZoneId.systemDefault()));
            plugin.getLogger().log(Level.WARNING, "Download: {0}", latestRelease.url());
            printBorder();
        } catch (IOException | InterruptedException x) {
            plugin.getLogger().log(Level.WARNING, "Failed to check unofficial GitHub releases: " + x.getMessage());

            if (x instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private @Nullable ReleaseInfo fetchLatestCustomRelease() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(CUSTOM_RELEASES_LATEST)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "text/html")
            .GET()
            .build();
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (response.statusCode() >= 400) {
            return null;
        }

        Matcher matcher = RELEASE_DATETIME_PATTERN.matcher(response.body());

        if (!matcher.find()) {
            return null;
        }

        Instant publishedAt = Instant.parse(matcher.group(1));
        String path = response.uri().getPath();
        String tag = path.substring(path.lastIndexOf('/') + 1);
        return new ReleaseInfo(tag, response.uri().toString(), publishedAt);
    }

    private @Nullable Instant parseBuildInstant(@Nonnull String fileName) {
        Matcher matcher = BUILD_TIMESTAMP_PATTERN.matcher(fileName);

        if (!matcher.find()) {
            return null;
        }

        try {
            LocalDateTime timestamp = LocalDateTime.parse(matcher.group(1), BUILD_TIMESTAMP_FORMATTER);
            return timestamp.atZone(ZoneId.systemDefault()).toInstant();
        } catch (DateTimeParseException x) {
            return null;
        }
    }

    private record ReleaseInfo(@Nonnull String tag, @Nonnull String url, @Nonnull Instant publishedAt) {}

}
