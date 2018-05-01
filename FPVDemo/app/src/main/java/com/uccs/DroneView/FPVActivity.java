package com.uccs.DroneView;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.TextureView.SurfaceTextureListener;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.uccs.DroneView.R;
import com.vikramezhil.droidspeech.DroidSpeech;
import com.vikramezhil.droidspeech.OnDSListener;

import java.util.List;
import java.util.Locale;

import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJIError;
import dji.common.flightcontroller.ConnectionFailSafeBehavior;
import dji.common.flightcontroller.FlightOrientationMode;
import dji.common.flightcontroller.virtualstick.FlightControlData;
import dji.common.flightcontroller.virtualstick.FlightCoordinateSystem;
import dji.common.flightcontroller.virtualstick.RollPitchControlMode;
import dji.common.flightcontroller.virtualstick.YawControlMode;
import dji.common.gimbal.CapabilityKey;
import dji.common.gimbal.Rotation;
import dji.common.gimbal.RotationMode;
import dji.common.product.Model;
import dji.common.util.CommonCallbacks;
import dji.common.util.DJIParamMinMaxCapability;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.gimbal.Gimbal;
import dji.sdk.sdkmanager.DJISDKManager;

import static com.uccs.DroneView.fragments.FragmentSettings.TAG_GIMBAL;
import static com.uccs.DroneView.fragments.FragmentSettings.TAG_SHARED_PREFS;
import static com.uccs.DroneView.fragments.FragmentSettings.TAG_THROTTLE;

/**
 * Main activity that is used to control the drone.
 *
 * Created by Damià Fuentes and Eric Velazquez
 * April 6th 2018
 */
public class FPVActivity extends Activity implements SensorEventListener, TextToSpeech.OnInitListener {

    private static final String TAG = FPVActivity.class.getName();
    private VideoFeeder.VideoDataCallback mReceivedVideoDataCallBack = null;

    // Codec for video live view
    private DJICodecManager mCodecManager = null;

    // UI views
    private TextureView mVideoSurface = null;
    private ImageView ivLeft = null;
    private ImageView ivRight = null;
    private TextView tvDebug;

    // To communicate with the drone
    private FlightController mFlightController = null;

    // To receive data from the phone sensors
    private SensorManager mSensorManager;

    // Data to send to the drone in order to move
    private float roll = 0.0f;
    private float pitch = 0.0f;
    private float yaw = 0.0f;
    private float throttle = 1f;
    private float gimbal = -1f;

    // False if drone is not flying, true if is it flying and data can be sent to the drone in order to move
    private boolean canSendData = false;

    // To recognize user speech and send specific data to the drone like "Take off"
    private DroidSpeech droidSpeech;

    // To vibrate the phone when something is going wrong
    private Vibrator vibrator;

    // To speech the error as we cannot see them in the screen
    private TextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initUI();

        setSettingsValues();

        // The callback for receiving the raw H264 video data for camera live view
        mReceivedVideoDataCallBack = new VideoFeeder.VideoDataCallback() {

            @Override
            public void onReceive(byte[] videoBuffer, int size) {
                if (mCodecManager != null) {
                    mCodecManager.sendDataToDecoder(videoBuffer, size);
                }
            }
        };

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        initTextToSpeech();
        initFlightController();
        initRecognition();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void setSettingsValues(){
        SharedPreferences prefs = getSharedPreferences(TAG_SHARED_PREFS, MODE_PRIVATE);
        gimbal = -((float) prefs.getInt(TAG_GIMBAL, 45));
        throttle = (float) prefs.getInt(TAG_THROTTLE, 1);
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
        initPreviewer();
        initPhoneSensors();
        initRecognition();
        initTextToSpeech();

        if(mVideoSurface == null) {
            Log.e(TAG, "mVideoSurfaceRigth is null");
        }
    }

    @Override
    public void onPause() {
        Log.e(TAG, "onPause");
        uninitPreviewer();
        uninitPhoneSensors();
        // In order to prevent accidental crashes
        land();
        uninitRecognition();
        uninitTextToSpeech();

        super.onPause();
    }

    @Override
    public void onStop() {
        Log.e(TAG, "onStop");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.e(TAG, "onDestroy");
        uninitFlightController();
        super.onDestroy();
    }

    private void initUI() {
        // init mVideoSurface
        mVideoSurface = (TextureView)findViewById(R.id.video_previewer_surface_right);
        ivLeft = (ImageView)findViewById(R.id.ivLeft);
        ivRight = (ImageView)findViewById(R.id.ivRigth);
        TextView tvStop = (TextView) findViewById(R.id.tvStop);
        TextView tvStart = (TextView) findViewById(R.id.tvStart);
        tvDebug = (TextView) findViewById(R.id.tvDebug);
        tvStop.setVisibility(View.INVISIBLE);
        tvStart.setVisibility(View.INVISIBLE);
        tvDebug.setVisibility(View.INVISIBLE);

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

    // Vibrate for 'millis' milliseconds
    private void vibrate(int millis){
        vibrator.vibrate(millis);
    }

    private void takeOff(){
        roll = 0.0f;
        pitch = 0.0f;
        yaw = 0.0f;
        mFlightController.startTakeoff(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                Log.d(TAG,"Take off done. Result: " + (djiError == null
                        ? "Success"
                        : djiError.getDescription()));
                canSendData = true;

                startSendingData();

                speech("Starting!");
            }
        });
    }

    private void land(){
        speech("Starting landing.");
        canSendData = false;
        mFlightController.startLanding(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                Log.d(TAG,"Landing done. Result: " + (djiError == null
                        ? "Success"
                        : djiError.getDescription()));
                speech("Starting!");
            }
        });
    }

    private void initPreviewer() {
        BaseProduct product = FPVApplication.getProductInstance();

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
        Camera camera = FPVApplication.getCameraInstance();
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
                mCodecManager = new DJICodecManager(FPVActivity.this, surface, width, height);
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
                Toast.makeText(FPVActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void switchCameraMode(SettingsDefinitions.CameraMode cameraMode){

        Camera camera = FPVApplication.getCameraInstance();
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

        final Camera camera = FPVApplication.getCameraInstance();
        if (camera != null) {

            SettingsDefinitions.ShootPhotoMode photoMode = SettingsDefinitions.ShootPhotoMode.SINGLE; // Set the camera capture mode as Single mode
            camera.setShootPhotoMode(photoMode, new CommonCallbacks.CompletionCallback(){
                    @Override
                    public void onResult(DJIError djiError) {
                        if (null == djiError) {
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    camera.startShootPhoto(new CommonCallbacks.CompletionCallback() {
                                        @Override
                                        public void onResult(DJIError djiError) {
                                            if (djiError == null) {
                                                showToast("take photo: success");
                                                final MediaPlayer mp = MediaPlayer.create(getApplicationContext(), R.raw.camera_shutter);
                                                mp.start();
                                            } else {
                                                showToast(djiError.getDescription());
                                            }
                                        }
                                    });
                                }
                            }, 1000);
                        }
                    }
            });
        }
    }

    // Method for start recording
    private void startRecord(){

        final Camera camera = FPVApplication.getCameraInstance();
        if (camera != null) {
            camera.startRecordVideo(new CommonCallbacks.CompletionCallback(){
                @Override
                public void onResult(DJIError djiError)
                {
                    if (djiError == null) {
                        speech("Video started recording");
                        showToast("Record video: success");
                    }else {
                        speech("Video could not start recording");
                        showToast(djiError.getDescription());
                    }
                }
            });
        }
    }

    // Method for stop recording
    private void stopRecord(){

        Camera camera = FPVApplication.getCameraInstance();
        if (camera != null) {
            camera.stopRecordVideo(new CommonCallbacks.CompletionCallback(){

                @Override
                public void onResult(DJIError djiError)
                {
                    if(djiError == null) {
                        speech("Video stopped recording");
                        showToast("Stop recording: success");
                    }else {
                        speech("Video could not stop recording. Try again.");
                        showToast(djiError.getDescription());
                    }
                }
            });
        }

    }

    private void initFlightController() {
        mFlightController = ModuleVerificationUtil.getFlightController();
        if (mFlightController == null) {
            speech("Flight controller could not be initialized");
            vibrate(1000);
            Log.e(TAG,"Flight controller could not be initialized");
            return;
        }

        mFlightController.getFlightAssistant().setCollisionAvoidanceEnabled(true,null);
        mFlightController.getFlightAssistant().setActiveObstacleAvoidanceEnabled(true,null);
        mFlightController.setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY);
        mFlightController.setYawControlMode(YawControlMode.ANGLE);
        mFlightController.setRollPitchControlMode(RollPitchControlMode.VELOCITY);

        // Go home on fail connection or very low battery
        mFlightController.setConnectionFailSafeBehavior(ConnectionFailSafeBehavior.GO_HOME, null);
        mFlightController.setSmartReturnToHomeEnabled(true,null);
        mFlightController.confirmSmartReturnToHomeRequest(true,null);

        Gimbal droneGimbal = DJISDKManager.getInstance().getProduct().getGimbal();
        Rotation.Builder builder = new Rotation.Builder().mode(RotationMode.ABSOLUTE_ANGLE).time(2);
        Log.d(TAG,"Gimbal: " + gimbal);
        Number value = ((DJIParamMinMaxCapability) (droneGimbal.getCapabilities().get(CapabilityKey.ADJUST_PITCH))).getMin();
        value = value.doubleValue()*0.1;
        Log.d(TAG,"Gimbal drone: " + value.floatValue());
        builder.pitch(gimbal);
        droneGimbal.rotate(builder.build(),null);

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

    private float[] mAccelerometerReading = new float[3];
    private float[] mMagnetometerReading = new float[3];
    private float[] mRotationMatrix = new float[9];
    private float[] mOrientationAngles = new float[3];

    private void initPhoneSensors(){
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if(mSensorManager == null){
            speech("Sensor manager could not be initialized");
            vibrate(1000);
            Log.e(TAG,"Sensor manager could not be initialized");
            return;
        }
        Sensor accelerometerSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor magnetometerSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        mSensorManager.registerListener(this,
                accelerometerSensor,
                SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this,
                magnetometerSensor,
                SensorManager.SENSOR_DELAY_NORMAL);

        mOrientationAngles = new float[3];
        updateOrientationAngles();
    }

    private void uninitPhoneSensors(){
        if(mSensorManager != null) mSensorManager.unregisterListener(this);
    }

    private float lastMobileOrientation = 0.0f;

    // Update phone orientation angles
    private void updateOrientationAngles(){
        // Rotation matrix based on current readings from accelerometer and magnetometer.
        SensorManager.getRotationMatrix(mRotationMatrix, null,
                mAccelerometerReading, mMagnetometerReading);
        // Express the updated rotation matrix as three orientation angles.
        SensorManager.getOrientation(mRotationMatrix, mOrientationAngles);
        convertToDegrees(mOrientationAngles);
    }


    float rollJoyControlMaxSpeed = 2.0f;
    float pitchJoyControlMaxSpeed = 2.0f;

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(sensorEvent.values, 0, mAccelerometerReading,
                    0, mAccelerometerReading.length);
        } else if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(sensorEvent.values, 0, mMagnetometerReading,
                    0, mMagnetometerReading.length);
        }

        // Update phone orientation angles
        updateOrientationAngles();

        if(mOrientationAngles[0] != 0.0f && mFlightController != null) {

            // YAW
            // Calculate the phone orientation difference from last iteration and sum it to the yaw
            float mobileOrientation = mOrientationAngles[0];
            float droneOrientation = mFlightController.getCompass().getHeading();

            if(yaw == 0.0f){
                // First iteration, set yaw as drone orientation
                yaw = droneOrientation;
            } else{
                // All other iterations, sum the phone orientation difference to the yaw
                yaw -= lastMobileOrientation - mobileOrientation;
            }
            // Save last phone orientation in order to calculate the increase in the next iteration
            lastMobileOrientation = mobileOrientation;

            // Fix the yaw degrees in case is over 180 or below -180. Example: -200 is converted to 160
            if (yaw > 180) yaw = -360 + yaw;
            if (yaw < -180) yaw = 360 + yaw;

            // ROLL
            // Phone roll goes from 0 to -180. 0 maximum forward. -180 maximum backward.
            // For us, -45 and -135 would be the maximum degrees for maximum speed (forward and backward)
            // Then, normalized values goes from -1 to 1 for max forward/backward
            float mobileRoll = mOrientationAngles[2];
            if (mobileRoll > -45){
                mobileRoll = -45.0f;
            } else if (mobileRoll < -135){
                mobileRoll = -135.0f;
            }
            float normalizedRoll = -(((Math.abs(mobileRoll)-45)/45)-1);

            if(Math.abs(normalizedRoll) < 0.2){
                normalizedRoll = 0;
            }

            // Convert it to the maximum speed
            roll = (rollJoyControlMaxSpeed * normalizedRoll);

            // PITCH
            // Phone pitch goes from 90 to -90. 90 maximum left. -90 maximum right.
            // Then, normalized values goes from -1 to 1 for max left/right
            float mobilePitch = mOrientationAngles[1];
            if (mobilePitch < -45){
                mobilePitch = -45.0f;
            } else if (mobilePitch > 45){
                mobilePitch = 45.0f;
            }
            float normalizedPitch = -(mobilePitch/45);

            if(Math.abs(normalizedPitch) < 0.2){
                normalizedPitch = 0;
            }

            // Convert it to the maximum speed
            pitch = (pitchJoyControlMaxSpeed * normalizedPitch);
        }

        FPVActivity.this.runOnUiThread(new Runnable() {
            public void run() {
                String str = "Pitch: " + roll + ", Roll: " + pitch + ", Yaw: " + yaw;
                tvDebug.setText(str);
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
            Log.e(TAG, "Flight controls are not available");
            speech("Flight controls are not available");
            vibrate(1000);
        }
    }

    private void initRecognition(){
        Log.d(TAG,"initRecognition");
        AudioManager audioManager=(AudioManager)getSystemService(Context.AUDIO_SERVICE);
        if(audioManager == null){
            Log.e(TAG,"AudioManager could not be initialized");
            speech("AudioManager could not be initialized");
            vibrate(1000);
            return;
        }
        audioManager.setStreamMute(AudioManager.STREAM_MUSIC, true);

        droidSpeech = new DroidSpeech(this, null);
        droidSpeech.setOnDroidSpeechListener(new OnDSListener() {
            @Override
            public void onDroidSpeechSupportedLanguages(String currentSpeechLanguage, List<String> supportedSpeechLanguages) {
//                if(supportedSpeechLanguages.contains("en-US")) {
//                    droidSpeech.setPreferredLanguage("en-US");
//                }
            }

            @Override
            public void onDroidSpeechRmsChanged(float rmsChangedValue) {}

            @Override
            public void onDroidSpeechLiveResult(String liveSpeechResult) {}

            @Override
            public void onDroidSpeechFinalResult(String finalSpeechResult) {
                Log.d(TAG,"onDroidSpeechFinalResult: " + finalSpeechResult);
                if(mFlightController != null) {
                    if (finalSpeechResult.contains("take off") || finalSpeechResult.contains("takeoff") || finalSpeechResult.contains("take of")) {
                        takeOff();
                    } else if (finalSpeechResult.contains("land")) {
                        land();
                    } else if (finalSpeechResult.contains("up")) {
                        throttle += 0.2f;
                    } else if (finalSpeechResult.contains("down")) {
                        throttle -= 0.2f;
                    } else if (finalSpeechResult.contains("photo")) {
                        captureAction();
                    }else if (finalSpeechResult.contains("start recording")) {
                        startRecord();
                    } else if (finalSpeechResult.contains("stop recording")) {
                        stopRecord();
                    } else if (finalSpeechResult.contains("repeat commands")) {
                        startingSpeech();
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

    private void startingSpeech(){
        speech("Welcome to the drone view. Hope you enjoy the flight! hahahaaa ");
    }

    private void uninitRecognition(){
        if( droidSpeech != null) droidSpeech.closeDroidSpeechOperations();
        droidSpeech = null;
    }

    private void initTextToSpeech(){
        tts = new TextToSpeech(this, this);
    }

    @Override
    public void onInit(int status) {
        if(status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(Locale.US);
            if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Lanuage data is missing or the language is not supported.
                Log.e(TAG, "Language is not available.");
            } else {
                // The TTS engine has been successfully initialized.
                Log.d(TAG,"TextToSpeech initialization succes.");
                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onDone(String utteranceId) {
                        if(droidSpeech != null) droidSpeech.startDroidSpeechRecognition();
                    }

                    @Override
                    public void onError(String utteranceId) {
                        if(droidSpeech != null) droidSpeech.startDroidSpeechRecognition();
                    }

                    @Override
                    public void onStart(String utteranceId) {
                        if(droidSpeech != null) droidSpeech.closeDroidSpeechOperations();
                    }
                });
                startingSpeech();
            }
        }else{
            Log.e(TAG,"TextToSpeech could not be initialized. Status " + status);
        }
    }



    private void uninitTextToSpeech(){
        if(tts != null){
            tts.stop();
            tts.shutdown();
        }
        tts = null;
    }

    private void speech(String stringToSpeak){
        if(tts == null){
            Log.e(TAG,"TextToSpeech not initialized");
            return;
        }
        tts.speak(stringToSpeak, TextToSpeech.QUEUE_FLUSH, null);
    }
}
