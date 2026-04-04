package com.tyndalehouse.step.core.data;

// TODO CANDIDATE FOR REMOVAL

import com.google.inject.Injector;
import com.tyndalehouse.step.core.data.create.PostProcessor;
import com.tyndalehouse.step.core.exceptions.StepInternalException;
import com.tyndalehouse.step.core.utils.IOUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.util.Version;
import org.crosswire.common.util.CWProject;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import static com.tyndalehouse.step.core.utils.StringUtils.isNotBlank;
import static com.tyndalehouse.step.core.utils.StringUtils.split;
import static org.apache.lucene.util.Version.LUCENE_30;

/**
 * A configuration of the entity, include the list of fields, etc.
 */
public class EntityConfiguration {
    private static final String UNABLE_TO_PARSE_CONFIGURATION_FILE = "Unable to parse configuration file";
    private static final String ENTITY_FIELDS_PREFIX = "entity.fields.";
    private final String name;
    private Map<String, FieldConfig> luceneFieldConfiguration;
    private Analyzer analyzerInstance;
    private PostProcessor postProcessorInstance;
    private String path;
    private final String entityHome;
    private final Injector injector;

    /**
     * Creates an entity configuration from a file.
     *
     * @param path the path to where we've stored our entities
     * @param entityName the name of the entity
     * @param injector the injector
     */
    public EntityConfiguration(final String path, final String entityName, final Injector injector) {
        System.out.println("EntityConfiguration()");
        System.out.println(path);
        System.out.println(entityName);
        this.entityHome = path;
        this.name = entityName;
        this.injector = injector;
        System.out.println("EntityConfiguration.loadProperties()");
        final Properties properties = loadProperties(entityName);
        System.out.println("EntityConfiguration.parseProperties()");
        parseProperties(properties);
    }

    /**
     * parses the properties related to an entity configuration
     *
     * @param properties the set of properties
     */
    @SuppressWarnings("unchecked")
    private void parseProperties(final Properties properties) {
        System.out.println("EntityConfiguration.parseProperties() ALPHA");
        try {
            System.out.println("EntityConfiguration.parseProperties() BRAVO");
            final String analyzerProperty = properties.getProperty("entity.analyzer");
            System.out.println("EntityConfiguration.parseProperties() CHARLIE");
            if (isNotBlank(analyzerProperty)) {
                System.out.println("EntityConfiguration.parseProperties() DELTA");
                System.out.println(analyzerProperty);
                // try the default constructor
                final Class<Analyzer> analyzerClass = (Class<Analyzer>) Class.forName(analyzerProperty);
                System.out.println("EntityConfiguration.parseProperties() ECHO");
                try {
                    System.out.println("EntityConfiguration.parseProperties() FOXTROT");
                    this.analyzerInstance = analyzerClass.newInstance();
                    System.out.println("EntityConfiguration.parseProperties() GOLF");
                } catch (final InstantiationException exception) {
                    System.out.println("EntityConfiguration.parseProperties() HOTEL");
                    this.analyzerInstance = analyzerClass.getConstructor(Version.class).newInstance(
                            Version.LUCENE_30);
                    System.out.println("EntityConfiguration.parseProperties() INDIA");
                }
            } else {
                System.out.println("EntityConfiguration.parseProperties() JULIETT");
                this.analyzerInstance = new StandardAnalyzer(LUCENE_30);
                System.out.println("EntityConfiguration.parseProperties() KILO");
            }
            System.out.println("EntityConfiguration.parseProperties() LIMA");

            final String processor = properties.getProperty("entity.postProcessor");
            System.out.println("EntityConfiguration.parseProperties() MIKE");
            if (isNotBlank(processor)) {
                System.out.println("EntityConfiguration.parseProperties() NOVEMBER " + processor);

                this.postProcessorInstance = (PostProcessor) this.injector.getInstance(Class
                        .forName(processor));
                System.out.println("EntityConfiguration.parseProperties() OSCAR");
            }
        } catch (final IllegalAccessException e) {
            System.out.println("IllegalAccessException");
            throw new StepInternalException(UNABLE_TO_PARSE_CONFIGURATION_FILE, e);
        } catch (final ClassNotFoundException e) {
            System.out.println("ClassNotFoundException");
            throw new StepInternalException(UNABLE_TO_PARSE_CONFIGURATION_FILE, e);
        } catch (final InvocationTargetException e) {
            System.out.println("InvocationTargetException");
            throw new StepInternalException(UNABLE_TO_PARSE_CONFIGURATION_FILE, e);
        } catch (final InstantiationException e) {
            System.out.println("InstantiationException");
            throw new StepInternalException(UNABLE_TO_PARSE_CONFIGURATION_FILE, e);
        } catch (final NoSuchMethodException e) {
            System.out.println("NoSuchMethodException");
            throw new StepInternalException(UNABLE_TO_PARSE_CONFIGURATION_FILE, e);
        }

        parseFieldConfigs(properties);
    }

    /**
     * Parses all field configuration
     *
     * @param properties the set of properties attached to an entity
     */
    private void parseFieldConfigs(final Properties properties) {
        int initialCapacity = properties.size() - 3;
        initialCapacity = initialCapacity > 0 ? initialCapacity : 0;
        this.luceneFieldConfiguration = new HashMap<String, FieldConfig>(initialCapacity);

        for (final Entry<Object, Object> p : properties.entrySet()) {
            if (p.getKey() instanceof String) {
                final String key = (String) p.getKey();
                if (key.startsWith(ENTITY_FIELDS_PREFIX)) {
                    parseFieldConfig(key.substring(ENTITY_FIELDS_PREFIX.length()), (String) p.getValue());
                }
            }
        }
    }

    /**
     * parses a single field configuration
     *
     * @param fieldName the name of the field
     * @param value value of the field
     */
    private void parseFieldConfig(final String fieldName, final String value) {
        final String[] parts = split(value, ",");
        final String[] rawFieldMappings = split(parts[0], "\\|");

        final FieldConfig fieldConfig;
        if(parts.length > 4) {
            fieldConfig = new FieldConfig(fieldName, rawFieldMappings, Field.Store.valueOf(parts[1]), Field.Index.valueOf(parts[2]), parts[3], Boolean.parseBoolean(parts[4]));
        } else if(parts.length > 3) {
            fieldConfig = new FieldConfig(fieldName, rawFieldMappings, Field.Store.valueOf(parts[1]), Field.Index.valueOf(parts[2]), parts[3]);
        } else {
            fieldConfig = new FieldConfig(fieldName, rawFieldMappings, Field.Store.valueOf(parts[1]), Field.Index.valueOf(parts[2]));
        }
        this.luceneFieldConfiguration.put(fieldName, fieldConfig);
    }

    /**
     * Loads the properties from file
     *
     * @param entityName the name of the entity
     * @return the set of properties
     */
    private Properties loadProperties(final String entityName) {
        InputStream resourceAsStream = null;
        try {
            resourceAsStream = getClass().getResourceAsStream(entityName + ".properties");
            final Properties properties = new Properties();
            properties.load(resourceAsStream);
            return properties;
        } catch (final IOException e) {
            throw new StepInternalException("Unable to load entity configuration " + entityName, e);
        } finally {
            IOUtils.closeQuietly(resourceAsStream);
        }
    }

    /**
     * @return the location at which the index is stored
     */
    public URI getLocation() {
        try {
            return CWProject.instance().getWriteableProjectSubdir(getPath(), true);
        } catch (final IOException e) {
            throw new StepInternalException("Unable to create step directory", e);
        }
    }

    /**
     * @return the relative path to the entity
     */
    private String getPath() {
        if (this.path == null) {
            this.path = this.entityHome + this.name;
        }
        return this.path;
    }

    /**
     * @return the name
     */
    public String getName() {
        return this.name;
    }

    /**
     * @param fieldName the field name
     * @return the configuration of this field
     */
    public FieldConfig getField(final String fieldName) {
        return this.luceneFieldConfiguration.get(fieldName);
    }

    /**
     * @return the luceneFieldConfiguration
     */
    public Map<String, FieldConfig> getLuceneFieldConfiguration() {
        return this.luceneFieldConfiguration;
    }

    /**
     * @return the analyzerInstance
     */
    public Analyzer getAnalyzerInstance() {
        return this.analyzerInstance;
    }

    /**
     * @return the postProcessorInstance
     */
    public PostProcessor getPostProcessorInstance() {
        return this.postProcessorInstance;
    }

    /**
     * @param fieldName the name of the field
     * @param fieldValue the value of that field
     * @return a {@link Fieldable} which represents these values
     */
    public Fieldable getField(final String fieldName, final String fieldValue) {
        return this.luceneFieldConfiguration.get(fieldName).getField(fieldValue);
    }
}
