package com.tyndalehouse.step.core.data.entities.impl;

// TODO CANDIDATE FOR REMOVAL

import com.google.inject.Injector;
import com.tyndalehouse.step.core.data.EntityConfiguration;
import com.tyndalehouse.step.core.data.EntityIndexReader;
import com.tyndalehouse.step.core.data.EntityManager;
import java.util.HashMap;
import java.util.Map;

public class EntityManagerImpl implements EntityManager {
    private final String indexPath;
    private final boolean memoryMapped;
    private final Map<String, EntityIndexReader> indexReaders = new HashMap<>();
    private final Injector injector;

    public EntityManagerImpl(boolean memoryMapped, String indexPath, Injector injector) {
        System.out.println("EntityManagerImpl()");
        this.memoryMapped = memoryMapped;
        this.indexPath = indexPath;
        this.injector = injector;
    }

    @Override
    public EntityIndexReader getReader(String entity) {
        System.out.println("EntityManagerImpl.getReader() " + entity);
        System.out.println("EntityManagerImpl.getReader:ALPHA");
        if (!indexReaders.containsKey(entity)) {
            System.out.println("EntityManagerImpl.getReader:BRAVO");
            // Manually create the reader without relying on complex Config logic
            EntityConfiguration config = new EntityConfiguration(indexPath, entity, this.injector);
            System.out.println("EntityManagerImpl.getReader:CHARLIE");
            indexReaders.put(entity, new EntityIndexReaderImpl(config, memoryMapped));
            System.out.println("EntityManagerImpl.getReader:DELTA");
        }
        System.out.println("EntityManagerImpl.getReader:ECHO");
        return indexReaders.get(entity);
    }

    @Override public EntityConfiguration getConfig(String name) {
        System.out.println("EntityManagerImpl.getConfig() " + name);
        return new EntityConfiguration(indexPath, name, this.injector);
    }

    @Override public void refresh(String entity) {}
    @Override public void close() {}
    @Override public EntityIndexWriterImpl getNewWriter(String entity) { return null; }
}
