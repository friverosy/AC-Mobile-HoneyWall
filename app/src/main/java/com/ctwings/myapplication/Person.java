package com.ctwings.myapplication;

/**
 * Created by nicolasmartin on 03-08-16.
 */
public class Person {
    String person_mongo_id;
    String person_name;
    String person_rut;
    String person_active;
    String person_company;
    int person_card;
    String person_type;

    public Person(String person_name, String person_mongo_id, String person_rut, String person_active, String person_company, Integer person_card, String person_type){

        this.person_mongo_id = person_mongo_id;
        this.person_name = person_name;
        this.person_rut = person_rut;
        this.person_active = person_active;
        this.person_company = person_company;
        this.person_card = person_card;
        this.person_type = person_type;
    }

    public String getPerson_mongo_id() {
        return person_mongo_id;
    }

    public void setPerson_mongo_id(String person_mongo_id) {
        this.person_mongo_id = person_mongo_id;
    }

    public String getPerson_name() {
        return person_name;
    }

    public void setPerson_name(String person_name) {
        this.person_name = person_name;
    }

    public String getPerson_rut() {
        return person_rut;
    }

    public void setPerson_rut(String person_rut) {
        this.person_rut = person_rut;
    }

    public String getPerson_active() {
        return person_active;
    }

    public void setPerson_active(String person_active) {
        this.person_active = person_active;
    }

    public String getPerson_company() {
        return person_company;
    }

    public void setPerson_company(String person_company) {
        this.person_company = person_company;
    }

    public int getPerson_card() {
        return person_card;
    }

    public void setPerson_card(int person_card) {
        this.person_card = person_card;
    }

    public String getPerson_type() {
        return person_type;
    }

    public void setPerson_type(String person_type) {
        this.person_type = person_type;
    }
}