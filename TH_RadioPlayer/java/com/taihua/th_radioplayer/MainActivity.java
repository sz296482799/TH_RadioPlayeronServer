package com.taihua.th_radioplayer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.app.Activity;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent it = new Intent(this, MainService.class);
        startService(it);
    }

}
