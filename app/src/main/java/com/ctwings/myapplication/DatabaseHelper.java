package com.ctwings.myapplication;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabaseLockedException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    //Instance of Helper to keep one conection all time
    private static DatabaseHelper sInstance;

    // Database Version
    private static final int DATABASE_VERSION = 1;
    // Database Name
    private static final String DATABASE_NAME = "Multiempresa";
    // Get context for use
    private Context context;

    // Table names
    private static final String TABLE_PERSON = "PERSON";
    private static final String TABLE_RECORD = "RECORD";
    private static final String TABLE_SETTING = "SETTING";

    //Person & Record table columns names
    public static final String PERSON_ID = "person_id";
    public static final String RECORD_ID = "record_id";
    public static final String PERSON_MONGO_ID = "person_mongo_id";
    public static final String PERSON_NAME = "person_name";
    public static final String PERSON_RUT = "person_rut";
    public static final String PERSON_TYPE = "person_type";
    public static final String RECORD_TYPE = "record_type";
    public static final String PERSON_ACTIVE = "person_active";
    public static final String PERSON_COMPANY = "person_company";
    public static final String RECORD_DATE = "record_date";
    public static final String RECORD_SYNC = "record_sync";
    public static final String PERSON_CARD = "person_card";

    // Setting Table Columns names
    /*private static final String SETTING_ID = "id";
    private static final String SETTING_URL = "url";
    private static final String SETTING_PORT = "port";*/

    private static final String[] PERSON_COLUMNS = {PERSON_ID, PERSON_MONGO_ID, PERSON_NAME, PERSON_RUT, PERSON_ACTIVE, PERSON_COMPANY, PERSON_CARD, PERSON_TYPE};
    private static final String[] RECORD_COLUMNS = {RECORD_ID, PERSON_MONGO_ID, PERSON_RUT, RECORD_TYPE, RECORD_DATE, RECORD_SYNC};

    public static synchronized DatabaseHelper getInstance(Context context) {
        //one single instance of DB
        if (sInstance == null) {
            sInstance = new DatabaseHelper(context.getApplicationContext());
        }
        return sInstance;
    }

    // SQL statement to create User table
    String CREATE_PERSON_TABLE = "CREATE TABLE " + TABLE_PERSON + " ( " +
            PERSON_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            PERSON_MONGO_ID + " TEXT, " +
            PERSON_NAME + " TEXT, " +
            PERSON_RUT + " TEXT, " +
            PERSON_ACTIVE + " TEXT, " +
            PERSON_COMPANY + " TEXT DEFAULT '', " +
            PERSON_CARD + " INTEGER, " +
            PERSON_TYPE + " TEXT)";

    String CREATE_RECORD_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_RECORD + " ( " +
            RECORD_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            PERSON_MONGO_ID + " TEXT, " +
            PERSON_RUT + " TEXT, " +
            RECORD_TYPE + " TEXT, " +
            RECORD_DATE + " INTEGER, " +
            RECORD_SYNC + " INTEGER)";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL(CREATE_PERSON_TABLE);

        db.execSQL(CREATE_RECORD_TABLE);

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_SETTING + " (" +
                "id INTEGER PRIMARY KEY, url TEXT, port INTEGET, id_pda INTEGER);");

        db.execSQL("CREATE INDEX people_idx_by_mongoid " +
                " ON " + TABLE_PERSON + " (" + PERSON_MONGO_ID + ");");

        db.execSQL("CREATE INDEX people_idx_by_run " +
                "  ON " + TABLE_PERSON + " (" + PERSON_RUT + ");");

        db.execSQL("CREATE INDEX people_idx_by_card " +
                "  ON " + TABLE_PERSON + " (" + PERSON_CARD + ");");

        db.execSQL("CREATE INDEX record_idx_by_sync " +
                " ON " + TABLE_RECORD + " (" + RECORD_SYNC + ");");

        db.execSQL("PRAGMA encoding = 'UTF-8';");

        //seed
        db.execSQL("INSERT INTO "+TABLE_SETTING+" (id_pda) VALUES (0);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older tables if it existed
        db.execSQL("DROP TABLE IF EXISTS person");
        //create fresh tables
        this.onCreate(db);
    }

    // People CRUD
    public void add_people(String json){
        final long startTime = System.currentTimeMillis();
        log_app log = new log_app();
        JSONArray json_db_array;
        SQLiteDatabase db = this.getWritableDatabase();
        DatabaseUtils.InsertHelper iHelp = new DatabaseUtils.InsertHelper(db, TABLE_PERSON);

        db.beginTransaction();
        try {
            json_db_array = new JSONArray(json);
            db.delete(TABLE_PERSON,null,null);
            String sRut;
            int pos;
            String sMongoPresonId;
            String sCompany;
            JSONObject jCompany;
            String sCompanyName;


            for (int i = 0; i < json_db_array.length(); i++) {
                iHelp.prepareForInsert();
                try {

                    //remove the 'dv' in the string 12345678-k => 12345678
                    sRut = json_db_array.getJSONObject(i).getString("rut");
                    pos = sRut.indexOf('-');
                    if(pos > 0 ) {
                        sRut = sRut.substring(0,pos);
                    }

                    iHelp.bind(iHelp.getColumnIndex(PERSON_RUT),sRut);
                    iHelp.bind(iHelp.getColumnIndex(PERSON_TYPE), json_db_array.getJSONObject(i).getString("type"));

                    sMongoPresonId = json_db_array.getJSONObject(i).getString("_id");

                    sCompany = json_db_array.getJSONObject(i).getString("company");
                    jCompany = new JSONObject(sCompany);
                    sCompanyName  = jCompany.getString("name");

                    iHelp.bind(iHelp.getColumnIndex(PERSON_MONGO_ID), sMongoPresonId);
                    iHelp.bind(iHelp.getColumnIndex(PERSON_ACTIVE), json_db_array.getJSONObject(i).getString("active"));

                    switch (json_db_array.getJSONObject(i).getString("type")) {
                        case "staff": // Employee
                            iHelp.bind(iHelp.getColumnIndex(PERSON_NAME), json_db_array.getJSONObject(i).getString("name"));
                            iHelp.bind(iHelp.getColumnIndex(PERSON_COMPANY), sCompanyName);
                            iHelp.bind(iHelp.getColumnIndex(PERSON_CARD), json_db_array.getJSONObject(i).getString("card"));
                            break;
                        case "contractor": // Contactor
                            iHelp.bind(iHelp.getColumnIndex(PERSON_NAME), json_db_array.getJSONObject(i).getString("name"));
                            iHelp.bind(iHelp.getColumnIndex(PERSON_COMPANY), sCompanyName);
                            iHelp.bind(iHelp.getColumnIndex(PERSON_CARD), json_db_array.getJSONObject(i).getString("card"));
                            break;
                        case "visitor": // Visit
                            if (!json_db_array.getJSONObject(i).getString("name").isEmpty())
                                iHelp.bind(iHelp.getColumnIndex(PERSON_NAME), json_db_array.getJSONObject(i).getString("name"));
                            if (!sCompanyName.isEmpty())
                                iHelp.bind(iHelp.getColumnIndex(PERSON_COMPANY), sCompanyName);
                            break;
                        default:
                            break;
                    }
                    iHelp.execute();
                } catch (Exception e) {
                    Log.e("json", json_db_array.getJSONObject(i).toString());
                    Log.e("ERROR", e.getMessage());
                }
            }
            db.setTransactionSuccessful();
        } catch (JSONException e) {
            e.printStackTrace();
            log.writeLog(context, "DBhelper:line 182", "ERROR", e.getMessage());
        } catch (IllegalStateException e) {
            e.printStackTrace();
            log.writeLog(context, "DBhelper:line 184", "ERROR", e.getMessage());
        } catch (SQLiteDatabaseLockedException e) {
            e.printStackTrace();
            log.writeLog(context, "DBhelper:line 186", "ERROR", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            log.writeLog(context, "DBhelper:line 186", "ERROR", e.getMessage());
        } finally {
            db.endTransaction();
            Log.i("insert people in", String.valueOf(System.currentTimeMillis() - startTime) + "ms");
        }
    }

    public Cursor get_one_person(String id){
        SQLiteDatabase db = getWritableDatabase();
        Cursor cursor = null;

        try {
            id.replace("%", ""); // Remove 0 at beginner
            cursor =
                    db.query(TABLE_PERSON, // a. table
                            PERSON_COLUMNS, // b. column names
                            PERSON_RUT + " = ? OR " + PERSON_CARD + " = ?", // c. selections
                            new String[]{String.valueOf(id), String.valueOf(id)}, // d. selections args
                            null, // e. group by
                            null, // f. having
                            null, // g. order by
                            null); // h. limit

            if (cursor != null) {
                    cursor.moveToFirst();
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (SQLiteDatabaseLockedException sdle) {
            sdle.printStackTrace();
        } catch (NumberFormatException nfe){
            nfe.printStackTrace();
        }
        //db.close();

        return cursor;
    }

    //Records
    public void add_record(Record record){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(PERSON_MONGO_ID, record.getPerson_mongo_id());
        values.put(RECORD_TYPE, record.getRecord_type());
        if (record.getRecord_person_rut() != null)
            values.put(PERSON_RUT, record.getRecord_person_rut());
        values.put(RECORD_DATE, record.getRecord_date());
        values.put(RECORD_SYNC, record.getRecord_sync());

        try {
            db.insert(TABLE_RECORD, null, values);
        } catch (SQLException e) {
            Log.e("DataBase Error", "Error to insert record: "+values);
            e.printStackTrace();
        }

        db.close();
    }

    public List<Record> getOfflineRecords(){
        SQLiteDatabase db = this.getReadableDatabase();
        log_app log = new log_app();
        List<Record> records = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_RECORD, // a. table
                            RECORD_COLUMNS, // b. column names
                            RECORD_SYNC + " = 0", // c. selections
                            null, // d. selections args
                            null, // e. group by
                            null, // f. having
                            null, // g. order by
                            null); // h. limit

            cursor.moveToFirst();
            Record record = new Record();
            while (!cursor.isAfterLast()) {
                try {
                    record.setRecord_id(cursor.getInt(cursor.getColumnIndex(RECORD_ID)));
                    record.setPerson_mongo_id(cursor.getString(cursor.getColumnIndex(PERSON_MONGO_ID)));
                    if (cursor.getString(cursor.getColumnIndex(PERSON_RUT)) != null)
                        record.setRecord_person_rut(cursor.getString(cursor.getColumnIndex(PERSON_RUT)));
                    record.setRecord_type(cursor.getString(cursor.getColumnIndex(RECORD_TYPE)));
                    record.setRecord_date(cursor.getLong(cursor.getColumnIndex(RECORD_DATE)));
                    record.setRecord_sync(cursor.getInt(cursor.getColumnIndex(RECORD_SYNC)));
                } catch (Exception e){
                    e.printStackTrace();
                } finally {
                    records.add(record);
                    record = null;
                }
                cursor.moveToNext();
            }
            cursor.close();
        } catch (Exception e) {
            log.writeLog(context, "DBhelper:line 238", "ERROR", e.getMessage());
            e.printStackTrace();
        } finally {
            cursor.close();
        }
        //db.close();
        return records;
    }

    public int record_desync_count(){
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + RECORD_ID + " FROM " + TABLE_RECORD +
                " WHERE " + RECORD_SYNC + " = 0;", null);
        int count = cursor == null?0:cursor.getCount();
        return count;
    }

    public int people_count(){
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + PERSON_ID + " FROM " + TABLE_PERSON, null);
        int count = cursor == null?0:cursor.getCount();
        return count;
    }

    public int employees_count(){
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + PERSON_ID + " FROM " + TABLE_PERSON +
                " WHERE " + PERSON_TYPE + " = 'staff';", null);
        int count = cursor == null?0:cursor.getCount();
        return count;
    }

    public int contractors_count(){
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + PERSON_ID + " FROM " + TABLE_PERSON + " " +
                "WHERE " + PERSON_TYPE + " = 'contractor';", null);
        int count = cursor == null?0:cursor.getCount();
        return count;
    }

    public int visits_count(){
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + PERSON_ID + " FROM " + TABLE_PERSON + " " +
                "WHERE " + PERSON_TYPE + " = 'visitor';", null);
        int count = cursor == null?0:cursor.getCount();
        return count;
    }

    public void clean_people(){
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_PERSON, null, null);
    }

    public void clean_records(){
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_RECORD, null, null);
    }

    public void update_record(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            ContentValues values = new ContentValues();
            values.put(RECORD_SYNC, 1);

            int i = db.update(TABLE_RECORD, //table
                    values, // column/value
                    RECORD_ID + "=" + id, // where
                    null);

            //db.close();
            if (i == 0) Log.e("Error updating record", String.valueOf(id));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Cursor get_config() {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_SETTING, null);
        return cursor;
    }

    public int get_config_id_pda() {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT id_pda FROM " + TABLE_SETTING, null);
        if (cursor.moveToFirst()) {
            return cursor.getInt(cursor.getColumnIndex("id_pda"));
        } else {
            return 0;
        }
    }

    public String get_config_url() {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT url FROM " + TABLE_SETTING, null);
        if (cursor.moveToFirst()) {
            return cursor.getString(cursor.getColumnIndex("id_pda"));
        } else {
            return "";
        }
    }

    public int get_config_port() {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT port FROM " + TABLE_SETTING, null);
        if (cursor.moveToFirst()) {
            return cursor.getInt(cursor.getColumnIndex("port"));
        } else {
            return 0;
        }
    }

    /*public void set_config_url(String url) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("url", url);
        db.update(TABLE_SETTING, cv, null, null);
    }

    public void set_config_port(int port) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("port", port);
        db.update(TABLE_SETTING, cv, null, null);
    }*/

    public void set_config_id_pda(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        Log.i("puting", String.valueOf(id));
        cv.put("id_pda", id);
        db.update(TABLE_SETTING, cv, null, null);
    }
}
