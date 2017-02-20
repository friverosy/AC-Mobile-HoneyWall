package com.ctwings.myapplication;

/**
 * Created by cristtopher on 04-03-16.
 */
public class Server {
    //private variables
    int _id;
    String _url;
    Integer _port;

    //Constructors
    public Server(){

    }

    public int get_id() {
        return _id;
    }

    public void set_id(int _id) {
        this._id = _id;
    }

    public String get_url() {
        return _url;
    }

    public void set_url(String _url) {
        this._url = _url;
    }

    public Integer get_port() {
        return _port;
    }

    public void set_port(Integer _port) {
        this._port = _port;
    }

    @Override
    public String toString() {
        return "Server [id="+_id+", url="+_url+", port="+_port+"]";
    }
}
