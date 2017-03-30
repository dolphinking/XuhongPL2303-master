package com.xuhong.xuhongpl2303_master;

import android.app.Activity;
import android.content.Context;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;


import tw.com.prolific.driver.pl2303.PL2303Driver;

public class MainActivity extends Activity {


    private static final String ACTION_USB_PERMISSION = "com.xuhong.xuhongpl2303_master.USB_PERMISSION";
    private static final String TAG = "==w";
    private TextView tv_showData;
    private PL2303Driver mSerial;
    private PL2303Driver.BaudRate mBaudrate = PL2303Driver.BaudRate.B9600;

    private byte[] rbuf = new byte[20];
    private StringBuffer sbHex = new StringBuffer();
    private String recieve_Data;

    private static final int HANDLER_RECIEVE = 102;

    private Thread thread;
    private Boolean isOpenThread =false;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == HANDLER_RECIEVE) {
              // recieve_Data = (String) msg.obj;
                //这里更新ui
                tv_showData.setText(recieve_Data);
                //同时把他们为null，重新new
                sbHex = null;
                sbHex = new StringBuffer();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        inintView();
         thread =new Thread(runnable);
    }

    private void inintView() {


        tv_showData = (TextView) findViewById(R.id.tv_showData);
        mBaudrate = PL2303Driver.BaudRate.B9600;
        // get service
        mSerial = new PL2303Driver((UsbManager) getSystemService(Context.USB_SERVICE),
                this, ACTION_USB_PERMISSION);
        // check USB host function.
        if (!mSerial.PL2303USBFeatureSupported()) {
            Toast.makeText(this, "无支持USB主机", Toast.LENGTH_SHORT)
                    .show();
            Log.d(TAG, "无支持USB主机");
            mSerial = null;
        }
        if (null == mSerial)
            return;

        if (mSerial.isConnected()) {

            mBaudrate = PL2303Driver.BaudRate.B9600;

            // if (!mSerial.InitByBaudRate(mBaudrate)) {
            if (!mSerial.InitByBaudRate(mBaudrate, 700)) {

                if (!mSerial.PL2303Device_IsHasPermission()) {
                    Toast.makeText(this, "打开失败，可能权限不够！", Toast.LENGTH_SHORT).show();
                }

                if (mSerial.PL2303Device_IsHasPermission() && (!mSerial.PL2303Device_IsSupportChip())) {
                    Toast.makeText(this, "打开失败，可能芯片不支持, 请选择PL2303HXD/RA/EA系列的芯片.", Toast.LENGTH_SHORT).show();
                }

            } else {
                Toast.makeText(this, "connected : ", Toast.LENGTH_SHORT).show();
            }
        }
        Log.d(TAG, "离开 openUsbSerial");
    }

    private void readData() {

        Log.d(TAG, "Enter  openUsbSerial");

        if (null == mSerial)
            return;

        if (mSerial.isConnected()) {

            mBaudrate = PL2303Driver.BaudRate.B9600;

            if (!mSerial.InitByBaudRate(mBaudrate, 700)) {
                if (!mSerial.PL2303Device_IsHasPermission()) {
                    Toast.makeText(this, "打开失败，也许缺少权限吧", Toast.LENGTH_SHORT).show();
                }

                if (mSerial.PL2303Device_IsHasPermission() && (!mSerial.PL2303Device_IsSupportChip())) {
                    Toast.makeText(this, "打开失败, 请选择PL2303HXD / RA / EA 系列芯片", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "connected : ", Toast.LENGTH_SHORT).show();
            }
        }
        thread.start();

    }





    private Runnable runnable=new Runnable() {
        @Override
        public void run() {
            byte[] temperRbuf =new byte[20];
            Log.d(TAG, "进入线程！");
            if (null == mSerial)
                return;
            if (!mSerial.isConnected())
                return;
            if (isOpenThread){
                Log.i(TAG, "跳出接收循环~" );
                return;
            }
            while (mSerial.isConnected()) {
                int length = mSerial.read(temperRbuf);
                if (length < 0) {
                    Log.d(TAG, "Fail to bulkTransfer(read data)");
                } else if (length > 0) {
                        for (int j = 0; j < length; j++) {
                            Log.i(TAG, "rbuf接受为：" + rbuf[j]);
                            sbHex.append((char) (rbuf[j] & 0x000000FF));
                        }
                    rbuf = null;
                    rbuf = new byte[20];
                    Log.d(TAG, "string接受为：" + recieve_Data);
                    Log.d(TAG, "sbHex接受为：" + sbHex.toString());
                    Message message = mHandler.obtainMessage();
                    message.what = HANDLER_RECIEVE;
                    message.obj = sbHex.toString();
                    mHandler.sendMessage(message);
                } else {
                    //Log.i(TAG, "rbuf接受长度为0");
                }
            }



        }
    };

    @Override
    protected void onDestroy() {
        Log.d(TAG, "Enter onDestroy");
        if (mSerial != null) {
            mSerial.end();
            mSerial = null;
            isOpenThread=true;
        }
        super.onDestroy();
        Log.d(TAG, "Leave onDestroy");
    }

    public void onResume() {
        super.onResume();
        String action = getIntent().getAction();
        Log.d(TAG, "onResume:" + action);
        if (!mSerial.isConnected()) {
            if (!mSerial.enumerate()) {
                Toast.makeText(this, "no more devices found", Toast.LENGTH_SHORT).show();
                return;
            } else {
                Log.d(TAG, "onResume:enumerate succeeded!");
            }
        }//如果连接
        Toast.makeText(this, "attached", Toast.LENGTH_SHORT).show();
        readData();
    }
}
