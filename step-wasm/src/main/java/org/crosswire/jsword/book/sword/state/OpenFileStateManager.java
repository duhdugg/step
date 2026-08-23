package org.crosswire.jsword.book.sword.state;

// TODO: WASM-CRITICAL

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.crosswire.jsword.book.BookException;
import org.crosswire.jsword.book.sword.BlockType;
import org.crosswire.jsword.book.sword.SwordBookMetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SHADOW CLASS: Single-threaded/WASM version.
 * This version removes the ScheduledExecutorService to prevent IllegalThreadStateException.
 */
public final class OpenFileStateManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenFileStateManager.class);
    private static volatile OpenFileStateManager manager;
    private final Map<SwordBookMetaData, Queue<OpenFileState>> metaToStates = new HashMap<SwordBookMetaData, Queue<OpenFileState>>();
    private volatile boolean shuttingDown;
    public static OrdinalStrongArray osArray;

    private OpenFileStateManager() {
        // WASM: No background threads or scheduled executors initialized here.
    }

    public static synchronized void init(final int cleanupIntervalSeconds, final int maxExpiry) {
        if (manager == null) {
            manager = new OpenFileStateManager();
        }
    }

    public static OpenFileStateManager instance() {
        if (manager == null) {
            synchronized (OpenFileStateManager.class) {
                if (manager == null) {
                    init(60, 60);
                }
            }
        }
        return manager;
    }

    public RawBackendState getRawBackendState(SwordBookMetaData metadata) throws BookException {
        ensureNotShuttingDown();
        RawBackendState state = getInstance(metadata);
        if (state == null) {
            return new RawBackendState(metadata);
        }
        return state;
    }

    public RawFileBackendState getRawFileBackendState(SwordBookMetaData metadata) throws BookException {
        ensureNotShuttingDown();
        RawFileBackendState state = getInstance(metadata);
        if (state == null) {
            return new RawFileBackendState(metadata);
        }
        return state;
    }

    public GenBookBackendState getGenBookBackendState(SwordBookMetaData metadata) throws BookException {
        ensureNotShuttingDown();
        GenBookBackendState state = getInstance(metadata);
        if (state == null) {
            return new GenBookBackendState(metadata);
        }
        return state;
    }

    public RawLDBackendState getRawLDBackendState(SwordBookMetaData metadata) throws BookException {
        ensureNotShuttingDown();
        RawLDBackendState state = getInstance(metadata);
        if (state == null) {
            return new RawLDBackendState(metadata);
        }
        return state;
    }

    public ZLDBackendState getZLDBackendState(SwordBookMetaData metadata) throws BookException {
        ensureNotShuttingDown();
        ZLDBackendState state = getInstance(metadata);
        if (state == null) {
            return new ZLDBackendState(metadata);
        }
        return state;
    }

    public ZVerseBackendState getZVerseBackendState(SwordBookMetaData metadata, BlockType blockType) throws BookException {
        ensureNotShuttingDown();
        ZVerseBackendState state = getInstance(metadata);
        if (state == null) {
            return new ZVerseBackendState(metadata, blockType);
        }
        return state;
    }

    @SuppressWarnings("unchecked")
    private <T extends OpenFileState> T getInstance(SwordBookMetaData metadata) {
        Queue<OpenFileState> availableStates = getQueueForMeta(metadata);
        final T state = (T) availableStates.poll();
        if (state != null) {
            state.setLastAccess(System.currentTimeMillis());
        }
        return state;
    }

    private Queue<OpenFileState> getQueueForMeta(SwordBookMetaData metadata) {
        Queue<OpenFileState> availableStates = metaToStates.get(metadata);
        if (availableStates == null) {
            synchronized (this) {
                availableStates = metaToStates.get(metadata);
                if (availableStates == null) {
                    availableStates = new ConcurrentLinkedQueue<OpenFileState>();
                    metaToStates.put(metadata, availableStates);
                }
            }
        }
        return availableStates;
    }

    public void release(OpenFileState fileState) {
        if (fileState == null || shuttingDown) {
            return;
        }

        fileState.setLastAccess(System.currentTimeMillis());
        
        // Corrected method name: getBookMetaData()
        SwordBookMetaData bmd = fileState.getBookMetaData();
        Queue<OpenFileState> queueForMeta = getQueueForMeta(bmd);
        
        // If queue is too large, release resources immediately
        if (queueForMeta.size() > 10) {
            fileState.releaseResources();
        } else {
            queueForMeta.offer(fileState);
        }
    }

    public void shutDown() {
        shuttingDown = true;
        for (Queue<OpenFileState> e : metaToStates.values()) {
            OpenFileState state;
            while ((state = e.poll()) != null) {
                state.releaseResources();
            }
        }
        metaToStates.clear();
    }

    private void ensureNotShuttingDown() throws BookException {
        if (shuttingDown) {
            throw new BookException("Unable to read book, application is shutting down.");
        }
    }

    public static class OrdinalStrongArray implements Serializable {
        private static final long serialVersionUID = 1L;
        public int[] ordinalOTHebrewOHB;
        public int[] ordinalOTHebrewRSV;
        public int[] ordinalOTGreek;
        public int[] ordinalNT;
        public byte[] hebrewAugStrong;
        public byte[] greekAugStrong;
        public short[] strongsWithAugmentsOTHebrew;
        public short[] strongsWithAugmentsOTGreek;
        public short[] strongsWithAugmentsNTGreek;
        public byte[] defaultAugmentOTHebrew;
        public byte[] defaultAugmentOTGreek;
        public byte[] defaultAugmentNTGreek;
    }

    public static synchronized void addOrdinalStrong(OrdinalStrongArray osArray) {
        OpenFileStateManager.osArray = osArray;
    }
}
