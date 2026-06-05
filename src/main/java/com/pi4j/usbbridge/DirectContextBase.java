package com.pi4j.usbbridge;

import com.pi4j.boardinfo.model.BoardInfo;
import com.pi4j.config.Config;
import com.pi4j.context.Context;
import com.pi4j.context.ContextBuilder;
import com.pi4j.context.ContextConfig;
import com.pi4j.context.ContextProperties;
import com.pi4j.context.impl.DefaultContextProperties;
import com.pi4j.event.InitializedEvent;
import com.pi4j.event.InitializedListener;
import com.pi4j.event.ShutdownEvent;
import com.pi4j.event.ShutdownListener;
import com.pi4j.exception.Pi4JException;
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
import com.pi4j.util.ExecutorPool;

import java.lang.module.Configuration;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * A context base implementation that bypasses providers and some other internal components that are not really
 * needed without providers.
 */
public abstract class DirectContextBase implements Context {
    protected final Object lock = new Object();
    protected final Map<String, IO> openIOs = new HashMap<>();
    protected final List<InitializedListener> initializedListeners = new ArrayList<>();
    protected final List<ShutdownListener> shutdownListeners = new ArrayList<>();
    protected final ExecutorService executorService = Executors.newCachedThreadPool() ;


    private boolean isShutdown = false;

    @Override
    public ContextConfig config() {
       throw new UnsupportedOperationException();
    }

    @Override
    public ContextProperties properties() {
        throw new UnsupportedOperationException();
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
        return executorService.submit(task);
    }

    @Override
    public Context shutdown() throws ShutdownException {
        synchronized (lock) {
            if (isShutdown) {
                return this;
            }
            List<Exception> exceptions = new ArrayList<>();
            for (IO io : openIOs.values()) {
                try {
                    io.shutdownInternal(this);
                } catch (Exception e) {
                    exceptions.add(e);
                }
            }
            openIOs.clear();
            for (ShutdownListener listener : shutdownListeners) {
                ShutdownEvent event = new ShutdownEvent(this);
                try {
                    listener.onShutdown(event);
                } catch (Exception e) {
                    exceptions.add(e);
                }
            }
            isShutdown = true;
            if (!exceptions.isEmpty()) {
                throw new Pi4JException("Exception(s) in shutdown: " + exceptions, exceptions.getFirst());
            }
        }
        return this;
    }

    @Override
    public Future<Context> asyncShutdown() {
        return executorService.submit(() -> {
            shutdown();
            return this;
        });
    }

    @Override
    public boolean isShutdown() {
        return isShutdown;
    }

    @Override
    public BoardInfo boardInfo() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends IO> T shutdown(String id) throws IOInvalidIDException, IONotFoundException, IOShutdownException {
        synchronized (lock) {
            T io = (T) openIOs.get(id);
            shutdown(io);
            return io;
        }
    }

    @Override
    public <T extends IO> void shutdown(T instance) throws IOInvalidIDException, IONotFoundException, IOShutdownException {
        synchronized (lock) {
            // We remove first so it's gone if shutdownInternal fails.
            openIOs.remove(instance.id());
            instance.shutdownInternal(this);
        }
    }

    @Override
    public Context removeAllInitializedListeners() {
        synchronized (lock) {
            initializedListeners.clear();
        }
        return this;
    }

    @Override
    public Context addListener(InitializedListener... initializedListeners) {
        synchronized (lock) {
            this.initializedListeners.addAll(Arrays.asList(initializedListeners));
        }
        return this;
    }

    @Override
    public Context removeListener(InitializedListener... initializedListeners) {
        synchronized (lock) {
            this.initializedListeners.removeAll(Arrays.asList(initializedListeners));
        }
        return this;
    }

    @Override
    public Context removeAllShutdownListeners() {
        synchronized (lock) {
            shutdownListeners.clear();
        }
        return this;
    }

    @Override
    public Context addListener(ShutdownListener... shutdownListeners) {
        synchronized (lock) {
            this.shutdownListeners.addAll(Arrays.asList(shutdownListeners));
        }
        return this;
    }

    @Override
    public Context removeListener(ShutdownListener... shutdownListeners) {
        synchronized (lock) {
            this.shutdownListeners.removeAll(Arrays.asList(shutdownListeners));
        }
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <I extends IO> I create(IOConfig ioConfig, IOType ioType) {
        synchronized (lock) {
            I port = createImpl(ioConfig, ioType);
            openIOs.put(port.id(), port);
            return port;
        }
    }

    @Override
    public <I extends IO> I create(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <I extends IO> I create(String s, IOType ioType) {
        throw new UnsupportedOperationException();
    }

    /** Called by implementations when (deferred) initialization is complete. */
    protected void notifyInitialized() {
        // Defensive copy instead?
        synchronized (lock) {
            InitializedEvent event = new InitializedEvent(this);
            for (InitializedListener listener : initializedListeners) {
                try {
                    listener.onInitialized(event);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /** This should be the "main" (only?) thing to be implemented by concrete subclasses. */
    abstract protected <I extends IO> I createImpl(IOConfig ioConfig, IOType ioType);

}
