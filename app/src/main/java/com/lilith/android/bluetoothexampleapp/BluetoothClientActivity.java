package com.lilith.android.bluetoothexampleapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.LayoutInflaterCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class BluetoothClientActivity extends AppCompatActivity {

    private final String TAG = "BluetoothClientActivity";
    public final int REQUEST_CODE_ENABLE_BLUETOOTH_AND_SEARCH_BLUETOOTH = 1;

    private Handler handler;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothBroadcastReceiver bluetoothBroadcastReceiver;

    private BluetoothLeScanner bluetoothLeScanner;
    private boolean scanning;
    private BluetoothScanCallback bluetoothScanCallback;

    private RecyclerView rvBluetoothItem;

    private LinearLayoutManager bluetoothItemLayoutManager;
    private BluetoothItemAdapter bluetoothItemAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluethooth_client);

        handler = new Handler();
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Device doesn't support Bluetooth", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        rvBluetoothItem = findViewById(R.id.rvBluetoothItem);

        bluetoothItemLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        bluetoothItemAdapter = new BluetoothItemAdapter();

        rvBluetoothItem.setLayoutManager(bluetoothItemLayoutManager);
        rvBluetoothItem.setAdapter(bluetoothItemAdapter);

        //bluetoothBroadcastReceiver = new BluetoothBroadcastReceiver(bluetoothItemAdapter);
        //IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        //registerReceiver(bluetoothBroadcastReceiver, filter);

        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        bluetoothScanCallback = new BluetoothScanCallback(bluetoothItemAdapter);

        this.checkBluetoothEnableAndSearchBluetooth();
    }

    public void checkBluetoothEnableAndSearchBluetooth() {
        Log.d(TAG, "checkBluetoothEnableAndSearchBluetooth: ");
        if (bluetoothAdapter.isEnabled()) {
            searchBluetooth();
            return;
        }

        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(intent, REQUEST_CODE_ENABLE_BLUETOOTH_AND_SEARCH_BLUETOOTH);
    }

    public void connection() {
        Log.d(TAG, "connection: ");
    }

    public void searchBluetooth() {
        Log.d(TAG, "searchBluetooth: ");
        /*
        if (!bluetoothAdapter.startDiscovery()) {
            Toast.makeText(this, "查詢藍芽失敗", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "查詢藍芽成功", Toast.LENGTH_SHORT).show();
         */

        bluetoothLeScanner.startScan(bluetoothScanCallback);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_CODE_ENABLE_BLUETOOTH_AND_SEARCH_BLUETOOTH:
                Log.d(TAG, "onActivityResult: REQUEST_CODE_ENABLE_BLUETOOTH_AND_SEARCH_BLUETOOTH");
                if (resultCode == RESULT_CANCELED) {
                    finish();
                    break;
                }

                searchBluetooth();
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: ");
        //unregisterReceiver(bluetoothBroadcastReceiver);
        bluetoothLeScanner.stopScan(bluetoothScanCallback);
    }
}

class BluetoothItemAdapter extends RecyclerView.Adapter<BluetoothItemViewHolder> {

    private List<BluetoothItem> bluetoothItemList = new ArrayList<>();

    @NonNull
    @Override
    public BluetoothItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.rv_item_bluetooth, parent, false);
        return new BluetoothItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BluetoothItemViewHolder holder, int position) {
        BluetoothItem item = bluetoothItemList.get(position);
        holder.getTvName().setText(item.getName());
        holder.getTvAddress().setText(item.getAddress());
    }

    @Override
    public int getItemCount() {
        return bluetoothItemList.size();
    }

    public void addBluetoothItem(BluetoothItem bluetoothItem) {
        bluetoothItemList.add(bluetoothItem);
        notifyItemInserted(bluetoothItemList.size() - 1);
    }
}

class BluetoothItemViewHolder extends RecyclerView.ViewHolder {

    private TextView tvName;
    private TextView tvAddress;

    public BluetoothItemViewHolder(@NonNull View itemView) {
        super(itemView);

        tvName = itemView.findViewById(R.id.tvName);
        tvAddress = itemView.findViewById(R.id.tvAddress);
    }

    public TextView getTvName() {
        return tvName;
    }

    public TextView getTvAddress() {
        return tvAddress;
    }
}

class BluetoothItem {

    private String name;
    private String address;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}

class BluetoothBroadcastReceiver extends BroadcastReceiver {

    private BluetoothItemAdapter bluetoothItemAdapter;

    public BluetoothBroadcastReceiver(BluetoothItemAdapter bluetoothItemAdapter) {
        this.bluetoothItemAdapter = bluetoothItemAdapter;
    }

    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
            // Discovery has found a device. Get the BluetoothDevice
            // object and its info from the Intent.
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            String name = device.getName();
            String address = device.getAddress(); // MAC address

            BluetoothItem item = new BluetoothItem();
            item.setName(name);
            item.setAddress(address);

            bluetoothItemAdapter.addBluetoothItem(item);
        }
    }
}

class BluetoothScanCallback extends ScanCallback {

    private BluetoothItemAdapter bluetoothItemAdapter;

    public BluetoothScanCallback(BluetoothItemAdapter bluetoothItemAdapter) {
        this.bluetoothItemAdapter = bluetoothItemAdapter;
    }

    @Override
    public void onScanResult(int callbackType, ScanResult result) {
        super.onScanResult(callbackType, result);

        BluetoothDevice device = result.getDevice();
        String name = device.getName();
        String address = device.getAddress(); // MAC address

        BluetoothItem item = new BluetoothItem();
        item.setName(name);
        item.setAddress(address);
        bluetoothItemAdapter.addBluetoothItem(item);
    }
}