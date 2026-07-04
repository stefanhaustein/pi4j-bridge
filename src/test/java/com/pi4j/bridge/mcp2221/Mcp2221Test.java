package com.pi4j.bridge.mcp2221;

import com.pi4j.context.Context;
import com.pi4j.drivers.sensor.Sensor;
import com.pi4j.drivers.sensor.SensorDetector;
import com.pi4j.drivers.sensor.environment.bmx280.Bmx280Driver;
import com.pi4j.drivers.sensor.environment.scd4x.Scd4xDriver;
import com.pi4j.io.SerialCircuitIO;
import com.pi4j.io.i2c.I2C;
import com.pi4j.io.i2c.I2CConfig;

import java.util.Arrays;
import java.util.List;

public class Mcp2221Test {

    static void readSensors(Context context) {
        System.out.println("Detecting sensors...");
        List<Sensor> sensorList = SensorDetector.detectI2cSensors(context, 0);
        System.out.println("Found: " + sensorList);

        for (Sensor sensor : sensorList) {
            double[] values = new double[sensor.getDescriptor().getValues().size()];
            sensor.readMeasurement(values);
            System.out.println("Sensor: " + sensor + ": " + Arrays.toString(values));
        }

    }

    static void main() {
        Mcp2221 context = new Mcp2221();
        context.setBaudRate(50_000);

/*
        I2C scd4xI2c = context.create(I2CConfig.newBuilder(context).bus(0).device(Scd4xDriver.I2C_ADDRESS).build());
        Scd4xDriver scd4xDriver = new Scd4xDriver(scd4xI2c);
        System.out.println(scd4xDriver.readMeasurement());

        SerialCircuitIO bmx280i2c = context.create(I2CConfig.newBuilder(context).bus(0).device(Bmx280Driver.ADDRESS_BME_280_PRIMARY).build());
        Bmx280Driver bmx280Driver = new Bmx280Driver(bmx280i2c);
        System.out.println(bmx280Driver.readMeasurement());
*/

       readSensors(context);
/*
        DigitalOutput red = bridge.create(DigitalOutputConfig.newBuilder(bridge).bcm(1).build());
        DigitalOutput yellow = bridge.create(DigitalOutputConfig.newBuilder(bridge).bcm(2).build());
        DigitalOutput green = bridge.create(DigitalOutputConfig.newBuilder(bridge).bcm(3).build());


        Delay delay = new Delay();

        Mcp23017Driver expander = new Mcp23017Driver(context.create(I2CConfig.newBuilder().bus(0).device(0x27)), null);
        expander.setIoDirections(0xffff, ConfigurableIoExpander.Direction.OUTPUT);
        TrafficLight trafficLight1 = new TrafficLight(
                expander.getOutput(8), expander.getOutput(9), expander.getOutput(10));

        while (true) {
            trafficLight1.setState(TrafficLight.State.RED);

            delay.setMillis(2000).materialize();
            trafficLight1.setState(TrafficLight.State.RED_YELLOW);

            delay.setMillis(1000).materialize();
            trafficLight1.setState(TrafficLight.State.GREEN);

            delay.setMillis(2000).materialize();
            trafficLight1.setState(TrafficLight.State.YELLOW);

            delay.setMillis(1000).materialize();
        }
        */

    }
}
