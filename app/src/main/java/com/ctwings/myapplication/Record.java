package com.ctwings.myapplication;

/**
 * Record object, contain register data.
 */
public class Record {

    int record_id;
    String person_mongo_id;
    String record_person_rut;
    String record_type;
    long record_date;
    int record_sync;

    public Record(Integer record_id, String mongo_person_id, String record_person_rut, String record_type, Long record_date, Integer record_sync){
        this.record_id = record_id;
        this.person_mongo_id = mongo_person_id;
        this.record_person_rut = record_person_rut;
        this.record_type = record_type;
        this.record_date = record_date;
        this.record_sync = record_sync;
    }

    public Record() {

    }

    public String getRecord_person_rut() {
        return record_person_rut;
    }

    public void setRecord_person_rut(String record_person_rut) {
        this.record_person_rut = record_person_rut;
    }

    public int getRecord_id() {
        return record_id;
    }

    public void setRecord_id(int record_id) {
        this.record_id = record_id;
    }

    public String getPerson_mongo_id() {
        return person_mongo_id;
    }

    public void setPerson_mongo_id(String person_mongo_id) {
        this.person_mongo_id = person_mongo_id;
    }

    public String getRecord_type() {
        return record_type;
    }

    public void setRecord_type(String record_type) {
        this.record_type = record_type;
    }

    public long getRecord_date() {
        return record_date;
    }

    public void setRecord_date(long record_date) {
        this.record_date = record_date;
    }

    public int getRecord_sync() {
        return record_sync;
    }

    public void setRecord_sync(int record_sync) {
        this.record_sync = record_sync;
    }
}