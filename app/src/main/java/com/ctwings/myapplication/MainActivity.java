package com.ctwings.myapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.device.ScanManager;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
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

import com.pnikosis.materialishprogress.ProgressWheel;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Handle register and people. Have the main business logic.
 */
public class MainActivity extends AppCompatActivity {

    private final int delayPeople = 10000 ; // 4 Min. 240000; 600000 10 min
    private final int delayRecords = 6000; // 4 Min. 240000; 480000 8 min
    private static String server = "http://axxezocloud.brazilsouth.cloudapp.azure.com:5001"; // Integration server
    //private static String server = "http://192.168.1.102:9000"; // Integration server
    //private static String server = "http://axxezo-test.brazilsouth.cloudapp.azure.com:9000"; // Test server
    private String idCompany = "";
    private String idSector = "";
    private String token = "";
    private int pdaNumber;
    private static String version = "cf0649f";
    private getPeopleTask getPeopleInstance;
    private postRecordsTask postRecordsInstance;

    private ImageView imageview;
    private EditText editTextRun;
    private TextView textViewName;
    private TextView textViewVersion;
    private String name = "";
    private TextView textViewCompany;
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
        editTextRun = (EditText) findViewById(R.id.editText_rut);
        textViewName = (TextView) findViewById(R.id.textView_name);
        textViewCompany = (TextView) findViewById(R.id.textView_company);
        textViewProfile = (TextView) findViewById(R.id.textView_profile);
        imageview = (ImageView) findViewById(R.id.imageView);
        mp3Dennied = MediaPlayer.create(MainActivity.this, R.raw.bad);
        mp3Permitted = MediaPlayer.create(MainActivity.this, R.raw.good);
        mp3Error = MediaPlayer.create(MainActivity.this, R.raw.error);
        textViewCompany.setVisibility(View.GONE);
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

    /**
     * Get data from barcode as byte array and parsed to string.
     * This obtained string is sent to the getPerson() method to be handled.
     */
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

    /**
     * Dictionary of MD5 hashes, each one executes a task.
     * @param barcodeStr Hash MD5
     *                   Sync offline registers,
     *                   get total of registers,
     *                   get total of people,
     *                   get total of employees,
     *                   get total of contractors,
     *                   get total of visitors,
     *                   drop people table,
     *                   drop record table,
     *                   call log viewer fragment.
     */
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
        textViewName.setText("");
        textViewCompany.setText("");
        textViewProfile.setText("");
        imageview.setImageDrawable(null);
        name = null;
    }

    /**
     * Method that asks each delayRecord time
     * if the number of records that are not synchronized (offline records (record_sync = 0)) with the backend,
     * calls the postRecords method that get all this offline records
     * and send to asynchronous method called postRecordsTask.
     */
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
                            e.printStackTrace();
                            log.writeLog(getApplicationContext(), "Main:line 419", "ERROR", e.getMessage());
                        }
                    }
                });
            }
        };
        timer.schedule(task, 0, delayRecords);
    }

    /**
     * It makes a call to the asynchronous task
     * that obtains by http get people from the API
     * Each delayPeople time.
     */
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

    /**
     * Get information about one person from local database (sqlite) and
     * Build a record object to be insert into local database as register (event)
     * @param rut
     */
    public void getPerson(String rut) {
        log_app log = new log_app();
        DatabaseHelper db = DatabaseHelper.getInstance(this);
        Cursor person = db.get_one_person(rut);

        textViewCompany.setVisibility(View.GONE);

        try {
            //build object with that values, then send to registerTarsk()
            Record record = new Record();

            // If has not a person.
            if (person.getCount() < 1) {
                new loadSound(3).execute();
                editTextRun.setText(rut);
                editTextRun.setVisibility(View.VISIBLE);
                record.setRecord_person_rut(rut);
                if (is_input)
                    imageview.setImageResource(R.drawable.dennied);
            } else {
                record.setPerson_mongo_id(person.getString(person.getColumnIndex("person_mongo_id")));
                if (person.getString(person.getColumnIndex("person_active")).equals("true")) {
                    new loadSound(2).execute();
                    editTextRun.setVisibility(View.GONE);
                    if (is_input)
                        imageview.setImageResource(R.drawable.permitted);
                } else {
                    new loadSound(3).execute();
                    editTextRun.setVisibility(View.VISIBLE);
                    if (is_input)
                        imageview.setImageResource(R.drawable.dennied);
                }

                switch (person.getString(person.getColumnIndex("person_type"))) {
                    case "staff":
                        textViewName.setText(person.getString(person.getColumnIndex("person_name")));
                        textViewProfile.setText("Empleado");
                        textViewCompany.setVisibility(View.GONE);
                        editTextRun.setVisibility(View.GONE);
                        break;
                    case "contractor":
                        textViewName.setText(person.getString(person.getColumnIndex("person_name")));
                        textViewProfile.setText("Subcontratista");
                        textViewCompany.setText(person.getString(person.getColumnIndex("person_company")));
                        textViewCompany.setVisibility(View.VISIBLE);
                        break;
                    case "visitor":
                        textViewProfile.setText("Visita");
                        // If could get the name of pdf417 show it.
                        try {
                            if (!person.getString(1).isEmpty()) {
                                textViewName.setText(person.getString(person.getColumnIndex("person_name")));
                            } else {
                                textViewName.setText(name);
                            }

                            // If have company show it.
                            if (!person.getString(person.getColumnIndex("person_company")).isEmpty()) {
                                textViewCompany.setText(person.getString(person.getColumnIndex("person_company")));
                                textViewCompany.setVisibility(View.VISIBLE);
                            } else {
                                textViewCompany.setVisibility(View.GONE);
                            }
                        } catch (NullPointerException npe) {
                            textViewName.setText("");
                            log.writeLog(getApplicationContext(), "Main:line 504", "ERROR", npe.getMessage());
                        }
                        break;
                }
            }

            person.close();

            record.setRecord_sync(0);
            record.setRecord_date(new Date().getTime());

            if (is_input) record.setRecord_type("entry");
            else record.setRecord_type("depart");

            // Save record on local database
            db.add_record(record);
        } catch (ArrayIndexOutOfBoundsException aiobe) {
            new loadSound(1).execute(); // Error sound.
            aiobe.printStackTrace();
            log.writeLog(getApplicationContext(), "Main:line 538", "ERROR", aiobe.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            new loadSound(1).execute(); // Error sound.
            log.writeLog(getApplicationContext(), "Main:line 542", "ERROR", e.getMessage());
        }
    }

    /**
     * Call to a sound to validation.
     * 1: Error, 2: Permitted, 3: Denied, 4: Stop all.
     */
    private class loadSound extends AsyncTask<Void, Void, Void> {
        private int typeSound = -1;

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

    /**
     * Generate a session and token on API and
     * Get from API, the company and sector related with to this PDA.
     *
     * This is a basic (necessary) data to traffic data with API.
     */
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

    /**
     * Make a background call to httpGet which,
     * using the url sent as parameter
     * returns the json sent by the API as a string
     *
     * OnPostExecute, Sends the json obtained as parameter
     * to the add_people method of the databaseHelper class
     * to be inserted into the local database
     */
    public class getPeopleTask extends AsyncTask<String, String, String> {

        protected void onPreExecute() {
            loading.setSpinSpeed(3);
            loading.setVisibility(View.VISIBLE);
        }

        /**
         *  Do a http get to obtain an array json with people.
         * @param params not used.
         * @return a http get response, its an array json.
         */
        protected String doInBackground(String... params) {
            if(token.equals("") || idCompany.equals("") || idSector.equals("")) return "204";
            else return httpGet(server + "/api/companies/" + idCompany + "/persons");
        }

        /**
         * Send to the dataBaseHelper the json Array it receives,
         * to insert it into the local database (sqlite).
         * @param json
         */
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

    /**
     * Do a HTTP get request.
     * @param dataUrl
     * @return http get response as string.
     */
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

    /**
     * Gets offline records as a list of records
     * that will be sent to postRecordsTask per parameter.
     */
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

    /**
     * Do a HTTP post to posted a register to API.
     * @param record object, contain data to build json.
     * @param url endpoint to receive a json.
     * @param client Http client library.
     */
    public void httpPost(Record record, String url, OkHttpClient client) {
        String json = "";
        DatabaseHelper db = DatabaseHelper.getInstance(this);
        log_app log = new log_app();
        JSONObject jsonObject = new JSONObject();
        final MediaType JSON
                = MediaType.parse("application/json; charset=utf-8");
        try {
            // Build jsonObject from record object
            jsonObject.accumulate("person", record.getPerson_mongo_id());
            jsonObject.accumulate("time", record.getRecord_date());
            jsonObject.accumulate("type", record.getRecord_type());

            if (record.getRecord_person_rut() != null)
                jsonObject.accumulate("rut", record.getRecord_person_rut());

            // Convert JSONObject to JSON to String
            json = jsonObject.toString();
            Log.i("json to POST", json);

            RequestBody body = RequestBody.create(JSON, json);

            // Create object okhttp
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Accept", "application/json")
                    .addHeader("Cache-Control","no-cache,no-store,max-age=0,must-revalidate")
                    .addHeader("Pragma","no-cache")
                    .addHeader("Expires","-1")
                    .addHeader("X-Content-Type-Options","nosniff")
                    .addHeader("Content-type", "application/json")
                    .addHeader("Authorization", "Bearer " + token)
                    .post(body)
                    .build();

            // Execute POST request to the given URL
            Response response = client.newCall(request).execute();
            String bodyResponse = response.body().string();
            if (response.isSuccessful()) {
                if (!bodyResponse.equals("{}")) {
                    // if has sync = 0 its becouse its an offline record to be will posted.
                    if (record.getRecord_sync() == 0) db.update_record(record.getRecord_id());
                } else Log.e("tmp empty", bodyResponse);
            } else Log.e(response.message(), bodyResponse);
        } catch (HttpHostConnectException hhc) {
            hhc.printStackTrace();
            log.writeLog(getApplicationContext(), "Main: POST method", "ERROR", hhc.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            log.writeLog(getApplicationContext(), "Main: POST method", "ERROR", e.getMessage());
        }
    }

    /**
     * It receives the list of records offline,
     * and calls for each record to the asynchronous httpPost() that performs the post.
     */
    public class postRecordsTask extends AsyncTask<Void, Void, Void> {

        private List<Record> records;

        postRecordsTask(List<Record> records) {
            this.records = records;
        }

        /**
         * Rove the list of offline records and each sends an obj record to the method httpPost()
         * @param params Record type list
         * @return Always null.
         */
        @Override
        protected Void doInBackground(Void... params) {
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
            }
            return null;
        }
    }

    public void makeToast(String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }
}