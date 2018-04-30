package com.dji.FPVDemo;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;

import com.dji.FPVDemo.fragments.FragmentStart;

public class StartActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        commitFragment(FragmentStart.newInstance(),"FragmentStart",false);
    }

    public void commitFragment(Fragment fragment, String fragmentTAG, Boolean addToBackStack){
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if(addToBackStack){
            ft.setCustomAnimations(
                    R.animator.slide_in_left,
                    R.animator.slide_out_right,
                    R.animator.slide_in_right,
                    R.animator.slide_out_left);
            ft.addToBackStack(fragmentTAG);
        }
        ft.replace(R.id.root, fragment, fragmentTAG);
        ft.commit();
    }
}
