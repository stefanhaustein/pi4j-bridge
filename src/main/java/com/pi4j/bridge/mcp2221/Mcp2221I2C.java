package com.pi4j.bridge.mcp2221;

import com.pi4j.io.i2c.*;

class Mcp2221I2C extends I2CBase {
    private static final int I2C_CHUNK_SIZE = 60;

    private final Mcp2221 bridge;
    private final int address;

    byte[] ioBuffer = new byte[2];

    Mcp2221I2C(Mcp2221 bridge, I2CConfig config) {
        // TODO: Add a bus...
        super(null, config, null);

        this.bridge = bridge;
        this.config = config;
        this.address = config.device();
    }

    @Override
    public void writeThenRead(byte[] writeBuffer, int writeOffset, int writeLength, int readDelayNanos, byte[] readBuffer, int readOffset, int readLength) {
        if (writeLength > 0) {
            bridge.i2cWrite(Command.I2C_WRITE_DATA, address, writeBuffer, writeOffset, writeLength);
        }

        if (readLength > 0) {
            bridge.i2cRead(Command.I2C_READ_DATA, address, readBuffer, readOffset, readLength);
        }
    }

    // Ideally, I2CBase would delegate all of these to write/read/writeThenRead; not sure why this isn't the case already...

    @Override
    public int read() {
        read(ioBuffer, 0, 1);
        return ioBuffer[0] & 255;
    }

    @Override
    public int write(byte b) {
        ioBuffer[0] = b;
        write(ioBuffer, 0, 1);
        return 1;
    }

    @Override
    public int readRegister(int i) {
        ioBuffer[0] = (byte) i;
        writeThenRead(ioBuffer, 0, 1, 0, ioBuffer, 0, 1);
        return ioBuffer[0] & 255;
    }

    @Override
    public int readRegister(byte[] register, byte[] data, int offset, int length) {
        writeThenRead(register, 0, register.length, 0, data, offset, length);
        return length;
    }

    @Override
    public int readRegister(int i, byte[] bytes, int offset, int count) {
        ioBuffer[0] = (byte) i;
        writeThenRead(ioBuffer, 0, 1, 0, bytes, offset, count);
        return count;
    }

    @Override
    public int writeRegister(int i, byte b) {
        ioBuffer[0] = (byte) i;
        ioBuffer[1] = b;
        write(ioBuffer, 0, 2);
        return 1;
    }

    @Override
    public int writeRegister(int i, byte[] bytes, int offset, int length) {
        byte[] combined = new byte[length + 1];
        combined[0] = (byte) i;
        System.arraycopy(bytes, offset, combined, 1, length);
        write(combined, 0, combined.length);
        return length;
    }

    @Override
    public int writeRegister(byte[] register, byte[] data, int offset, int length) {
        byte[] combined = new byte[ register.length + length];
        System.arraycopy(register, 0, combined, 0, register.length);
        System.arraycopy(data, offset, combined, register.length, length);
        write(combined, 0, combined.length);
        return length;
    }
}
