package ducdh8.seminar.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

public class FileTransferActivity extends AppCompatActivity {

    Button btnBrowse, btnListDevices, btnListen, btnSend;
    TextView txtFilePath, txtStatus;
    BluetoothAdapter adapter;
    BluetoothDevice[] btArray;
    ListView listView;
    Uri uri;

    private final int REQUEST_ENABLE_BLUETOOTH = 1;
    public static final int STATE_LISTENING = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;
    public static final int STATE_CONNECTION_FAILED = 4;
    public static final int STATE_MESSAGE_RECEIVED = 5;

    public static final String APP_NAME = "BTTransferring";
    public static final UUID MY_UUID =
            UUID.fromString("8ce255c0-223a-11e0-ac64-0803450c9a66");

    SendReceive sendReceive;

    // Dùng Handler để tương tác với Message truyen tu Handler cua
    // thiec bi nay den Handler cua thiec bi khac.
    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {

            switch (msg.what)
            {
                case STATE_LISTENING:
                    txtStatus.setText("Listening");
                    break;
                case STATE_CONNECTING:
                    txtStatus.setText("Connecting");
                    break;
                case STATE_CONNECTED:
                    txtStatus.setText("Connected");
                    break;
                case STATE_CONNECTION_FAILED:
                    txtStatus.setText("Connection Failed");
                    break;
                case STATE_MESSAGE_RECEIVED:
                    byte[] readBuff = (byte[]) msg.obj;
                    String tempMsg = new String(readBuff,0, msg.arg1);

                    String[] temp = tempMsg.split("\\*", 1);
                    String type = temp[0];
                    String content = temp[1];
                    String fileName = (new Date()).toString() + "." + type;

                    try (FileOutputStream fos =
                                 openFileOutput(fileName, MODE_PRIVATE)) {
                        saveToInternal(fos, content);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    txtFilePath.setText(content);
                    break;
            }
            return true;
        }
    });

    private void saveToInternal(FileOutputStream fos, String content) {
        OutputStreamWriter osw = null;
        try {
            osw = new OutputStreamWriter(fos);
            osw.write(content);
            osw.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_transfer);

        initViewByIds();

        adapter = BluetoothAdapter.getDefaultAdapter();

        if (adapter != null) {
            if (!adapter.isEnabled()) {
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent, REQUEST_ENABLE_BLUETOOTH);
            }
        }

        implementListeners();
    }

    private void initViewByIds() {
        btnBrowse = findViewById(R.id.btnBrowse);
        btnListDevices = findViewById(R.id.btnListDevices);
        btnListen = findViewById(R.id.btnListen);
        btnSend = findViewById(R.id.btnSend);

        listView = findViewById(R.id.listView);

        txtFilePath = findViewById(R.id.txtFilePath);
        txtStatus = findViewById(R.id.txtStatus);
    }

    private void implementListeners() {
        btnListDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Set<BluetoothDevice> bt = adapter.getBondedDevices();
                String[] strings = new String[bt.size()];
                btArray = new BluetoothDevice[bt.size()];
                int index = 0;

                if (bt.size() > 0) {
                    for (BluetoothDevice device : bt) {
                        btArray[index] = device;
                        strings[index] = device.getName();
                        index++;
                    }
                    ArrayAdapter<String> arrayAdapter =
                            new ArrayAdapter<>(getApplicationContext(),
                                    android.R.layout.simple_list_item_1, strings);
                    listView.setAdapter(arrayAdapter);
                }
            }
        });

        btnListen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ServerClass serverClass = new ServerClass();
                serverClass.start();
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ClientClass client = new ClientClass(btArray[position]);
                client.start();
                txtStatus.setText("Connecting");
            }
        });

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    String content = getFileContentFromUri(uri);
                    String fileType = getFileTypeFromUri(uri, getContentResolver());
                    String message = fileType + "*" + content;
                    sendReceive.write(message.getBytes());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private String getFileTypeFromUri(Uri u, ContentResolver resolver) {
        String type = "";
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        type = mime.getExtensionFromMimeType(resolver.getType(u));
        System.out.println(">>>>> type: " + type);
        System.out.println(">>>>> resolver: " + resolver.getType(u));
        // With "data.txt" will this will return ".txt"
        // resolver.getType(uri) will return "text/plain"
        return type;
    }

    private String getFileContentFromUri(Uri u) throws Exception {
        InputStream is = getApplicationContext().getContentResolver()
                .openInputStream(u);
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        StringBuffer sb = new StringBuffer();
        String temp = "";
        while ((temp = br.readLine()) != null) {
            sb.append(temp);
        }
        return sb.toString();
    }

    public void clickToBrowse(View view) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("text/*");
        startActivityForResult(intent, 10);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case 10:
                if (resultCode == RESULT_OK) {
                    String path = data.getData().getLastPathSegment();
                    uri = data.getData();
                    txtFilePath.setText(path);
                }
                break;
        }
    }

    private class ServerClass extends Thread {

        private BluetoothServerSocket serverSocket;

        public ServerClass() {
            try {
                serverSocket =
                        adapter.listenUsingRfcommWithServiceRecord(APP_NAME, MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            BluetoothSocket socket = null;

            while (socket == null) {
                try {

                    Message message = Message.obtain();
                    message.what = STATE_CONNECTING;
                    handler.sendMessage(message);

                    socket = serverSocket.accept();
                } catch (IOException e) {
                    e.printStackTrace();
                    Message message = Message.obtain();
                    message.what = STATE_CONNECTION_FAILED;
                    handler.sendMessage(message);
                }

                if(socket != null) {
                    Message message = Message.obtain();
                    message.what = STATE_CONNECTED;
                    handler.sendMessage(message);

                    sendReceive = new SendReceive(socket);
                    sendReceive.start();
                    break;
                }
            }
        }
    }

    private class ClientClass extends Thread {
        private BluetoothDevice device;
        private BluetoothSocket socket;

        public ClientClass (BluetoothDevice serverDevice) {
            device = serverDevice;

            try {
                socket = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {
            try {
                socket.connect();
                Message message = Message.obtain();
                message.what = STATE_CONNECTED;
                handler.sendMessage(message);

                sendReceive = new SendReceive(socket);
                sendReceive.start();

            } catch (IOException e) {
                e.printStackTrace();
                Message message = Message.obtain();
                message.what = STATE_CONNECTION_FAILED;
                handler.sendMessage(message);
            }
        }
    }

    private class SendReceive extends Thread {
        private final BluetoothSocket bluetoothSocket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public SendReceive (BluetoothSocket socket) {
            bluetoothSocket = socket;
            InputStream tempIn = null;
            OutputStream tempOut = null;

            try {
                tempIn = bluetoothSocket.getInputStream();
                tempOut = bluetoothSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            inputStream = tempIn;
            outputStream = tempOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int size;

            while (true) {
                try {
                    size = inputStream.read(buffer);
                    handler.obtainMessage(STATE_MESSAGE_RECEIVED, size,-1, buffer)
                            .sendToTarget();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void write(byte[] bytes) {
            try {
                outputStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
