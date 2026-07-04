package com.pi4j.bridge.mcp2221;

enum Command {
    GET_GPIO_VALUES(0x51, true),
    GET_SRAM_SETTINGS(0x61, true),

    I2C_GET_DATA(0x40, false),
    I2C_WRITE_DATA(0x90, false),
    I2C_WRITE_DATA_NO_STOP(0x94, false),
    I2C_WRITE_DATA_REPEATED_START(0x92, false),
    I2C_READ_DATA(0x91, false),

    SET_GPIO_OUTPUT_VALUES(0x50, true),
    SET_SRAM_SETTINGS(0x60, true),

    STATUS_SET_PARAMETERS(0x10, true);

    public final byte code;
    public final boolean autoRetry;

    Command(int code, boolean autoRetry) {
        this.code = (byte) code;
        this.autoRetry = autoRetry;
    }
}
