package com.ctwings.myapplication;

import android.app.ActivityOptions;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import static com.ctwings.myapplication.MainActivity.getApplicationVersionString;

public class aboutUS extends AppCompatActivity {
    private TextView textViewVersion;
    private Button button_contact_us;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_us);
        textViewVersion = (TextView) findViewById(R.id.TextView_version);
        button_contact_us = (Button) findViewById(R.id.button_contact_us);
        textViewVersion.setText("Versión: " + getApplicationVersionString(this));
        button_contact_us.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), email_fill.class);
                ActivityOptions options =
                        ActivityOptions.makeCustomAnimation(v.getContext(), R.anim.slide_from_right, R.anim.exit_to_left);
                v.getContext().startActivity(intent, options.toBundle());
            }
        });

    }
}
