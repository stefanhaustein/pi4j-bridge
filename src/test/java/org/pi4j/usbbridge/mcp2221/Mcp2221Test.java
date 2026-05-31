package org.pi4j.usbbridge.mcp2221;

import com.pi4j.drivers.sensor.environment.bmx280.Bmx280Driver;
import com.pi4j.io.SerialCircuitIO;

public class Mcp2221Test {

    static void main() {
        Mcp2221 bridge = new Mcp2221();

        SerialCircuitIO i2c = bridge.openI2c(Bmx280Driver.ADDRESS_BME_280_PRIMARY);

        Bmx280Driver driver = new Bmx280Driver(i2c);
        System.out.println(driver.readMeasurement());
    }
}
