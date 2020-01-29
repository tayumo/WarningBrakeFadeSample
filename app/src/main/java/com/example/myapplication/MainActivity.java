package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;


public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;

    private long startTime;
    private long stopTime;
    private long elapsedTimeMs;
    private long brakingTimeMs;
    private AlertDialog notifyEnableEngineBrakeDialog;
    static private long MONITORING_INTERVAL_MS = 500;
    static private long BRAKE_FADE_CAUSING_TIME_MS = 5000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
    }

    @Override
    protected void onResume() {
        super.onResume();

        startTime = System.currentTimeMillis();
        stopTime = System.currentTimeMillis();
        elapsedTimeMs = 0;
        brakingTimeMs = 0;
        notifyEnableEngineBrakeDialog = null;

        Sensor gyro = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if(gyro != null){
            sensorManager.registerListener(this, gyro, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    private long calculateOnChangedIntervalTimeMs(){
        stopTime = System.currentTimeMillis();
        long intervalTimeMs = stopTime - startTime;
        startTime = stopTime;
        return intervalTimeMs;
    }

    private float calcPitchDegree(float ax, float az){
        return (float)( Math.atan2(az,ax) * 180.0 / Math.PI );
    }

    private boolean isBraking( float pitch ){
        return (pitch > 120.0f) ? true : false;
    }

    private void checkBrakeTime( float pitch ){
        boolean alreadyShowPopup = (BRAKE_FADE_CAUSING_TIME_MS <= brakingTimeMs) ? true : false;

        if( isBraking(pitch) ){
            brakingTimeMs += MONITORING_INTERVAL_MS;
        }else{
            brakingTimeMs = 0;
        }

        if(BRAKE_FADE_CAUSING_TIME_MS <= brakingTimeMs){
            if( !alreadyShowPopup ) {
                notifyEnableEngineBrakeDialog = new AlertDialog.Builder(this)
                        .setTitle("警告-フェード現象!")
                        .setMessage("エンジンブレーキを有効にしてください")
                        .setPositiveButton("OK", null)
                        .show();
            }
        }else{
            if( notifyEnableEngineBrakeDialog != null) {
                notifyEnableEngineBrakeDialog.dismiss();
                notifyEnableEngineBrakeDialog = null;
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float aX = event.values[0];
            float aY = event.values[1];
            float aZ = event.values[2];

            elapsedTimeMs += calculateOnChangedIntervalTimeMs();

            if( elapsedTimeMs > MONITORING_INTERVAL_MS ) {
                Log.d("debug", "Accelerometer X:" + aX + " Y:" + aY + " Z:" + aZ);

                float pitch = calcPitchDegree(aY, aZ);
                Log.d("debug", "pitch:" + pitch);

                checkBrakeTime( pitch );
                elapsedTimeMs -= MONITORING_INTERVAL_MS;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
