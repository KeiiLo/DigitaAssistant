package com.wercup.rcup.digitalassistant.BLEService;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.SparseArray;
import android.widget.Toast;

import com.wercup.rcup.digitalassistant.MainActivity;
import com.wercup.rcup.digitalassistant.Tools.SensorTagData;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by KeiLo on 09/12/16.
 */
public class BLEService implements BluetoothAdapter.LeScanCallback {
    /**
 * Log/Debug
 */
private static final String TAG = "BLEService";

    /**
     * Cheat
     */
//    private static final String DEVICE_NAME = "Smart Sole 001";
//    private static final String DEVICE_MAC = "D5:5A:3E:35:3D:34";
    private static final String DEVICE_MAC = "C9:40:D6:D4:10:F8";

    /**
     * All services/characteristics/descriptor UUIDs
     */
    /**
     * Energy Service
     */
    private static final UUID ENERGY_SERVICE = UUID.fromString("00002300-1212-efde-1523-785fef13d123");
    private static final UUID ENERGY_DATA_CHAR = UUID.fromString("00002301-1212-efde-1523-785fef13d123");
    private static final UUID ENERGY_CONFIG_CHAR = UUID.fromString("00002302-1212-efde-1523-785fef13d123");
    /**
     * Accelerometer Service
     */
    private static final UUID ACCEL_SERVICE = UUID.fromString("00002400-1212-efde-1523-785fef13d123");
    private static final UUID ACCEL_DATA_CHAR = UUID.fromString("00002401-1212-efde-1523-785fef13d123");
    private static final UUID ACCEL_CONFIG_CHAR = UUID.fromString("00002402-1212-efde-1523-785fef13d123");
    /**
     * Step Counter Service
     */
    private static final UUID PRESSURE_SERVICE = UUID.fromString("00002500-1212-efde-1523-785fef13d123");
    private static final UUID PRESSURE_DATA_CHAR = UUID.fromString("00002501-1212-efde-1523-785fef13d123");
    private static final UUID PRESSURE_CONFIG_CHAR = UUID.fromString("00002502-1212-efde-1523-785fef13d123");
    /**
     * Client Configuration Descriptor
     */
    private static final UUID CONFIG_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");


    /**
     * BluetoothGatt attributes
     */
    private BluetoothGatt mGatt;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private BluetoothDevice mDevice;
    private ScanCallback scanCallBack;
    private SparseArray<BluetoothDevice> mDevices;

    /**
     * Class instance (Singleton service)
     */
    private static BLEService mInstance;

    /**
     * Guard against unset service notifications
     */
    private boolean mEnabled;
    private boolean scanning = false;
    /**
     * Context of the instance
     */
    private Context mContext;

    public static BLEService getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new BLEService(context);
        }
        return mInstance;
    }

    private BLEService(Context context) {
        mContext = context;
        BluetoothManager mBluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        mDevices = new SparseArray<>();
    }

    /**
     * Just a bunch of getters/setters
     */
    public BluetoothGatt getmGatt() {
        return mGatt;
    }

    public BluetoothAdapter getmBluetoothAdapter() {
        return mBluetoothAdapter;
    }

    /**
     * Handler to process multiple events on the main thread
     **/
    private static final int MSG_ENERGY = 301;
    private static final int MSG_ENERGY_CONFIG = 302;
    private static final int MSG_ACCEL = 401;
    private static final int MSG_ACCEL_CONFIG = 402;
    private static final int MSG_PRESSURE = 501;
    private static final int MSG_PRESSURE_CONFIG = 502;
    private static final int MSG_STATE = 200;
    private static final int MSG_PROGRESS = 201;
    private static final int MSG_DISMISS = 202;
    private static final int MSG_CLEAR = 203;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            byte[] trame;
            BluetoothGattCharacteristic mCharacteristic;
            switch (msg.what) {
                case MSG_ACCEL:
                    mCharacteristic = (BluetoothGattCharacteristic) msg.obj;
                    trame = mCharacteristic.getValue();
                    if (trame == null) {
                        Log.w(TAG, "Error obtaining accel value");
                        return;
                    } else if (mEnabled) {
                        Intent intent = new Intent();
                        intent.setAction(MainActivity.NOTIFICATION_SERVICE);
                        mContext.sendBroadcast(intent.putExtra("accel", trame));
                    }
                    break;
                case MSG_PRESSURE:
                    mCharacteristic = (BluetoothGattCharacteristic) msg.obj;
                    trame = mCharacteristic.getValue();
                    if (trame == null) {
                        Log.w(TAG, "Error obtaining pressure value");
                        return;
                    } else if (mEnabled) {
                        Intent intent = new Intent();
                        intent.setAction(MainActivity.NOTIFICATION_SERVICE);
                        mContext.sendBroadcast(intent.putExtra("pressure", trame));
                    }
                    break;
                case MSG_ENERGY:
                    mCharacteristic = (BluetoothGattCharacteristic) msg.obj;
                    trame = mCharacteristic.getValue();
                    if (trame == null) {
                        Log.w(TAG, "Error obtaining energy value");
                        return;
                    } else if (mEnabled) {
                        Intent intent = new Intent();
                        intent.setAction(MainActivity.NOTIFICATION_SERVICE);
                        mContext.sendBroadcast(intent.putExtra("energy", trame));
                    }
                    break;

                case MSG_ACCEL_CONFIG:
                    mCharacteristic = (BluetoothGattCharacteristic) msg.obj;
                    trame = mCharacteristic.getValue();
                    if (trame == null) {
                        Log.w(TAG, "Error obtaining accel config return value");
                        return;
                    } else {
                        Log.i(TAG, "Response From Accel" + SensorTagData.bytesToHex(trame));
                        Intent intent = new Intent();
                        intent.setAction(MainActivity.NOTIFICATION_SERVICE);
                        mContext.sendBroadcast(intent.putExtra("accelconfig", trame));
                    }
                    break;
                case MSG_PRESSURE_CONFIG:
                    mCharacteristic = (BluetoothGattCharacteristic) msg.obj;
                    trame = mCharacteristic.getValue();
                    if (trame == null) {
                        Log.w(TAG, "Error obtaining pressure config return value");
                        return;
                    } else {
                        Log.i(TAG, "Response From Pressure" + SensorTagData.bytesToHex(trame));
                        Intent intent = new Intent();
                        intent.setAction(MainActivity.NOTIFICATION_SERVICE);
                        Log.e(TAG, "Pressure config is :" + SensorTagData.bytesToHex(trame));
                        mContext.sendBroadcast(intent.putExtra("pressureconfig", trame));
                    }
                    break;
                case MSG_ENERGY_CONFIG:
                    mCharacteristic = (BluetoothGattCharacteristic) msg.obj;
                    trame = mCharacteristic.getValue();
                    if (trame == null) {
                        Log.w(TAG, "Error obtaining energy config return value");
                        return;
                    } else {
                        Log.i(TAG, "Response From Energy" + SensorTagData.bytesToHex(trame));
                        Intent intent = new Intent();
                        intent.setAction(MainActivity.NOTIFICATION_SERVICE);
                        mContext.sendBroadcast(intent.putExtra("energyconfig", trame));
                    }
                    break;
                case MSG_STATE:
                    boolean connected = (boolean) msg.obj;
                    Intent intent = new Intent();
                    intent.setAction(MainActivity.NOTIFICATION_SERVICE);
                    mContext.sendBroadcast(intent.putExtra("state", connected));

                case MSG_PROGRESS:
//                    mProgress.setMessage((String) msg.obj);
//                    if (!mProgress.isShowing()) {
//                        mProgress.show();
//                    }
                    break;
                case MSG_DISMISS:
                    readEnergyConfig(getmGatt());
                    break;
                case MSG_CLEAR:
//                    clearDisplayValues();
                    break;
            }
        }
    };

    /**
     * Allow the scan to be stopped by making two threads
     */

    /* private Runnable mStopRunnable = new Runnable() {
        @Override
        public void run() {
            stopScan();
        }
    };

    private Runnable mStartRunnable = new Runnable() {
        @Override
        public void run() {
            startScan();
        }
    };

    private Runnable mConnectToDevice = new Runnable() {
        @Override
        public void run() {
            connectToDevice(mDevice);
        }
    }; */

    /**
     * Explicit function name FTW
     */
    public void startScan() {
        Log.d(TAG, "Scanning devices");
        if (!scanning) {
            scanning = !scanning;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Log.d(TAG, "start le scan");
                ScanFilter scanFilter = new ScanFilter.Builder()
                        .build();
                ArrayList<ScanFilter> filters = new ArrayList<>();
                filters.add(scanFilter);

                Log.e(TAG, "Scanner 21");
                ScanSettings settings = new ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                        .build();
                scanCallBack = new ScanCallback() {
                    @Override
                    public void onScanResult(int callbackType, ScanResult result) {
                        processResult(result);
                    }

                    @Override
                    public void onBatchScanResults(List<ScanResult> results) {
                        super.onBatchScanResults(results);
                    }

                    @Override
                    public void onScanFailed(int errorCode) {
                        super.onScanFailed(errorCode);
                    }
                };

                mBluetoothLeScanner.startScan(filters, settings, scanCallBack);
            } else {
                mBluetoothAdapter.startLeScan(this);
            }
        }
    }

    /**
     * Method for API 21 or higher
     */
    private void processResult(ScanResult result) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Log.d(TAG, "process result");
            BluetoothDevice device = result.getDevice();
            if (device != null && device.getName() != null) {
                if (device.getAddress().equals(DEVICE_MAC)) {
//                    if (device.getAddress().equals(DEVICE_MAC)) {
                    mDevice = device;
                    Log.e(TAG, "Device found: " + mDevice.getName() + ": " + mDevice.getAddress());
                    stopScan();
                }
            }
        }
    }

    /**
     * Explicit function name FTW
     */
    private void stopScan() {
        scanning = !scanning;
        if (mDevice == null) {
            Toast.makeText(mContext, "Couldn't find our device", Toast.LENGTH_LONG).show();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mBluetoothLeScanner.stopScan(scanCallBack);
        } else {
            mBluetoothAdapter.stopLeScan(this);
        }
        if (mDevice != null) {
            connectToDevice(mDevice);
        }
    }


    /**
     * Method for API 18
     */
    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
        Log.i(TAG, "New LE Device: " + device.getAddress() + " @ " + rssi);
        if (DEVICE_MAC.equals(device.getAddress())) {
            Log.e(TAG, "User's Mac Address is:" + DEVICE_MAC);
            Log.e(TAG, "Device found: " + device.getName() + ": " + device.getAddress());
            mDevices.put(device.hashCode(), device);
            mDevice = device;
            stopScan();
        }
    }

    /**
     * Explicit function name FTW
     */
    private BluetoothGatt connectToDevice(BluetoothDevice device) {
        Toast.makeText(mContext, "Connecting to " + mDevice.getName(), Toast.LENGTH_SHORT).show();
        if (mGatt == null) {
            mGatt = device.connectGatt(mContext, true, mGattCallback);
        }
        Toast.makeText(mContext, "Connected", Toast.LENGTH_SHORT).show();
        return mGatt;
    }


    /**
     * This is the main point for the Bluetooth service to work,
     * from enabling notifications en sending data to the Main Activity
     */
    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        private int mState = 0;

        private void reset() {
            mState = 0;
        }

        private void advance() {
            Log.e("Advance", "Called ! mState is " + mState);
            mState++;
        }


        /**
         * Pretty explicit but hey. In order to receive notifications from the device,
         *  or be able to send data to it
         */
        private void setNotifyNextSensor(BluetoothGatt gatt) {
            mEnabled = false;
            BluetoothGattCharacteristic characteristic;
            Log.e("mState", String.valueOf(mState));
            switch (mState) {
                case 0:
                    Log.d(TAG, "Set notify pressure");
                    characteristic = gatt.getService(PRESSURE_SERVICE)
                            .getCharacteristic(PRESSURE_DATA_CHAR);
                    break;
                case 1:
                    Log.d(TAG, "Enabling pressure config");
                    characteristic = gatt.getService(PRESSURE_SERVICE)
                            .getCharacteristic(PRESSURE_CONFIG_CHAR);
                    break;
                default:
                    mEnabled = true;
                    mHandler.sendEmptyMessage(MSG_DISMISS);
                    BLEService.readPressureConfig(getmGatt());
                    Log.i(TAG, "All Sensors Enabled");
                    return;
            }

            /*
             * Enable local notifications
             */
            gatt.setCharacteristicNotification(characteristic, true);

            /*
             * Enable remote indication -> Mainly for settings purpose
             */
            if (characteristic.getUuid().equals(PRESSURE_CONFIG_CHAR)) {
                Log.e("BLE", "Indication enabled on Energy config");
                BluetoothGattDescriptor desc = characteristic.getDescriptor(CONFIG_DESCRIPTOR);
                desc.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                gatt.writeDescriptor(desc);
            }
            /*
             * Enable remote notifications -> listening to the data that is broadcast by the Device
             */
            BluetoothGattDescriptor desc = characteristic.getDescriptor(CONFIG_DESCRIPTOR);
            desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            gatt.writeDescriptor(desc);
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.d(TAG, "Connection State Change: " + status + " -> " + connectionState(newState));
            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                /*
                 * Once successfully connected, we must next discover all the services on the
                 * device before we can read and write their characteristics.
                 */
                gatt.discoverServices();
                mHandler.sendMessage(Message.obtain(null, MSG_STATE, true));
            } else if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_DISCONNECTED) {
                /*
                 * If the device is disconnected, start scanning
                 */
                Log.e(TAG, "Lost device");
                startScan();
                mHandler.sendMessage(Message.obtain(null, MSG_STATE, false));
            } else if (status != BluetoothGatt.GATT_SUCCESS) {
                /*
                 * If there is a failure at any step, start scanning
                 */
                Log.e(TAG, "Unknown error");
//                gatt.disconnect();
//                gatt.close();
                mHandler.sendMessage(Message.obtain(null, MSG_STATE, false));

                startScan();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d(TAG, "Services Discovered: " + status);
            reset();
            setNotifyNextSensor(gatt);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {

            if (PRESSURE_DATA_CHAR.equals(characteristic.getUuid())) {
                mHandler.sendMessage(Message.obtain(null, MSG_PRESSURE, characteristic));
            }
            if (PRESSURE_CONFIG_CHAR.equals(characteristic.getUuid())) {
                mHandler.sendMessage(Message.obtain(null, MSG_PRESSURE_CONFIG, characteristic));
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            advance();
            setNotifyNextSensor(gatt);
        }
    };

    private String connectionState(int status) {
        switch (status) {
            case BluetoothProfile.STATE_CONNECTED:
                return "Connected";
            case BluetoothProfile.STATE_DISCONNECTED:
                return "Disconnected";
            case BluetoothProfile.STATE_CONNECTING:
                return "Connecting";
            case BluetoothProfile.STATE_DISCONNECTING:
                return "Disconnecting";
            default:
                return String.valueOf(status);
        }
    }

    private static void readEnergyConfig(BluetoothGatt gatt) {
        Log.e(TAG, "Read Energy config");
        byte[] readConfig = new byte[]{(byte) 0x11};
        BluetoothGattCharacteristic readConfigChar = gatt.getService(BLEService.ENERGY_SERVICE).getCharacteristic(BLEService.ENERGY_CONFIG_CHAR);
        readConfigChar.setValue(readConfig);
        gatt.writeCharacteristic(readConfigChar);
    }

    public static void readAccelConfig(BluetoothGatt gatt) {
        byte[] readConfig = new byte[]{(byte) 0x21};
        BluetoothGattCharacteristic readConfigChar = gatt.getService(BLEService.ACCEL_SERVICE).getCharacteristic(BLEService.ACCEL_CONFIG_CHAR);
        readConfigChar.setValue(readConfig);
        gatt.writeCharacteristic(readConfigChar);
    }

    public static void readPressureConfig(BluetoothGatt gatt) {
        byte[] readConfig = new byte[]{(byte) 0x31};
        BluetoothGattCharacteristic readConfigChar = gatt.getService(BLEService.PRESSURE_SERVICE).getCharacteristic(BLEService.PRESSURE_CONFIG_CHAR);
        readConfigChar.setValue(readConfig);
        gatt.writeCharacteristic(readConfigChar);
    }

    public static void sendEnergyConfig(BluetoothGatt gatt) {
        ByteBuffer b = ByteBuffer.allocate(4);
        b.putInt(BLESettings.getEnergyRefreshRate());
        byte[] result = b.array();
        byte[] sendConfig = new byte[]{(byte) 0x10, result[2], result[3]};
        Log.i("Tete", "Send Energy " + SensorTagData.bytesToHex(sendConfig));
        BluetoothGattCharacteristic sendConfigChar = gatt.getService(BLEService.ENERGY_SERVICE).getCharacteristic(BLEService.ENERGY_CONFIG_CHAR);
        sendConfigChar.setValue(sendConfig);
        gatt.writeCharacteristic(sendConfigChar);
    }

    private static byte[] prepAccelConfig() {
        ByteBuffer b = ByteBuffer.allocate(4);
        b.putInt(BLESettings.getAccelRefreshRate());
        byte[] result = b.array();
        int firstByte = (BLESettings.getBandwidth() << 2) + BLESettings.getFullScaleSelection();
        int secondByte = (BLESettings.getxAxis() << 5) + (BLESettings.getyAxis() << 4) + (BLESettings.getzAxis() << 3) + BLESettings.getAccelOutputRate();

        return new byte[]{(byte) firstByte, (byte) secondByte, result[2], result[3]};
    }

    public static void sendAccelConfig(BluetoothGatt gatt) {

        byte[] result = prepAccelConfig();
        byte[] sendConfig = new byte[]{(byte) 0x20, result[0], result[1], result[2], result[3]};
        Log.i(TAG, "Send Accel " + SensorTagData.bytesToHex(sendConfig));
        BluetoothGattCharacteristic sendConfigChar = gatt.getService(BLEService.ACCEL_SERVICE).getCharacteristic(BLEService.ACCEL_CONFIG_CHAR);
        sendConfigChar.setValue(sendConfig);
        gatt.writeCharacteristic(sendConfigChar);
    }

    private static byte[] prepPressureConfig() {
        int lowerByte = (BLESettings.getLCOMPInput() << 5) + (BLESettings.getComparatorThres() << 1) + BLESettings.getEnableHyst();
        int upperByte = (BLESettings.getResetCounter() << 5) + (BLESettings.getReadyEvent() << 4) + (BLESettings.getDownEvent() << 3) +
                (BLESettings.getUpEvent() << 2) + (BLESettings.getCrossEvent() << 1) + BLESettings.getLCOMPState();
        return new byte[]{(byte) lowerByte, (byte) upperByte};
    }

    public static void sendPressureConfig(BluetoothGatt gatt) {
        byte[] result = prepPressureConfig();
        BLESettings.setResetCounter(0);
        byte[] sendConfig = new byte[]{(byte) 0x30, result[0], result[1]};
        Log.i(TAG, "Send Pressure " + SensorTagData.bytesToHex(sendConfig));
        BluetoothGattCharacteristic sendConfigChar = gatt.getService(BLEService.PRESSURE_SERVICE).getCharacteristic(BLEService.PRESSURE_CONFIG_CHAR);
        sendConfigChar.setValue(sendConfig);
        gatt.writeCharacteristic(sendConfigChar);
    }
}
