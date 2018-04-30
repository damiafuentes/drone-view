package com.dji.FPVDemo.fragments;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.dji.FPVDemo.FPVDemoApplication;
import com.dji.FPVDemo.MainActivity;
import com.dji.FPVDemo.R;
import com.dji.FPVDemo.StartActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.log.DJILog;
import dji.sdk.base.BaseProduct;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;

/**
 * Fragment that is used to establish the connection with the drone wifi.
 *
 * Created by Damià Fuentes and Eric Velazquez
 * April 6th 2018
 */
public class FragmentConnection extends Fragment implements View.OnClickListener{

    private static final String TAG = FragmentConnection.class.getName();

    public FragmentConnection(){
        // Required empty public constructor
    }

    public static FragmentConnection newInstance() {
        return new FragmentConnection();
    }

    private TextView tvConnectionStatus;
    private TextView tvGoBack;
    private TextView tvStart;
    public StartActivity activity;

    private static final String[] REQUIRED_PERMISSION_LIST = new String[]{
            Manifest.permission.VIBRATE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.RECORD_AUDIO,
    };
    private List<String> missingPermission = new ArrayList<>();
    private AtomicBoolean isRegistrationInProgress = new AtomicBoolean(false);
    private static final int REQUEST_PERMISSION_CODE = 12345;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_connection, container, false);
        tvConnectionStatus = (TextView) view.findViewById(R.id.text_connection_status);
        tvGoBack = (TextView) view.findViewById(R.id.tvGoBack);
        tvStart = (TextView) view.findViewById(R.id.tvFly);
        tvStart.setOnClickListener(this);
        tvGoBack.setOnClickListener(this);
        setTextViewBackground(tvStart, R.drawable.disabled_button);
        tvStart.setEnabled(false);
        return view;
    }

    private void setTextViewBackground(TextView tv, int id){
        int pL = tv.getPaddingLeft();
        int pT = tv.getPaddingTop();
        int pR = tv.getPaddingRight();
        int pB = tv.getPaddingBottom();

        tv.setBackgroundResource(id);
        tv.setPadding(pL, pT, pR, pB);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        activity = (StartActivity) getActivity();

        checkAndRequestPermissions();

        // Register the broadcast receiver for receiving the device connection's changes.
        IntentFilter filter = new IntentFilter();
        filter.addAction(FPVDemoApplication.FLAG_CONNECTION_CHANGE);
        activity.registerReceiver(mReceiver, filter);
    }

    /**
     * Checks if there is any missing permissions, and
     * requests runtime permission if needed.
     */
    private void checkAndRequestPermissions() {
        // Check for permissions
        for (String eachPermission : REQUIRED_PERMISSION_LIST) {
            if (ContextCompat.checkSelfPermission(activity, eachPermission) != PackageManager.PERMISSION_GRANTED) {
                missingPermission.add(eachPermission);
            }
        }
        // Request for missing permissions
        if (!missingPermission.isEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(activity,
                    missingPermission.toArray(new String[missingPermission.size()]),
                    REQUEST_PERMISSION_CODE);
        }

    }


    /**
     * Result of runtime permission request
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Check for granted permission and remove from missing list
        if (requestCode == REQUEST_PERMISSION_CODE) {
            for (int i = grantResults.length - 1; i >= 0; i--) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    missingPermission.remove(permissions[i]);
                }
            }
        }
        // If there is enough permission, we will start the registration
        if (missingPermission.isEmpty()) {
            startSDKRegistration();
        } else {
            showToast("Missing permissions!!!");
        }
    }

    private void startSDKRegistration() {
        if (isRegistrationInProgress.compareAndSet(false, true)) {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    showToast( "registering, pls wait...");
                    DJISDKManager.getInstance().registerApp(activity, new DJISDKManager.SDKManagerCallback() {
                        @Override
                        public void onRegister(DJIError djiError) {
                            if (djiError == DJISDKError.REGISTRATION_SUCCESS) {
                                DJILog.e("App registration", DJISDKError.REGISTRATION_SUCCESS.getDescription());
                                DJISDKManager.getInstance().startConnectionToProduct();
                                showToast("Register Success");
                            } else {
                                showToast( "Register sdk fails, check network is available");
                            }
                            Log.v(TAG, djiError.getDescription());
                        }

                        @Override
                        public void onProductChange(BaseProduct oldProduct, BaseProduct newProduct) {
                            Log.d(TAG, String.format("onProductChanged oldProduct:%s, newProduct:%s", oldProduct, newProduct));
                        }
                    });
                }
            });
        }
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
    }

    @Override
    public void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        if(activity != null) activity.unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    protected BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            refreshSDKRelativeUI();
        }
    };

    private void refreshSDKRelativeUI() {
        BaseProduct mProduct = FPVDemoApplication.getProductInstance();

        if (null != mProduct && mProduct.isConnected()) {

            Log.v(TAG, "refreshSDK: True");
            setTextViewBackground(tvStart, R.drawable.accent_button);
            tvStart.setEnabled(true);

            String str = mProduct instanceof Aircraft ? "DJIAircraft" : "DJIHandHeld";
            tvConnectionStatus.setText("Status: " + str + " connected");
        } else {

            Log.v(TAG, "refreshSDK: False");
            setTextViewBackground(tvStart, R.drawable.disabled_button);
            tvStart.setEnabled(false);

            tvConnectionStatus.setText(R.string.connection_loose);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.tvFly: {
                Intent intent = new Intent(activity, MainActivity.class);
                startActivity(intent);
                break;
            }
            case R.id.tvGoBack:{
                if(activity != null) activity.getSupportFragmentManager().popBackStack();
                break;
            }
            default:
                break;
        }
    }

    private void showToast(final String toastMsg) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(activity, toastMsg, Toast.LENGTH_LONG).show();

            }
        });
    }
}
