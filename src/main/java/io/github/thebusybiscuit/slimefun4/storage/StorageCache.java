package io.github.thebusybiscuit.slimefun4.storage;

import com.google.common.annotations.Beta;

import io.github.thebusybiscuit.slimefun4.storage.data.PlayerData;

import org.apache.commons.lang.Validate;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A caching {@link Storage} decorator that keeps player data in memory and
 * forwards writes through a save queue.
 */
@Beta
@ThreadSafe
public class StorageCache implements Storage {

    private final Storage backend;
    private final StorageSaveQueue saveQueue = new StorageSaveQueue();
    private final ConcurrentHashMap<UUID, PlayerData> cache = new ConcurrentHashMap<>();

    /**
     * Creates a new {@link StorageCache}.
     *
     * @param backend
     *            The wrapped storage backend
     */
    public StorageCache(@Nonnull Storage backend) {
        Validate.notNull(backend, "Storage backend must not be null");
        this.backend = backend;
    }

    @Override
    public PlayerData loadPlayerData(UUID uuid) {
        return cache.computeIfAbsent(uuid, backend::loadPlayerData);
    }

    @Override
    public void savePlayerData(UUID uuid, PlayerData data) {
        cache.put(uuid, data);
        saveQueue.enqueue(uuid, data, backend);
    }

    @Override
    public void initialize() throws StorageException {
        backend.initialize();
    }

    @Override
    public void shutdown() {
        saveQueue.shutdown();
        backend.shutdown();
    }

    @Override
    public boolean isAvailable() {
        return backend.isAvailable();
    }

    @Override
    public String getBackendName() {
        return "Cache -> " + backend.getBackendName();
    }

    /**
     * Invalidates the cached entry for a player.
     * Must be called after the final save on player quit to prevent unbounded memory growth.
     *
     * @param uuid
     *            The player UUID to invalidate
     */
    public void invalidate(@Nonnull UUID uuid) {
        Validate.notNull(uuid, "UUID must not be null");
        cache.remove(uuid);
    }

    Optional<PlayerData> getCached(@Nonnull UUID uuid) {
        return Optional.ofNullable(cache.get(uuid));
    }

    /**
     * Returns the wrapped backend.
     *
     * @return The wrapped backend
     */
    public @Nonnull Storage getBackend() {
        return backend;
    }
}
