package com.lilith.android.bluetoothexampleapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.view.LayoutInflaterCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class BluetoothClientActivity extends AppCompatActivity {

    private final String TAG = "BluetoothClientActivity";
    public final int REQUEST_CODE_ENABLE_BLUETOOTH_AND_SEARCH_BLUETOOTH = 1;
    public final int REQUEST_CODE_BLUETOOTH_PERMISSIONS = 2;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothBroadcastReceiver bluetoothBroadcastReceiver;

    private RecyclerView rvBluetoothItem;

    private LinearLayoutManager bluetoothItemLayoutManager;
    private BluetoothItemAdapter bluetoothItemAdapter;

    private ConnectThread connectThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluethooth_client);

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

        bluetoothItemAdapter.setOnItemClickListener(item -> connection(item.getDevice()));

        // 註冊查詢藍芽廣播
        bluetoothBroadcastReceiver = new BluetoothBroadcastReceiver(bluetoothItemAdapter);
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(bluetoothBroadcastReceiver, filter);

        // 權限要求
        boolean checkPermissionsResult = checkPermissions();
        if (!checkPermissionsResult)
            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
            }, REQUEST_CODE_BLUETOOTH_PERMISSIONS);

        // 檢查藍芽狀態和開始查詢藍芽設備
        if (checkPermissionsResult)
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

    public void connection(BluetoothDevice device) {
        Log.d(TAG, "connection: ");

        // 取消查詢藍芽裝置
        if (bluetoothAdapter.isDiscovering())
            bluetoothAdapter.cancelDiscovery();

        // 關閉藍芽連線
        if (connectThread != null)
            connectThread.cancel();

        connectThread = new ConnectThread(device);
        connectThread.start();
    }

    public void searchBluetooth() {
        Log.d(TAG, "searchBluetooth: ");

        if (!bluetoothAdapter.startDiscovery()) {
            Toast.makeText(this, "無法提供藍芽查詢服務", Toast.LENGTH_SHORT).show();
            return;
        }
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

        // 取消查詢藍芽裝置
        if (bluetoothAdapter.isDiscovering())
            bluetoothAdapter.cancelDiscovery();

        // 關閉藍芽連線
        if (connectThread != null)
            connectThread.cancel();

        // 取消查詢藍芽廣播
        unregisterReceiver(bluetoothBroadcastReceiver);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case REQUEST_CODE_BLUETOOTH_PERMISSIONS:
                boolean checkPermissionsResult = checkPermissions();
                if (!checkPermissionsResult) {
                    finish();
                    break;
                }

                checkBluetoothEnableAndSearchBluetooth();
                break;
        }
    }

    private boolean checkPermissions() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "缺少ACCESS_FINE_LOCATION權限", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "缺少ACCESS_COARSE_LOCATION權限", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "缺少ACCESS_BACKGROUND_LOCATION權限", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }
}

class BluetoothItemAdapter extends RecyclerView.Adapter<BluetoothItemViewHolder> {

    private List<BluetoothItem> bluetoothItemList = new ArrayList<>();
    private OnItemClickListener onItemClickListener;

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

        holder.itemView.setOnClickListener(view -> {
            if (onItemClickListener != null)
                onItemClickListener.onItemClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return bluetoothItemList.size();
    }

    public void addBluetoothItem(BluetoothItem bluetoothItem) {
        bluetoothItemList.add(bluetoothItem);
        notifyItemInserted(bluetoothItemList.size() - 1);
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
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
    private BluetoothDevice device;

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

    public BluetoothDevice getDevice() {
        return device;
    }

    public void setDevice(BluetoothDevice device) {
        this.device = device;
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
            item.setDevice(device);

            bluetoothItemAdapter.addBluetoothItem(item);
        }
    }
}

class ConnectThread extends Thread {

    private String TAG = "ConnectThread";

    private BluetoothSocket socket;
    private BluetoothDevice device;

    private BufferedWriter writer;
    private BufferedReader reader;

    public ConnectThread(BluetoothDevice device) {
        this.device = device;

        try {
            UUID uuid = UUID.randomUUID();
            socket = device.createRfcommSocketToServiceRecord(uuid);
        } catch (IOException e) {
            Log.e(TAG, "Socket's create() method failed", e);
        }
    }

    @Override
    public void run() {
        try {
            socket.connect();

            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            sendMessage("hello bluetooth");
            receiveMessageHandler(reader.readLine());
        } catch (IOException connectException) {
            try {
                socket.close();
            } catch (IOException closeException) {
                Log.e(TAG, "Could not close the client socket", closeException);
            }
        }
    }

    public void cancel() {
        try {
            socket.close();
        } catch (IOException e) {
            Log.e(TAG, "Could not close the client socket", e);
        }
    }

    public void sendMessage(String senaMessage) {
        Log.d(TAG, "senaMessage: " + senaMessage);
        try {
            writer.write(senaMessage);
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void receiveMessageHandler(String receiveMessage) {
        Log.d(TAG, "receiveMessageHandler: " + receiveMessage);
    }
}

interface OnItemClickListener {
    void onItemClick(BluetoothItem item);
}