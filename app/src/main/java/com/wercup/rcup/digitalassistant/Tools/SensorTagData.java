package com.wercup.rcup.digitalassistant.Tools;

import android.bluetooth.BluetoothGattCharacteristic;

/**
 * Created by KeiLo on 09/12/16.
 */

public class SensorTagData {

        /**
         * Function that tells if a TapTap has been sent
         * @param characteristic BluetoothGattCharacteristic that is being read
         * @return true of false depending if there is a TapTap or not
         */
        public static boolean isTapTap(BluetoothGattCharacteristic characteristic) {
            return (characteristic.getValue()[0] == 1);
        }

        /**
         * Function that returns the number of steps read from the BLE notification
         * @param c BluetoothGattCharacteristic that is being read
         * @return Step count as an integer
         */
        public static int[] getSteps(BluetoothGattCharacteristic c) {
            return shortUnsignedAtOffset(c);
        }

        /**
         * Get accelerometer XYZ-axis values and ambient temperature
         * @param c BluetoothGattCharacteristic that is being read
         * @return integer array containing the 4 useful values (Tested and OK)
         */
        public static int[] extractAccelCoefficients(BluetoothGattCharacteristic c) {
            int[] coefficients = new int[4];

//        Log.e("Accel", "Byte 0 : " + byteToHex(c.getValue()[0]) + byteToHex(c.getValue()[1]));
            coefficients[0] = shortSignedAtOffset(c, 0);
//        Log.e("Accel", "Byte 1 : " + byteToHex(c.getValue()[2]) + byteToHex(c.getValue()[3]));
            coefficients[1] = shortSignedAtOffset(c, 2);
//        Log.e("Accel", "Byte 2 : " + byteToHex(c.getValue()[4]) + byteToHex(c.getValue()[5]));
            coefficients[2] = shortSignedAtOffset(c, 4);
//        Log.e("Accel", "Byte 3 : " + byteToHex(c.getValue()[6]) + byteToHex(c.getValue()[7]));
            coefficients[3] = getTemp(c);

            return coefficients;
        }


        /**
         * Simple function to convert byte to Hex String
         */
        public static String byteToHex(byte b) {
            return String.format("%02X", b);
        }

        /**
         * Function to convert byte array to Hex string
         */
        final static protected char[] hexArray = "0123456789ABCDEF".toCharArray();

        public static String bytesToHex(byte[] bytes) {
            char[] hexChars = new char[bytes.length * 2];
            for (int j = 0; j < bytes.length; j++) {
                int v = bytes[j] & 0xFF;
                hexChars[j * 2] = hexArray[v >>> 4];
                hexChars[j * 2 + 1] = hexArray[v & 0x0F];
            }
            return new String(hexChars);
        }

        /**
         * Simple conversion from byte array to bits
         */
        public static String bytesToBinary(byte[] bytes)
        {
            StringBuilder sb = new StringBuilder(bytes.length * Byte.SIZE);
            for( int i = 0; i < Byte.SIZE * bytes.length; i++ )
                sb.append((bytes[i / Byte.SIZE] << i % Byte.SIZE & 0x80) == 0 ? '0' : '1');
            return sb.toString();
        }

        /**
         * Simple conversion from byte array to bits
         */
        public static String byteToBinary(byte b)
        {
            StringBuilder sb = new StringBuilder(Byte.SIZE);
            for( int i = 0; i < Byte.SIZE; i++ )
                sb.append((b << i % Byte.SIZE & 0x80) == 0 ? '0' : '1');
            return sb.toString();
        }

        /**
         * Process battery related values
         * @param c BluetoothGattCharacteristic that is being read
         * @return integer array containing Current and Voltage
         */
        public static double[] getBatteryLevel (BluetoothGattCharacteristic c) {
            Integer[] rawValues = shortSignedAtOffsetBattery(c);
            double[] processedValues = new double[2];
//        Log.e("Battery", String.valueOf(rawValues[0]));
            processedValues[0] = (double) rawValues[0];
            processedValues[1] = (double) rawValues[1] / 1000;
            return processedValues;
        }

        /**
         * Custom function that converts raw notification data to an integer array
         * @param c BluetoothGattCharacteristic that is being read
         * @return raw battery related values in an array
         */
        private static Integer[] shortSignedAtOffsetBattery(BluetoothGattCharacteristic c) {
            Integer[] batteryValues = new Integer[2];
            Integer intensity = c.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT8, 0);
            Integer lowerByte = c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1);
            Integer upperByte = c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 2);
            batteryValues[0] = intensity;
            batteryValues[1] = (lowerByte << 8) + upperByte;
            return batteryValues;
        }

        /**
         * Get Two's complement from data
         * @param c BluetoothGattCharacteristic that is being read
         * @param offset offset by byte
         * @return Signed integer value
         */
        private static Integer shortSignedAtOffset(BluetoothGattCharacteristic c, int offset) {
            Integer lowerByte = c.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT8, offset);
            Integer upperByte = c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset + 1);

            return (lowerByte << 8) + upperByte;
        }

        //TODO: No use for this function at the moment
        public static Integer shortUnsignedAtOffset(BluetoothGattCharacteristic c, int offset) {
            Integer lowerByte = c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset);
            Integer upperByte = c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset + 1); // Note: interpret MSB as unsigned.

            return (lowerByte << 8) + upperByte;
        }

        /**
         * This function compute the ambient temperature following the instructions given in the communication protocol 1.0.2
         * @param c BluetoothGattCharacteristic that is being read
         * @return the temperature as an signed integer (can be a negative value, tests proved it)
         */
        private static Integer getTemp (BluetoothGattCharacteristic c) {
            byte[] rawData = c.getValue();
            byte[] tempData = new byte[2];
            int processedTemp;
            System.arraycopy(rawData, 6, tempData, 0, 2);
            processedTemp = (((tempData[0] << 8) + tempData[1]) >> 5) / 8 + 25;
//        Log.e("Temp Processed value", String.valueOf(processedTemp));

            return  processedTemp;
        }

        /**
         * This function is custom made for counting steps which are sent to us in the form of an array of 4 bytes
         * @param c BluetoothGattCharacteristic that is being read
         * @return Step value in unsigned int
         */
        private static int[] shortUnsignedAtOffset(BluetoothGattCharacteristic c) {
            Integer lowerByte = c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1);
            Integer lower2Byte = c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 2);
            Integer lower3Byte = c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 3);
            Integer upperByte = c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 4); // Note: interpret MSB as unsigned.
            return new int[] {(lowerByte << 8) + lower2Byte, (lower3Byte << 8) + upperByte};
        }
}
