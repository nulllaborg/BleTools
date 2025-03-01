package com.nulllab.blutooth.service;


import java.util.List;
import java.util.UUID;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.nulllab.blutooth.test.DeviceConnect;
import com.nulllab.blutooth.utils.DateUtil;
import com.nulllab.blutooth.utils.Utils;

public class BleService extends Service {
    private final static String TAG = "BleService";

    public BluetoothManager mBluetoothManager;
    public BluetoothAdapter mBluetoothAdapter;
    public BluetoothGatt mBluetoothGatt;

    private String mbluetoothDeviceAddress;
    public int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    // 为了传送状态响应状态，要有几条ACTION
    public final static String ACTION_GATT_CONNECTED = "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE = "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String ACTION_CHAR_READED = "com.example.bluetooth.le.ACTION_CHAR_READED";
    public final static String BATTERY_LEVEL_AVAILABLE = "com.example.bluetooth.le.BATTERY_LEVEL_AVAILABLE";
    public final static String EXTRA_DATA = "com.example.bluetooth.le.EXTRA_DATA";
    public final static String EXTRA_STRING_DATA = "com.example.bluetooth.le.EXTRA_STRING_DATA";
    public final static String EXTRA_DATA_LENGTH = "com.example.bluetooth.le.EXTRA_DATA_LENGTH";
    public final static String ACTION_GATT_RSSI = "com.example.bluetooth.le.ACTION_GATT_RSSI";
    public final static String EXTRA_DATA_RSSI = "com.example.bluetooth.le.ACTION_GATT_RSSI";
    // 集中常用的
    public static final UUID RX_ALART_UUID = UUID
            .fromString("00001802-0000-1000-8000-00805f9b34fb");
    public static final UUID RX_SERVICE_UUID = UUID
            .fromString("0000ffe0-0000-1000-8000-00805f9b34fb");// DE5BF728-D711-4E47-AF26-65E3012A5DC7
    public static final UUID MY_SERVICE_UUID = UUID
            .fromString("0000fff0-0000-1000-8000-00805f9b34fb");
    public static final UUID MY_CHAR_UUID = UUID
            .fromString("0000fff4-0000-1000-8000-00805f9b34fb");
    public static final UUID RX_CHAR_UUID = UUID
            .fromString("00002A06-0000-1000-8000-00805f9b34fb");// DE5BF729-D711-4E47-AF26-65E3012A5DC7
    public static final UUID TX_CHAR_UUID = UUID
            .fromString("0000ffe1-0000-1000-8000-00805f9b34fb");// DE5BF72A-D711-4E47-AF26-65E3012A5DC7
    public static final UUID CCCD = UUID
            .fromString("00002902-0000-1000-8000-00805f9b34fb");
    public static final UUID C22D = UUID
            .fromString("00002902-0000-1000-8000-00805f9b34fb");
    public static final UUID BATTERY_SERVICE_UUID = UUID
            .fromString("0000180f-0000-1000-8000-00805f9b34fb");
    public static final UUID BATTERY_CHAR_UUID = UUID
            .fromString("00002a19-0000-1000-8000-00805f9b34fb");

    private final IBinder mBinder = new LocalBinder();
    public String notify_result;
    public String notify_string_result;
    public int notify_result_length;

    @SuppressLint("NewApi")
    public BluetoothGattCallback mBluetoothGattCallback = new
            BluetoothGattCallback() {

                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                                    int newState) {
                    String intentAction;
                    Log.i("SSS__",newState+"");
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        intentAction = ACTION_GATT_CONNECTED;
                        mConnectionState = STATE_CONNECTED;
                        broadcastUpdate(intentAction);
                        gatt.discoverServices();
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        intentAction = ACTION_GATT_DISCONNECTED;
                        mConnectionState = STATE_DISCONNECTED;
                        broadcastUpdate(intentAction);
                    }
                }

                @Override
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                    } else {
                        Log.w("mylog", "service is null");
                    }
                }

                @Override
                public void onCharacteristicRead(BluetoothGatt gatt,
                                                 BluetoothGattCharacteristic characteristic, int status) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        getChartacteristicValue(characteristic);
                    } else {
                        Log.v(TAG, " BluetoothGatt Read Failed!");
                    }

                }

                @Override
                public void onCharacteristicChanged(BluetoothGatt gatt,
                                                    BluetoothGattCharacteristic characteristic) {
                    Log.d("waldo", "onCharacteristicChanged: update character data");
                    broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                }

                @Override
                public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
                    // TODO Auto-generated method stub
                    super.onReadRemoteRssi(gatt, rssi, status);
                    Intent rssiIntent = new Intent();
                    rssiIntent.putExtra(EXTRA_DATA_RSSI, rssi);
                    rssiIntent.setAction(ACTION_GATT_RSSI);
                    sendBroadcast(rssiIntent);
                    if (mBluetoothGatt != null) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                // TODO Auto-generated method stub
                                try {
                                    Thread.sleep(1500);
                                    mBluetoothGatt.readRemoteRssi();
                                } catch (InterruptedException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                            }
                        }).start();
                    }

                }

            };

    @SuppressLint("NewApi")
    private void getChartacteristicValue(
            BluetoothGattCharacteristic characteristic) {
        // TODO Auto-generated method stub
        List<BluetoothGattDescriptor> des = characteristic.getDescriptors();
        Intent mIntent = new Intent(ACTION_CHAR_READED);
        int desSize = des.size();
        Log.d(TAG, "getChartacteristicValue: desSize:" + desSize);
        for (int i=0; i<desSize; i++) {
            mIntent.putExtra("desriptor"+(i+1), des.get(i).getUuid().toString());
            Log.d(TAG, "getChartacteristicValue: desriptor"+(i+1) +": " + des.get(i).getUuid().toString());
        }
        mIntent.putExtra("StringValue", characteristic.getStringValue(0));
        String hexValue = Utils.bytesToHex(characteristic.getValue());
        mIntent.putExtra("HexValue", hexValue.toString());
        mIntent.putExtra("time", DateUtil.getCurrentDatatime());
        sendBroadcast(mIntent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return mBinder;
    }

    private void broadcastUpdate(String action) {
        Intent mIntent = new Intent(action);
        sendBroadcast(mIntent);
    }

    @SuppressLint("NewApi")
    private void broadcastUpdate(String action,
                                 BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent();
        intent.setAction(action);
        final byte[] data = characteristic.getValue();
        Log.d("waldo", "broadcastUpdate: data:" + data);
        final String stringData = characteristic.getStringValue(0);
        Log.d("waldo", "broadcastUpdate: stringData:" + stringData);
        if (data != null && data.length > 0) {
            final StringBuilder stringBuilder = new StringBuilder(data.length);
            for (byte byteChar : data) {
                stringBuilder.append(String.format("%X", byteChar));
            }
            if (stringData != null) {
                intent.putExtra(EXTRA_STRING_DATA, stringData);
            } else {
                Log.v("waldo", "characteristic.getStringValue is null");
            }
            notify_result = stringBuilder.toString();
            notify_string_result = stringData;
            notify_result_length = data.length;
            intent.putExtra(EXTRA_DATA, notify_result);
            intent.putExtra(EXTRA_DATA_LENGTH, notify_result_length);
        }
        sendBroadcast(intent);
    }

    @SuppressLint("NewApi")
    public boolean init() {
        IntentFilter bleSeviceFilter = new IntentFilter();
        bleSeviceFilter.addAction(DeviceConnect.FIND_DEVICE_ALARM_ON);
        bleSeviceFilter.addAction(DeviceConnect.CANCEL_DEVICE_ALARM);
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) this
                    .getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
            mBluetoothAdapter = mBluetoothManager.getAdapter();
        }
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }
        return true;
    }

    @SuppressLint("NewApi")
    public boolean connect(String bleAddress) {
        // TODO Auto-generated method stub
        if (mBluetoothAdapter == null || bleAddress == null) {
            Log.w(TAG,
                    "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        if (mbluetoothDeviceAddress != null
                && bleAddress.equals(mbluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }

        }
        final BluetoothDevice device = mBluetoothAdapter
                .getRemoteDevice(bleAddress);

        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        mBluetoothGatt = device
                .connectGatt(this, false, mBluetoothGattCallback);
        mbluetoothDeviceAddress = bleAddress;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    public void disconnect() {
        // TODO Auto-generated method stub
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    public class LocalBinder extends Binder {
        public BleService getService() {
            return BleService.this;
        }
    }

    @SuppressLint("NewApi")
    public void close(BluetoothGatt gatt) {
        gatt.disconnect();
        gatt.close();
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.cancelDiscovery();
            mBluetoothAdapter = null;
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // TODO Auto-generated method stub
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        this.close(mBluetoothGatt);
    }
}