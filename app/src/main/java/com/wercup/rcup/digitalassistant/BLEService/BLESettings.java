package com.wercup.rcup.digitalassistant.BLEService;

import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import com.wercup.rcup.digitalassistant.Tools.SensorTagData;

/**
 * Created by KeiLo on 09/12/16.
 */

public class BLESettings {


    private static int energyRefreshRate;

    private static int accelRefreshRate;
    private static int accelOutputRate;
    private static int zAxis;
    private static int yAxis;
    private static int xAxis;
    private static int fullScaleSelection;
    private static int bandwidth;

    private static int enableHyst;
    private static int comparatorThres;
    private static int LCOMPInput;
    private static int LCOMPState;
    private static int crossEvent;
    private static int upEvent;
    private static int downEvent;
    private static int readyEvent;

    private static int resetCounter;

    public static int getResetCounter() {
        return resetCounter;
    }

    public static void setResetCounter(int resetCounter) {
        BLESettings.resetCounter = resetCounter;
    }

    public static int getEnergyRefreshRate() {
        return energyRefreshRate;
    }

    private static void setEnergyRefreshRate(int energyRefreshRate) {
        BLESettings.energyRefreshRate = energyRefreshRate;
    }

    public static int getAccelRefreshRate() {
        return accelRefreshRate;
    }

    private static void setAccelRefreshRate(int accelRefreshRate) {
        BLESettings.accelRefreshRate = accelRefreshRate;
    }

    public static int getAccelOutputRate() {
        return accelOutputRate;
    }

    private static void setAccelOutputRate(int accelOutputRate) {
        BLESettings.accelOutputRate = accelOutputRate;
    }

    public static int getzAxis() {
        return zAxis;
    }

    private static void setzAxis(int zAxis) {
        BLESettings.zAxis = zAxis;
    }

    public static int getyAxis() {
        return yAxis;
    }

    private static void setyAxis(int yAxis) {
        BLESettings.yAxis = yAxis;
    }

    public static int getxAxis() {
        return xAxis;
    }

    private static void setxAxis(int xAxis) {
        BLESettings.xAxis = xAxis;
    }

    public static int getFullScaleSelection() {
        return fullScaleSelection;
    }

    private static void setFullScaleSelection(int fullScaleSelection) {
        BLESettings.fullScaleSelection = fullScaleSelection;
    }

    public static int getBandwidth() {
        return bandwidth;
    }

    private static void setBandwidth(int bandwidth) {
        BLESettings.bandwidth = bandwidth;
    }

    public static int getEnableHyst() {
        return enableHyst;
    }

    private static void setEnableHyst(int enableHyst) {
        BLESettings.enableHyst = enableHyst;
    }

    public static int getComparatorThres() {
        return comparatorThres;
    }

    private static void setComparatorThres(int comparatorThres) {
        BLESettings.comparatorThres = comparatorThres;
    }

    public static int getLCOMPInput() {
        return LCOMPInput;
    }

    private static void setLCOMPInput(int LCOMPInput) {
        BLESettings.LCOMPInput = LCOMPInput;
    }

    public static int getLCOMPState() {
        return LCOMPState;
    }

    private static void setLCOMPState(int LCOMPState) {
        BLESettings.LCOMPState = LCOMPState;
    }

    public static int getCrossEvent() {
        return crossEvent;
    }

    private static void setCrossEvent(int crossEvent) {
        BLESettings.crossEvent = crossEvent;
    }

    public static int getUpEvent() {
        return upEvent;
    }

    private static void setUpEvent(int upEvent) {
        BLESettings.upEvent = upEvent;
    }

    public static int getDownEvent() {
        return downEvent;
    }

    private static void setDownEvent(int downEvent) {
        BLESettings.downEvent = downEvent;
    }

    public static int getReadyEvent() {
        return readyEvent;
    }

    private static void setReadyEvent(int readyEvent) {
        BLESettings.readyEvent = readyEvent;
    }


    public static void parsePressureConfig(BluetoothGattCharacteristic c) {
        byte[] trame = c.getValue();
        int hyst = trame[1] & 1;
        int LCOMPInput = trame[1] >> 5;
        int CompThres = ((trame[1] - (LCOMPInput << 5)) >>> 1);
        int LCOMPState = trame[2] & 1;
        int cross = (trame[2] >> 1) & 1;
        int up = (trame[2] >> 2) & 1;
        int down = (trame[2] >> 3) & 1;
        int ready = (trame[2] >> 4) & 1;
        setEnableHyst(hyst);
        setLCOMPInput(LCOMPInput);
        setComparatorThres(CompThres);
        setLCOMPState(LCOMPState);
        setCrossEvent(cross);
        setUpEvent(up);
        setDownEvent(down);
        setReadyEvent(ready);
    }

    public static void parseEnergyConfig(BluetoothGattCharacteristic c) {
        int energyRefresh = SensorTagData.shortUnsignedAtOffset(c, 1);
        setEnergyRefreshRate(energyRefresh);
        Log.e("Parse Energy Config", "value " + energyRefresh);
    }

    public static void parseAccelConfig(BluetoothGattCharacteristic c) {
        byte[] trame = c.getValue();
        int accelRefresh = SensorTagData.shortUnsignedAtOffset(c, 3);

        int padding = (trame[2] >> 3);
        int outputRate= (trame[2] - (padding << 3));
        int z = (trame[2] >> 3) & 1;
        int y = (trame[2] >> 4) & 1;
        int x = (trame[2] >> 5) & 1;
        int fullScale = (trame[1] << 6) >>> 6;
        int bandwidth = (trame[1] << 6) & 1;
        setAccelRefreshRate(accelRefresh);
        setAccelOutputRate(outputRate);
        setzAxis(z);
        setyAxis(y);
        setxAxis(x);
        setFullScaleSelection(fullScale);
        setBandwidth(bandwidth);
    }

}
