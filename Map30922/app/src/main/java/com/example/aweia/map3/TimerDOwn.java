package com.example.aweia.map3;

import android.os.CountDownTimer;


import android.app.Activity;
import android.os.Bundle;
import android.os.CountDownTimer;




public class TimerDOwn extends Thread {

    public CountDownTimer cTimer;

    public TimerDOwn(CountDownTimer cTimer){
        this.cTimer = cTimer;
    }

    @Override
    public void run() {
        cTimer.start();
    }
}
