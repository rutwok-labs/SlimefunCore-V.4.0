package io.github.thebusybiscuit.slimefun4.storage;

import com.google.common.annotations.Beta;

import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.storage.data.PlayerData;

import org.apache.commons.lang.Validate;

import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

/**
 * Serializes storage writes through a single background worker.
 */
@Beta
public class StorageSaveQueue {

    private final LinkedBlockingQueue<SaveTask> queue = new LinkedBlockingQueue<>();
    private final ConcurrentHashMap<UUID, SaveTask> dedupIndex = new ConcurrentHashMap<>();
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final Thread worker;

    /**
     * Creates and starts a new {@link StorageSaveQueue}.
     */
    public StorageSaveQueue() {
        worker = new Thread(this::runWorker, "Slimefun-StorageSaveQueue");
        worker.setDaemon(true);
        worker.setUncaughtExceptionHandler((thread, throwable) ->
            Slimefun.logger().log(Level.SEVERE, throwable,
                () -> "StorageSaveQueue worker thread crashed - player data saves have stopped! Restart the server immediately."));
        worker.start();
    }

    /**
     * Enqueues a save request for the given player.
     *
     * @param uuid
     *            The player UUID to save
     * @param data
     *            The data snapshot to persist
     * @param backend
     *            The backend that should perform the save
     */
    public void enqueue(@Nonnull UUID uuid, @Nonnull PlayerData data, @Nonnull Storage backend) {
        Validate.notNull(uuid, "UUID must not be null");
        Validate.notNull(data, "PlayerData must not be null");
        Validate.notNull(backend, "Storage backend must not be null");

        SaveTask task = new SaveTask(uuid, data.snapshot(true), backend);
        dedupIndex.put(uuid, task);
        queue.offer(task);

        int queueDepth = queue.size();
        if (queueDepth > 200) {
            Slimefun.logger().log(Level.SEVERE, "Storage save queue depth is critically high: {0} pending write tasks. The server may be under save pressure.", queueDepth);
        } else if (queueDepth > 50) {
            Slimefun.logger().log(Level.INFO, "Storage save queue depth is high: {0} pending write tasks", queueDepth);
        }
    }

    /**
     * Stops the worker and flushes all queued saves before returning.
     */
    public void shutdown() {
        running.set(false);
        worker.interrupt();

        try {
            worker.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        drainSynchronously();
    }

    private void runWorker() {
        while (running.get()) {
            try {
                SaveTask task = queue.take();
                processQueuedTask(task);
            } catch (InterruptedException ignored) {
                if (!running.get()) {
                    return;
                }
            }
        }
    }

    private void processQueuedTask(@Nonnull SaveTask task) {
        SaveTask latest = dedupIndex.get(task.uuid());
        if (latest != task) {
            return;
        }

        if (dedupIndex.remove(task.uuid(), task)) {
            persist(task);
        }
    }

    private void drainSynchronously() {
        List<SaveTask> pendingTasks = new ArrayList<>(dedupIndex.values());
        pendingTasks.sort(Comparator.comparing(task -> task.uuid().toString()));
        dedupIndex.clear();
        queue.clear();

        for (SaveTask task : pendingTasks) {
            persist(task);
        }
    }

    private void persist(@Nonnull SaveTask task) {
        try {
            task.backend().savePlayerData(task.uuid(), task.data());
        } catch (Exception x) {
            Slimefun.logger().log(Level.WARNING, x, () -> "Could not save player data for \"" + task.uuid() + "\" using backend \"" + task.backend().getBackendName() + '"');
        }
    }

    private record SaveTask(UUID uuid, PlayerData data, Storage backend) {
    }
}
