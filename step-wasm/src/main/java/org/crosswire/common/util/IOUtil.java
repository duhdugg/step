package org.crosswire.common.util;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.zip.ZipFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SHADOW CLASS: Fully neutered for WASM environment.
 * Provides stream manipulation and resource management without native dependencies.
 */
public final class IOUtil {
    private IOUtil() {
    }

    /**
     * Copies data from an InputStream to an OutputStream.
     * This is the method required by NetUtil.copy().
     * * @param in  The source stream
     * @param out The destination stream
     * @throws IOException if an I/O error occurs
     */
    public static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[8192];
        int len;
        try {
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
        } finally {
            close(in);
            close(out);
        }
    }

    /**
     * Closes any Closeable object quietly.
     *
     * * @param closeable The resource to close
     */
    public static void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ex) {
                log.error("Error closing resource", ex);
            }
        }
    }

    /**
     * Specialized close for ZipFile to maintain compatibility with 
     * legacy JSword signatures.
     */
    public static void close(ZipFile closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ex) {
                log.error("Error closing ZipFile", ex);
            }
        }
    }

    /**
     * Quietly closes an InputStream.
     */
    public static void close(InputStream in) {
        close((Closeable) in);
    }

    /**
     * Quietly closes an OutputStream.
     */
    public static void close(OutputStream out) {
        close((Closeable) out);
    }

    /**
     * Quietly closes a Reader.
     */
    public static void close(Reader in) {
        close((Closeable) in);
    }

    /**
     * Quietly closes a Writer.
     */
    public static void close(Writer out) {
        close((Closeable) out);
    }

    private static final Logger log = LoggerFactory.getLogger(IOUtil.class);
}
