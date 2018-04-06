package com.dji.FPVDemo;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.TextureView.SurfaceTextureListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.vikramezhil.droidspeech.DroidSpeech;
import com.vikramezhil.droidspeech.OnDSListener;

import java.util.List;

import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJIError;
import dji.common.flightcontroller.FlightOrientationMode;
import dji.common.flightcontroller.virtualstick.FlightControlData;
import dji.common.flightcontroller.virtualstick.FlightCoordinateSystem;
import dji.common.flightcontroller.virtualstick.RollPitchControlMode;
import dji.common.flightcontroller.virtualstick.YawControlMode;
import dji.common.gimbal.CapabilityKey;
import dji.common.gimbal.Rotation;
import dji.common.gimbal.RotationMode;
import dji.common.product.Model;
import dji.common.useraccount.UserAccountState;
import dji.common.util.CommonCallbacks;
import dji.common.util.DJIParamMinMaxCapability;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.gimbal.Gimbal;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.sdk.useraccount.UserAccountManager;

public class MainActivity extends Activity implements SensorEventListener {

    private static final String TAG = MainActivity.class.getName();
    protected VideoFeeder.VideoDataCallback mReceivedVideoDataCallBack = null;

    // Codec for video live view
    protected DJICodecManager mCodecManager = null;

    protected TextureView mVideoSurface = null;
    protected ImageView ivLeft = null;
    protected ImageView ivRight = null;

    private Handler handler;

    private FlightController mFlightController = null;

    private TextView tvStop;
    private TextView tvStart;
    private TextView tvDebug;

    private SensorManager mSensorManager;
    private SensorEventListener gyroscopeSensorListener;

    private float roll = 0.0f;
    private float pitch = 0.0f;
    private float yaw = 0.0f;
    private float throttle = 0.5f;

    private boolean canSendData = false;

    private DroidSpeech droidSpeech;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        handler = new Handler();

        initUI();

        // The callback for receiving the raw H264 video data for camera live view
        mReceivedVideoDataCallBack = new VideoFeeder.VideoDataCallback() {

            @Override
            public void onReceive(byte[] videoBuffer, int size) {
                if (mCodecManager != null) {
                    mCodecManager.sendDataToDecoder(videoBuffer, size);
                }
            }
        };

        initFlightController();
        initRecognition();
    }

    protected void onProductChange() {
        initPreviewer();
//        loginAccount();
    }

    private void loginAccount(){

        UserAccountManager.getInstance().logIntoDJIUserAccount(this,
                new CommonCallbacks.CompletionCallbackWith<UserAccountState>() {
                    @Override
                    public void onSuccess(final UserAccountState userAccountState) {
                        Log.e(TAG, "Login Success");
                    }
                    @Override
                    public void onFailure(DJIError error) {
                        showToast("Login Error:"
                                + error.getDescription());
                    }
                });
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
        initPreviewer();
        onProductChange();
        initGyroscopoe();

        if(mVideoSurface == null) {
            Log.e(TAG, "mVideoSurfaceRigth is null");
        }
    }

    @Override
    public void onPause() {
        Log.e(TAG, "onPause");
        uninitPreviewer();
        canSendData = false;
        if(mSensorManager != null) mSensorManager.unregisterListener(this);
        super.onPause();
    }

    @Override
    public void onStop() {
        Log.e(TAG, "onStop");
        super.onStop();
    }

    public void onReturn(View view){
        Log.e(TAG, "onReturn");
        this.finish();
    }

    @Override
    protected void onDestroy() {
        Log.e(TAG, "onDestroy");
        uninitPreviewer();
        uninitFlightController();
        if( droidSpeech != null) droidSpeech.closeDroidSpeechOperations();
        droidSpeech = null;
        super.onDestroy();
    }

    private void initUI() {
        // init mVideoSurface
        mVideoSurface = (TextureView)findViewById(R.id.video_previewer_surface_right);
        ivLeft = (ImageView)findViewById(R.id.ivLeft);
        ivRight = (ImageView)findViewById(R.id.ivRigth);
        tvStop = (TextView) findViewById(R.id.tvStop);
        tvStart = (TextView) findViewById(R.id.tvStart);
        tvDebug = (TextView) findViewById(R.id.tvDebug);
        tvDebug.setVisibility(View.GONE);

        if (null != mVideoSurface) {
            mVideoSurface.setSurfaceTextureListener(surfaceTextureListener);
        }

        tvStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                land();
            }
        });

        tvStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                takeOff();
            }
        });
    }

    private void takeOff(){
        mFlightController.startTakeoff(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                Log.d(TAG,"Take off done. Result: " + (djiError == null
                        ? "Success"
                        : djiError.getDescription()));
                canSendData = true;

                startSendingData();
            }
        });
    }

    private void land(){
        canSendData = false;
        mFlightController.startLanding(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                Log.d(TAG,"Landing done. Refsult: " + (djiError == null
                        ? "Success"
                        : djiError.getDescription()));
            }
        });
    }

    private void initPreviewer() {

        BaseProduct product = FPVDemoApplication.getProductInstance();

        if (product == null || !product.isConnected()) {
            showToast(getString(R.string.disconnected));
        } else {
            if (null != mVideoSurface) {
                mVideoSurface.setSurfaceTextureListener(surfaceTextureListener);
            }
            if (!product.getModel().equals(Model.UNKNOWN_AIRCRAFT)) {
                VideoFeeder.getInstance().getPrimaryVideoFeed().setCallback(mReceivedVideoDataCallBack);
            }
        }
    }

    private void uninitPreviewer() {
        Camera camera = FPVDemoApplication.getCameraInstance();
        if (camera != null){
            // Reset the callback
            VideoFeeder.getInstance().getPrimaryVideoFeed().setCallback(null);
        }
    }

    private SurfaceTextureListener surfaceTextureListener = new SurfaceTextureListener()
    {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            if (mCodecManager == null) {
                mCodecManager = new DJICodecManager(MainActivity.this, surface, width, height);
            }
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            Log.e(TAG, "onSurfaceTextureSizeChanged");
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            Log.e(TAG,"onSurfaceTextureDestroyed");
            if (mCodecManager != null) {
                mCodecManager.cleanSurface();
                mCodecManager = null;
            }

            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            Bitmap bitmap = mVideoSurface.getBitmap();
            if(ivRight != null)
                ivRight.setImageBitmap(bitmap);
            if(ivLeft != null)
                ivLeft.setImageBitmap(bitmap);
        }
    };

    public void showToast(final String msg) {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void switchCameraMode(SettingsDefinitions.CameraMode cameraMode){

        Camera camera = FPVDemoApplication.getCameraInstance();
        if (camera != null) {
            camera.setMode(cameraMode, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError error) {

                    if (error == null) {
                        showToast("Switch Camera Mode Succeeded");
                    } else {
                        showToast(error.getDescription());
                    }
                }
            });
            }
    }

    // Method for taking photo
    private void captureAction(){

        final Camera camera = FPVDemoApplication.getCameraInstance();
        if (camera != null) {

            SettingsDefinitions.ShootPhotoMode photoMode = SettingsDefinitions.ShootPhotoMode.SINGLE; // Set the camera capture mode as Single mode
            camera.setShootPhotoMode(photoMode, new CommonCallbacks.CompletionCallback(){
                    @Override
                    public void onResult(DJIError djiError) {
                        if (null == djiError) {
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    camera.startShootPhoto(new CommonCallbacks.CompletionCallback() {
                                        @Override
                                        public void onResult(DJIError djiError) {
                                            if (djiError == null) {
                                                showToast("take photo: success");
                                            } else {
                                                showToast(djiError.getDescription());
                                            }
                                        }
                                    });
                                }
                            }, 2000);
                        }
                    }
            });
        }
    }

    // Method for starting recording
    private void startRecord(){

        final Camera camera = FPVDemoApplication.getCameraInstance();
        if (camera != null) {
            camera.startRecordVideo(new CommonCallbacks.CompletionCallback(){
                @Override
                public void onResult(DJIError djiError)
                {
                    if (djiError == null) {
                        showToast("Record video: success");
                    }else {
                        showToast(djiError.getDescription());
                    }
                }
            }); // Execute the startRecordVideo API
        }
    }

    // Method for stopping recording
    private void stopRecord(){

        Camera camera = FPVDemoApplication.getCameraInstance();
        if (camera != null) {
            camera.stopRecordVideo(new CommonCallbacks.CompletionCallback(){

                @Override
                public void onResult(DJIError djiError)
                {
                    if(djiError == null) {
                        showToast("Stop recording: success");
                    }else {
                        showToast(djiError.getDescription());
                    }
                }
            }); // Execute the stopRecordVideo API
        }

    }

    private void initFlightController() {
        mFlightController = ModuleVerificationUtil.getFlightController();
        if (mFlightController == null) {
            Log.e(TAG,"Flight controller could not be initialized");
            return;
        }

        mFlightController.getFlightAssistant().setCollisionAvoidanceEnabled(true,null);
        mFlightController.getFlightAssistant().setActiveObstacleAvoidanceEnabled(true,null);
        mFlightController.setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY);
        mFlightController.setYawControlMode(YawControlMode.ANGLE);
        mFlightController.setRollPitchControlMode(RollPitchControlMode.VELOCITY);

        Gimbal gimbal = DJISDKManager.getInstance().getProduct().getGimbal();
        Number value = ((DJIParamMinMaxCapability) (gimbal.getCapabilities().get(CapabilityKey.ADJUST_PITCH))).getMin();
        Rotation.Builder builder = new Rotation.Builder().mode(RotationMode.ABSOLUTE_ANGLE).time(2);
        value = value.doubleValue()*0.1;
        builder.pitch(value.floatValue());
        gimbal.rotate(builder.build(),null);

        mFlightController.setFlightOrientationMode(FlightOrientationMode.AIRCRAFT_HEADING,
                new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        Log.d(TAG,"Aircraft heading. Result: " + (djiError == null
                                ? "Success"
                                : djiError.getDescription()));
                    }
                });

        mFlightController.setVirtualStickModeEnabled(true, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                Log.d(TAG,"Virtual stick enabled. Result: " + (djiError == null
                        ? "Success"
                        : djiError.getDescription()));
            }
        });
    }

    private boolean isFlightControllerSupported() {
        return DJISDKManager.getInstance().getProduct() != null &&
                DJISDKManager.getInstance().getProduct() instanceof Aircraft &&
                ((Aircraft) DJISDKManager.getInstance().getProduct()).getFlightController() != null;
    }

    private float[] mAccelerometerReading = new float[3];
    private float[] mMagnetometerReading = new float[3];
    private float[] mRotationMatrix = new float[9];
    private float[] mOrientationAngles = new float[3];
    private float calibrationAngle = 1000.0f;

    private void initGyroscopoe(){
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor accelerometerSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor magnetometerSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        mSensorManager.registerListener(this,
                accelerometerSensor,
                SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this,
                magnetometerSensor,
                SensorManager.SENSOR_DELAY_NORMAL);

        // Rotation matrix based on current readings from accelerometer and magnetometer.
        SensorManager.getRotationMatrix(mRotationMatrix, null,
                mAccelerometerReading, mMagnetometerReading);

        // Express the updated rotation matrix as three orientation angles.
        mOrientationAngles = new float[3];
        SensorManager.getOrientation(mRotationMatrix, mOrientationAngles);

        convertToDegrees(mOrientationAngles);
    }

    private float lastMobileOrientation = 0.0f;

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(sensorEvent.values, 0, mAccelerometerReading,
                    0, mAccelerometerReading.length);
        }
        else if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(sensorEvent.values, 0, mMagnetometerReading,
                    0, mMagnetometerReading.length);
        }

        // Update rotation matrix, which is needed to update orientation angles.
        SensorManager.getRotationMatrix(mRotationMatrix, null,
                mAccelerometerReading, mMagnetometerReading);
        SensorManager.getOrientation(mRotationMatrix, mOrientationAngles);

        convertToDegrees(mOrientationAngles);
        if(mOrientationAngles[0] != 0.0f) {

            /////// YAW ///////

            float mobileOrientation = mOrientationAngles[0];
            float droneOrientation = mFlightController.getCompass().getHeading();

            if(yaw == 0.0f){
                // First iteration
                yaw = droneOrientation;
            } else{
                // All other iterations
                yaw -= lastMobileOrientation - mobileOrientation;
            }
            lastMobileOrientation = mobileOrientation;

            // Fix the yaw degrees in case is over 180 or below -180. Example: -200 is converted to 160
            if (yaw > 180) yaw = -360 + yaw;
            if (yaw < -180) yaw = 360 + yaw;

            ///////// ROLL /////////
            // Phone roll goes from 0 to -180. 0 maximum forward. -180 maximum backward.
            // For us, -45 and -135 would be the maximum degrees for maximum speed (forward and backward)
            float rollJoyControlMaxSpeed = 10.0f;
            float mobileRoll = mOrientationAngles[2];
            if (mobileRoll > -45){
                mobileRoll = -45.0f;
            }
            else if (mobileRoll < -135){
                mobileRoll = -135.0f;
            }
            float normalizedRoll = -(((Math.abs(mobileRoll)-45)/45)-1);

            if(Math.abs(normalizedRoll) < 0.2){
                normalizedRoll = 0;
            }

            roll = (rollJoyControlMaxSpeed * normalizedRoll);

            ///////// PITCH /////////
            // Phone roll goes from 90 to -90. 90 maximum left. -90 maximum rigth. We convert that
            // so the values gos from -10 to 10. -10 maximum left and 10 maximum right
            float pitchJoyControlMaxSpeed = 10.0f;
            float mobilePitch = mOrientationAngles[1];
            if (mobilePitch < -45){
                mobilePitch = -45.0f;
            }
            else if (mobilePitch > 45){
                mobilePitch = 45.0f;
            }
            float normalizedPitch = -(mobilePitch/45);

            if(Math.abs(normalizedPitch) < 0.2){
                normalizedPitch = 0;
            }

            pitch = (pitchJoyControlMaxSpeed * normalizedPitch);

            Log.d(TAG,"AAA Pitch" + pitch);
        }

        MainActivity.this.runOnUiThread(new Runnable() {
            public void run() {
                tvDebug.setText("Pitch: " + roll + ", Roll: " + pitch + ", Yaw: " + yaw);
            }
        });
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {}

    private void convertToDegrees(float[] vector){
        for (int i = 0; i < vector.length; i++){
            vector[i] = Math.round(Math.toDegrees(vector[i]));
        }
    }

    private void uninitFlightController(){
        canSendData = false;
    }

    private void startSendingData(){
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(canSendData) {
                    sendFlightControlData();
                    startSendingData();
                }
            }
        }, 200);
    }

    private void sendFlightControlData() {
        Log.d(TAG, "available: " + mFlightController.isVirtualStickControlModeAvailable());
        if (mFlightController.isVirtualStickControlModeAvailable() && yaw != 0.0f) {
            Log.d(TAG, "Sending command to drone");

            mFlightController.sendVirtualStickFlightControlData(
                    new FlightControlData(
                            pitch,
                            roll,
                            yaw,
                            throttle),
                    new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            Log.d(TAG, "CompletionCallback. Result: " + (djiError == null
                                    ? "Success"
                                    : djiError.getDescription()));
                        }
                    });
            Log.d(TAG, "Pitch: " + roll + ", Roll: " + pitch + ", Yaw: " + yaw + ", Throttle: " + throttle);

        } else {
            Log.e(TAG, "Flight not available");
        }
    }

    private void initRecognition(){
        Log.d(TAG,"initRecognition");
        AudioManager audioManager=(AudioManager)getSystemService(Context.AUDIO_SERVICE);
        audioManager.setStreamMute(AudioManager.STREAM_MUSIC, true);

        droidSpeech = new DroidSpeech(this, null);
        droidSpeech.setOnDroidSpeechListener(new OnDSListener() {
            @Override
            public void onDroidSpeechSupportedLanguages(String currentSpeechLanguage, List<String> supportedSpeechLanguages) {}

            @Override
            public void onDroidSpeechRmsChanged(float rmsChangedValue) {}

            @Override
            public void onDroidSpeechLiveResult(String liveSpeechResult) {}

            @Override
            public void onDroidSpeechFinalResult(String finalSpeechResult) {
                Log.d(TAG,"onDroidSpeechFinalResult: " + finalSpeechResult);
                if(mFlightController != null) {
                    if (finalSpeechResult.contains("take off")) {
                        takeOff();
                    } else if (finalSpeechResult.contains("land")) {
                        land();
                    } else if (finalSpeechResult.contains("up")) {
                        throttle += 0.5f;
                    } else if (finalSpeechResult.contains("down")) {
                        throttle -= 0.5f;
                    } else if (finalSpeechResult.contains("photo")) {
                        captureAction();
                    }else if (finalSpeechResult.contains("start recording")) {
                        startRecord();
                    } else if (finalSpeechResult.contains("stop recording")) {
                        stopRecord();
                    }
                }else Log.e(TAG,"mFlightController is null");
            }

            @Override
            public void onDroidSpeechClosedByUser() {
                Log.d(TAG,"onDroidSpeechClosedByUser: ");
            }

            @Override
            public void onDroidSpeechError(String errorMsg) {
                Log.e(TAG,"onDroidSpeechError: " + errorMsg);
            }
        });

        // Start speech recognition, check null because onDestroy could be called in between the method
        if(droidSpeech != null) droidSpeech.startDroidSpeechRecognition();
    }
}
