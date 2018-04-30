package com.dji.FPVDemo.fragments;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.dji.FPVDemo.R;
import com.dji.FPVDemo.StartActivity;


public class FragmentSettings extends Fragment {

    public TextView tvGoBack;

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
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        activity = (StartActivity) getActivity();

        tvGoBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.getSupportFragmentManager().popBackStack();
            }
        });

    }
}
