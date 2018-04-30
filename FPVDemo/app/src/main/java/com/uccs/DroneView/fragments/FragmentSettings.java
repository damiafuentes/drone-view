package com.uccs.DroneView.fragments;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.uccs.DroneView.R;
import com.uccs.DroneView.StartActivity;

import static android.content.Context.MODE_PRIVATE;


public class FragmentSettings extends Fragment {

    public static String TAG_SHARED_PREFS = "TAG_SHARED_PREFS";
    public static String TAG_THROTTLE = "TAG_THROTTLE";
    public static String TAG_GIMBAL = "TAG_GIMBAL";

    public TextView tvGoBack;

    private RadioGroup rgGimbal;
    private RadioButton rbGimbalStraigth;
    private RadioButton rbGimbal45;
    private RadioButton rbGimbalDown;

    private RadioGroup rgThrottle;
    private RadioButton rbThrottle1;
    private RadioButton rbThrottle5;
    private RadioButton rbThrottle10;

    public StartActivity activity;

    public FragmentSettings() {
        // Required empty public constructor
    }

    public static FragmentSettings newInstance() {
        return new FragmentSettings();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        tvGoBack = view.findViewById(R.id.tvGoBack);
        rbThrottle1 = view.findViewById(R.id.rbThrottle1);
        rbThrottle5 = view.findViewById(R.id.rbThrottle5);
        rbThrottle10 = view.findViewById(R.id.rbThrottle10);
        rbGimbalStraigth = view.findViewById(R.id.rbGimbalStraigth);
        rbGimbal45 = view.findViewById(R.id.rbGimbal45);
        rbGimbalDown = view.findViewById(R.id.rbGimbalDown);
        rgGimbal = view.findViewById(R.id.rgGimbal);
        rgThrottle = view.findViewById(R.id.rgThrottle);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        activity = (StartActivity) getActivity();

        SharedPreferences prefs = activity.getSharedPreferences(TAG_SHARED_PREFS, MODE_PRIVATE);
        int gimbal = prefs.getInt(TAG_GIMBAL, 45);
        int throttle = prefs.getInt(TAG_THROTTLE, 1);
        switch (throttle){
            case 1:
                rbThrottle1.setChecked(true);
                rbThrottle5.setChecked(false);
                rbThrottle10.setChecked(false);
                break;
            case 5:
                rbThrottle1.setChecked(false);
                rbThrottle5.setChecked(true);
                rbThrottle10.setChecked(false);
                break;
            case 10:
                rbThrottle1.setChecked(false);
                rbThrottle5.setChecked(false);
                rbThrottle10.setChecked(true);
                break;
        }

        switch (gimbal){
            case 1:
                rbGimbalStraigth.setChecked(true);
                rbGimbal45.setChecked(false);
                rbGimbalDown.setChecked(false);
                break;
            case 45:
                rbGimbalStraigth.setChecked(false);
                rbGimbal45.setChecked(true);
                rbGimbalDown.setChecked(false);
                break;
            case 85:
                rbGimbalStraigth.setChecked(false);
                rbGimbal45.setChecked(false);
                rbGimbalDown.setChecked(true);
                break;
        }
        tvGoBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.getSupportFragmentManager().popBackStack();
            }
        });

        rgGimbal.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                SharedPreferences.Editor editor = activity.getSharedPreferences(TAG_SHARED_PREFS, MODE_PRIVATE).edit();
                switch (checkedId){
                    case R.id.rbGimbalStraigth:
                        editor.putInt(TAG_GIMBAL, 1);
                        break;
                    case R.id.rbGimbal45:
                        editor.putInt(TAG_GIMBAL, 45);
                        break;
                    case R.id.rbGimbalDown:
                        editor.putInt(TAG_GIMBAL, 85);
                        break;
                }
                editor.apply();
            }
        });

        rgThrottle.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                SharedPreferences.Editor editor = activity.getSharedPreferences(TAG_SHARED_PREFS, MODE_PRIVATE).edit();
                switch (checkedId){
                    case R.id.rbThrottle1:
                        editor.putInt(TAG_THROTTLE, 1);
                        break;
                    case R.id.rbThrottle5:
                        editor.putInt(TAG_THROTTLE, 5);
                        break;
                    case R.id.rbThrottle10:
                        editor.putInt(TAG_THROTTLE, 10);
                        break;
                }
                editor.apply();
            }
        });

    }
}
