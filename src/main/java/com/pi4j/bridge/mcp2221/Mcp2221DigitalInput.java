package com.pi4j.bridge.mcp2221;

import com.pi4j.context.Context;
import com.pi4j.io.gpio.digital.*;

import java.util.function.Consumer;

class Mcp2221DigitalInput extends DigitalInputBase {
    private final Mcp2221 bridge;
    private final int pin;
    private DigitalState state = DigitalState.UNKNOWN;

    Mcp2221DigitalInput(Mcp2221 bridge, DigitalInputConfig config) {
        super(null, config);
        this.bridge = bridge;
        this.pin = config.getBcm();
        if (pin < 0 || pin > 3) {
            throw new IllegalArgumentException("Invalid pin number: " + pin);
        }
        if (bridge.openIOs[pin] != null) {
            throw new IllegalStateException("Pin " + pin + " is already in use");
        }
        bridge.setGpioConfiguration(pin, Mcp2221.PinMode.GPIO);
        bridge.setGpioDirection(pin, Mcp2221.GpioDirection.INPUT);
        bridge.openIOs[pin] = this;
        bridge.updateInputStates();
    }

    @Override
    public DigitalState state() {
        if (!hasListenersOrBindings()) {
            bridge.updateInputStates();
        }
        return state;
    }

    void setState(DigitalState newState) {
        if (this.state != newState) {
            this.state = newState;
            stateChangeEventManager.dispatch(
                    new DigitalStateChangeEvent<>(this, newState)
            );
        }
    }

    @Override
    public DigitalInput shutdownInternal(Context context) {
        super.shutdownInternal(context);
        bridge.openIOs[pin] = null;
        bridge.checkForInputListeners();
        return this;
    }

    @Override
    public DigitalInput addListener(DigitalStateChangeListener... listener) {
        super.addListener(listener);
        bridge.checkForInputListeners();
        return this;
    }

    public DigitalInput removeListener(DigitalStateChangeListener... listener) {
        super.removeListener(listener);
        bridge.checkForInputListeners();
        return this;
    }
}
