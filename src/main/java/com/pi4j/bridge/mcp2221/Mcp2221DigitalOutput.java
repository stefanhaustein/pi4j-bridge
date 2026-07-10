package com.pi4j.bridge.mcp2221;

import com.pi4j.context.Context;
import com.pi4j.io.gpio.digital.*;

class Mcp2221DigitalOutput extends DigitalOutputBase {

    private final Mcp2221 bridge;
    final int pin;

    Mcp2221DigitalOutput(Mcp2221 bridge, DigitalOutputConfig config) {
        super(null, config);
        this.bridge = bridge;
        // bridge.checkConflictingConfig(config);
        pin = config.getBcm();
        if (pin < 0 || pin > 3) {
            throw new IllegalArgumentException("Invalid pin number: " + pin);
        }
        if (bridge.openIOs[pin] != null) {
            throw new IllegalStateException("Pin " + pin + " is already in use");
        }
        bridge.setGpioConfiguration(pin, Mcp2221.PinMode.GPIO);
        bridge.setGpioDirection(pin, Mcp2221.GpioDirection.OUTPUT);
    }

    @Override
    public DigitalOutput state(DigitalState state) {
        super.state(state);
        bridge.setGpioValue(pin, state.isHigh());
        return this;
    }

    @Override
    public DigitalOutput shutdownInternal(Context context) {
        super.shutdownInternal(context);
        bridge.openIOs[pin] = null;
        return this;
    }

}
