package org.pi4j.usbbridge.mcp2221;

import java.util.Arrays;
import org.hid4java.*;
import com.pi4j.io.SerialCircuitIO;

public class Mcp2221 {
    final HidDevice device;

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
    }


    public SerialCircuitIO openI2c(int address) {
        return new Mcp2221I2c(this, address);
    }

}
