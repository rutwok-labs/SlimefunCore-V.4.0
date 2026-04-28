package io.github.thebusybiscuit.slimefun4.storage.migration;

import com.google.common.annotations.Beta;

import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.storage.Storage;
import io.github.thebusybiscuit.slimefun4.storage.data.PlayerData;

import org.apache.commons.lang.Validate;

import javax.annotation.Nonnull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Migrates player data from one storage backend to another.
 */
@Beta
public final class StorageMigrator {

    private static final Method MARK_ALL_DIRTY_METHOD = resolveMarkAllDirtyMethod();

    private StorageMigrator() {
    }

    /**
     * Migrates the given player UUIDs from one backend to another.
     *
     * @param source
     *            The source backend
     * @param destination
     *            The destination backend
     * @param playerUUIDs
     *            The players to migrate
     *
     * @return A {@link MigrationResult} describing the run
     */
    public static @Nonnull MigrationResult migrate(@Nonnull Storage source, @Nonnull Storage destination, @Nonnull Set<UUID> playerUUIDs) {
        Validate.notNull(source, "Source storage must not be null");
        Validate.notNull(destination, "Destination storage must not be null");
        Validate.notNull(playerUUIDs, "Player UUIDs must not be null");

        Instant startedAt = Instant.now();
        int succeeded = 0;
        int failed = 0;
        List<UUID> failedUUIDs = new ArrayList<>();

        for (UUID uuid : playerUUIDs) {
            try {
                PlayerData sourceData = source.loadPlayerData(uuid);
                PlayerData migratedData = sourceData.snapshot(false);
                markAllDirty(migratedData);
                destination.savePlayerData(uuid, migratedData);

                PlayerData destinationData = destination.loadPlayerData(uuid);
                if (
                    sourceData.getResearches().size() != destinationData.getResearches().size()
                    || sourceData.getBackpacks().size() != destinationData.getBackpacks().size()
                    || sourceData.getWaypoints().size() != destinationData.getWaypoints().size()
                ) {
                    failed++;
                    failedUUIDs.add(uuid);
                    Slimefun.logger().log(Level.WARNING, "Storage migration verification failed for player {0}", uuid);
                    continue;
                }

                succeeded++;
                Slimefun.logger().log(Level.FINE, "Successfully migrated player data for {0}", uuid);
            } catch (Exception x) {
                failed++;
                failedUUIDs.add(uuid);
                Slimefun.logger().log(Level.WARNING, x, () -> "Storage migration failed for player \"" + uuid + '"');
            }
        }

        Duration duration = Duration.between(startedAt, Instant.now());
        Slimefun.logger().log(Level.INFO, "Migration complete: {0} succeeded, {1} failed in {2} ms",
            new Object[] { succeeded, failed, duration.toMillis() });

        return new MigrationResult(succeeded, failed, List.copyOf(failedUUIDs), duration);
    }

    private static void markAllDirty(@Nonnull PlayerData data) {
        try {
            MARK_ALL_DIRTY_METHOD.invoke(data);
        } catch (IllegalAccessException | InvocationTargetException x) {
            throw new IllegalStateException("Could not mark migrated player data dirty", x);
        }
    }

    private static @Nonnull Method resolveMarkAllDirtyMethod() {
        try {
            Method method = PlayerData.class.getDeclaredMethod("markAllDirty");
            method.setAccessible(true);
            return method;
        } catch (ReflectiveOperationException x) {
            throw new ExceptionInInitializerError(x);
        }
    }

    /**
     * The result of a storage migration run.
     *
     * @param succeeded
     *            How many players migrated successfully
     * @param failed
     *            How many players failed migration
     * @param failedUUIDs
     *            The list of players that failed migration
     * @param duration
     *            The total duration of the migration
     */
    @Beta
    public record MigrationResult(int succeeded, int failed, List<UUID> failedUUIDs, Duration duration) {
    }
}
