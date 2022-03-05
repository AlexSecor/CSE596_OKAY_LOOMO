package com.example.cvg_demo1;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;

import com.google.android.material.snackbar.Snackbar;
import com.segway.robot.algo.Pose2D;
import com.segway.robot.algo.minicontroller.CheckPoint;
import com.segway.robot.algo.minicontroller.CheckPointStateListener;
import com.segway.robot.sdk.base.bind.ServiceBinder;
import com.segway.robot.sdk.locomotion.sbv.Base;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // For Loomo
    Base mBase;

    // lastLocation will initialize at home, or 0
    private int lastLocation = 0;
    public int getLastLocation() {
        return lastLocation;
    }
    public void setLastLocation(int lastLocation) {
        this.lastLocation = lastLocation;
    }

    private float accumulativeX = 0;
    public float getAccumulativeX() {
        return accumulativeX;
    }
    public void setAccumulativeX(float accumulativeX) {
        this.accumulativeX = accumulativeX;
    }

    private float accumulativeY = 0;
    public float getAccumulativeY() {
        return accumulativeY;
    }
    public void setAccumulativeY(float accumulativeY) {
        this.accumulativeY = accumulativeY;
    }

    private int faceRight = 0;
    public int getFaceRight() {
        return faceRight;
    }
    public void setFaceRight(int faceRight) {
        this.faceRight = faceRight;
    }

    private int faceLeft = 0;
    public int getFaceLeft() {
        return faceLeft;
    }
    public void setFaceLeft(int faceLeft) {
        this.faceLeft = faceLeft;
    }

    // list that holds the distances between each door (206 and 207 share a distance)
    List<Float> distances = Arrays.asList(0f, 1.9f, 3.45f, 0f, 3.6f, 3.5f);

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

    // sets the destination that Loomo should follow
    public void setDestination(View view) {
        // each button has a tag. This serves as a destination number that corresponds to a door.
        // 1 - 205, 2 - 206, 3 - 207, 4 - 208, 5 - 209, 0 - Home
        Integer destinationNumber = Integer.parseInt((String)view.getTag());

        if (destinationNumber != lastLocation)
        {
            mapCheckpoints(destinationNumber);

            setLastLocation(destinationNumber);
        }
    }

    public boolean isGoHomeSelected(int destinationNumber)
    {
        // if any button other than "go home" was selected
        if (destinationNumber > 0)
        {
            return false;
        }

        return true;
    }

    public boolean isLoomoDrivingForwards(int destinationNumber, int temp)
    {
        if (temp < destinationNumber)
        {
            return true;
        }

        return false;
    }

    public int isLoomoFacingLeftOrRight()
    {
        int rightTemp = getFaceRight();
        int leftTemp = getFaceLeft();

        if (rightTemp == 1 && leftTemp == 0)
        {
            return 1;
        }
        else if (leftTemp == 1 && rightTemp == 0)
        {
            return 2;
        }
        else if (rightTemp == 0 && leftTemp == 0)
        {
            return 0;
        }
        else
        {
            // there was an error
            this.finishAffinity();
        }

        return 0;
    }

    public void alignLoomo()
    {
        int loomoDirection = isLoomoFacingLeftOrRight();

        // 1 = right
        if (loomoDirection == 1)
        {
            faceLeft();
            setFaceRight(0);
            setFaceLeft(0);
        }
        // 2 = left
        else if (loomoDirection == 2)
        {
            faceRight();
            setFaceLeft(0);
            setFaceRight(0);
        }
    }

    // checks whether Loomo is going from room 206 to 207, whose doors are located on a corner (requiring no movement by Loomo)
    public boolean checkIfAdjacentDoors(int destinationNumber)
    {
        if ((destinationNumber == 2 && lastLocation == 3) || (destinationNumber == 3 && lastLocation == 2))
        {
            return true;
        }

        return false;
    }

    // moves Loomo based on the checkpoint selected
    public void mapCheckpoints(int destinationNumber)
    {
        // if any button other than "go home" was clicked
        if(!isGoHomeSelected(destinationNumber))
        {
            // check if Loomo is going from room 206 to 207
            // if Loomo isn't:
            if (checkIfAdjacentDoors(destinationNumber) == false)
            {
                int temp = getLastLocation();
                float distanceTemp;
                // a list of checkpoints will be constructed as the list traverses from door to door
                //List<Float> checkpoints = Arrays.asList();
                ArrayList<Float> checkpoints = new ArrayList<Float>();

                // check if Loomo is traversing forwards (205 - 209)
                if (isLoomoDrivingForwards(destinationNumber, temp))
                {
                    while (isLoomoDrivingForwards(destinationNumber, temp))
                    {
                        // if temp is home, skip it
                        /*
                        if (temp == 0)
                        {
                            temp++;
                        }
                        */

                        // commented this out above because when it was going from door
                        // 205 to 206 with 205 is the last position, it adds both 2.1 and 3.8 to the checkpoints
                        temp++;

                        distanceTemp = distances.get(temp);

                        checkpoints.add(distanceTemp);
                    }

                    // the flag is set to 0 meaning that the list should be processed as forward traversal
                    moveLoomo(checkpoints, 0, destinationNumber);

                    // this is checked because at door 3 Loomo will need to be facing forwards
                    if (destinationNumber != 3)
                    {
                        //faceRight();
                        //setFaceRight(1);
                        //setFaceLeft(0);
                    }
                }
                // check if Loomo is traversing backwards (209 - 205)
                else if (temp > destinationNumber)
                {
                    while (temp >= destinationNumber)
                    {
                        distanceTemp = distances.get(temp);

                        checkpoints.add(distanceTemp);

                        temp--;
                    }

                    moveLoomo(checkpoints, 1, destinationNumber);

                    if (destinationNumber != 2)
                    {
                        //faceLeft();
                    }
                }

                return;
            }
            // if Loomo is:
            else
            {
                // turn Loomo on a dime to look at the door
                if (destinationNumber > getLastLocation())
                {
                    //faceLeft();
                    //setFaceLeft(1);
                    //setFaceRight(0);
                }
                else
                {
                    //faceRight();
                    //setFaceRight(1);
                    //setFaceLeft(0);
                }
            }

            return;
        }
        else
        {
            // Loomo is going home
            sendLoomoHome();
        }
    }

    public void sendLoomoHome()
    {
        mBase.setControlMode(Base.CONTROL_MODE_NAVIGATION);
        mBase.cleanOriginalPoint();
        Pose2D pose2D = mBase.getOdometryPose(-1);
        mBase.setOriginalPoint(pose2D);

        // if Loomo is at door 208 or 209
        if (getLastLocation() == 4 || getLastLocation() == 5)
        {
            float x = accumulativeY;
            float y = accumulativeX;

            // Loomo will travel to 209 - 207
            mBase.addCheckPoint(-x, 0f);

            mBase.setControlMode(Base.CONTROL_MODE_NAVIGATION);
            mBase.cleanOriginalPoint();
            pose2D = mBase.getOdometryPose(-1);
            mBase.setOriginalPoint(pose2D);

            // Loomo will travel to 207 - Home
            mBase.addCheckPoint(0f, y);

            // reset the accumulativeX and accumulative Y
            setAccumulativeX(0f);
            setAccumulativeY(0f);
        }
        else
        {
            mBase.addCheckPoint(0f, 0f, (float)(7 * Math.PI) / 6);

            mBase.setControlMode(Base.CONTROL_MODE_NAVIGATION);
            mBase.cleanOriginalPoint();
            pose2D = mBase.getOdometryPose(-1);
            mBase.setOriginalPoint(pose2D);

            // Loomo will travel to the right (209 - 207/206)
            //mBase.addCheckPoint(0f, -accumulativeY);
            mBase.addCheckPoint(0f, accumulativeY);
            // Loomo will travel backwards (207/206 - Home)
            //mBase.addCheckPoint(-accumulativeX, 0f);
            mBase.addCheckPoint(-accumulativeX, 0f);

            // reset the accumulativeX and accumulative Y
            setAccumulativeX(0f);
            setAccumulativeY(0f);
        }
    }

    public void moveLoomo(ArrayList<Float> checkpoints, int flag, int destinationNumber)
    {
        mBase.setControlMode(Base.CONTROL_MODE_NAVIGATION);
        mBase.cleanOriginalPoint();
        Pose2D pose2D = mBase.getOdometryPose(-1);
        mBase.setOriginalPoint(pose2D);

        int i;
        float accumulativeX = getAccumulativeX();
        // I have a local variable made because I want this to clear every time a button is pressed
        // the property accumulativeX is used for a more global scale
        float localAccumulativeX = 0;
        float localAccumulativeY = 0;
        boolean isTurned = false;

        // Loomo will end by facing right or left at the doors; align Loomo to look forwards again
        //alignLoomo();

        for (i = 0; i < checkpoints.size(); i++)
        {
            // uses the 0f for 207 and the flags to know when to turn left or right
            if ((checkpoints.get(i) == 0f || (distances.get(destinationNumber - 1) == 0) && getLastLocation() != 0 && getLastLocation() != 1))
            {
                // is turned in relation to the starting position
                isTurned = true;
                i++;
            }

            // this is Loomo going forwards (Home - 207)
            if (!isTurned)
            {
                // If the user selects 206 instead of 205 to 206, Loomo is working with the same set of checkpoints
                // in other words, if 206 is selected Loomo will go to 2.1f and then instead of traveling a separate
                // 3.8f, it travels to 3.8f. Meaning it's only going to travel 1.7f.
                localAccumulativeX = localAccumulativeX + checkpoints.get(i);
                accumulativeX = accumulativeX + checkpoints.get(i);

                mBase.addCheckPoint(localAccumulativeX, 0f);

                // sets the property accumulativeX so it stores the entirety of the distance traveled in the x direction
                setAccumulativeX(accumulativeX);
            }
            // this is Loomo turning left to go to rooms 208 and 209
            else if (isTurned && flag == 0 && (destinationNumber != 2 && destinationNumber != 3))
            {
                // if the buttons 208 or 209 was selected from home, the checkpoints need to be handled differently
                if (localAccumulativeX > 0)
                {
                    mBase.addCheckPoint(localAccumulativeX, localAccumulativeY, 1.8f);

                    localAccumulativeY = localAccumulativeY + checkpoints.get(i);
                    accumulativeY = accumulativeY + checkpoints.get(i);

                    mBase.addCheckPoint(localAccumulativeX, checkpoints.get(i));

                    setAccumulativeY(accumulativeY);
                }
                else if (localAccumulativeX == 0)
                {
                    if (getLastLocation() == 2 || getLastLocation() == 3)
                    {
                        mBase.addCheckPoint(localAccumulativeX, localAccumulativeY, 1.8f);

                        localAccumulativeY = localAccumulativeY + checkpoints.get(i);
                        accumulativeY = accumulativeY + checkpoints.get(i);

                        mBase.addCheckPoint(0, checkpoints.get(i));

                        setAccumulativeY(accumulativeY);
                    }
                    else
                    {
                        mBase.addCheckPoint(localAccumulativeX, localAccumulativeY, 1.8f);

                        localAccumulativeY = localAccumulativeY + checkpoints.get(i);
                        accumulativeY = accumulativeY + checkpoints.get(i);

                        mBase.addCheckPoint(0, checkpoints.get(i));

                        setAccumulativeY(accumulativeY);
                    }
                }
            }

            // TODO - for now Loomo only goes one direction (205 - 209)
        }
    }

    public void faceRight()
    {
        // I need to know if Loomo is facing right or left if it reaches a spot and the user clicks the button again
        //mBase.addCheckPoint(accumulativeX, accumulativeY, (float)(-1 * Math.PI)/2);
        mBase.cleanOriginalPoint();
        Pose2D pose2D = mBase.getOdometryPose(-1);
        mBase.setOriginalPoint(pose2D);

        mBase.addCheckPoint(0, 0, (float)(-1 * Math.PI)/2);
    }

    public void faceLeft()
    {
        //mBase.addCheckPoint(accumulativeX, accumulativeY, (float)(Math.PI) / 2);
        mBase.cleanOriginalPoint();
        Pose2D pose2D = mBase.getOdometryPose(-1);
        mBase.setOriginalPoint(pose2D);

        mBase.addCheckPoint(0, 0, (float)(Math.PI)/2);
    }



}