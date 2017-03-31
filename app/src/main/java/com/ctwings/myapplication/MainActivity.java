package com.ctwings.myapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.device.ScanManager;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Vibrator;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;
import com.pnikosis.materialishprogress.ProgressWheel;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.SyncBasicHttpContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private final int delayPeople = 36000 ; // 4 Min. 240000; 600000 10 min
    private final int delayRecords = 2400; // 4 Min. 240000; 480000 8 min
    private static String server = "http://axxezocloud.brazilsouth.cloudapp.azure.com:5001"; // Integration server
    //private static String server = "http://axxezo-test.brazilsouth.cloudapp.azure.com:9000"; // Test server
    private String idCompany = "";
    private String idSector = "";
    private String token = "";
    private int pdaNumber;
    private static String version = "f2fadba";
    private getPeopleTask getPeopleInstance;
    private postRecordsTask postRecordsInstance;

    private ImageView imageview;
    private EditText editTextRun;
    private EditText editTextFullName;
    private TextView textViewVersion;
    private String name = "";
    private EditText editTextCompany;
    private TextView textViewProfile;
    private ProgressWheel loading;
    private boolean is_input;

    private final static String SCAN_ACTION = "urovo.rcv.message";//扫描结束action
    private Vibrator mVibrator;
    private ScanManager mScanManager;
    private SoundPool soundpool = null;
    private int soundid;
    private String barcodeStr;
    private String barcodeCache;
    private boolean isScaning = false;
    private Switch mySwitch;
    MediaPlayer mp3Dennied;
    MediaPlayer mp3Permitted;
    MediaPlayer mp3Error;
    DatabaseHelper db = new DatabaseHelper(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //remove it
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "something", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                mVibrator.vibrate(100);
            }
        });

        // Get initial setup
        new getSetupTask().execute();

        // Insert Test data
        //testRecords(200);
        
        //create the log file
        File log = new File(this.getFilesDir() + File.separator + "AccessControl.log");
        if (!log.isFile()) {
            try {
                log.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        //call the loading library in xml file
        loading = (ProgressWheel) findViewById(R.id.loading);
        loading.setVisibility(View.GONE);
        
        // Start Asynctask loop to check every delayPeople time, if need update people.
        updatePeople();
        // Asynctask to start sending records to each delayRecords time to API.
        updateRecords();

        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        editTextRun = (EditText) findViewById(R.id.editText_run);
        editTextFullName = (EditText) findViewById(R.id.editText_fullname);
        editTextCompany = (EditText) findViewById(R.id.editText_company);
        textViewProfile = (TextView) findViewById(R.id.textView_profile);
        imageview = (ImageView) findViewById(R.id.imageView);
        mp3Dennied = MediaPlayer.create(MainActivity.this, R.raw.bad);
        mp3Permitted = MediaPlayer.create(MainActivity.this, R.raw.good);
        mp3Error = MediaPlayer.create(MainActivity.this, R.raw.error);
        editTextCompany.setVisibility(View.GONE);
        mySwitch = (Switch) findViewById(R.id.mySwitch);
        mySwitch.setChecked(true);
        textViewVersion = (TextView) findViewById(R.id.textView_version);
        textViewVersion.setText("Versión: " + version);

        // set by default
        is_input = true;

        mySwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    is_input = true;
                } else {
                    is_input = false;
                }
            }
        });

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (editTextRun.getText().toString().isEmpty()) {
                    editTextRun.setHint("Ingrese Rut");
                    editTextRun.setHintTextColor(Color.RED);
                    editTextRun.requestFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(editTextRun, InputMethodManager.SHOW_IMPLICIT);
                } else getPerson(editTextRun.getText().toString());
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_refresh) {
            reset();
            return true;
        } else if (id == R.id.action_setting) {
            Intent i = new Intent(this, Setting.class);
            startActivity(i);
        }

        return super.onOptionsItemSelected(item);
    }

    private BroadcastReceiver mScanReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            log_app log = new log_app();

            // TODO Auto-generated method stub
            try {
                new loadSound(4).execute();

                isScaning = false;
                //soundpool.play(soundid, 1, 1, 0, 0, 1);

                mVibrator.vibrate(100);
                cleanEditText();

                byte[] barcode = intent.getByteArrayExtra("barocode");
                int barocodelen = intent.getIntExtra("length", 0);
                byte barcodeType = intent.getByteExtra("barcodeType", (byte) 0);
                barcodeStr = new String(barcode, 0, barocodelen);
                String rawCode = barcodeStr;

                int flag = 0; // 0 for end without k, 1 with k
                int flagSetUp = 0; // 0 for no config QR code.

                if (barcodeType == 28) { // QR code
                    if (barcodeStr.startsWith("CONFIG-AXX-")) {
                        flagSetUp = 1;
                        SetUp(barcodeStr);
                    } else {
                        // get only rut
                        barcodeStr = barcodeStr.substring(
                                barcodeStr.indexOf("RUN=") + 4,
                                barcodeStr.indexOf("&type"));
                        // remove dv.
                        barcodeStr = barcodeStr.substring(0, barcodeStr.indexOf("-"));
                    }
                } else if (barcodeType == 1 || barcodeStr.startsWith("00")) {
                    //Log.i("Debugger", "CARD");
                } else if (barcodeType == 17) { // PDF417
                    String rutValidator = barcodeStr.substring(0, 8);
                    rutValidator = rutValidator.replace(" ", "");
                    rutValidator = rutValidator.endsWith("K") ? rutValidator.replace("K", "0") : rutValidator;
                    char dv = barcodeStr.substring(8, 9).charAt(0);
                    boolean isvalid = ValidarRut(Integer.parseInt(rutValidator), dv);
                    if (isvalid)
                        barcodeStr = rutValidator;
                    else { //try validate rut size below 10.000.000
                        rutValidator = barcodeStr.substring(0, 7);
                        rutValidator = rutValidator.replace(" ", "");
                        rutValidator = rutValidator.endsWith("K") ? rutValidator.replace("K", "0") : rutValidator;
                        dv = barcodeStr.substring(7, 8).charAt(0);
                        isvalid = ValidarRut(Integer.parseInt(rutValidator), dv);
                        if (isvalid)
                            barcodeStr = rutValidator;
                        else
                            log.writeLog(getApplicationContext(), "Main:line 262", "ERROR", "rut invalido " + barcodeStr);
                    }
                    name = "";
                }

                if (flagSetUp == 0)
                    getPerson(barcodeStr);
                barcodeCache = barcodeStr; // Used to avoid 2 records in a row.
            } catch (NullPointerException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    private void SetUp(String barcodeStr) {
        switch (barcodeStr) {
            case "CONFIG-AXX-637B55B8AA55C7C7D3810E0CE05B1E80":
                // Offline record Syncronize
                if (db.record_desync_count() > 0) {
                    postRecords();
                    makeToast("Sincronizados!");
                } else
                    makeToast("No hay registros offline para sincronizar");
                break;
            case "CONFIG-AXX-F5CCAFFD2C2225A7CE0FBEC87993F6EF":
                // Offline record counter
                makeToast(String.valueOf(db.record_desync_count()) + " Registros aun no sincronizados");
                break;
            case "CONFIG-AXX-75687092BFAE94A0CBF81572E2C8C015":
                // People counter
                makeToast(String.valueOf(db.people_count()) + " Personas");
                break;
            case "CONFIG-AXX-C78768F72CBE1C08A4AFD98285FE0C7D":
                // Employee counter
                makeToast(String.valueOf(db.employees_count()) + " Empleados");
                break;
            case "CONFIG-AXX-B71580A4F60179BC005D359A8344FA63":
                // Contractors counter
                makeToast(String.valueOf(db.contractors_count()) + " Contratistas");
                break;
            case "CONFIG-AXX-4B6DA20544C994DAE45088C4A80C25F4":
                // Visits counter
                makeToast(String.valueOf(db.visits_count()) + " Visitas");
                break;
            case "CONFIG-AXX-CD0A4191D9CC5214650E32E13EFBD086":
                // Drop people table
                db.clean_people();
                makeToast("Tabla personas vaciada.");
            case "CONFIG-AXX-A11C9984001C27A12CC09A3C53B39ADF":
                // Drop record table
                db.clean_records();
                makeToast("Tabla records vaciada.");
                break;
            case "CONFIG-AXX-6rVLydzn651RsZZ3dqWk":
                // Call LOG
                Intent intent = new Intent(this, log_show.class);
                startActivity(intent);
                break;
            default:
                makeToast("Código de configuración incorrecto!");
                break;
        }
    }

    public boolean ValidarRut(int rut, char dv) {
        dv = dv == 'k' ? dv = 'K' : dv;
        int m = 0, s = 1;
        for (; rut != 0; rut /= 10) {
            s = (s + rut % 10 * (9 - m++ % 6)) % 11;
        }
        return dv == (char) (s != 0 ? s + 47 : 75);
    }

    private void initScan() {
        // TODO Auto-generated method stub
        mScanManager = new ScanManager();
        mScanManager.openScanner();
        mScanManager.switchOutputMode(0);
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        if (mScanManager != null) {
            mScanManager.stopDecode();
            isScaning = false;
        }
        unregisterReceiver(mScanReceiver);
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        initScan();
        //UpdateDb();
        IntentFilter filter = new IntentFilter();
        filter.addAction(SCAN_ACTION);
        registerReceiver(mScanReceiver, filter);
    }

    public void reset() {
        initScan();
        //cleanEditText();
        barcodeStr = "";
        IntentFilter filter = new IntentFilter();
        filter.addAction(SCAN_ACTION);
        registerReceiver(mScanReceiver, filter);
    }

    public void cleanEditText(){
        editTextRun.setText("");
        editTextFullName.setText("");
        editTextCompany.setText("");
        textViewProfile.setText("");
        imageview.setImageDrawable(null);
        name = null;
    }

    public String getCurrentDateTime() {
        Calendar cal = Calendar.getInstance();
        Date currentLocalTime = cal.getTime();
        DateFormat date = new SimpleDateFormat("yyy-MM-dd HH:mm:ss.S");
        String localTime = date.format(currentLocalTime);
        return localTime;
    }

    public void updateRecords() {
        final DatabaseHelper db = DatabaseHelper.getInstance(this);
        Timer timer = new Timer();
        final Handler handler = new Handler();
        final log_app log = new log_app();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        try {
                            // First call, postRecordsInstance will be null, so instantiate it.
                            if (postRecordsInstance == null) {
                                postRecords();
                            } else if (db.record_desync_count() > 0 && 
                                    postRecordsInstance.getStatus() != AsyncTask.Status.RUNNING){
                                // If it is already instantiated
                                postRecords();
                            }
                        } catch (Exception e) {
                            log.writeLog(getApplicationContext(), "Main:line 419", "ERROR", e.getMessage());
                        }
                    }
                });
            }
        };
        timer.schedule(task, 0, delayRecords);
    }

    public void updatePeople() {
        Timer timer = new Timer();
        final Handler handler = new Handler();
        final log_app log = new log_app();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        try {
                            getPeopleInstance = new getPeopleTask();
                            getPeopleInstance.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                        } catch (Exception e) {
                            log.writeLog(getApplicationContext(), "Main:line 397", "ERROR", e.getMessage());
                        }
                    }
                });
            }
        };
        timer.schedule(task, 0, delayPeople);
    }

    public void getPerson(String rut) {
        log_app log = new log_app();
        DatabaseHelper db = DatabaseHelper.getInstance(this);
        String personJson = db.get_one_person(rut);
        editTextCompany.setVisibility(View.GONE);
        String[] arr = personJson.split(";");
        try {
            // set editText here before any exceptions.
            editTextRun.setText(arr[2]);
            //build object with that values, then send to registerTarsk()
            Record record = new Record();
            record.setPerson_run(rut);

            if (arr[3].equals("true")) {
                mp3Permitted.start();
                //is_permitted = true;
                record.setPerson_is_permitted(1);
                if (is_input)
                    imageview.setImageResource(R.drawable.permitted);
            } else {
                mp3Dennied.start();
                // if has card number define as denied and as employee
                //is_permitted = false;
                record.setPerson_is_permitted(0);
                if (is_input)
                    imageview.setImageResource(R.drawable.dennied);
            }

            switch (arr[8]) {
                case "staff":
                    editTextFullName.setText(arr[0]);
                    record.setPerson_fullname(arr[0]);
                    textViewProfile.setText("Empleado");
                    break;
                case "contractor":
                    editTextFullName.setText(arr[0]);
                    record.setPerson_fullname(arr[0]);
                    textViewProfile.setText("Subcontratista");
                    editTextCompany.setText(arr[4]);
                    editTextCompany.setVisibility(View.VISIBLE);
                    break;
                case "visitor":
                    textViewProfile.setText("Visita");
                    // Show denied image, but internally setup record as permitted.
                    record.setPerson_is_permitted(1);
                    // If could get the name of pdf417 show it.

                    try {
                        if (!arr[0].isEmpty()) {
                            editTextFullName.setText(arr[0]);
                            record.setPerson_fullname(arr[0]);
                        } else {
                            editTextFullName.setText(name);
                            record.setPerson_fullname(name);
                        }
                    } catch (NullPointerException npe) {
                        editTextFullName.setText("");
                        record.setPerson_fullname("");
                        log.writeLog(getApplicationContext(), "Main:line 504", "ERROR", npe.getMessage());
                    }

                    // If have company show it.
                    if (!arr[4].isEmpty()) {
                        editTextCompany.setText(arr[4]);
                        editTextCompany.setVisibility(View.VISIBLE);
                    } else {
                        editTextCompany.setVisibility(View.GONE);
                    }
                    break;
            }

            record.setPerson_mongo_id(arr[1]);
            record.setPerson_profile(arr[8]);
            record.setPerson_company(arr[4]);
            record.setPerson_place(arr[5]);
            if (arr[7].equals("null")) arr[7] = "0"; // Card -> For Contractors it 0.
            record.setPerson_company_code(arr[6]);
            record.setPerson_card(Integer.parseInt(arr[7]));
            record.setRecord_sync(0);
            record.setRecord_bus(0);

            if (is_input) {
                record.setRecord_is_input(1);
                record.setRecord_input_datetime(getCurrentDateTime());
            } else {
                record.setRecord_is_input(0);
                record.setRecord_output_datetime(getCurrentDateTime());
            }

            // Save record on local database
            db.add_record(record);
        } catch (ArrayIndexOutOfBoundsException aiobe) {
            new loadSound(1).execute(); // Error sound.
            log.writeLog(getApplicationContext(), "Main:line 538", "ERROR", aiobe.getMessage());
        } catch (Exception e) {
            new loadSound(1).execute(); // Error sound.
            log.writeLog(getApplicationContext(), "Main:line 542", "ERROR", e.getMessage());
        }
    }

    private class loadSound extends AsyncTask<Void, Void, Void> {
        private int typeSound = -1;

        /*  Asyntask to play sounds in background
         *  1 Error
         *  2 Permitted
         *  3 Denied
         *  4 stop all
         */
        private loadSound(int typeSound) {
            this.typeSound = typeSound;
        }

        @Override
        protected Void doInBackground(Void... params) {
            switch (typeSound) {
                case 1:
                    if (mp3Error.isPlaying()) mp3Error.pause();
                    mp3Error.seekTo(0);
                    mp3Error.start();
                    break;
                case 2:
                    if (mp3Permitted.isPlaying()) mp3Permitted.pause();
                    mp3Permitted.seekTo(0);
                    mp3Permitted.start();
                    break;
                case 3:
                    if (mp3Dennied.isPlaying()) mp3Dennied.pause();
                    mp3Dennied.seekTo(0);
                    mp3Dennied.start();
                    break;
                case 4:
                    if (mp3Error.isPlaying()) mp3Error.pause();
                    mp3Error.seekTo(0);
                    if (mp3Dennied.isPlaying()) mp3Dennied.pause();
                    mp3Dennied.seekTo(0);
                    if (mp3Permitted.isPlaying()) mp3Permitted.pause();
                    mp3Permitted.seekTo(0);
                    break;
            }
            return null;
        }
    }

    public class getSetupTask extends AsyncTask<String, String, String>{

        private Exception exception;
        InputStream inputStream;
        String url = server + "/auth/local";

        @Override
        protected String doInBackground(String... params) {
            log_app log = new log_app();
            String json = "{}";
            String result = "";
            JSONObject jsonObject = new JSONObject();
            HttpClient httpclient = new DefaultHttpClient();

            // Retrieve TOKEN
            try {
                HttpPost httpPost = new HttpPost(url);

                jsonObject.accumulate("rut", "supervisor");
                jsonObject.accumulate("password", "supervisor");

                StringEntity se = new StringEntity(jsonObject.toString());
                httpPost.setEntity(se);

                httpPost.setHeader("Accept", "application/json");
                httpPost.setHeader("Content-type", "application/json");

                HttpResponse httpResponse = httpclient.execute(httpPost);
                inputStream = httpResponse.getEntity().getContent();

                if (inputStream != null) {
                    result = convertInputStreamToString(inputStream);
                    if (httpResponse.getStatusLine().getStatusCode() == 200) {
                        //token = result;
                        JSONObject jsonToken = new JSONObject(result);
                        token = jsonToken.getString("token");

                        // Retrieve the company, and sector
                        try {
                            String serialNumber = Build.SERIAL;
                            json = httpGet(server + "/api/pdas/" + serialNumber);
                            if (!json.equals("408")){
                                JSONArray json_array;
                                json_array = new JSONArray(json);
                                // Set global vars
                                idCompany = json_array.getJSONObject(0).getString("company");
                                idSector = json_array.getJSONObject(0).getString("sector");
                                Log.d("token", token);
                                Log.d("Company", idCompany);
                                Log.d("Sector", idSector);
                            }
                        } catch (JSONException jex) {
                            jex.printStackTrace();
                            log.writeLog(getApplicationContext(), "Main:line 572", "ERROR", jex.getMessage());
                        }
                    }
                } else {
                    result = String.valueOf(httpResponse.getStatusLine().getStatusCode());
                }
                //result its the json to sent
                if (result.startsWith("http://")) result = "204"; //no content

            } catch (HttpHostConnectException hhc) {
                Log.w("---", "offline");
            } catch (Exception e) {
                e.printStackTrace();
                this.exception = e;
            }
            return result;
        }
    }

    public class getPeopleTask extends AsyncTask<String, String, String> {

        protected void onPreExecute() {
            loading.setSpinSpeed(3);
            loading.setVisibility(View.VISIBLE);
        }

        protected String doInBackground(String... params) {

            if(token.equals("")) {
                //haven't got the token yet, do not issue any request, wait for the RetrieveTokenTask to finish first.
                return "204";
            } else if(!(idCompany.equals("") || idSector.equals(""))) {
                return httpGet(server + "/api/companies/" + idCompany + "/persons");
            } else {
                return "204";
            }
        }

        protected void onProgressUpdate(String... progress) {
            return;
        }

        protected void onPostExecute(String json) {
             // When response its 200, json save data no code.
            if (json != "408" && json != "204") {
                try {
                    db.add_people(json);
                } catch (IllegalStateException ise) {
                    ise.printStackTrace();
                }
            }
            loading.setVisibility(View.GONE);
        }
    }

    public String httpGet(String dataUrl) {

        String contentAsString = "";
        URL url;
        HttpURLConnection connection = null;

        if (!token.equals("")) {
            try {
                // Create connection
                url = new URL(dataUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                // Setting headers
                connection.setRequestProperty("Authorization", "Bearer " + token);
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setConnectTimeout(2000);
                connection.connect();

                int response = connection.getResponseCode();

                // Get Response
                InputStream is = connection.getInputStream();
                if (response != 200) contentAsString = String.valueOf(response);
                else contentAsString = convertInputStreamToString(is);
            } catch (Exception e) {
                e.printStackTrace();
                contentAsString = "408"; // Request Timeout
            }
            if (connection != null) connection.disconnect();
            if (contentAsString.length() <= 2) { contentAsString = "204"; }// No content
        } else Log.e("Error", "Token missing");
        return contentAsString;
    }

    public void postRecords() {
        DatabaseHelper db = DatabaseHelper.getInstance(this);
        List<Record> records = db.getOfflineRecords();
        postRecordsInstance = new postRecordsTask(records);
        postRecordsInstance.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private static String convertInputStreamToString(InputStream inputStream) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        String result = "";
        while ((line = bufferedReader.readLine()) != null)
            result += line;

        inputStream.close();
        return result;

    }

    public void httpPost(Record record, String url, OkHttpClient client) {
        String json = "";
        DatabaseHelper db = DatabaseHelper.getInstance(this);
        log_app log = new log_app();
        JSONObject jsonObject = new JSONObject();
        final MediaType JSON
                = MediaType.parse("application/json; charset=utf-8");
        try {
            // Build jsonObject from record object
            jsonObject.accumulate("person", record.getMongoId());

            DateFormat formatter = new SimpleDateFormat("yyy-MM-dd HH:mm:ss.S");
            if (record.getRecord_is_input() == 1) {
                jsonObject.accumulate("type", "entry");
                Date date = formatter.parse(record.getRecord_input_datetime());
                jsonObject.accumulate("time", date.getTime());
            } else {
                jsonObject.accumulate("type", "depart");
                Date date = formatter.parse(record.getRecord_output_datetime());
                jsonObject.accumulate("time", date.getTime());
            }

            // 4. convert JSONObject to JSON to String

            json = jsonObject.toString();
            Log.i("json to POST", json);

            RequestBody body = RequestBody.create(JSON, json);

            //create object okhttp
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Accept", "application/json")
                    .addHeader("Content-type", "application/json")
                    .addHeader("Authorization", "Bearer " + token)
                    .post(body)
                    .build();
            // 8. Execute POST request to the given URL
            Response response = client.newCall(request).execute();
            String bodyResponse = response.body().string();
            if (response.isSuccessful()) {
                if (!bodyResponse.equals("{}")) {
                    // if has sync = 0 its becouse its an offline record to be will posted.
                    if (record.getRecord_sync() == 0) db.update_record(record.getRecord_id());
                } else Log.e("tmp empty", bodyResponse);
            } else Log.e("Error", response.message());
        } catch (HttpHostConnectException hhc) {
            log.writeLog(getApplicationContext(), "Main: POST method", "ERROR", hhc.getMessage());
        } catch (Exception e) {
            log.writeLog(getApplicationContext(), "Main: POST method", "ERROR", e.getMessage());
        }
    }

    public class postRecordsTask extends AsyncTask<Void, Void, String> {

        private List<Record> records;

        postRecordsTask(List<Record> records) {
            this.records = records;
        }

        @Override
        protected String doInBackground(Void... params) {
            String postReturn = "";
            DatabaseHelper db = DatabaseHelper.getInstance(getApplicationContext());
            pdaNumber = db.get_config_id_pda();
            final OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(1, TimeUnit.SECONDS)
                    .writeTimeout(0, TimeUnit.SECONDS)
                    .readTimeout(0, TimeUnit.SECONDS)
                    .build();

            if (!idSector.equals("")) {
                for (int i = 0; i < records.size(); i++) {
                    Record record = records.get(i);
                    httpPost(record, server + "/api/sectors/" + idSector + "/registers/", client);
                }
            } else {
                Log.e("Error", "idSector missing");
                return "";
            }

            return postReturn;
        }
    }

    public void makeToast(String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    public void testRecords(int loop) {
        DatabaseHelper db = DatabaseHelper.getInstance(this);
        for (int i = 0; i < loop; i++) {
            Record records = new Record();
            int random = (int) Math.floor(Math.random() * (30000000 - 10000000) + loop);
            int random2 = (int) Math.floor(Math.random() * (99999 - 10000) + loop);
            records.setPerson_card(random2);
            records.setPerson_fullname("Test " + i);
            records.setPerson_run(random + "");
            records.setRecord_is_input(1);
            records.setPerson_is_permitted(1);
            records.setRecord_sync(0);
            records.setPerson_profile("E");
            try {
                Thread.sleep(100);
                records.setRecord_input_datetime(getCurrentDateTime());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            db.add_record(records);
        }
    }
}