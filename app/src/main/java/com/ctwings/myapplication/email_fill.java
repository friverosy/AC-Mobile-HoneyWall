package com.ctwings.myapplication;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class email_fill extends AppCompatActivity {

    private EditText subject;
    private EditText message;
    private Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_fill);
        subject = (EditText) findViewById(R.id.editText_subject);
        message = (EditText) findViewById(R.id.editText_message_mail);
        button = (Button) findViewById(R.id.button_send_email);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!subject.getText().toString().isEmpty()) {
                    if (!message.getText().toString().isEmpty()) {
                        sendEmail();
                    } else
                        message.setError("Este campo es requerido");
                } else
                    subject.setError("Este campo es requerido");

            }
        });


    }

    protected void sendEmail() {
        Log.i("Send email", "");
        String[] TO = {"support@axxezo.com"};
        String[] CC = {""};
        Intent emailIntent = new Intent(Intent.ACTION_SEND);

        emailIntent.setData(Uri.parse("mailto:"));
        emailIntent.setType("text/plain");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, TO);
        emailIntent.putExtra(Intent.EXTRA_CC, CC);
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject.getText().toString());
        emailIntent.putExtra(Intent.EXTRA_TEXT, message.getText().toString());

        try {
            startActivity(Intent.createChooser(emailIntent, "Send mail..."));
            finish();
            Log.i("Finished sending email.", "");
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "There is no email client installed.", Toast.LENGTH_SHORT).show();
        }
    }
}
