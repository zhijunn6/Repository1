package com.smarttrack.app;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class SplashScreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        //Creating a thread that will sleep for 5 seconds, then redirect to the main screen of the app
        Thread splashS = new Thread() {

            @Override
            public void run() {
                try {
                    //Thread sleeps for five seconds
                    sleep(3 * 1000);
                    //After five seconds, redirect to MainActivity intent
                    Intent intent = new Intent(getApplicationContext(), LanguageSelectionActivity.class);
                    startActivity(intent);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        };

        //Start thread
        splashS.start();
    }
}
