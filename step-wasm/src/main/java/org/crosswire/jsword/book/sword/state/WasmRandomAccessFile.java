package org.crosswire.jsword.book.sword.state;

import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.EOFException;
import java.io.Closeable;
import java.io.UncheckedIOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;

/**
 * SHADOW CLASS: Memory-backed RandomAccessFile for WASM.
 */
public class WasmRandomAccessFile implements Closeable {
    private byte[] data;
    private int pointer = 0;

    /**
     * By throwing ONLY FileNotFoundException, we:
     * 1. Satisfy TreeKeyIndex: It catches IOException (parent of FNFE).
     * 2. Satisfy RawBackendState: It catches FNFE specifically.
     */
    public WasmRandomAccessFile(File file, String mode) throws FileNotFoundException {
        if (!file.exists()) {
            throw new FileNotFoundException("File not found in VFS: " + file.getPath());
        }

        try {
            this.data = Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            // We wrap the broader IOException in a RuntimeException.
            // This prevents the "unreported exception" error in RawBackendState
            // while still allowing the code to fail if a read error occurs.
            throw new UncheckedIOException(e);
        }
    }

    public int read() {
        return (pointer < data.length) ? (data[pointer++] & 0xff) : -1;
    }

    public int read(byte[] b, int off, int len) {
        if (pointer >= data.length) return -1;
        int count = Math.min(len, data.length - pointer);
        System.arraycopy(data, pointer, b, off, count);
        pointer += count;
        return count;
    }

    public void readFully(byte[] b) throws IOException {
        if (read(b, 0, b.length) < b.length) {
            throw new EOFException();
        }
    }

    public long getFilePointer() {
        return (long) pointer;
    }

    public void seek(long pos) {
        this.pointer = (int) pos;
    }

    public long length() {
        return (long) (data != null ? data.length : 0);
    }

    @Override
    public void close() {
        data = null;
    }

    public void write(byte[] b) throws IOException {
        throw new IOException("Read-only VFS.");
    }

    public FileChannel getChannel() {
        return null;
    }
}
