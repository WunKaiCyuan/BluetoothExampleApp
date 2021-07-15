package com.lilith.android.bluetoothexampleapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void openBluetoothClientActivity(View view){
        Intent intent = new Intent(this, BluetoothClientActivity.class);
        startActivity(intent);
    }

    public void openBluetoothServerActivity(View view){
        Toast.makeText(this, "NONE", Toast.LENGTH_SHORT).show();
    }
}