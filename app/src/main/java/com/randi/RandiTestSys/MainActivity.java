package com.randi.RandiTestSys;

import android.app.admin.DeviceAdminReceiver;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;

import com.ftdi.j2xx.D2xxManager;
import com.ftdi.j2xx.FT_Device;

import java.util.Timer;


public class MainActivity extends AppCompatActivity {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    public D2xxManager ftD2xx;
    private UsbDevice device;
    private UsbManager manager;
    FT_Device ftUSBDev;

    static Context DeviceInformationContext;


    // create variables
    private TextView magVal;
    Timer checkSleepTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Button readPwrCtrlATtiny;
        Button readMassimo;
        Button readSensors;
        Button readMagDetect;
        Button readTouchCtrlr;
        TextView readPwrCtrlATtinyresults;
        TextView readMassimoResults;
        TextView readSensorResults;
        TextView readMagDetectResults;
        TextView readTouchCtrlrResults;
        ToggleButton readpump; //see definition below
        ToggleButton readvalve1, readvalve2;


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        magVal = (TextView) findViewById(R.id.textViewMagresults);
        readpump = (ToggleButton) findViewById(R.id.toggleButtonPump); //assign variable to button on screen
        readvalve1 = (ToggleButton) findViewById(R.id.toggleButtonValve1);
        readvalve2 = (ToggleButton) findViewById(R.id.toggleButtonValve2);
        readPwrCtrlATtiny = (Button) findViewById(R.id.buttonPwrCntrlATtiny);
        readPwrCtrlATtinyresults = (TextView) findViewById(R.id.textViewPwrCtrlATtinyResults);
        readMassimo = (Button) findViewById(R.id.buttonReadMassimo);
        readMassimoResults = (TextView) findViewById(R.id.textViewMassimoResults);
        readSensors = (Button) findViewById(R.id.buttonReadSensors);
        readSensorResults = (TextView) findViewById(R.id.textViewSensorResults);
        readMagDetect = (Button) findViewById(R.id.buttonReadMagDetect);
        readMagDetectResults = (TextView) findViewById(R.id.textViewMagresults);

        // create a listener for readpump button
        //  1) select function setOnClickListener
        //  2) type new in parens and select View.OnClickListener()
        //      code in brackets is automatically created
        readpump.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                i2cCtrl0x69(readpump.isChecked(), readvalve1.isChecked(), readvalve2.isChecked());

//                int tmp = Integer.parseInt(magVal.getText().toString());  //bogus code to show how to get numbers from the form
//                magVal.setText(String.valueOf(-tmp));

            }
        });
        readvalve1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                i2cCtrl0x69(readpump.isChecked(), readvalve1.isChecked(), readvalve2.isChecked());
            }
        });

        //alternative implementation
        //  see https://droidrant.com/can-be-replaced-with-lambda-android/
        readvalve2.setOnClickListener(v -> i2cCtrl0x69(readpump.isChecked(), readvalve1.isChecked(), readvalve2.isChecked()));
        readPwrCtrlATtiny.setOnClickListener(v -> i2cReadATtiny(readPwrCtrlATtinyresults));
        readMassimo.setOnClickListener(v -> UARTreadMassimo(readMassimoResults));
        readSensors.setOnClickListener(v -> UARTreadSensors(readSensorResults));
        readMagDetect.setOnClickListener(v -> i2cReadMagDetect(readMagDetectResults));
    }





    public void i2cReadTouchCtrlr(TextView results)
    {
        int i2c_2;
        int i2cReadVal;
        StringBuilder Str_result;
        int[] I2CrawVal;

        I2CrawVal = new int[2];

        i2c_2 = openPort("/dev/i2c-2");


        if(i2c_2 > 0) {
            byte[] cmdArray = {0x41, 0x40};

            cmdArray[1] = 0x40;     //get firmware number
            sendI2CPort(i2c_2, cmdArray, 2);
            i2cReadVal = readI2CPort(i2c_2, (byte) 0x41, I2CrawVal, 2);
//            Str_result = new StringBuilder("Firmware: " + String.format("%6.2fC [0x%04x 0x%02x%02x]\n",i2cReadVal, i2cReadVal, I2CrawVal[1], I2CrawVal[0]));
            Str_result = new StringBuilder("What's the deal????");



            results.setText(Str_result);
            closePort(i2c_2);

        }
    }





    public void i2cCtrl0x68(View v)
    {
        boolean checked = ((RadioButton) v).isChecked();
        int i2c_1;
        i2c_1 = openPort("/dev/i2c-1");

        if(i2c_1 > 0) {
            byte[] cmdArray = {0x68, 0};

            if(v.getId() == R.id.radioButtonAlarmHigh)
                cmdArray[1] = (byte) 0x3;
            else if(v.getId() == R.id.radioButtonAlarmMed)
                cmdArray[1] = (byte) 0x41;
            else if(v.getId() == R.id.radioButtonAlarmLow)
                cmdArray[1] = (byte) 0x81;

            sendI2CPort(i2c_1, cmdArray, 2);
            closePort(i2c_1);
        }
    }




    private void i2cCtrl0x69(boolean pump, boolean valve1, boolean valve2)
    {
        int i2c_1;
        i2c_1 = openPort("/dev/i2c-1");

        if(i2c_1 > 0) {
            byte[] cmdArray = {0x69, 0};

            if(pump)
                cmdArray[1] = 0x10;

            if(valve1)
                cmdArray[1] += 0x40;

            if(valve2)
                cmdArray[1] += 0x80;

            sendI2CPort(i2c_1, cmdArray, 2);
            closePort(i2c_1);
        }
    }



    private void i2cReadMagDetect(TextView results)
    {
        int i2c_1;
        int i2cReadVal;
        StringBuilder Str_result;
        int[] I2CrawVal;

        I2CrawVal = new int[4];

        i2c_1 = openPort("/dev/i2c-1");

        if(i2c_1 > 0) {
            byte[] cmdArray = {0x60, 0x1f};

            sendI2CPort(i2c_1, cmdArray, 2);
            i2cReadVal = readI2CPort(i2c_1, (byte) 0x60, I2CrawVal, 4);
            int result = (int)(((I2CrawVal[2] & 0xf) * 256) + I2CrawVal[3]);
            if((I2CrawVal[2] & 0x8) > 0)    //is the 12-bit value negative?
                result -= 0x1000;           //sign extend if 12-bit number is negative

            Str_result = new StringBuilder(String.format("%6d [0x%04x 0x%02x%02x%02x%02x]\n", result, result, I2CrawVal[0], I2CrawVal[1], I2CrawVal[2], I2CrawVal[3]));

            results.setText(Str_result);

            closePort(i2c_1);
        }
    }




    private void i2cReadATtiny(TextView results)
    {
        int i2c_3;
        int i2cReadVal;
        StringBuilder Str_result;
        int[] I2CrawVal;

        I2CrawVal = new int[2];

        i2c_3 = openPort("/dev/i2c-3");

        if(i2c_3 > 0) {
            byte[] cmdArray = {0x33, 8};

            //read temperature
            cmdArray[1] = 8;
            sendI2CPort(i2c_3, cmdArray, 2);
            i2cReadVal = readI2CPort(i2c_3, (byte) 0x33, I2CrawVal, 2);
            Str_result = new StringBuilder("Battery Temperature: " + String.format("%6.2fC [0x%04x 0x%02x%02x]\n",(i2cReadVal/10.0-273.15), i2cReadVal, I2CrawVal[1], I2CrawVal[0]));
//            Str_result.append(("Battery Temperature: " + String.format("%6.2fC\n",(i2cReadVal/10.0-273.15))).toString());

            //read battery voltage
            cmdArray[1] = 9;
            sendI2CPort(i2c_3, cmdArray, 2);
            i2cReadVal = readI2CPort(i2c_3, (byte) 0x33, I2CrawVal, 2);
//            Str_result = new StringBuilder("Battery Voltage: " + String.valueOf(i2cReadVal/1000.0) + "V\n");
            Str_result.append(("Battery Voltage: " + String.valueOf(i2cReadVal/1000.0) + "V" + String.format(" [0x%04x 0x%02x%02x]\n", i2cReadVal, I2CrawVal[1], I2CrawVal[0])).toString());

            //read battery current
            cmdArray[1] = 10;
            sendI2CPort(i2c_3, cmdArray, 2);
            i2cReadVal = readI2CPort(i2c_3, (byte) 0x33, I2CrawVal, 2);
            Str_result.append(("Battery Current: " + String.valueOf(i2cReadVal/1000.0) + "A" + String.format(" [0x%04x 0x%02x%02x]\n", i2cReadVal, I2CrawVal[1], I2CrawVal[0])).toString());

            //read battery avg current
            cmdArray[1] = 11;
            sendI2CPort(i2c_3, cmdArray, 2);
            i2cReadVal = readI2CPort(i2c_3, (byte) 0x33, I2CrawVal, 2);
            Str_result.append(("Battery Average Current: " + String.valueOf(i2cReadVal/1000.0) + "A" + String.format(" [0x%04x 0x%02x%02x]\n", i2cReadVal, I2CrawVal[1], I2CrawVal[0])).toString());

            //read battery %capacity
            cmdArray[1] = 13;
            sendI2CPort(i2c_3, cmdArray, 2);
            i2cReadVal = readI2CPort(i2c_3, (byte) 0x33, I2CrawVal, 2);
            Str_result.append(("Battery Capacity: " + String.valueOf(i2cReadVal) + "%" + String.format(" [0x%04x 0x%02x%02x]\n", i2cReadVal, I2CrawVal[1], I2CrawVal[0])).toString());

            //read battery Amp Capacity
            cmdArray[1] = 15;
            sendI2CPort(i2c_3, cmdArray, 2);
            i2cReadVal = readI2CPort(i2c_3, (byte) 0x33, I2CrawVal, 2);
            Str_result.append(("Battery Capacity: " + String.valueOf(i2cReadVal/1000.0) + "Ah" + String.format(" [0x%04x 0x%02x%02x]\n", i2cReadVal, I2CrawVal[1], I2CrawVal[0])).toString());

            //read battery charging Current
            cmdArray[1] = 0x14;
            sendI2CPort(i2c_3, cmdArray, 2);
            i2cReadVal = readI2CPort(i2c_3, (byte) 0x33, I2CrawVal, 2);
            Str_result.append(("Battery Charge Current: " + String.valueOf(i2cReadVal/1000.0) + "A" + String.format(" [0x%04x 0x%02x%02x]\n", i2cReadVal, I2CrawVal[1], I2CrawVal[0])).toString());

            //read battery charging Voltage
            cmdArray[1] = 0x15;
            sendI2CPort(i2c_3, cmdArray, 2);
            i2cReadVal = readI2CPort(i2c_3, (byte) 0x33, I2CrawVal, 2);
            Str_result.append(("Battery Charge Voltage: " + String.valueOf(i2cReadVal/1000.0) + "V" + String.format(" [0x%04x 0x%02x%02x]\n", i2cReadVal, I2CrawVal[1], I2CrawVal[0])).toString());

            //read charger charging Voltage
            cmdArray[1] = (byte) 0x16;
            sendI2CPort(i2c_3, cmdArray, 2);
            i2cReadVal = readI2CPort(i2c_3, (byte) 0x33, I2CrawVal, 2);
            Str_result.append(("Charger Status: " + String.format(" [0x%04x 0x%02x%02x]\n", i2cReadVal, I2CrawVal[1], I2CrawVal[0])).toString());

            results.setText(Str_result);

            closePort(i2c_3);
        }
    }


    private static final int PULSEOX_RCRD_SIZE = 21;
    private void UARTreadMassimo(TextView results)
    {
        int ttymxc0;
        StringBuilder Str_result;
        int[] UARTreadVal;

        UARTreadVal= new int[PULSEOX_RCRD_SIZE];

        ttymxc0 = openPort("/dev/ttymxc0");
        if(ttymxc0 > 0 && cfgUART(ttymxc0, true) != -1){
            int numReturned = readUARTport(ttymxc0, UARTreadVal,PULSEOX_RCRD_SIZE);
            if( numReturned > 0){
               Str_result = new StringBuilder("");
               for(int i=0; i<numReturned; i++)
                   Str_result.append((String.format("%x ", UARTreadVal[i])).toString());

               results.setText(Str_result);
           }else
                results.setText("NO DATA");

            closePort(ttymxc0);
        } else
            results.setText("NO PORT");
    }



    private static final int SENSOR_RCRD_SIZE = 10;
    private void UARTreadSensors(TextView results)
    {
        int ttymxc2;
        StringBuilder Str_result;
        int[] UARTreadVal;

        UARTreadVal= new int[SENSOR_RCRD_SIZE+10];


        ttymxc2 = openPort("/dev/ttymxc2");

        if(ttymxc2 > 0 && cfgUART(ttymxc2, false) != -1){
            byte[] sendVal={'A'};

            if(sendUARTPort(ttymxc2, sendVal, 1) == 1) {
                if(readUARTport(ttymxc2, UARTreadVal,SENSOR_RCRD_SIZE) > 0){
                    if(UARTreadVal[0] == 0xAA) {
                        Str_result = new StringBuilder("GOT DATA\n");

                        Str_result.append((String.format("Diff:\t%5d\t[0x%02x%02x]\n", ((((UARTreadVal[1] & 0x3f)<<8) + UARTreadVal[2]) - 0x2000), (UARTreadVal[1] & 0x3f), UARTreadVal[2])).toString());
                        Str_result.append((String.format("Amb:\t%5d\t[0x%02x%02x]\n", ((((UARTreadVal[5] & 0x3f)<<8) + UARTreadVal[6]) - 0x2000), (UARTreadVal[5] & 0x3f), UARTreadVal[6])).toString());
                        switch (UARTreadVal[9]) {
                            case 0:
                                Str_result.append("None");
                                break;

                            case 1:
                                Str_result.append("Error");
                                break;

                            case 2:
                                Str_result.append("Adult");
                                break;

                            case 3:
                                Str_result.append("NeoNat");
                                break;

                            default:
                                Str_result.append("Bad Read");
                                break;

                        }
                        Str_result.append((String.format(" %d", UARTreadVal[9])).toString());
                    }else
                        Str_result = new StringBuilder("BAD HEADER");
                } else
                    Str_result = new StringBuilder("NO DATA");
                //Str_result = new StringBuilder("Battery Temperature: " + String.format("%6.2fC\n", (UARTreadVal / 10.0 - 273.15)));
                results.setText(Str_result);
            }
            try {
                closePort(ttymxc2);
            } catch (Exception e) {
                Log.e("UART", "Error closing port: ", e);
            }
        }
    }






            //JNI calls
    public static native int openPort(String path);
    public static native void closePort(int fd);
    public static native int cfgUART(int fd, boolean Baud9600);
    public static native int sendI2CPort(int fd, byte[] sendStr, int numChars);
    public static native int readI2CPort(int fd, byte addr, int[] rcvBuf, int numChars);
    public static native int sendUARTPort(int fd, byte[] sendStr, int numChars);
    public static native int readUARTport(int fd, int[] rcvBuf, int numChars);
}


