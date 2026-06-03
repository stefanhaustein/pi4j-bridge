package com.pi4j.usbbridge.mcp2221;

import com.pi4j.io.i2c.*;

class I2CImpl extends I2CBase {
    private static final int I2C_CHUNK_SIZE = 60;

    private static final byte I2C_WRITE_DATA = (byte) 0x90;
    private static final byte I2C_READ_DATA = (byte) 0x91;
    private static final byte I2C_GET_DATA = (byte) 0x40;

    private final Mcp2221 bridge;
    private final int address;

    byte[] usbBuffer = new byte[64];
    byte[] ioBuffer = new byte[2];

    I2CImpl(Mcp2221 bridge, I2CConfig config) {
        // TODO: Add a bus...
        super(null, config, null);

        this.bridge = bridge;
        this.config = config;
        this.address = config.device();
    }

    @Override
    public void writeThenRead(byte[] writeBuffer, int writeOffset, int writeLength, int readDelayNanos, byte[] readBuffer, int readOffset, int readLength) {
        if (writeLength > 0) {
            if (writeLength > I2C_CHUNK_SIZE) {
                throw new IllegalArgumentException("TODO: Support > 60 byte write");
            }
            usbBuffer[0] = I2C_WRITE_DATA;
            usbBuffer[1] = (byte) writeLength;
            usbBuffer[2] = (byte) (writeLength >>> 8);
            usbBuffer[3] = (byte) ((address << 1) | 0);
            System.arraycopy(writeBuffer, writeOffset, usbBuffer, 4, writeLength);

            bridge.device.write(usbBuffer, 64, (byte) 0);
            bridge.device.read(usbBuffer);

            if (usbBuffer[0] != I2C_WRITE_DATA) {
                throw new IllegalStateException("Command echo failed; was: " + usbBuffer[0]);
            }
            if (usbBuffer[1] != 0) {
                throw new IllegalStateException("Unexpected response code " + usbBuffer[1]);
            }
        }

        if (readLength > 0) {
            usbBuffer[0] = I2C_READ_DATA;
            usbBuffer[1] = (byte) readLength;
            usbBuffer[2] = (byte) (readLength >>> 8);
            usbBuffer[3] = (byte) ((address << 1) | 1);

            bridge.device.write(usbBuffer, 64, (byte) 0);
            bridge.device.read(usbBuffer);

            if (usbBuffer[0] != I2C_READ_DATA) {
                throw new IllegalStateException("Command echo failed; was: " + usbBuffer[0]);
            }
            if (usbBuffer[1] != 0) {
                throw new IllegalStateException("Unexpected response code " + usbBuffer[1]);
            }

            int readCount = 0;
            while (readCount < readLength) {
                usbBuffer[0] = I2C_GET_DATA;
                bridge.device.write(usbBuffer, 64, (byte) 0);
                bridge.device.read(usbBuffer);
                if (usbBuffer[0] != I2C_GET_DATA) {
                    throw new IllegalStateException("Command echo failed; was: " + usbBuffer[0]);
                }
                if (usbBuffer[1] != 0) {
                    throw new IllegalStateException("Unexpected response code " + usbBuffer[1]);
                }
                int count = usbBuffer[3] & 0xff;
                if (count > 60) {
                    throw new IllegalStateException("Unexpected byte count " + count);
                }
                System.arraycopy(usbBuffer, 4, readBuffer, readOffset + readCount, count);
                readCount += count;
            }
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
