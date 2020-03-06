package ducdh8.seminar.bluetooth;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Environment;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BluetoothActivity extends AppCompatActivity {

    Button btOn, btOff, btSearch, btList;
    ListView lstView;

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_VISIBLE_BT = 2;
    private static final int DEFAULT_ACTION = 3;
    private static final int REMOVE_BOND_ACTION = 4;
    private static final int CREATE_BOND_ACTION = 5;
    private static final int REGISTERED_BROADCAST = 1;
    private static final int UNREGISTERED_BROADCAST = 0;

    private int status;
    private int action;

    private TextView txtStatus;
    private BluetoothAdapter adapter;
    private ArrayAdapter<String> btArrayAdapter;
    private List<BluetoothDevice> listFindDevice;
    private final BroadcastReceiver broadcast = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            System.out.println(">>>>> onReceive");
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                String deviceName = device.getName();

                deviceName = (deviceName == null) ? "Unknown Device" : deviceName;
                btArrayAdapter.add(deviceName + "\n"
                                + device.getAddress());
                btArrayAdapter.notifyDataSetChanged();
                listFindDevice.add(device);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);

        initViewsById();

        status = UNREGISTERED_BROADCAST;
        action = DEFAULT_ACTION;

        adapter = BluetoothAdapter.getDefaultAdapter();

        updateStatus();

        btArrayAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1);
        lstView.setAdapter(btArrayAdapter);

        listFindDevice = new ArrayList<>();

        lstView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                BluetoothDevice selectedDevice = listFindDevice.get(position);
                try {
                    Method method = null;
                    if (action == CREATE_BOND_ACTION) {
                        method = selectedDevice.getClass().getMethod(
                                "createBond", (Class[]) null);
                        Log.d("createBond", "in createBond method");
                    } else if (action == REMOVE_BOND_ACTION) {
                        method = selectedDevice.getClass().getMethod(
                                "removeBond", (Class[]) null);
                        Log.d("removeBond", "in removeBond method");
                    }

                    try {
                        if (method != null) {
                            method.invoke(selectedDevice, (Object[]) null);
                        }
                    } catch (Exception e) {
                        Log.d("Pairing", e.getMessage());
                    }
                } catch (NoSuchMethodException e) {
                    Log.d("Reflecting Method", e.getMessage());
                }
            }
        });
    }

    private void initViewsById() {
        txtStatus = findViewById(R.id.txtStatus);
        btOn = findViewById(R.id.btOn);
        btOff = findViewById(R.id.btOff);
        btSearch = findViewById(R.id.btSearch);
        btList = findViewById(R.id.btList);
        lstView = findViewById(R.id.lstDevices);
    }

    private void updateStatus() {
        if (adapter == null) {
            btOn.setEnabled(false);
            btOff.setEnabled(false);
            btSearch.setEnabled(false);
            btList.setEnabled(false);
            txtStatus.setText("Status: Bluetooth is not supported on this device");
            return;
        } else if (adapter.isEnabled()) {
            txtStatus.setText("Status: Enable");
        } else {
            txtStatus.setText("Status: Disable");
        }
    }

    public void clickToTurnOn(View view) {
        if (!adapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, REQUEST_ENABLE_BT);
        } else {
            Toast.makeText(this, "Bluetooth is already on",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT || requestCode == REQUEST_VISIBLE_BT) {
            if (adapter.isEnabled()) {
                txtStatus.setText("Status: Enable");
                Toast.makeText(this, "Bluetooth is turn on",
                        Toast.LENGTH_LONG).show();
            } else {
                txtStatus.setText("Status: Disable");
            }
        }
    }

    public void clickToTurnOff(View view) {
        adapter.disable(); // required android.permission.BLUETOOTH_ADMIN
        txtStatus.setText("Status: Disable");
        Toast.makeText(this, "Bluetooth is turned off",
                Toast.LENGTH_LONG).show();
    }

    public void clickToVisible(View view) {
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        startActivityForResult(intent, REQUEST_VISIBLE_BT);
    }

    public void clickToListDevices(View view) {
        Set<BluetoothDevice> pairedDevices;

        btArrayAdapter.clear();
        listFindDevice.clear();
        btArrayAdapter.notifyDataSetChanged();

        pairedDevices = adapter.getBondedDevices();
        for (BluetoothDevice device : pairedDevices) {
            btArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            listFindDevice.add(device);
        }
        btArrayAdapter.notifyDataSetChanged();
        action = REMOVE_BOND_ACTION;
    }


    public void clickToSearch(View view) {
        listFindDevice.clear();
        btArrayAdapter.clear();

        action = CREATE_BOND_ACTION;

        new Thread(new Runnable() {
            @Override
            public void run() {
                if (adapter.isDiscovering()) {
                    adapter.cancelDiscovery();
                } else {
                    adapter.startDiscovery();
                    registerReceiver(broadcast,
                            new IntentFilter(BluetoothDevice.ACTION_FOUND));
                    status = REGISTERED_BROADCAST;
                }
            }
        }).start();
    }

    public void clickToTransfer(View view) {
        Intent intent = new Intent(this, FileTransferActivity.class);

        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (status == REGISTERED_BROADCAST) {
            unregisterReceiver(broadcast);
            status = UNREGISTERED_BROADCAST;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
    }
}
