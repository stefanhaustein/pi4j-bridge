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
import com.pi4j.io.gpio.analog.*;
import com.pi4j.io.gpio.digital.*;
import com.pi4j.io.i2c.I2C;
import com.pi4j.io.i2c.I2CConfig;
import com.pi4j.io.pwm.Pwm;
import com.pi4j.io.pwm.PwmConfig;
import com.pi4j.io.spi.Spi;
import com.pi4j.io.spi.SpiConfig;
import com.pi4j.platform.Platforms;
import com.pi4j.provider.Providers;
import com.pi4j.registry.Registry;
import com.pi4j.runtime.Runtime;
import com.pi4j.runtime.impl.DefaultRuntime;

import java.util.concurrent.Future;

/** A context implementation that bypasses providers etc. */
public abstract class DirectContextBase implements Context {
    // TODO: Reduce this hackery...
    private final ContextConfig config = ContextBuilder.newInstance().toConfig();
    private final Runtime runtime = DefaultRuntime.newInstance(this);
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
        // TODO: Figure out if we need to do anything here
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends IO> void shutdown(T instance) throws IOInvalidIDException, IONotFoundException, IOShutdownException {
        // TODO: Figure out if we need to do anything here
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
        return null;
    }

    @Override
    public Context addListener(ShutdownListener... shutdownListeners) {
        return null;
    }

    @Override
    public Context removeListener(ShutdownListener... shutdownListeners) {
        return null;
    }

    /**
     * We expect subclasses to implement specific create methods, so
     * we invert the dispatch mechanism here: Instead of the specific create methods
     * delegating to this generic methods, we provide concrete methods throwing
     * unuspported errors for each type, and here we dispatch to the concrete methods by
     * type.
     */
    @Override
    @SuppressWarnings("unchecked")
    public <I extends IO> I create(IOConfig ioConfig, IOType ioType) {
        return (I) switch (ioType) {
            case ANALOG_INPUT -> create((AnalogInputConfig) ioConfig);
            case ANALOG_OUTPUT -> create((AnalogOutputConfig) ioConfig);
            case DIGITAL_INPUT -> create((DigitalInputConfig) ioConfig);
            case DIGITAL_OUTPUT -> create((DigitalOutputConfig) ioConfig);
            case PWM -> create((PwmConfig) ioConfig);
            case I2C -> create((I2CConfig) ioConfig);
            case SPI -> create((SpiConfig) ioConfig);
        };
    }

    @Override
    public <I extends IO> I create(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <I extends IO> I create(String s, IOType ioType) {
        throw new UnsupportedOperationException();
    }


    @Override
    public AnalogOutput create(AnalogOutputConfig config) {
        throw new UnsupportedOperationException("AnalogOutput not supported");
    }

    @Override
    public AnalogInput create(AnalogInputConfig config) {
        throw new UnsupportedOperationException("AnalogInput not supported");
    }

    @Override
    public DigitalOutput create(DigitalOutputConfig config) {
        throw new UnsupportedOperationException("DigitalOutput not supported");
    }

    @Override
    public DigitalInput create(DigitalInputConfig config) {
        throw new UnsupportedOperationException("DigitalInput not supported");
    }

    @Override
    public Pwm create(PwmConfig config) {
        throw new UnsupportedOperationException("Pwm not supported");
    }

    @Override
    public I2C create(I2CConfig config) {
        throw new UnsupportedOperationException("I2C not supported");
    }

    @Override
    public Spi create(SpiConfig config) {
        throw new UnsupportedOperationException("SPI not supported");
    }
}
