package org.stepbible.wasm;

import com.tyndalehouse.step.core.models.OsisWrapper;
import com.tyndalehouse.step.core.service.BibleInformationService;
import com.tyndalehouse.step.core.service.impl.BibleInformationServiceImpl;
import com.tyndalehouse.step.core.service.jsword.impl.*;
import com.tyndalehouse.step.core.service.helpers.VersionResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonInclude;

import org.graalvm.webimage.api.JS;
import org.graalvm.webimage.api.JSString;
import org.crosswire.jsword.book.sword.SwordBookDriver;
import org.crosswire.jsword.book.Book;
import org.crosswire.jsword.book.Books;
import org.crosswire.jsword.book.install.Installer;

import java.io.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Properties;
import javax.inject.Provider;
import com.tyndalehouse.step.core.service.jsword.JSwordMetadataService;

import com.tyndalehouse.step.core.service.PassageOptionsValidationService;
import com.tyndalehouse.step.core.service.impl.PassageOptionsValidationServiceImpl;
import com.tyndalehouse.step.core.models.ClientSession;
import java.util.Locale;
import com.tyndalehouse.step.core.service.jsword.JSwordPassageService;
import com.tyndalehouse.step.core.service.jsword.impl.JSwordPassageServiceImpl;
import com.tyndalehouse.step.core.service.jsword.JSwordSearchService;
import com.tyndalehouse.step.core.service.jsword.impl.JSwordSearchServiceImpl;
import com.tyndalehouse.step.core.service.MorphologyService;
import com.tyndalehouse.step.core.service.impl.MorphologyServiceImpl;
import com.tyndalehouse.step.core.data.EntityManager;
import com.tyndalehouse.step.core.data.entities.impl.EntityManagerImpl;

import com.google.inject.Injector;

public class Bridge {
    private static final String STEP_HOME = "/opt/step/homes";
    private static BibleInformationService bibleService;
    private static ObjectMapper objectMapper;

    static {
        // Only setup basic logging and system props initially
        setupStreams();
        System.setProperty("user.home", STEP_HOME);
        System.setProperty("sword.home", STEP_HOME + "/sword");
        System.setProperty("jsword.home", STEP_HOME + "/jsword");

        objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    private static void setupStreams() {
        PrintStream jsPrintStream = new PrintStream(new JSConsoleOutputStream());
        System.setOut(jsPrintStream);
        System.setErr(jsPrintStream);
    }

    private static void initJSwordConfig() {
        org.crosswire.common.util.CWProject.instance().setFrontendName("step");
        org.crosswire.jsword.versification.BookName.setFullBookName(false);
    }

    /**
     * Call this from JS AFTER all files (conf, properties, ztext)
     * have been seeded to /opt/step/homes
     */
    public String initEngine(String val) {
        try {
            System.out.println("JAVA: Starting initEngine...");
            initJSwordConfig();

            // Trigger JSword to scan the seeded directory
            System.out.println("registerDriver");
            Books.installed().registerDriver(SwordBookDriver.instance());

            // Build the Service Stack
            Properties emptyProps = new Properties();
            List<Installer> emptyInstallers = new ArrayList<>();
            List<String> emptyStrings = new ArrayList<>();

            JSwordMetadataService jswordMetadata;
            System.out.println("versionResolver");
            VersionResolver versionResolver = new VersionResolver(emptyProps);
            System.out.println("jswordVersification");
            JSwordVersificationServiceImpl jswordVersification = new JSwordVersificationServiceImpl(versionResolver);
            jswordMetadata = new JSwordMetadataServiceImpl(jswordVersification, versionResolver);


            // optionsValidationService
            // 1. Create a provider for the ClientSession
            // Reasoning: STEP uses the session to determine user-specific settings like language.
            // In this WASM context, we use the 'SimpleSession' class already defined at the bottom of your file.
            System.out.println("clientSessionProvider");
            Provider<ClientSession> clientSessionProvider = new Provider<ClientSession>() {
                @Override
                public ClientSession get() {
                    return new SimpleSession();
                }
            };
            // 2. Instantiate the Validation Service
            // Reasoning: This service is responsible for determining which display options (like
            // Strong's numbers or Greek/Hebrew morphology) are valid for a given Bible version.
            System.out.println("optionsService");
            PassageOptionsValidationService optionsService = new PassageOptionsValidationServiceImpl(
                jswordMetadata,
                clientSessionProvider
            );


            System.out.println("moduleService");
            JSwordModuleServiceImpl moduleService = new JSwordModuleServiceImpl(
                emptyInstallers, emptyInstallers, jswordVersification, versionResolver
            );

            System.out.println("injector");
            Injector injector = new WasmStaticInjector();

            System.out.println("entityManager");
            EntityManagerImpl entityManager = new EntityManagerImpl(true, STEP_HOME + "homes/jsword/step/entities/", injector);

            System.out.println("morphology");
            MorphologyServiceImpl morphology = new MorphologyServiceImpl(entityManager);

            System.out.println("jswordPassage");
            JSwordPassageService jswordPassage = new JSwordPassageServiceImpl(
                jswordVersification, morphology, null, null, versionResolver, optionsService
            );

            System.out.println("jswordSearch");
            JSwordSearchService jswordSearch = new JSwordSearchServiceImpl(
                    jswordVersification,
                    jswordMetadata,
                    jswordPassage
            );

            System.out.println("bibleService");
            bibleService = new BibleInformationServiceImpl(
                emptyStrings, optionsService, jswordPassage, moduleService,
                jswordMetadata, jswordSearch, entityManager, jswordVersification, versionResolver
            );

            List<Book> books = Books.installed().getBooks();
            System.out.println("JAVA: ENGINE_READY - Books found: " + books.size());
            for (int i = 0; i < books.size(); i++) {
                Book book = books.get(i);
                System.out.println("JAVA: Book: " + book.getName());
            }
            return "SUCCESS";
        } catch (Throwable t) {
            t.printStackTrace();
            return "ERROR: " + t.getMessage();
        }
    }

    @JS(args = {"json"}, value = "return JSON.parse(json);")
    private static native Object parseJson(JSString json);

    @JS(args = {"msg"}, value = "console.log('JAVA:', msg);")
    public static native void log(JSString msg);

    @JS(args = {"getPassageFn", "seedFn", "initEngineFn"}, value =
        "globalThis.getPassageText = getPassageFn;" +
        "globalThis.seedFile = seedFn;" +
        "globalThis.initEngine = initEngineFn;")
    private static native void export(java.util.function.Function<JSString, Object> getPassageFn,
                                      java.util.function.BiConsumer<JSString, JSString> seedFn,
                                      java.util.function.Function<JSString, String> initEngineFn);

    public Object getPassageTextJS(String version, String reference) {
        try {
            if (bibleService == null) {
                return parseJson(JSString.of("{\"error\":\"Engine not initialized.\"}"));
            }

            // 1. Log the attempt for the WASM console
            System.out.println("Fetching: " + version + " | " + reference);

            OsisWrapper wrapper = bibleService.getPassageText(version, reference, null, null, null, "en");

            // 2. Check if the wrapper itself is null before serialization
            if (wrapper == null) {
                return parseJson(JSString.of("{\"error\":\"OsisWrapper returned null for " + version + "\"}"));
            }

            return parseJson(JSString.of(objectMapper.writeValueAsString(wrapper)));

        } catch (Throwable e) {
            // 3. Capture full diagnostic info
            java.io.StringWriter sw = new java.io.StringWriter();
            java.io.PrintWriter pw = new java.io.PrintWriter(sw);
            e.printStackTrace(pw);
            String stackTrace = sw.toString().replace("\"", "\\\"").replace("\n", "\\n");

            String diagnostic = "{" +
                "\"error\":\"" + e.getClass().getName() + "\"," +
                "\"message\":\"" + (e.getMessage() != null ? e.getMessage().replace("\"", "\\\"") : "null") + "\"," +
                "\"stackTrace\":\"" + stackTrace + "\"" +
                "}";

            return parseJson(JSString.of(diagnostic));
        }
    }

    public void seedFile(String path, String b64) {
        try {
            File f = new File(STEP_HOME + "/" + path);
            if (f.getParentFile() != null) f.getParentFile().mkdirs();
            try (FileOutputStream fos = new FileOutputStream(f)) {
                fos.write(Base64.getDecoder().decode(b64));
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static void main(String[] args) {
        Bridge b = new Bridge();
        export(
            (req) -> {
                String[] p = req.asString().split("\\|");
                if (p.length < 2) return parseJson(JSString.of("{\"error\":\"Invalid Format\"}"));
                return b.getPassageTextJS(p[0], p[1]);
            },
            (path, b64) -> b.seedFile(path.asString(), b64.asString()),
            (val) -> b.initEngine(val.asString())
        );
    }
}

class JSConsoleOutputStream extends OutputStream {
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    @Override public void write(int b) {
        if (b == '\n') {
            Bridge.log(JSString.of(buffer.toString()));
            buffer.reset();
        } else {
            buffer.write(b);
        }
    }
}

class SimpleSession implements ClientSession {
    @Override
    public String getSessionId() {
        return "wasm-session";
    }

    @Override
    public String getIpAddress() {
        return "127.0.0.1";
    }

    @Override
    public String getLanguage() {
        return "en";
    }

    @Override
    public Locale getLocale() {
        return Locale.ENGLISH;
    }

    @Override
    public String getParam(String name) {
        return null;
    }

    @Override
    public InputStream getAttachment(String name) {
        return null;
    }
}
