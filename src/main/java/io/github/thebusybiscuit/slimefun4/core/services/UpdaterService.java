package io.github.thebusybiscuit.slimefun4.core.services;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
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
    private static final URI CUSTOM_RELEASES_LATEST = URI.create("https://api.github.com/repos/" + CUSTOM_RELEASE_REPOSITORY + "/releases/latest");
    private static final String USER_AGENT = "SlimefunCoreV4.0-Updater";
    private static final Pattern BUILD_NUMBER_PATTERN = Pattern.compile("(\\d{4})(?=\\.jar$)");
    private static final Pattern TAG_NAME_PATTERN = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern HTML_URL_PATTERN = Pattern.compile("\"html_url\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern PUBLISHED_AT_PATTERN = Pattern.compile("\"published_at\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern ASSET_PATTERN = Pattern.compile("\\{[^\\{\\}]*\"name\"\\s*:\\s*\"([^\"]+)\"[^\\{\\}]*\"browser_download_url\"\\s*:\\s*\"([^\"]+)\"[^\\{\\}]*\\}", Pattern.DOTALL);
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
     * Official builds auto-download updates, unofficial builds check GitHub releases.
     */
    public void start() {
        if (updater != null) {
            updater.start();
        } else {
            printBorder();
            plugin.getLogger().log(Level.WARNING, "It looks like you are using an unofficially modified build of Slimefun!");
            plugin.getLogger().log(Level.WARNING, "GitHub release checks are enabled for this unofficial build.");
            plugin.getLogger().log(Level.WARNING, "When a newer release is found it will be downloaded for the next restart.");
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
        return Slimefun.getCfg().getBoolean("options.auto-update");
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
     * Compatibility note: unofficial builds now check the custom GitHub repository
     * and stage the newest jar in the server update folder for the next restart.
     */
    private void checkCustomReleaseAsync() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, this::checkCustomRelease);
    }

    private void checkCustomRelease() {
        try {
            ReleaseInfo latestRelease = fetchLatestCustomRelease();
            Integer currentBuild = parseBuildNumber(pluginFile.getName());

            if (latestRelease == null) {
                plugin.getLogger().log(Level.INFO, "Could not determine the latest GitHub release for this unofficial SlimefunCore build.");
                return;
            }

            if (currentBuild == null || latestRelease.buildNumber() == null) {
                plugin.getLogger().log(Level.INFO, "Could not compare build numbers for automatic updates.");
                plugin.getLogger().log(Level.INFO, "Current jar: {0}", pluginFile.getName());
                plugin.getLogger().log(Level.INFO, "Latest GitHub release tag: {0}", latestRelease.tag());
                plugin.getLogger().log(Level.INFO, "Latest GitHub release: {0}", latestRelease.url());
                return;
            }

            if (latestRelease.buildNumber() > currentBuild) {
                printBorder();
                plugin.getLogger().log(Level.WARNING, "A newer SlimefunCore release is available on GitHub.");
                plugin.getLogger().log(Level.WARNING, "Current build: {0}", pluginFile.getName());
                plugin.getLogger().log(Level.WARNING, "Latest asset: {0}", latestRelease.assetName());
                plugin.getLogger().log(Level.WARNING, "Latest tag: {0}", latestRelease.tag());
                plugin.getLogger().log(Level.WARNING, "Published: {0}", latestRelease.publishedAt());
                plugin.getLogger().log(Level.WARNING, "Download: {0}", latestRelease.url());

                if (downloadLatestRelease(latestRelease)) {
                    plugin.getLogger().log(Level.WARNING, "The new jar was downloaded into the update folder and will apply on the next restart.");
                }

                printBorder();
                return;
            }

            plugin.getLogger().log(Level.INFO, "SlimefunCore is already on the latest GitHub release.");
            plugin.getLogger().log(Level.INFO, "Current build: {0}", pluginFile.getName());
            plugin.getLogger().log(Level.INFO, "Latest asset: {0}", latestRelease.assetName());
            plugin.getLogger().log(Level.INFO, "Latest tag: {0}", latestRelease.tag());
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
            .header("Accept", "application/vnd.github+json")
            .GET()
            .build();
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (response.statusCode() >= 400) {
            return null;
        }

        String body = response.body();
        String tag = matchFirst(body, TAG_NAME_PATTERN);
        String htmlUrl = matchFirst(body, HTML_URL_PATTERN);
        String publishedAtRaw = matchFirst(body, PUBLISHED_AT_PATTERN);

        if (tag == null || htmlUrl == null || publishedAtRaw == null) {
            return null;
        }

        Matcher assetMatcher = ASSET_PATTERN.matcher(body);
        String assetName = null;
        String downloadUrl = null;

        while (assetMatcher.find()) {
            String candidateName = assetMatcher.group(1);
            String candidateUrl = assetMatcher.group(2);

            if (candidateName.startsWith("SlimefunCore4-") && candidateName.endsWith(".jar") && !candidateName.endsWith("-sources.jar")) {
                assetName = candidateName;
                downloadUrl = candidateUrl;
                break;
            }
        }

        if (assetName == null || downloadUrl == null) {
            return null;
        }

        Integer buildNumber = parseBuildNumber(assetName);
        return new ReleaseInfo(tag, htmlUrl, Instant.parse(publishedAtRaw), assetName, downloadUrl, buildNumber);
    }

    private @Nullable Integer parseBuildNumber(@Nonnull String fileName) {
        Matcher matcher = BUILD_NUMBER_PATTERN.matcher(fileName);

        if (!matcher.find()) {
            return null;
        }

        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException x) {
            return null;
        }
    }

    private @Nullable String matchFirst(@Nonnull String input, @Nonnull Pattern pattern) {
        Matcher matcher = pattern.matcher(input);
        return matcher.find() ? matcher.group(1) : null;
    }

    private boolean downloadLatestRelease(@Nonnull ReleaseInfo latestRelease) {
        try {
            File updateFolder = plugin.getServer().getUpdateFolderFile();

            if (!updateFolder.exists() && !updateFolder.mkdirs()) {
                plugin.getLogger().log(Level.WARNING, "Could not create the server update folder: {0}", updateFolder.getAbsolutePath());
                return false;
            }

            Path target = updateFolder.toPath().resolve(latestRelease.assetName());

            // Compatibility note: always download through a temporary file so a partial transfer never becomes the live update.
            Path temporary = Files.createTempFile(updateFolder.toPath(), "slimefun-update-", ".jar");

            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(latestRelease.downloadUrl()))
                    .header("User-Agent", USER_AGENT)
                    .GET()
                    .build();

                HttpResponse<Path> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofFile(temporary));

                if (response.statusCode() >= 400) {
                    plugin.getLogger().log(Level.WARNING, "Failed to download the latest GitHub release asset. Response code: {0}", response.statusCode());
                    Files.deleteIfExists(temporary);
                    return false;
                }

                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
                return true;
            } finally {
                Files.deleteIfExists(temporary);
            }
        } catch (IOException | InterruptedException x) {
            plugin.getLogger().log(Level.WARNING, "Failed to download the latest unofficial GitHub release: " + x.getMessage());

            if (x instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }

            return false;
        }
    }

    private record ReleaseInfo(@Nonnull String tag, @Nonnull String url, @Nonnull Instant publishedAt, @Nonnull String assetName, @Nonnull String downloadUrl, @Nullable Integer buildNumber) {}

}
