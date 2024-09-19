package com.example.dmbcs;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;


import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class TerminalActivity extends AppCompatActivity {

    private static final String TAG = "DEBUG_TE";

    Button button_connect;
    Button buttonShare;
    Boolean bBTConnected = false;
    Spinner spinner;
    static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    BluetoothSocket BTSocket = null;
    BluetoothAdapter btAdapter = null;
    Set<BluetoothDevice> btDevice = null;
    BluetoothDevice device = null;
    TextView tvReceivedMessage;
    TextView textView1;
    TextView textView2;
    TextView textView3;
    ImageView battery1;
    ImageView battery2;
    classBTInitDataCommunication cBTInitSendReceive = null;
    private StringBuilder receivedData = new StringBuilder();
    private boolean alertShown = false;

    static public final int BT_CON_STATUS_NOT_CONNECTED = 0;
    static public final int BT_CON_STATUS_CONNECTING = 1;
    static public final int BT_CON_STATUS_CONNECTED = 2;
    static public final int BT_CON_STATUS_FAILED = 3;
    static public final int BT_CON_STATUS_CONNECTiON_LOST = 4;
    static public int iBTConnectionStatus = BT_CON_STATUS_NOT_CONNECTED;

    static final int BT_STATE_LISTENING = 1;
    static final int BT_STATE_CONNECTING = 2;
    static final int BT_STATE_CONNECTED = 3;
    static final int BT_STATE_CONNECTION_FAILED = 4;
    static final int BT_STATE_MESSAGE_RECEIVED = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terminal);

        Log.d(TAG, "onCreate-Start");

        button_connect = findViewById(R.id.button_connect);
        buttonShare = findViewById(R.id.button_share);
        spinner = findViewById(R.id.spinnerBTPairedDevices);
        tvReceivedMessage = findViewById(R.id.idMATextViewReceivedMessage);
        tvReceivedMessage.setMovementMethod(new ScrollingMovementMethod());
        textView1 = findViewById(R.id.tvbat1);
        textView2 = findViewById(R.id.tvbat2);
        textView3 = findViewById(R.id.tvtemp);
        battery1=findViewById(R.id.battery1);
        battery2=findViewById(R.id.battery2);

        button_connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "button_connect is clicked");
                if (bBTConnected == false) {
                    if (spinner.getSelectedItemPosition() == 0) {
                        Toast.makeText(getApplicationContext(), "Please select bluetooth device", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String SelectedDevice = spinner.getSelectedItem().toString();
                    Log.d(TAG, "Selected Device" + SelectedDevice);

                    for (BluetoothDevice BTDev : btDevice) {
                        if (SelectedDevice.equals(BTDev.getName())) {
                            device = BTDev;
                            Log.d(TAG, "Selected Device" + device.getAddress());
                            cBluetoothConnect cBTConnect = new cBluetoothConnect(device);
                            cBTConnect.start();
//                            try {
//                                Log.d(TAG, "Creating socket, my uuid " + MY_UUID);
//                                BTSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
//                                Log.d(TAG, "Connecting to device");
//                                BTSocket.connect();
//                                Log.d(TAG, "Connected");
//                                button_connect.setText("Disconnect");
//                                bBTConnected = true;
//
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                                Log.e(TAG, "Exception = " + e.getMessage());
//                                bBTConnected = false;
//                            }
                        }
                    }
                } else {
                    Log.d(TAG, "Disconnecting BTConnection");
                    if (BTSocket != null && BTSocket.isConnected()) {
                        try {
                            BTSocket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                            Log.d(TAG, "BTDisconnect Exp " + e.getMessage());
                        }
                    }
                    button_connect.setText("Connect");
                    bBTConnected = false;
                }
            }
        });
        buttonShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Button Click buttonShare");


                Log.d(TAG, "sharing : " + tvReceivedMessage.getText().toString());
                Intent intentShare = new Intent(Intent.ACTION_SEND);
                intentShare.setType("text/plain");
                intentShare.putExtra(Intent.EXTRA_SUBJECT, "Share BTTerminal message");
                intentShare.putExtra(Intent.EXTRA_TEXT, tvReceivedMessage.getText().toString());
                startActivity(Intent.createChooser(intentShare, "Sharing BT Terminal"));

            }
        });
    }

    @SuppressLint("MissingPermission")
    void getBTPairedDevices() {

        Log.d(TAG, "getBTPairedDevices - start ");
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null) {
            Log.e(TAG, "getBTPairedDevices , BTAdaptor null ");
            return;
        } else if (!btAdapter.isEnabled()) {
            Log.e(TAG, "getBTPairedDevices , BT not enabled");
            return;
        }

        btDevice = btAdapter.getBondedDevices();
        Log.d(TAG, "getBTPairedDevices , Paired devices count = " + btDevice.size());

        for (BluetoothDevice BTDev : btDevice) {
            Log.d(TAG, BTDev.getName() + ", " + BTDev.getAddress());
        }
    }

    void populateSpinnerWithBTPairedDevices() {
        ArrayList<String> alPairedDevices = new ArrayList<>();
        alPairedDevices.add("  SELECT BLUETOOTH");
        for (BluetoothDevice BTDev : btDevice) {
            alPairedDevices.add(BTDev.getName());
        }
        final ArrayAdapter<String> aaPairedDevices = new ArrayAdapter<String>(this, androidx.appcompat.R.layout.support_simple_spinner_dropdown_item, alPairedDevices);
        aaPairedDevices.setDropDownViewResource(androidx.appcompat.R.layout.support_simple_spinner_dropdown_item);
        spinner.setAdapter(aaPairedDevices);
    }

    @Override
    protected void onResume() {

        super.onResume();
        Log.d(TAG, "onResume-Resume");
        getBTPairedDevices();
        populateSpinnerWithBTPairedDevices();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void back(View view) {
        Log.d(TAG, "Back Button clicked");
        Intent i = new Intent(TerminalActivity.this, MainActivity.class);
        startActivity(i);
        finish();

    }

    public class cBluetoothConnect extends Thread {
        private BluetoothDevice device1;


        public cBluetoothConnect(BluetoothDevice device) {
            Log.i(TAG, "classBTConnect-start");

            device1 = device;
            try {
                BTSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (Exception exp) {
                Log.e(TAG, "classBTConnect-exp" + exp.getMessage());
            }
        }

        public void run() {
            try {
                BTSocket.connect();
                Message message = Message.obtain();
                message.what = BT_STATE_CONNECTED;
                handler.sendMessage(message);

            } catch (IOException e) {
                e.printStackTrace();
                Message message = Message.obtain();
                message.what = BT_STATE_CONNECTION_FAILED;
                handler.sendMessage(message);
            }
        }
    }

    public class classBTInitDataCommunication extends Thread {
        private final BluetoothSocket bluetoothSocket;
        private InputStream inputStream = null;
        private OutputStream outputStream = null;

        public classBTInitDataCommunication(BluetoothSocket socket) {
            Log.i(TAG, "classBTInitDataCommunication-start");

            bluetoothSocket = socket;

            try {
                inputStream = bluetoothSocket.getInputStream();
                outputStream = bluetoothSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "classBTInitDataCommunication-start, exp " + e.getMessage());
            }
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            while (BTSocket.isConnected()) {
                try {
                    bytes = inputStream.read(buffer);
                    handler.obtainMessage(BT_STATE_MESSAGE_RECEIVED, bytes, -1, buffer).sendToTarget();
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "BT disconnect from decide end, exp " + e.getMessage());
                    iBTConnectionStatus = BT_CON_STATUS_CONNECTiON_LOST;
                    try {
//disconnect bluetooth
                        Log.d(TAG, "Disconnecting BTConnection");
                        if (BTSocket != null && BTSocket.isConnected()) {

                            BTSocket.close();
                        }
                        button_connect.setText("Connect");
                        bBTConnected = false;
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }

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

    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {

            switch (msg.what) {
                case BT_STATE_LISTENING:
                    Log.d(TAG, "BT_STATE_LISTENING");
                    break;
                case BT_STATE_CONNECTING:
                    iBTConnectionStatus = BT_CON_STATUS_CONNECTING;
                    button_connect.setText("Connecting..");
                    Log.d(TAG, "BT_STATE_CONNECTING");
                    break;
                case BT_STATE_CONNECTED:

                    iBTConnectionStatus = BT_CON_STATUS_CONNECTED;

                    Log.d(TAG, "BT_CON_STATUS_CONNECTED");
                    button_connect.setText("Disconnect");

                    cBTInitSendReceive = new classBTInitDataCommunication(BTSocket);
                    cBTInitSendReceive.start();

                    bBTConnected = true;
                    break;
                case BT_STATE_CONNECTION_FAILED:
                    iBTConnectionStatus = BT_CON_STATUS_FAILED;
                    Log.d(TAG, "BT_STATE_CONNECTION_FAILED");
                    bBTConnected = false;
                    break;

                case BT_STATE_MESSAGE_RECEIVED:
                    byte[] readBuff = (byte[]) msg.obj;
                    String tempMsg = new String(readBuff, 0, msg.arg1);
                    Log.d(TAG, "Message receive ( " + tempMsg.length() + " ) data : " + tempMsg);
                    processData(tempMsg);
                    tvReceivedMessage.append(tempMsg);
                    break;
            }
            return true;
        }
    });
    double percentage1=0;
    double percentage2=0;

    private void processData(String data) {
        receivedData.append(data);
        // Assuming data is received in the format "value1 value2 value3"
        String[] values = receivedData.toString().split(" ");
        receivedData.setLength(0); // Clear the StringBuilder
        String v1=values[1];
        String v2=values[2];
        String temp=values[3];
        double tempValue = Double.parseDouble(temp);


        // Convert  value1 into percentage1

                try {
                    double floatValue = Double.parseDouble(v1);
                    percentage1 = (floatValue / 6.5) * 100; // Assuming the maximum value is 100
//                    percentageData.append(percentage).append("% ");
                } catch (NumberFormatException e) {
                    // Handle the case where the value cannot be parsed as a double
                    Log.e(TAG, "Error parsing value as double: " + v1, e);
                }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textView1.setText(String.format("Battery 1="+"%.2f%%", percentage1));
            }
        });

        // Convert value2 into percentage2

        try {
            double floatValue = Double.parseDouble(v2);
            percentage2 = (floatValue / 6.5) * 100; // Assuming the maximum value is 100
//            percentageData.append(percentage).append("% ");
        } catch (NumberFormatException e) {
            // Handle the case where the value cannot be parsed as a double
            Log.e(TAG, "Error parsing value as double: " + v2, e);
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textView2.setText(String.format("Battery 2="+"%.2f%%", percentage2));
                textView3.setText("Temperature ="+temp+"°C");
            }
        });

        if(percentage1<1){
            battery1.setImageResource(R.drawable.empty_battery);
        } else if (percentage1>=1 && percentage1<=25) {
            battery1.setImageResource(R.drawable.battery_25);
        }
        else if (percentage1>25 && percentage1<=50) {
            battery1.setImageResource(R.drawable.battery_50);
        }
        else if (percentage1>50 && percentage1<=75) {
            battery1.setImageResource(R.drawable.battery_75);
        }
        else {
            battery1.setImageResource(R.drawable.full_battery);
        }

        if(percentage2<1){
            battery2.setImageResource(R.drawable.empty_battery);
        } else if (percentage2>=1 && percentage2<=25) {
            battery2.setImageResource(R.drawable.battery_25);
        }
        else if (percentage2>25 && percentage2<=50) {
            battery2.setImageResource(R.drawable.battery_50);
        }
        else if (percentage2>50 && percentage2<=75) {
            battery2.setImageResource(R.drawable.battery_75);
        }
        else {
            battery2.setImageResource(R.drawable.full_battery);
        }

        if(tempValue>35 && !alertShown){
            alertShown = true;
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Temperature is above 35°C. Please take necessary precautions.")
                    .setTitle("High Temperature Alert")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // User clicked OK button, do nothing or handle as needed
                            dialog.dismiss(); // Dismiss the dialog
                        }
                    });
            AlertDialog dialog = builder.create();
            dialog.show();

        }
    }
        }




