package com.example.aweia.map3;

import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.InputStream;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by aweia on 2017/5/1.
 */

public class GetDataThread extends Thread{
    BluetoothSocket btsocket;
    private InputStream InStream;
    private ECGview ecgview ;
    private int uselessData = 0 ;
    int data[] = new int[2000];
    private Queue<Integer> RowData = new LinkedList<>();
    private Handler bpmHandler;
    private Heart heart = new Heart();

    public GetDataThread(BluetoothSocket btsocket, ECGview ecgview, Handler bpmHandler){
        this.btsocket = btsocket ;
        this.ecgview = ecgview;
        this.bpmHandler = bpmHandler;
    }

    @Override
    public void run() {
        int count = 0 ;
        while (true) {
            try {
                if(btsocket.getInputStream().available() > 2) {

                    InStream = btsocket.getInputStream();       //讀取進來的資料放在InputStream
                    int temp = InStream.read();                 //temp儲存接收的心跳資料 經過下面兩行處理成原始訊號
                    temp <<= 8;
                    temp += InStream.read();

                    ecgview.addValue(temp);
                    RowData.offer(temp);
                    count++;

                    if(temp > 700)
                        uselessData++;

                    if(count>2000) {
                        count=0;
                        for (int i = 0; i < 2000; i++)
                            data[i] = RowData.poll();

                        displayBPM(heart.getbpm(data,uselessData));
                        uselessData = 0 ;
                        RowData.clear();
                    }
                }
            } catch (Exception e) {
                Log.e("Data_Counter", e.toString()  + "Line = " + e.getStackTrace()[0].getLineNumber());
            }
        }
    }

    private void displayBPM(String bpm){
        Bundle bundle = new Bundle();
        bundle.putString("bpm",bpm);
        Message message = new Message();
        message.setData(bundle);
        bpmHandler.sendMessage(message);
    }

}
