package com.dji.FPVDemo;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FragmentTutorial#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FragmentTutorial extends Fragment {

    public TextView tvGotIt;
    public TextView tvWatchVideo;

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
        tvWatchVideo = view.findViewById(R.id.tvVideo);
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
    }
}
