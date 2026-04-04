package org.stepbible.wasm;

// TODO: WASM CRITICAL

import com.google.inject.Binding;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.MembersInjector;
import com.google.inject.Provider;
import com.google.inject.Scope;
import com.google.inject.TypeLiteral;
import com.google.inject.spi.TypeConverterBinding;
import com.tyndalehouse.step.core.data.processors.MorphologyProcessor;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A manual, reflection-free Injector implementation for the STEP WASM environment.
 * This satisfies the Guice Injector interface without using Guice's internal 
 * scanning, preventing AssertionError and compilation issues in GraalVM.
 */
public class WasmStaticInjector implements Injector {

    private final Map<String, Object> instances = new HashMap<>();

    public WasmStaticInjector() {
        // Explicitly instantiate the processor needed by EntityConfiguration.
        instances.put("com.tyndalehouse.step.core.data.processors.MorphologyProcessor", 
                      new MorphologyProcessor());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getInstance(Class<T> type) {
        Object instance = this.instances.get(type.getName());
        
        if (instance != null) {
            // The "Double-Cast" Strategy:
            // 1. Cast to (Object) to reset inference.
            // 2. Cast to (T) to satisfy the return type.
            // This bypasses the 'capture of ? super T' bounds check.
            return (T) (Object) instance;
        }
        
        System.out.println("WasmStaticInjector: No manual binding found for " + type.getName());
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getInstance(Key<T> key) {
        // Use the same double-cast logic for Key-based lookups
        return (T) (Object) this.getInstance(key.getTypeLiteral().getRawType());
    }

    // --- Disambiguating com.google.inject.Module from java.lang.Module ---

    @Override 
    public Injector createChildInjector(Iterable<? extends com.google.inject.Module> modules) { 
        return null; 
    }

    @Override 
    public Injector createChildInjector(com.google.inject.Module... modules) { 
        return null; 
    }

    // --- Required Interface Methods (Stubs) ---

    @Override public void injectMembers(Object instance) {}
    
    @Override public <T> MembersInjector<T> getMembersInjector(TypeLiteral<T> typeLiteral) { return null; }
    
    @Override public <T> MembersInjector<T> getMembersInjector(Class<T> type) { return null; }
    
    @Override public Map<Key<?>, Binding<?>> getBindings() { return Collections.emptyMap(); }
    
    @Override public Map<Key<?>, Binding<?>> getAllBindings() { return Collections.emptyMap(); }
    
    @Override public <T> Binding<T> getBinding(Key<T> key) { return null; }
    
    @Override public <T> Binding<T> getBinding(Class<T> type) { return null; }
    
    @Override public <T> Binding<T> getExistingBinding(Key<T> key) { return null; }
    
    @Override public <T> List<Binding<T>> findBindingsByType(TypeLiteral<T> type) { return Collections.emptyList(); }
    
    @Override public <T> Provider<T> getProvider(Key<T> key) { return null; }
    
    @Override public <T> Provider<T> getProvider(Class<T> type) { return null; }
    
    @Override public Injector getParent() { return null; }
    
    @Override public Map<Class<? extends Annotation>, Scope> getScopeBindings() { return Collections.emptyMap(); }
    
    @Override public Set<TypeConverterBinding> getTypeConverterBindings() { return Collections.emptySet(); }
}
