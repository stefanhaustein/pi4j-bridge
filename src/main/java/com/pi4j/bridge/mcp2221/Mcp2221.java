package com.pi4j.bridge.mcp2221;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Function;

import com.pi4j.io.IO;
import com.pi4j.io.IOConfig;
import com.pi4j.io.IOType;
import com.pi4j.io.gpio.digital.DigitalOutputConfig;
import com.pi4j.io.i2c.I2C;
import com.pi4j.io.i2c.I2CConfig;
import com.pi4j.util.Delay;
import org.hid4java.*;

import com.pi4j.bridge.DirectContextBase;

public class Mcp2221 extends DirectContextBase {
    static final int RETRY_COUNT = 4;
    static final double TIMEOUT_MS = 200;

    final HidDevice device;
    final byte[] sendBuffer = new byte[64];
    final byte[] receiveBuffer = new byte[64];
    final IO[] openIOs = new IO[4];
    final Delay delay = new Delay();

    boolean i2cDirty = false;

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
        device.open();
        readI2cStatus();  // Make sure status is not null.
        setBaudRate(100_000);
        releaseI2c();
    }

    void setBaudRate(final int baudRate) {
        send(Command.STATUS_SET_PARAMETERS, sendBuffer -> {
            sendBuffer[3] = 0x20;
            sendBuffer[4] = (byte) (Math.round(12_000_000.0 / baudRate) - 2);
        });
    }

    I2cStatus readI2cStatus() {
        return receive(Command.STATUS_SET_PARAMETERS, I2cStatus::new);
    }

    void releaseI2c() {
        I2cStatus i2cStatus = readI2cStatus();
        if (i2cStatus.initialized()) {
            for (int i = 0; i < RETRY_COUNT; i++) {
                send(Command.STATUS_SET_PARAMETERS, sendBuffer -> { sendBuffer[2] = 0x10; });
                i2cStatus = readI2cStatus();
                if (!i2cStatus.timeout() && i2cStatus.scl() && i2cStatus.sda()) {
                    i2cDirty = false;
                    return;
                }
                delay.setMillis(10).materialize();
            }
        }
        i2cStatus = readI2cStatus();

        if (!i2cStatus.scl()) {
            i2cDirty = true;
            throw new IllegalStateException("SCL is low. I2C bus is busy or missing a pull-up resistor.");
        }
        if (!i2cStatus.sda()) {
            i2cDirty = true;
            throw new IllegalStateException("SDA is low. Missing pull-up resistor, I2C bus is busy or slave device in the middle of sending data.");
        }
        if (i2cStatus.timeout()) {
            i2cDirty = true;
            throw new IllegalStateException("I2C bus timeout");
        }

        i2cDirty = false;
    }

    void i2cWrite(Command command, int address, byte[] data, int offset, int length) {
        if (i2cDirty) {
            releaseI2c();
        }
        long startTime = System.currentTimeMillis();
        if (length > 60) {
            throw new UnsupportedOperationException("TODO: Support > 60 byte write");
        }
        do {
            if (transfer(
                    command,
                    sendBuffer -> {
                        sendBuffer[1] = (byte) length;
                        sendBuffer[2] = (byte) (length >>> 8);
                        sendBuffer[3] = (byte) ((address << 1) | 0);
                        System.arraycopy(data, offset, sendBuffer, 4, length);
                    },
                    receiveBuffer -> receiveBuffer[1] == 0)) {
                return;
            }
        } while (System.currentTimeMillis() - startTime < TIMEOUT_MS);
        i2cDirty = true;
        throw new IllegalStateException("I2C write timeout");
    }

    void i2cRead(Command command, int address, byte[] data, int offset, int length) {
        if (i2cDirty) {
            releaseI2c();
        }
        long startTime = System.currentTimeMillis();
        System.out.println("I2C_READ_DATA: " + length + " address: " + address);
        send(Command.I2C_READ_DATA, usbBuffer -> {
            usbBuffer[1] = (byte) length;
            usbBuffer[2] = (byte) (length >>> 8);
            usbBuffer[3] = (byte) ((address << 1) | 1);
        });

        int[] counters = new int[2]; // Hack for writing count/errorcoumt from a lambda...
        //
        while (counters[0] < length) {
            receive(Command.I2C_GET_DATA,
                    usbBuffer -> {
                        int count = usbBuffer[3] & 0xff;
                        if (count == 127 && counters[1]++ < 4) {
                            System.err.println("I2C_GET_DATA returned 127 bytes hickup");
                        } else if (count > 60) {
                            throw new IllegalStateException("Unexpected byte count " + count);
                        } else {
                            System.arraycopy(usbBuffer, 4, data, offset + counters[0], count);
                            counters[0] += count;
                        }
                        return null;
                    });
        }
    }

    <R> R transfer(Command command, Consumer<byte[]>  send, Function<byte[], R> receive) {
        synchronized (lock) {
            System.out.println("Command: " + command);
            Arrays.fill(sendBuffer, (byte) 0);
            sendBuffer[0] = command.code;
            if (send != null) {
                send.accept(sendBuffer);
            }
            for (int i = 0; i < (command.autoRetry ? RETRY_COUNT : 1); i++) {
                //delay.setMillis(100).materialize();
                System.out.println("- sending: " + Arrays.toString(sendBuffer));
                device.write(sendBuffer, 64, (byte) 0);
                //   delay.setMillis(10).materialize();
                Arrays.fill(receiveBuffer, (byte) 0);
                device.read(receiveBuffer);
                System.out.println("- received: " + Arrays.toString(receiveBuffer));
                if (receiveBuffer[0] != command.code) {
                    continue;
                }
                // If there is a handler for the received content, let it handle errors, too...
                if (receiveBuffer[1] == 0 || receive != null) {
                  return receive == null ? null : receive.apply(receiveBuffer);
                }
            }
            if (receiveBuffer[0] != command.code) {
                System.out.println("Unexpected command code loopback: " + (receiveBuffer[0] & 0xff) + "; expected " + command.code);
                throw new IllegalStateException("Unexpected command code loopback: " + (receiveBuffer[0] & 0xff) + "; expected " + command);
            }
            System.out.println("Error code received for command " + command  + " : " + receiveBuffer[1] + "; expected: 0");
            throw new IllegalStateException("Error code received for command " + command  + " : " + receiveBuffer[1] + "; expected: 0");
        }
    }

    void send(Command command, Consumer<byte[]> input) {
        transfer(command, input, null);
    }

    <R> R receive(Command command, Function<byte[], R> output) {
        return transfer(command, null, output);
    }

    void setGpioConfiguration(int pin, PinMode mode) {
        byte[] modes = new byte[4];
        receive(Command.GET_SRAM_SETTINGS,
                receivedBuffer -> {
                    System.arraycopy(receivedBuffer, 22, modes, 0, 4);
                    return null;
        });

        modes[pin] = (byte) mode.ordinal();

        send(Command.SET_SRAM_SETTINGS,
            sendBuffer -> {
                sendBuffer[7] = (byte) 0b1000_0000;
                System.arraycopy(modes, 0, sendBuffer, 8, 4);
            });
    }


    void setGpioDirection(int pin, GpioDirection direction) {
        send(Command.SET_GPIO_OUTPUT_VALUES, sendBuffer -> {
            sendBuffer[4 * pin + 4] = 1;
            sendBuffer[4 * pin + 5] = (byte) direction.ordinal();
        });
    }

    void setGpioValue(int pin, boolean value) {
        send(Command.SET_GPIO_OUTPUT_VALUES, sendBuffer -> {
            sendBuffer[4 * pin + 2] = 1;
            sendBuffer[4 * pin + 3] = value ? (byte) 1 : (byte) 0;
        });
    }


    @Override
    public I2C create(I2CConfig config) {
        return new Mcp2221I2C(this, config);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected IO createImpl(IOConfig ioConfig, IOType ioType) {
        return switch (ioType) {
            case I2C -> new Mcp2221I2C(this, (I2CConfig) ioConfig);
            case DIGITAL_OUTPUT -> new Mcp2221DigitalOutput(this, (DigitalOutputConfig) ioConfig);
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
