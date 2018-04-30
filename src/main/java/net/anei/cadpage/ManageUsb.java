package net.anei.cadpage;

import java.util.HashMap;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;

/**
 * Class probing USB connections looking to establish communications with a
 * Tetra pager
 */

public class ManageUsb {
  
  private static final String ACTION_USB_PERMISSION = "net.anei.cadpage.ManageUsb.USB_PERMISSION";

  private UsbManager mgr = null;
  private CommDevice commDevice = null;

  public void probe(Context context) {
    Log.v("Starting USB Probe");
    if (mgr == null) mgr = (UsbManager) context.getSystemService(Context.USB_SERVICE);

    Log.v("\nUSB accessory list");
    UsbAccessory[] accList = mgr.getAccessoryList();
    if (accList != null) {
      for (UsbAccessory acc : accList) {
        Log.v(acc.toString());
      }
    }

    Log.v("\nUSB Host list");
    HashMap<String, UsbDevice> devMap = mgr.getDeviceList();
    if (devMap != null) {
      for (String devKey : devMap.keySet()) {
        UsbDevice usbDev = devMap.get(devKey);
        Log.v("Device Key:" + devKey);
        dumpUsbDevice(usbDev);
        connectDevice(context, usbDev);
      }
    }
  }

  private void dumpUsbDevice(UsbDevice usbDev) {
    Log.v ("Device Name:" + usbDev.getDeviceName() +
        "\n   VendorId:" + Integer.toHexString(usbDev.getVendorId()) +
        "\n   ProductId:" + Integer.toHexString(usbDev.getProductId()) +
        "\n   DeviceId:" + Integer.toHexString(usbDev.getDeviceId()) +
        "\n   Class:" + Integer.toHexString(usbDev.getDeviceClass()) +
        "\n   Subclass:" + Integer.toHexString(usbDev.getDeviceSubclass()) +
        "\n   Protocol:" + Integer.toHexString(usbDev.getDeviceProtocol()) +
        "\n   Permitted:" + mgr.hasPermission(usbDev));
    for (int intrNdx = 0; intrNdx < usbDev.getInterfaceCount(); intrNdx++) {
      Log.v("   Interface " + intrNdx);
      UsbInterface usbIntr = usbDev.getInterface(intrNdx);
      Log.v("      Id:" + Integer.toHexString(usbIntr.getId()) +
          "\n      Class:" + Integer.toHexString(usbIntr.getInterfaceClass()) +
          "\n      Sublass:" + Integer.toHexString(usbIntr.getInterfaceSubclass()) +
          "\n      Protocol:" + Integer.toHexString(usbIntr.getInterfaceProtocol()));
      for (int endNdx = 0; endNdx < usbIntr.getEndpointCount(); endNdx++) {
        Log.v("      Endpoint " + endNdx);
        UsbEndpoint usbEnd = usbIntr.getEndpoint(endNdx);
        Log.v("         Address:" + Integer.toHexString(usbEnd.getAddress()) +
            "\n         Attributes:" + Integer.toHexString(usbEnd.getAttributes()) +
            "\n         Direction:" + usbEnd.getDirection() +
            "\n         Interval:" + usbEnd.getInterval() +
            "\n         PacketSize:" + usbEnd.getMaxPacketSize() +
            "\n         Type:" + usbEnd.getType());

      }
    }
  }

  public void onReceive(Context context, Intent intent) {
    if (mgr == null) mgr = (UsbManager) context.getSystemService(Context.USB_SERVICE);
    ContentQuery.dumpIntent(intent);

    String action = intent.getAction();
    if (action == null) return;
    switch (action) {

    case UsbManager.ACTION_USB_ACCESSORY_ATTACHED:
    case UsbManager.ACTION_USB_ACCESSORY_DETACHED:
      Log.v(action);
      UsbAccessory acc = intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
      Log.v(acc == null ? "null" : acc.toString());
      break;

    case UsbManager.ACTION_USB_DEVICE_ATTACHED:
    case UsbManager.ACTION_USB_DEVICE_DETACHED:
      Log.v(action);
      UsbDevice dev = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
      Log.v("Device:" + dev);

      if (action.equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
        connectDevice(context, dev);
      } else {
        if (dev != null && commDevice != null) {
          UsbDevice dev2 = commDevice.getDevice();
          if (dev2 != null && dev.getDeviceName().equals(dev.getDeviceName())) {
            commDevice.close();
            commDevice = null;
          }
        }
      }
      break;

    case ACTION_USB_PERMISSION:
      synchronized (this) {
        UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
        UsbAccessory accessory = intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);

        if (device != null) {
          if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
            Log.w("Permission granted for USB device:" + device);
            connectDevice(context, device);
          } else {
            Log.w("Permission deined for USB device:" + device);
          }
        } else if (accessory != null) {
          if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
            Log.w("Permission granted for USB accessory:" + accessory);
          } else {
            Log.w("Permission denied for USB accessory:" + accessory);
          }
        }
      }
    }
  }

  private static final int SEPURA_VENDOR_ID = 0x403;
  private static final int SEPURA_PRODUCT_ID = 0x6001;

  private void connectDevice(Context context, UsbDevice device) {

    if (commDevice != null) return;

    if (device.getVendorId() == SEPURA_VENDOR_ID && device.getProductId() == SEPURA_PRODUCT_ID) {

      // If we do not have permission to use this device, ask for it
      if (!mgr.hasPermission(device)) {
        Intent intent = new Intent(ACTION_USB_PERMISSION);
        intent.setClass(context, UsbReceiver.class);
        PendingIntent pIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        mgr.requestPermission(device, pIntent);
        return;
      }

      if (commDevice == null) {
        commDevice = new CommDevice(mgr, device);
        if (!commDevice.start()) commDevice = null;
      }
    }
  }

  private static final ManageUsb instance = new ManageUsb();
  
  public static ManageUsb instance() {
    return instance;
  }
}
