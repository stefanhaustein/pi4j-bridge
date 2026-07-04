package com.pi4j.bridge.mcp2221;

import java.util.Arrays;

class I2cStatus {
    private final byte[] data;

    I2cStatus(byte[] data) {
        this.data = Arrays.copyOf(data, data.length);
    }

    boolean initialized() {
        return data[21] != 0;
    }

    boolean timeout() {
        return data[8] != 0;
    }

    boolean ack() {
        return (data[20] & (1 << 6)) == 0;
    }

    boolean scl() {
        return data[22] != 0;
    }

    boolean sda() {
        return data[23] != 0;
    }


}
