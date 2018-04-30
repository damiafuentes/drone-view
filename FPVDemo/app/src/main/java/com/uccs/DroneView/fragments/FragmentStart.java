package com.uccs.DroneView.fragments;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.uccs.DroneView.R;
import com.uccs.DroneView.StartActivity;


public class FragmentStart extends Fragment {

    public TextView tvStart;
    public TextView tvTutorial;
    public ImageView ivSettings;

    public StartActivity activity;

    public FragmentStart() {
        // Required empty public constructor
    }

    public static FragmentStart newInstance() {
        return new FragmentStart();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_start, container, false);
        tvStart = view.findViewById(R.id.tvStart);
        tvTutorial = view.findViewById(R.id.tvTutorial);
        ivSettings = view.findViewById(R.id.ivSettings);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        activity = (StartActivity) getActivity();

        tvStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.commitFragment(FragmentConnection.newInstance(),"FragmentConnection",true);
            }
        });

        tvTutorial.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.commitFragment(FragmentTutorial.newInstance(),"FragmentTutorial",true);
            }
        });

        ivSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.commitFragment(FragmentSettings.newInstance(),"FragmentSettings",true);
            }
        });
    }
}
