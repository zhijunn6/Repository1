package com.smarttrack.app;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;

import java.util.Locale;

public class LanguageSelectionActivity extends AppCompatActivity {
    Button selectEng, selectFrc, selectIta, selectChn;
    private static final int REQUEST_FINE_LOCATION = 1;
    private static final int REQUEST_COARSE_LOCATION = 2;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_language_selection);

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            AlertDialog alertDialog = new AlertDialog.Builder(LanguageSelectionActivity.this).create();
            alertDialog.setTitle("SmarTrack");
            alertDialog.setMessage("Sorry, BLE is not supported on this device");
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Ok",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    });
            alertDialog.show();
        }

        selectEng = findViewById(R.id.selectEngButton);
        selectFrc = findViewById(R.id.selectFrcButton);
        selectIta = findViewById(R.id.selectItaButton);
        selectChn = findViewById(R.id.selectChnButton);

        selectEng.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Code here executes on main thread after user presses button
                setLocale("en");
                startActivity(new Intent(LanguageSelectionActivity.this, DeviceScanActivity.class));
            }
        });

        selectFrc.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Code here executes on main thread after user presses button
                setLocale("fr");
                startActivity(new Intent(LanguageSelectionActivity.this, DeviceScanActivity.class));
            }
        });

        selectIta.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Code here executes on main thread after user presses button
                setLocale("it");
                startActivity(new Intent(LanguageSelectionActivity.this, DeviceScanActivity.class));
            }
        });

        selectChn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Code here executes on main thread after user presses button
                setLocale("zh");
                startActivity(new Intent(LanguageSelectionActivity.this, DeviceScanActivity.class));
            }
        });
    }

    public void setLocale(String lang) {
//        Locale myLocale = new Locale(lang);
        Resources res = getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        Configuration conf = res.getConfiguration();
//        conf.locale = myLocale;
//        res.updateConfiguration(conf, dm);
//        Intent refresh = new Intent(this, LanguageSelectionActivity.class);
//        finish();
//        startActivity(refresh);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1){
            conf.setLocale(new Locale(lang.toLowerCase()));
        }
        else{
            conf.locale = new Locale(lang.toLowerCase());
        }
        res.updateConfiguration(conf, dm);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        this.finishAffinity();
//    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.app_name)
                .setMessage(R.string.exit_string)
                .setPositiveButton(R.string.yes_string, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finishAffinity();
                    }
                })
                .setNegativeButton(R.string.no_string, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }
}
