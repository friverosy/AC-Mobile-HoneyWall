package com.ctwings.myapplication;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class log_show extends AppCompatActivity {
    private static final String LOG_NAME = "MultiExport.log";
    private String logType;
    private TextView showlog;
    private StringBuilder text = new StringBuilder();
    private Button clearLog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_app);
        showlog = (TextView) findViewById(R.id.fill_log);
        clearLog=(Button) findViewById(R.id.button_clear_log);
        clearLog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearLog();
            }
        });
        if (readLog() != null && !readLog().toString().equals("")) {
            showlog.setText(readLog());
            //showlog.setMovementMethod(new ScrollingMovementMethod());
        }

    }

    private StringBuilder readLog() {
        StringBuilder stringBuilder = null;
        try {
            InputStream inputStream = openFileInput(LOG_NAME);

            if (inputStream != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                stringBuilder = new StringBuilder();
                while ((receiveString = bufferedReader.readLine()) != null) {
                    stringBuilder.append(receiveString);
                    stringBuilder.append("\n\r");
                    stringBuilder.append("-----------------------------------------------------------------------");
                    //stringBuilder.append(System.getProperty("line.separator"));

                }
                inputStream.close();
                //content = stringBuilder.toString();
            }

        } catch (FileNotFoundException e) {
            Log.e("file state", "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e("file state", "Can not read file: " + e.toString());
        }
        return stringBuilder;
    }

    private void clearLog() {
        File log = new File(getFilesDir() + File.separator + LOG_NAME);
        try {
            log.delete();
            if (!log.exists()) {
                log.createNewFile();
                readLog();
                Toast.makeText(this, "Log Limpio", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
