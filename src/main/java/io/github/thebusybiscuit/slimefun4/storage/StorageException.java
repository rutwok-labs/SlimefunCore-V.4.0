package io.github.thebusybiscuit.slimefun4.storage;

import com.google.common.annotations.Beta;

/**
 * Signals that a storage backend could not be initialized or used safely.
 */
@Beta
public class StorageException extends Exception {

    private static final long serialVersionUID = 7168137030741821619L;

    /**
     * Creates a new {@link StorageException}.
     *
     * @param message
     *            A human-readable description of the failure
     */
    public StorageException(String message) {
        super(message);
    }

    /**
     * Creates a new {@link StorageException}.
     *
     * @param message
     *            A human-readable description of the failure
     * @param cause
     *            The original cause of this failure
     */
    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
