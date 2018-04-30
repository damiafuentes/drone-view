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


public class FragmentTutorial extends Fragment {

    public TextView tvGotIt;
    public TextView tvWatchVideo;
    public TextView tvSeeVoiceCommands;

    public StartActivity activity;

    public FragmentTutorial() {
        // Required empty public constructor
    }

    public static FragmentTutorial newInstance() {
        return new FragmentTutorial();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_tutorial, container, false);
        tvGotIt = view.findViewById(R.id.tvGotIt);
        tvWatchVideo = view.findViewById(R.id.tvWatchVideo);
        tvSeeVoiceCommands = view.findViewById(R.id.tvVoiceCommands);
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

        tvWatchVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        tvSeeVoiceCommands.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.commitFragment(FragmentVoiceCommands.newInstance(),"FragmentVoiceCommands",true);
            }
        });
    }
}
