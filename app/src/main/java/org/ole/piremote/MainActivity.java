package org.ole.piremote;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    String service_uuid = "807e2f5e-a6a5-55bf-b122-2f1ca33d0f48";
    String uuid_pictrl = "1d46c805-eeeb-57fe-bcc5-b74aabf13b53";
    String TAG = "piremote";
    int SCAN_PERIOD = 10_000;
    boolean scanning = false;

    Handler h = new Handler();

    private enum Requests {
        REQUEST_INVALID(0),
        REQUEST_ENABLE_BT(1);

        private final int value;

        Requests(int value) { this.value = value; }

        public static Requests fromInt(int i) {
            switch (i) {
                case 1: return REQUEST_ENABLE_BT;
                default: return REQUEST_INVALID;
            }
        }
    }

    private class ScanResult {
        BluetoothDevice device;
        int rssi;
        byte[] record;

        ScanResult(BluetoothDevice device, int rssi, byte[] record) {
            this.device = device;
            this.rssi = rssi;
            this.record = record;
        }

        public String toString() {
            return this.device.toString();
        }
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "broadcast: " + intent.toString());
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int requestResult, Intent intent) {
        switch (Requests.fromInt(requestCode)) {
            case REQUEST_ENABLE_BT:
                // result of requesting BT turn on
                Log.d(TAG, "bluetooth enable request result: " + Integer.toString(requestResult));
        }
    }

    protected void registerFilters() {
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);
    }

    protected void unregisterFilters() {
        unregisterReceiver(receiver);
    }

    protected void setupBT() {
        final BluetoothAdapter bt = BluetoothAdapter.getDefaultAdapter();
        final List<ScanResult> results = new ArrayList<>();
        final ArrayAdapter<ScanResult> adapt = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, results);

        ListView view = (ListView)findViewById(R.id.listview);
        view.setAdapter(adapt);

        if (bt == null) {
            Log.d(TAG, "no Bluetooth adapter?");
            return;
        }

        if (!bt.isEnabled()) {
            Log.d(TAG, "Bluetooth turned off?");
            // TODO ask to enable BT here

            Intent x = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(x, Requests.REQUEST_ENABLE_BT.ordinal());
        }

        Set<BluetoothDevice> devices = bt.getBondedDevices();

        if (devices.size() > 0) {
            // TODO list already bonded devices first?

        }

        final BluetoothAdapter.LeScanCallback scanCb = new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                Log.d(TAG, "scan result: " + scanRecord.toString());
                // TODO add to list
                results.add(new ScanResult(device, rssi, scanRecord));
                adapt.notifyDataSetChanged();
            }
        };

        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                scanning = false;
                bt.stopLeScan(scanCb);

            }
        }, SCAN_PERIOD);

        bt.startLeScan(scanCb);
        scanning = true;

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String coarse = "android.permission.ACCESS_COARSE_LOCATION";

        if (checkSelfPermission(coarse) != PackageManager.PERMISSION_GRANTED) {
            String[] perms = {coarse};
            requestPermissions(perms, 0);
        }

        registerFilters();
        setupBT();

        /*
        String[] items = {"one", "two", "three"};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, items);
        ListView view = (ListView)findViewById(R.id.listview);
        view.setAdapter(adapter);
        */

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterFilters();
    }
}
