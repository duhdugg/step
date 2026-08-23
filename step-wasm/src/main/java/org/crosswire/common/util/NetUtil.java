package org.crosswire.common.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SHADOW CLASS: Fully neutered and completed for WASM.
 * Contains all public API methods from the original NetUtil to prevent NoSuchMethodErrors.
 */
public final class NetUtil {
    private NetUtil() {}

    public static final String PROTOCOL_FILE = "file";
    public static final String PROTOCOL_HTTP = "http";
    public static final String PROTOCOL_FTP = "ftp";
    public static final String PROTOCOL_JAR = "jar";
    public static final String INDEX_FILE = "index.txt";
    public static final String SEPARATOR = "/";
    public static final String AUTH_SEPERATOR_USERNAME = "@";
    public static final String AUTH_SEPERATOR_PASSWORD = ":";

    private static File cachedir;

    // --- URI & FILE STATE ---

    public static URI copy(URI uri) {
        try { return new URI(uri.toString()); } catch (URISyntaxException e) { return null; }
    }

    public static void makeDirectory(URI orig) throws MalformedURLException {
        File file = new File(orig.getPath());
        if (!file.exists() && !file.mkdirs()) {
            throw new MalformedURLException("Failed to create directory: " + orig);
        }
    }

    public static void makeFile(URI orig) throws IOException {
        File file = new File(orig.getPath());
        if (!file.exists() && !file.createNewFile()) {
            throw new IOException("Failed to create file: " + orig);
        }
    }

    public static boolean isFile(URI uri) {
        return PROTOCOL_FILE.equals(uri.getScheme()) && new File(uri.getPath()).isFile();
    }

    public static boolean isDirectory(URI uri) {
        return PROTOCOL_FILE.equals(uri.getScheme()) && new File(uri.getPath()).isDirectory();
    }

    public static boolean canWrite(URI uri) {
        return PROTOCOL_FILE.equals(uri.getScheme()) && new File(uri.getPath()).canWrite();
    }

    public static boolean canRead(URI uri) {
        return PROTOCOL_FILE.equals(uri.getScheme()) && new File(uri.getPath()).canRead();
    }

    public static boolean move(URI oldUri, URI newUri) {
        return new File(oldUri.getPath()).renameTo(new File(newUri.getPath()));
    }

    public static boolean delete(URI orig) {
        return new File(orig.getPath()).delete();
    }

    // --- DIRECTORY LISTING ---

    public static String[] list(URI uri, URIFilter filter) throws IOException {
        try {
            return listByIndexFile(lengthenURI(uri, INDEX_FILE), filter);
        } catch (FileNotFoundException ex) {
            if (PROTOCOL_FILE.equals(uri.getScheme())) {
                return listByFile(uri, filter);
            }
            return new String[0];
        }
    }

    public static String[] listByFile(URI uri, URIFilter filter) throws MalformedURLException {
        File fdir = new File(uri.getPath());
        if (!fdir.isDirectory()) throw new MalformedURLException("Not a directory");
        return fdir.list(new URIFilterFilenameFilter(filter));
    }

    public static String[] listByIndexFile(URI index) throws IOException {
        return listByIndexFile(index, new DefaultURIFilter());
    }

    public static String[] listByIndexFile(URI index, URIFilter filter) throws IOException {
        try (InputStream in = getInputStream(index);
             BufferedReader din = new BufferedReader(new InputStreamReader(in), 8192)) {
            List<String> list = new ArrayList<>();
            String line;
            while ((line = din.readLine()) != null) {
                int commentPos = line.indexOf('#');
                String name = (commentPos != -1 ? line.substring(0, commentPos) : line).trim();
                if (name.length() > 0 && !name.equals(INDEX_FILE) && filter.accept(name)) {
                    list.add(name);
                }
            }
            return list.toArray(new String[0]);
        }
    }

    // --- STREAMS & CONTENT ---

    public static InputStream getInputStream(URI uri) throws IOException {
        if (PROTOCOL_FILE.equals(uri.getScheme())) return new FileInputStream(uri.getPath());
        throw new IOException("WASM: No network access for " + uri.getScheme());
    }

    public static OutputStream getOutputStream(URI uri) throws IOException {
        return getOutputStream(uri, false);
    }

    public static OutputStream getOutputStream(URI uri, boolean append) throws IOException {
        if (PROTOCOL_FILE.equals(uri.getScheme())) return new FileOutputStream(uri.getPath(), append);
        throw new IOException("WASM: No network write access");
    }

    public static File getAsFile(URI uri) throws IOException {
        if (PROTOCOL_FILE.equals(uri.getScheme())) return new File(uri.getPath());
        throw new IOException("WASM: getAsFile only supports file scheme.");
    }

    public static void copy(URI src, URI dest) throws IOException {
        IOUtil.copy(getInputStream(src), getOutputStream(dest));
    }

    // --- URI MANIPULATION ---

    public static URI shortenURI(URI orig, String strip) throws MalformedURLException {
        String path = orig.getPath();
        if (path.endsWith("/") || path.endsWith("\\")) path = path.substring(0, path.length() - 1);
        if (!path.endsWith(strip)) throw new MalformedURLException("URI does not end with " + strip);
        try {
            return new URI(orig.getScheme(), orig.getUserInfo(), orig.getHost(), orig.getPort(), 
                           path.substring(0, path.length() - strip.length()), "", "");
        } catch (URISyntaxException e) { throw new MalformedURLException(e.getMessage()); }
    }

    public static URI lengthenURI(URI orig, String extra) {
        try {
            StringBuilder path = new StringBuilder(orig.getPath());
            boolean pathEnds = path.length() > 0 && (path.charAt(path.length() - 1) == '/' || path.charAt(path.length() - 1) == '\\');
            boolean extraStarts = extra.length() > 0 && (extra.charAt(0) == '/' || extra.charAt(0) == '\\');
            
            if (extraStarts && pathEnds) path.append(extra.substring(1));
            else if (!extraStarts && !pathEnds) path.append(SEPARATOR).append(extra);
            else path.append(extra);

            return new URI(orig.getScheme(), orig.getUserInfo(), orig.getHost(), orig.getPort(), path.toString(), orig.getQuery(), orig.getFragment());
        } catch (URISyntaxException ex) { return null; }
    }

    // --- METADATA & PROPERTIES ---

    public static PropertyMap loadProperties(URI uri) throws IOException {
        try (InputStream is = getInputStream(uri)) {
            PropertyMap prop = new PropertyMap();
            prop.load(is);
            return prop;
        }
    }

    public static void storeProperties(PropertyMap properties, URI uri, String title) throws IOException {
        try (OutputStream out = getOutputStream(uri)) {
            PropertyMap temp = new PropertyMap();
            temp.putAll(properties);
            temp.store(out, title);
        }
    }

    public static int getSize(URI uri) { return getSize(uri, null, null); }
    public static int getSize(URI uri, String proxyHost) { return getSize(uri, proxyHost, null); }
    public static int getSize(URI uri, String proxyHost, Integer proxyPort) {
        if (PROTOCOL_FILE.equals(uri.getScheme())) return (int) new File(uri.getPath()).length();
        return 0;
    }

    public static long getLastModified(URI uri) { return getLastModified(uri, null, null); }
    public static long getLastModified(URI uri, String proxyHost) { return getLastModified(uri, proxyHost, null); }
    public static long getLastModified(URI uri, String proxyHost, Integer proxyPort) {
        if (PROTOCOL_FILE.equals(uri.getScheme())) return new File(uri.getPath()).lastModified();
        return new Date().getTime();
    }

    public static boolean isNewer(URI left, URI right) { return getLastModified(left) > getLastModified(right); }
    public static boolean isNewer(URI left, URI right, String pHost) { return isNewer(left, right); }
    public static boolean isNewer(URI left, URI right, String pH, Integer pP) { return isNewer(left, right); }

    // --- CACHE ---

    public static File getURICacheDir() { return cachedir; }
    public static void setURICacheDir(File cache) { cachedir = cache; }

    // --- CONVERSIONS ---

    public static URI getURI(File file) { return file.toURI(); }
    public static URI getTemporaryURI(String prefix, String suffix) throws IOException { return getURI(File.createTempFile(prefix, suffix)); }
    public static URI toURI(URL url) { try { return new URI(url.toExternalForm()); } catch (URISyntaxException e) { return null; } }
    public static URL toURL(URI uri) { try { return uri.toURL(); } catch (MalformedURLException e) { return null; } }

    // --- INNER CLASSES ---

    public static class URIFilterFilenameFilter implements FilenameFilter {
        private URIFilter filter;
        public URIFilterFilenameFilter(URIFilter filter) { this.filter = filter; }
        @Override public boolean accept(File arg0, String name) { return filter.accept(name); }
    }

    public static class IsDirectoryURIFilter implements URIFilter {
        private URI parent;
        public IsDirectoryURIFilter(URI parent) { this.parent = parent; }
        @Override public boolean accept(String name) { return NetUtil.isDirectory(NetUtil.lengthenURI(parent, name)); }
    }

    private static final Logger log = LoggerFactory.getLogger(NetUtil.class);
}
