package com.lxbnjupt.pluginactivitydemo.activity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.lxbnjupt.pluginactivitydemo.R;

public class MainActivity extends AppCompatActivity {

    private Button btnStartPluginActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnStartPluginActivity = (Button) findViewById(R.id.tv_start_plugin_activity);
        btnStartPluginActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, PluginActivity.class);
                startActivity(intent);
            }
        });
    }
}
