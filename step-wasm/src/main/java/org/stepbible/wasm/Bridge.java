package org.stepbible.wasm;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provides;

import com.tyndalehouse.step.core.models.ClientSession;
import com.tyndalehouse.step.core.models.OsisWrapper;
import com.tyndalehouse.step.core.guice.StepCoreModule;
import com.tyndalehouse.step.rest.controllers.BibleController;
import com.tyndalehouse.step.rest.controllers.SearchController;
import com.tyndalehouse.step.rest.controllers.ModuleController;

import javax.inject.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Base64;

public class Bridge {
    public Injector injector;
    public BibleController bibleController;
    public SearchController searchController;
    public ModuleController moduleController;
    private static final String STEP_HOME = "/files/step/homes";

    static {
        System.setProperty("user.home", STEP_HOME);
        System.setProperty("sword.home", STEP_HOME + "/sword");
        System.setProperty("jsword.home", STEP_HOME + "/jsword");
    }

    public Bridge() {
        try {
            System.err.println("=== INITIALIZING STEP-WASM BRIDGE ===");

            // Force a pure-Java ProxySelector to prevent JNI net.dll/libnet loading
            ProxySelector.setDefault(new ProxySelector() {
                @Override
                public List<Proxy> select(URI uri) {
                    return Collections.singletonList(Proxy.NO_PROXY);
                }

                @Override
                public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
                    // No-op for WASM runtime
                }
            });

            // Disable system proxy checks and dynamic byte generation
            System.setProperty("java.net.useSystemProxies", "false");
            System.setProperty("guice_bytecode_gen_option", "DISABLED");

            List<Module> modules = new ArrayList<Module>();
            modules.add(new StepCoreModule());

            modules.add(new AbstractModule() {
                @Override
                protected void configure() {}

                @Provides
                public ClientSession provideClientSession() {
                    return new ClientSession() {
                        @Override public Locale getLocale() { return Locale.getDefault(); }
                        @Override public String getLanguage() { return Locale.getDefault().getLanguage(); }
                        @Override public InputStream getAttachment(String path) { return null; }
                        @Override public String getParam(String key) { return null; }
                        @Override public String getIpAddress() { return "127.0.0.1"; }
                        @Override public String getSessionId() { return "wasm-session"; }
                    };
                }
            });

            System.err.println("Creating Guice Injector...");
            this.injector = Guice.createInjector(modules);

            System.err.println("Retrieving BibleController instance...");
            this.bibleController = this.injector.getInstance(BibleController.class);
            this.searchController = this.injector.getInstance(SearchController.class);
            this.moduleController = this.injector.getInstance(ModuleController.class);

            System.err.println("=== STEP-WASM BRIDGE INITIALIZED SUCCESSFULLY ===");

        } catch (Throwable t) {
            System.err.println("!!! STEP-WASM BRIDGE INITIALIZATION FAILED !!!");
            System.err.println("Exception Type: " + t.getClass().getName());
            System.err.println("Message: " + t.getMessage());

            t.printStackTrace(System.err);

            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            System.err.println("Full Stack Trace:\n" + sw.toString());
            System.err.println("================================================");

            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            } else {
                throw new RuntimeException("Failed to initialize Bridge", t);
            }
        }
    }

    public static void writeBinaryFile(String filePath, String base64Data, int mTime) throws IOException {
        File file = new File(filePath);
        File parent = file.getParentFile();

        if (parent != null && !parent.exists()) {
            boolean created = parent.mkdirs();
            if (!created && !parent.exists()) {
                throw new IOException("Failed to create parent directories: " + parent.getAbsolutePath());
            }
        }

        byte[] data = Base64.getDecoder().decode(base64Data);

        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(data);
            file.setLastModified(mTime);
        }
    }

    public static int getFileMtime(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            return 0;
        }
        return (int) file.lastModified();
    }
}
