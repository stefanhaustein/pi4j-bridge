package com.pi4j.usbbridge.mcp2221;

import java.util.Arrays;

import com.pi4j.io.IO;
import com.pi4j.io.IOConfig;
import com.pi4j.io.IOType;
import com.pi4j.io.gpio.digital.DigitalOutputConfig;
import com.pi4j.io.i2c.I2C;
import com.pi4j.io.i2c.I2CConfig;
import org.hid4java.*;

import com.pi4j.usbbridge.DirectContextBase;

public class Mcp2221 extends DirectContextBase {
    final HidDevice device;
    final byte[] sendBuffer = new byte[64];
    final byte[] receiveBuffer = new byte[64];
    final IO[] openIOs = new IO[4];

    static HidDevice findMcp2221() {
        HidServicesSpecification hidServicesSpecification = new HidServicesSpecification();
        hidServicesSpecification.setAutoStart(false);
        HidServices hidServices = HidManager.getHidServices(hidServicesSpecification);

        for (HidDevice hidDevice : hidServices.getAttachedHidDevices()) {
            if (hidDevice.getVendorId() == 0x04d8 && hidDevice.getProductId() == 0xdd) {
                return hidDevice;
            }
        }

        throw new IllegalStateException("MCP2221 Not found");
    }

    // TODO: Support multiple devices
    public Mcp2221() {
        device = findMcp2221();
        System.out.println("Found I2C device: " + device);
        device.open();

        byte[] request = new byte[64];
        byte[] response = new byte[64];
        request[0] = 0x10;

        device.write(request, 64, (byte) 0);
        device.read(response);

        System.out.println("MCP2221 Configuration: " + Arrays.toString(response));

        // TODO: Query and change on demand only; consider changing back after shutdown.
        setGpioConfiguration(Mcp2221.PinMode.GPIO, Mcp2221.PinMode.GPIO, Mcp2221.PinMode.GPIO, Mcp2221.PinMode.GPIO);
    }

    void prepareBuffer(int command) {
        Arrays.fill(sendBuffer, (byte) 0);
        sendBuffer[0] = (byte) command;
    }

    void transfer() {
        device.write(sendBuffer, 64, (byte) 0);
        device.read(receiveBuffer);
    }

    void setGpioConfiguration(PinMode mode0, PinMode mode1, PinMode mode2, PinMode mode3) {
        prepareBuffer(0x60);
        sendBuffer[7] = (byte) 0b1000_0000;
        sendBuffer[8] = (byte) mode0.ordinal();
        sendBuffer[9] = (byte) mode1.ordinal();
        sendBuffer[1] = (byte) mode2.ordinal();
        sendBuffer[11] = (byte) mode3.ordinal();
        transfer();
        if (receiveBuffer[0] != 0x60 || receiveBuffer[1] != 0) {
            throw new IllegalStateException("Failed to set GPIO config");
        }
        System.out.println("GPIO config result: " + Arrays.toString(receiveBuffer));
    }

    void setGpioDirection(int pin, GpioDirection direction) {
        prepareBuffer(0x50);
        sendBuffer[4 * pin + 4] = 1;
        sendBuffer[4 * pin + 5] = (byte) direction.ordinal();
        transfer();
        if (receiveBuffer[0] != 0x50 || receiveBuffer[1] != 0) {
            throw new IllegalStateException("Failed to set GPIO direction");
        }
        System.out.println("setGpioDirection result: " + Arrays.toString(receiveBuffer));
    }

    void setGpioValue(int pin, boolean value) {
        prepareBuffer(0x50);
        sendBuffer[4 * pin + 2] = 1;
        sendBuffer[4 * pin + 3] = value ? (byte) 1 : (byte) 0;
        transfer();
        if (receiveBuffer[0] != 0x50 || receiveBuffer[1] != 0) {
            throw new IllegalStateException("Failed to set GPIO value");
        }
        System.out.println("setGpioDirection result: " + Arrays.toString(receiveBuffer));
    }


    @Override
    public I2C create(I2CConfig config) {
        return new I2CImpl(this, config);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected IO createImpl(IOConfig ioConfig, IOType ioType) {
        return switch (ioType) {
            case I2C -> new I2CImpl(this, (I2CConfig) ioConfig);
            case DIGITAL_OUTPUT -> new DigitalOutputImpl(this, (DigitalOutputConfig) ioConfig);
            // TODO: Add Digital IO based on gpio methods.
            default -> throw new UnsupportedOperationException("Unsupported IO type: " + ioType);
        };
    }

    enum PinMode {
        GPIO, DEDICATED, ALT_0, ALT_1, ALT_2
    }

    enum GpioDirection {
        OUTPUT, INPUT
    }
}
