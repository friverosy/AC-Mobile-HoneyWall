package com.ctwings.myapplication;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class log_app {
    private static final String LOG_NAME = "Multiempresa.log";
    private String logType;
    private String content;

    public log_app(String logType, String content) {
        this.logType = logType;
        this.content = content;
    }

    //empty constructor
    public log_app() {
    }

    public static String getLogName() {
        return LOG_NAME;
    }

    public String getLogType() {
        return logType;
    }

    public void setLogType(String logType) {
        this.logType = logType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void writeLog(Context context,String Location, String LogType, String content) {
        File log = new File(context.getFilesDir() + File.separator + LOG_NAME);
        if (!log.isFile()) {
            try {
                log.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        String message = "[" + Location + "]" + "["+getCurrentDateTime()+"]" + " [" + LogType + "]" + ": " + content + "\n";
        FileOutputStream outputStream = null;
        try {
            outputStream = context.openFileOutput(LOG_NAME, Context.MODE_APPEND);
            outputStream.write(message.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            try {
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String getCurrentDateTime() {
        Calendar cal = Calendar.getInstance();
        Date currentLocalTime = cal.getTime();
        DateFormat date = new SimpleDateFormat("yyy-MM-dd HH:mm:ss");
        String localTime = date.format(currentLocalTime);
        return localTime;
    }

    private String readLog(Context context) {

        String content = "";

        try {
            InputStream inputStream = context.openFileInput(LOG_NAME);

            if (inputStream != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();
                while ((receiveString = bufferedReader.readLine()) != null) {
                    stringBuilder.append(receiveString);
                }
                inputStream.close();
                content = stringBuilder.toString();
            }
        } catch (FileNotFoundException e) {
            Log.e("login activity", "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e("login activity", "Can not read file: " + e.toString());
        }

        return content;
    }
}
