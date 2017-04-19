package com.ctwings.myapplication;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.NumberPicker;

public class Setting extends AppCompatActivity {
    NumberPicker np;
    DatabaseHelper db = new DatabaseHelper(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        np = (NumberPicker) findViewById(R.id.numberPicker);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        np.setMinValue(0);
        np.setMaxValue(10);
        np.setWrapSelectorWheel(false);
        np.setValue(1);

        Load();

        np.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {

            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                // TODO Auto-generated method stub
                Log.i("set", String.valueOf(newVal));
                Save(newVal);
            }
        });
    }

    public void Load(){
        try {
            if(db != null){
                Cursor c = db.get_config();

                Integer id_pda = 0;

                if(c.moveToFirst()) {
                    do {
                        id_pda = c.getInt(3);
                    } while (c.moveToNext());
                }

                np.setValue(id_pda);
                Log.i("saved", String.valueOf(id_pda));
                if (!c.isClosed()) c.close();
            }
        } catch (NullPointerException n){
            n.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        db.close();
    }

    public void Save(int value){
        Log.i("value", String.valueOf(value));
        db.set_config_id_pda(value);
    }
}
