package com.uccs.DroneView.fragments;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.uccs.DroneView.R;
import com.uccs.DroneView.StartActivity;


public class FragmentVoiceCommands extends Fragment {

    public TextView tvGotIt;

    public StartActivity activity;

    public FragmentVoiceCommands() {
        // Required empty public constructor
    }

    public static FragmentVoiceCommands newInstance() {
        return new FragmentVoiceCommands();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_voice_commands, container, false);
        tvGotIt = view.findViewById(R.id.tvGotIt);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        activity = (StartActivity) getActivity();

        tvGotIt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.getSupportFragmentManager().popBackStack();
            }
        });
    }
}
