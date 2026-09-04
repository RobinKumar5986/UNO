package com.kgjr.uno.screens.fragments.exeHelper;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.util.Log;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** USB serial connection to the board: permission, open, write, close. */
public class SerialLink implements SerialInputOutputManager.Listener {

    public interface Listener {
        void onLog(String message);

        /** The port dropped on its own — a pulled cable, a loose plug. Not a deliberate close. */
        void onDisconnected(String reason);
    }

    private static final String TAG = "SerialLink";
    private static final int BAUD_RATE = 9600;
    private static final int WRITE_TIMEOUT_MS = 1000;

    private final Context context;
    private final Listener listener;
    private final String permissionAction;
    private final UsbManager usbManager;

    private UsbSerialPort port;
    private SerialInputOutputManager ioManager;
    private ExecutorService ioExecutor;
    private boolean receiverRegistered;

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!permissionAction.equals(intent.getAction())) return;

            if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                connect();
            } else {
                log("USB permission denied");
            }
        }
    };

    public SerialLink(Context context, Listener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
        this.permissionAction = this.context.getPackageName() + ".USB_PERMISSION";
        this.usbManager = (UsbManager) this.context.getSystemService(Context.USB_SERVICE);
    }

    public void register() {
        if (receiverRegistered) return;
        receiverRegistered = true;

        IntentFilter filter = new IntentFilter(permissionAction);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(usbReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            context.registerReceiver(usbReceiver, filter);
        }
    }

    public void unregister() {
        if (!receiverRegistered) return;
        receiverRegistered = false;
        try {
            context.unregisterReceiver(usbReceiver);
        } catch (IllegalArgumentException ignored) {
        }
    }

    public boolean isConnected() {
        return port != null;
    }

    /** Opens the first serial device, asking for USB permission first when needed. */
    public void connect() {
        if (usbManager == null) {
            log("USB not available on this device");
            return;
        }

        List<UsbSerialDriver> drivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);
        if (drivers.isEmpty()) {
            log("No USB serial device found");
            return;
        }

        UsbSerialDriver driver = drivers.get(0);
        UsbDevice device = driver.getDevice();

        if (!usbManager.hasPermission(device)) {
            Intent intent = new Intent(permissionAction);
            intent.setPackage(context.getPackageName());
            int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_MUTABLE : 0;
            usbManager.requestPermission(device, PendingIntent.getBroadcast(context, 0, intent, flags));
            return;
        }
        open(driver);
    }

    public void reconnect() {
        close();
        connect();
    }

    private void open(UsbSerialDriver driver) {
        close();
        try {
            UsbSerialPort serialPort = driver.getPorts().get(0);
            serialPort.open(usbManager.openDevice(driver.getDevice()));
            serialPort.setParameters(BAUD_RATE, UsbSerialPort.DATABITS_8,
                    UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);

            port = serialPort;
            ioManager = new SerialInputOutputManager(serialPort, this);
            ioExecutor = Executors.newSingleThreadExecutor();
            ioExecutor.submit(ioManager);

            log("Connected");
        } catch (Exception e) {
            log("Failed to open port: " + e.getMessage());
            close();
        }
    }

    public boolean write(String command) {
        UsbSerialPort serialPort = port;
        if (serialPort == null) {
            log("Not connected");
            return false;
        }
        try {
            serialPort.write((command + "\n").getBytes(StandardCharsets.UTF_8), WRITE_TIMEOUT_MS);
            log("Sent: " + command);
            return true;
        } catch (IOException e) {
            log("Write failed: " + e.getMessage());
            return false;
        }
    }

    public void close() {
        if (ioManager != null) {
            ioManager.stop();
            ioManager = null;
        }
        if (ioExecutor != null) {
            ioExecutor.shutdownNow();
            ioExecutor = null;
        }
        if (port != null) {
            try {
                port.close();
            } catch (IOException ignored) {
            }
            port = null;
        }
    }

    @Override
    public void onNewData(byte[] data) {
        log("Received: " + new String(data, StandardCharsets.UTF_8).trim());
    }

    @Override
    public void onRunError(Exception e) {
        String reason = "Connection lost: " + e.getMessage();
        log(reason);
        close();

        if (listener != null) listener.onDisconnected(reason);
    }

    private void log(String message) {
        Log.d(TAG, message);
        if (listener != null) listener.onLog(message);
    }
}