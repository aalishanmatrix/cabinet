package com.afollestad.cabinet;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.os.Environment;
import com.afollestad.cabinet.fragments.DirectoryFragment;

public class MainActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        navigate(new File(Environment.getExternalStorageDirectory().getAbsolutePath()));
    }

    public void navigate(File directory) {
        FragmentTransaction trans = getFragmentManager().beginTransaction();
        trans.replace(R.id.content_frame, new DirectoryFragment(directory));
        trans.addToBackStack(null);
        trans.commit();
    }
}