
package com.reactlibrary;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import android.content.Context;
import android.util.Log;

import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.bridge.Arguments;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;

public class RNSimpleCompassModule extends ReactContextBaseJavaModule implements SensorEventListener {

    private final ReactApplicationContext reactContext;

    private static Context mApplicationContext;
    private int mAzimuth = 0; // degree
    private int mFilter = 1;
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private float[] orientation = new float[3];
    private float[] rMat = new float[9];
    private static final String TAG = "COMPASS";

    public RNSimpleCompassModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        mApplicationContext = reactContext.getApplicationContext();
    }

    @Override
    public String getName() {
        return "RNSimpleCompass";
    }

    //see https://stackoverflow.com/questions/37252567/how-to-return-a-boolean-from-reactmethod-in-react-native

    @ReactMethod
    public void start(int filter, final Promise promise) {

        final int NO_SENSOR = -1;
        final int ROTATION_VECTOR_SENSOR = 0;
        final int GEO_ROTATION_VECTOR_SENSOR = 1;
        int sensorFound = NO_SENSOR;

        if (mSensorManager == null) {
            mSensorManager = (SensorManager) mApplicationContext.getSystemService(Context.SENSOR_SERVICE);
        }

        if (mSensor == null) {
            mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            if (mSensor==null)
            {Log.d(TAG,"Sensore di rotazione non supportato");
                mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR);
                if (mSensor==null)
                    Log.e(TAG, "Non è stato possibile trovare alcun sensore che supporti la bussola");
                else {
                    Log.d(TAG,"Sensore di geo-rotazione abilitato. Bussola OK!");
                    sensorFound = GEO_ROTATION_VECTOR_SENSOR;
                }
            }
            else
            {
                Log.d(TAG,"Sensore di rotazione abilitato. Bussola OK!");
                sensorFound = ROTATION_VECTOR_SENSOR;
            }
        }

        mFilter = filter;
        if (mSensor!=null)
        {

            Log.d(TAG,"Registro il Listener della bussola...");
            mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_UI);
            Log.d(TAG,"Risolvo la promise!!!");
            promise.resolve(sensorFound);
        }
        else promise.reject("-1", "No sensor for digital compass available!");



    }

    @ReactMethod
    public void stop() {
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if( event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR || event.sensor.getType() == Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR  ){
            // calculate th rotation matrix
            SensorManager.getRotationMatrixFromVector(rMat, event.values);
            // get the azimuth value (orientation[0]) in degree
            int newAzimuth = (int) ( Math.toDegrees( SensorManager.getOrientation( rMat, orientation )[0] ) + 360 ) % 360;

            //dont react to changes smaller than the filter value
            if (Math.abs(mAzimuth - newAzimuth) <    mFilter) {
                return;
            }

            mAzimuth = newAzimuth;

            getReactApplicationContext()
                    .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit("HeadingUpdated", mAzimuth);
        }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
