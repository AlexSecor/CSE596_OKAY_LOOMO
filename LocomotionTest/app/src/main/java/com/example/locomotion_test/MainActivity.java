package com.example.locomotion_test;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;

import com.google.android.material.snackbar.Snackbar;
import com.segway.robot.algo.Pose2D;
import com.segway.robot.algo.minicontroller.CheckPoint;
import com.segway.robot.algo.minicontroller.CheckPointStateListener;
import com.segway.robot.sdk.base.bind.ServiceBinder;
import com.segway.robot.sdk.locomotion.sbv.Base;

public class MainActivity extends AppCompatActivity {

    Base mBase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Loomo Implementation
        mBase = Base.getInstance();
        mBase.bindService(getApplicationContext(), new ServiceBinder.BindStateListener() {
            @Override
            public void onBind() {
                mBase.setOnCheckPointArrivedListener(new CheckPointStateListener() {
                    @Override
                    public void onCheckPointArrived(CheckPoint checkPoint, Pose2D realPose, boolean isLast) {

                    }

                    @Override
                    public void onCheckPointMiss(CheckPoint checkPoint, Pose2D realPose, boolean isLast, int reason) {

                    }
                });
            }

            @Override
            public void onUnbind(String reason) {

            }
        });
    }


    public void moveLoomo(View view) {
        mBase.setControlMode(Base.CONTROL_MODE_NAVIGATION);
        mBase.cleanOriginalPoint();
        Pose2D pose2D = mBase.getOdometryPose(-1);
        mBase.setOriginalPoint(pose2D);
        mBase.addCheckPoint(3f, 0f);
        try {
            wait(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mBase.addCheckPoint(7f, 0);
        try {
            wait(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mBase.addCheckPoint(10f, 0f);
        Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();
    }
}