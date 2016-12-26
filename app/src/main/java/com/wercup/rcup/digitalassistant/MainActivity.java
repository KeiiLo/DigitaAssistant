package com.wercup.rcup.digitalassistant;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.wercup.rcup.digitalassistant.BLEService.BLEService;
import com.wercup.rcup.digitalassistant.BLEService.BLESettings;
import com.wercup.rcup.digitalassistant.Tools.SensorTagData;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "TAG";
    final private static int REQUEST_CODE_ENABLE_BLUETOOTH = 7;
    final private static int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 124;

    private TextView tDate, tSteps, tCal, tKms, tTapTap;
    private ImageView vConstell, vCal;
    private GraphView graph;
    private Button invisibruh;

    private String day;
    private String month;
    private String date;

    private int stepCount = 0;
    private int stepTotal = 0;
    private int tapTapTotal = 0;
    private double kCalperMinute = 3 * 3.5 * 70 / 200;
    private int loop = 0;
    private ArrayList<Integer> steps;
    private BLEService mBLEService;

    private Runnable mTimer;
    private Runnable mReset;
    private boolean resetting = false;

    private int[] moonRes = new int[] {
            R.drawable.moon00001,
            R.drawable.moon00002,
            R.drawable.moon00003,
            R.drawable.moon00004,
            R.drawable.moon00005,
            R.drawable.moon00006,
            R.drawable.moon00007,
            R.drawable.moon00008
    };
    private int[] constellRes = new int[] {
            R.drawable.constell00001,
            R.drawable.constell00002,
            R.drawable.constell00003,
            R.drawable.constell00004,
            R.drawable.constell00005,
            R.drawable.constell00006,
            R.drawable.constell00007,
            R.drawable.constell00008,
            R.drawable.constell00009,
            R.drawable.constell00010,
            R.drawable.constell00011,
            R.drawable.constell00012,
            R.drawable.constell00013,
            R.drawable.constell00014,
            R.drawable.constell00015,
            R.drawable.constell00016,
            R.drawable.constell00017,
            R.drawable.constell00018,
            R.drawable.constell00019,
            R.drawable.constell00020,
            R.drawable.constell00021,
            R.drawable.constell00022,
            R.drawable.constell00023,
            R.drawable.constell00024,
            R.drawable.constell00025,
            R.drawable.constell00026,
            R.drawable.constell00027,
            R.drawable.constell00028,
            R.drawable.constell00029,
            R.drawable.constell00030,
            R.drawable.constell00031,
            R.drawable.constell00032,
            R.drawable.constell00033,
            R.drawable.constell00034,
            R.drawable.constell00035,
            R.drawable.constell00036,
            R.drawable.constell00037,
            R.drawable.constell00038,
            R.drawable.constell00039,
            R.drawable.constell00040,
            R.drawable.constell00041,
            R.drawable.constell00042,
            R.drawable.constell00043,
            R.drawable.constell00044,
            R.drawable.constell00045,
            R.drawable.constell00046,
            R.drawable.constell00047,
            R.drawable.constell00048,
            R.drawable.constell00049,
            R.drawable.constell00050,
            R.drawable.constell00051,
            R.drawable.constell00052,
            R.drawable.constell00053,
            R.drawable.constell00054,
            R.drawable.constell00055,
            R.drawable.constell00056,
            R.drawable.constell00057,
            R.drawable.constell00058,
            R.drawable.constell00059
    };

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkPermissions();
        /*invisibruh = (Button) findViewById(R.id.invisibruh);
        invisibruh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateConstell(stepCount / 2);
                updateMoon(stepCount);
                stepCount += 2;
                stepTotal += 2;
                tSteps.setText(String.format(Locale.FRANCE, "%d steps", stepTotal));
                double kms = stepTotal * randomValue();
                if (kms < 1000) {
                    tKms.setText(String.format(Locale.FRANCE, "%.2f m", kms));
                } else {
                    tKms.setText(String.format(Locale.FRANCE, "%.4f Km", kms / 1000));
                }
            }
        });*/
        tDate = (TextView) findViewById(R.id.date);
        tSteps = (TextView) findViewById(R.id.steps);
        tTapTap = (TextView) findViewById(R.id.taptap_count);
        tCal = (TextView) findViewById(R.id.cal);
        tKms = (TextView) findViewById(R.id.kms);
        vConstell = (ImageView) findViewById(R.id.constellation);
        vCal = (ImageView) findViewById(R.id.moon);

        Calendar c = Calendar.getInstance();
        day = c.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.US);
        month = c.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.US);
        date = String.valueOf(c.get(Calendar.DAY_OF_MONTH));
        switch (date) {
            case "1":
                date += "st";
                break;
            case "2":
                date += "nd";
                break;
            case "3":
                date += "rd";
                break;
            default:
                date += "th";
                break;
        }

        Thread t = new Thread() {

            @Override
            public void run() {
                try {
                    while (!isInterrupted()) {
                        Thread.sleep(1000);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Calendar c = Calendar.getInstance();
                                Integer hour = c.get(Calendar.HOUR_OF_DAY);
                                Integer minutes = c.get(Calendar.MINUTE);
                                String sDate = String.format(Locale.US, "%s %s %s - %02dh%02d", day, month, date, hour, minutes);
                                tDate.setText(sDate);
                            }
                        });
                    }
                } catch (InterruptedException e) {
                }
            }
        };
        t.start();
        final Thread calories = new Thread() {

            @Override
            public void run() {
                try {
                    while (!isInterrupted()) {
                        Thread.sleep(5000);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                loop++;
                                String sCal = String.format(Locale.FRANCE, "%.2f %s", kCalperMinute * loop / 30, "kCal burnt");
                                tCal.setText(sCal);
                            }
                        });
                    }
                } catch (InterruptedException e) {
                }
            }
        };
        calories.start();
        steps = new ArrayList<>();
        mBLEService = BLEService.getInstance(this);
        IntentFilter filter = new IntentFilter();
        filter.addAction(NOTIFICATION_SERVICE);
        this.registerReceiver(dataReceiver, filter);
        mBLEService.startScan();
    }


    private final BroadcastReceiver dataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            BluetoothGattCharacteristic accelCharacteristic = new BluetoothGattCharacteristic(UUID.fromString("00000001-1212-efde-1523-785fef13d123"), BluetoothGattCharacteristic.PERMISSION_READ, BluetoothGattCharacteristic.PERMISSION_READ);
            BluetoothGattCharacteristic pressureCharacteristic = new BluetoothGattCharacteristic(UUID.fromString("00000002-1212-efde-1523-785fef13d123"), BluetoothGattCharacteristic.PERMISSION_READ, BluetoothGattCharacteristic.PERMISSION_READ);
            BluetoothGattCharacteristic energyCharacteristic = new BluetoothGattCharacteristic(UUID.fromString("00000003-1212-efde-1523-785fef13d123"), BluetoothGattCharacteristic.PERMISSION_READ, BluetoothGattCharacteristic.PERMISSION_READ);
            BluetoothGattCharacteristic accelConfigCharacteristic = new BluetoothGattCharacteristic(UUID.fromString("00000004-1212-efde-1523-785fef13d123"), BluetoothGattCharacteristic.PERMISSION_READ, BluetoothGattCharacteristic.PERMISSION_READ);
            BluetoothGattCharacteristic pressureConfigCharacteristic = new BluetoothGattCharacteristic(UUID.fromString("00000005-1212-efde-1523-785fef13d123"), BluetoothGattCharacteristic.PERMISSION_READ, BluetoothGattCharacteristic.PERMISSION_READ);
            BluetoothGattCharacteristic energyConfigCharacteristic = new BluetoothGattCharacteristic(UUID.fromString("00000006-1212-efde-1523-785fef13d123"), BluetoothGattCharacteristic.PERMISSION_READ, BluetoothGattCharacteristic.PERMISSION_READ);
            energyCharacteristic.setValue(intent.getByteArrayExtra("energy"));
            accelCharacteristic.setValue(intent.getByteArrayExtra("accel"));
            pressureCharacteristic.setValue(intent.getByteArrayExtra("pressure"));
            energyConfigCharacteristic.setValue(intent.getByteArrayExtra("energyconfig"));
            accelConfigCharacteristic.setValue(intent.getByteArrayExtra("accelconfig"));
            pressureConfigCharacteristic.setValue(intent.getByteArrayExtra("pressureconfig"));
            if (energyCharacteristic.getValue() != null) {
                mHandler.sendMessage(Message.obtain(null, MSG_ENERGY_DATA, energyCharacteristic));
            }
            if (accelCharacteristic.getValue() != null) {
                mHandler.sendMessage(Message.obtain(null, MSG_ACCEL_DATA, accelCharacteristic));
            }
            if (pressureCharacteristic.getValue() != null) {
                mHandler.sendMessage(Message.obtain(null, MSG_PRESSURE_DATA, pressureCharacteristic));
            }
            if (energyConfigCharacteristic.getValue() != null) {
                mHandler.sendMessage(Message.obtain(null, MSG_ENERGY_CONFIG, energyConfigCharacteristic));
            }
            if (accelConfigCharacteristic.getValue() != null) {
                mHandler.sendMessage(Message.obtain(null, MSG_ACCEL_CONFIG, accelConfigCharacteristic));
            }
            if (pressureConfigCharacteristic.getValue() != null) {
                mHandler.sendMessage(Message.obtain(null, MSG_PRESSURE_CONFIG, pressureConfigCharacteristic));
            }
        }
    };

    /**
     * Handler to process multiple events on the main thread
     **/
    private static final int MSG_ENERGY_CONFIG = 302;
    private static final int MSG_ACCEL_CONFIG = 402;
    private static final int MSG_PRESSURE_CONFIG = 502;
    private static final int MSG_ENERGY_DATA = 301;
    private static final int MSG_ACCEL_DATA = 401;
    private static final int MSG_PRESSURE_DATA = 501;
    private BluetoothGattCharacteristic mCharacteristic = null;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            byte[] trame;
            switch (msg.what) {
                case MSG_PRESSURE_DATA:
                    mCharacteristic = (BluetoothGattCharacteristic) msg.obj;
//                    if (mCharacteristic.getValue()[0] != 1) {
                    if (resetting && stepCount == 0)
                        resetting = false;
                    if (resetting == false) {
                        if (mCharacteristic.getValue()[0] == 1) {
                            tapTapTotal++;
                            tTapTap.setText(String.format(Locale.FRANCE, "%d constellations annihilated", tapTapTotal));
                            stepTotal -= 2;
                            BLESettings.setResetCounter(1);
                            mBLEService.sendPressureConfig(mBLEService.getmGatt());
                        } else {
                            updateConstell(stepCount / 2);
                            updateMoon(stepTotal);
                            stepCount += 2;
                            stepTotal += 2;
                            tSteps.setText(String.format(Locale.FRANCE, "%d steps", stepTotal));
                            double kms = stepTotal * randomValue();
                            if (kms < 1000) {
                                tKms.setText(String.format(Locale.FRANCE, "%.2f m", kms));
                            } else {
                                tKms.setText(String.format(Locale.FRANCE, "%.4f Km", kms / 1000));
                            }
                        }
                    }

                    Log.e("STEPS", "STOP " + stepCount);
//                    }
                    break;
                case MSG_PRESSURE_CONFIG:
                    mCharacteristic = (BluetoothGattCharacteristic) msg.obj;
                    trame = mCharacteristic.getValue();
                    if (trame == null) {
                        Log.w(TAG, "Error obtaining pressure config return value");
                        return;
                    } else {
                        Log.i(TAG, "Value in Hex: " + SensorTagData.bytesToHex(trame));
                        Log.i(TAG, "Value in bit: " + SensorTagData.bytesToBinary(trame));
                        switch (trame[0]) {
                            case (byte) 0xB0:
                                Toast.makeText(MainActivity.this, "Let's destroy this constellation !!!", Toast.LENGTH_SHORT).show();
                                resetting = true;
                                for (int i = 0; stepCount >= i; i += 2) {
                                    mHandler.postDelayed(mTimer, 200 * i / 2);
                                }
                                break;
                            case (byte) 0xB1:
                                BLESettings.parsePressureConfig(mCharacteristic);
                                break;
                        }
                    }
                    break;
            }
        }
    };

    private void initBLE() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBlueTooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBlueTooth, REQUEST_CODE_ENABLE_BLUETOOTH);
        }
    }

    private void checkPermissions() {
        requestPermissions(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE},
                REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
        initBLE();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mTimer = new Runnable() {
            @Override
            public void run() {
                Log.e("TAG", "LOL " + stepCount);
                stepCount -= 2;
                if (stepCount < 0)
                    stepCount = 0;
                updateConstell(stepCount / 2);
            }
        };
        mReset = new Runnable() {
            @Override
            public void run() {
                Log.e("TAG", "LOL " + stepCount);
                stepCount = 0;
            }
        };
    }

    private double randomValue() {
        Random r = new Random();
        double rangeMin = 0.7d;
        double rangeMax = 0.8d;
        double randomValue = rangeMin + (rangeMax - rangeMin) * r.nextDouble();
        return randomValue;
    }

    private void updateConstell(int i) {
        if (stepCount < 118 && stepCount >= 0)
            vConstell.setImageDrawable(getDrawable(constellRes[i]));
    }

    private void updateMoon(int i) {
        double res = (i * 8 / 100) % 8;
        Log.e("TAG", String.valueOf(res));
        vCal.setImageDrawable(getDrawable(moonRes[(int) res]));
    }

    @Override
    protected void onPause() {
        super.onPause();
        mHandler.removeCallbacks(mTimer);
//        mHandler.removeCallbacks(mReset);
    }
}
