package com.pi4j.usbbridge;

import com.pi4j.boardinfo.model.BoardInfo;
import com.pi4j.context.Context;
import com.pi4j.context.ContextBuilder;
import com.pi4j.context.ContextConfig;
import com.pi4j.context.ContextProperties;
import com.pi4j.context.impl.DefaultContextProperties;
import com.pi4j.event.InitializedListener;
import com.pi4j.event.ShutdownListener;
import com.pi4j.exception.ShutdownException;
import com.pi4j.io.IO;
import com.pi4j.io.IOConfig;
import com.pi4j.io.IOType;
import com.pi4j.io.exception.IOInvalidIDException;
import com.pi4j.io.exception.IONotFoundException;
import com.pi4j.io.exception.IOShutdownException;
import com.pi4j.platform.Platforms;
import com.pi4j.provider.Providers;
import com.pi4j.registry.Registry;
import com.pi4j.runtime.Runtime;
import com.pi4j.runtime.impl.DefaultRuntime;

import java.util.concurrent.Future;

/** A context implementation that bypasses providers etc. */
public abstract class DirectContextBase implements Context {
    // TODO: Reduce this hackery; some should be removable with the contextless config change.
    private final ContextConfig config = ContextBuilder.newInstance().toConfig();
    private final Runtime runtime = DefaultRuntime.newInstance(this);
    // This should be killable with the lastest v2 snapshot
    private final ContextProperties properties = DefaultContextProperties.newInstance(runtime.properties());

    @Override
    public ContextConfig config() {
       return config;
    }

    @Override
    public ContextProperties properties() {
        return properties;
    }

    @Override
    public Providers providers() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Registry registry() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Platforms platforms() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Future<?> submitTask(Runnable task) {
        return this.runtime.submitTask(task);
    }

    /** {@inheritDoc} */
    @Override
    public Context shutdown() throws ShutdownException {
        // shutdown the runtime
        this.runtime.shutdown();
        return this;
    }


    @Override
    public Future<Context> asyncShutdown() {
        return this.runtime.asyncShutdown();
    }

    @Override
    public boolean isShutdown() {
        return this.runtime.isShutdown();
    }

    @Override
    public BoardInfo boardInfo() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends IO> T shutdown(String id) throws IOInvalidIDException, IONotFoundException, IOShutdownException {
        // TODO: Support in order to keep track of IO instances to shut down...
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends IO> void shutdown(T instance) throws IOInvalidIDException, IONotFoundException, IOShutdownException {
        // TODO: Support in order to keep track of IO instances to shut down...
        throw new UnsupportedOperationException();
    }

    @Override
    public Context removeAllInitializedListeners() {
        runtime.removeAllInitializedListeners();
        return this;
    }

    @Override
    public Context addListener(InitializedListener... initializedListeners) {
        runtime.addListener(initializedListeners);
        return this;
    }

    @Override
    public Context removeListener(InitializedListener... initializedListeners) {
        runtime.removeListener(initializedListeners);
        return this;
    }

    @Override
    public Context removeAllShutdownListeners() {
        runtime.removeAllShutdownListeners();
        return this;
    }

    @Override
    public Context addListener(ShutdownListener... shutdownListeners) {
        runtime.addListener(shutdownListeners);
        return this;
    }

    @Override
    public Context removeListener(ShutdownListener... shutdownListeners) {
        runtime.removeListener(shutdownListeners);
        return this;
    }


    @Override
    @SuppressWarnings("unchecked")
    public <I extends IO> I create(IOConfig ioConfig, IOType ioType) {
        I port = createImpl(ioConfig, ioType);
        // TODO: Register the IO instance with the runtimeRegistry(?) for shutdown here.
        return port;
    }

    @Override
    public <I extends IO> I create(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <I extends IO> I create(String s, IOType ioType) {
        throw new UnsupportedOperationException();
    }


    /** This should be the "main" (only?) thing to be implemented by concrete subclasses. */
    abstract protected <I extends IO> I createImpl(IOConfig ioConfig, IOType ioType);

}
