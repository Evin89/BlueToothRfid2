package com.example.bluetoothrfid2;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final java.util.UUID mUUID = UUID.randomUUID();
    private static final String TAG = " Bluetooth App";

    Handler mHandler;
    final int RECEIVE_MESSAGE = 1;
    private static String address = "00:18:E5:04:B7:AC";

    private Button btOn,
            btOff,
            btShow,
            btScan;

    private ListView listView;

    private Long previousTime;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothSocket btSocket = null;
    private ConnectedThread  mConnectedThread;

    private Intent enableBluetoothIntent;
    private int REQUEST_ENABLE_BT;

    private ArrayList<String> stringArrayList = new ArrayList<String>();

    BroadcastReceiver mBroadcastReceiver;

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);



        btOn = (Button) findViewById(R.id.btOnBtn);
        btOff = (Button) findViewById(R.id.btOffBtn);
        btShow = (Button) findViewById(R.id.btShowBtn);
        btScan = (Button) findViewById(R.id.btScanBtn);
        listView = (ListView) findViewById(R.id.listView);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        REQUEST_ENABLE_BT = 1;

        String mUUID = UUID.randomUUID().toString();

        exeButton();
        bluetoothOnMethod();
        bluetoothOffMethod();

        // Floating action button
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothDevice.ACTION_FOUND.equals(action)){
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    stringArrayList.add(device.getName());
//                    ArrayAdapter(re)wz
                }
            }
        };

        mHandler = new Handler() {
            public void handleMessage(android.os.Message msg) {
                switch (msg.what) {
                    case RECEIVE_MESSAGE:                                                   // if receive massage
                        byte[] readBuf = (byte[]) msg.obj;
                        String strIncom = new String(readBuf, 0, msg.arg1);                 // create string from bytes array
                        Toast.makeText(getBaseContext(), strIncom, Toast.LENGTH_SHORT).show();
                        Log.d("INCOME", "INCOME: " + strIncom);

                        break;
                }
            };
        };

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();       // get Bluetooth adapter
        checkBTState();

    }

    // Function to create Bluetooth Socket for connection
    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        if(Build.VERSION.SDK_INT >= 10){
            try {
                final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", new Class[] { UUID.class });
                return (BluetoothSocket) m.invoke(device, mUUID);
            } catch (Exception e) {
                Log.e(TAG, "Could not create Insecure RFComm Connection",e);
            }
        }
        return  device.createRfcommSocketToServiceRecord(mUUID);
    }


    @Override
    public void onResume () {
        super.onResume();

        Log.d(TAG, "+++ onResume - Trying To Connect +++");

        // Pointer to BT device.
        BluetoothDevice btDevice = mBluetoothAdapter.getRemoteDevice(address);

        try {
            btSocket = createBluetoothSocket(btDevice);
        } catch (IOException e){
            errorExit("Fatal Error", "In onResume() socket failed to create" + e.getMessage() + "...");
        }

        mBluetoothAdapter.cancelDiscovery();

        Log.d(TAG, "+++ onResume: Connecting +++");

        try {
            Log.d(TAG,"aaaaa");
            btSocket.connect();
            Log.d(TAG, "+++ onResume: Connection OK +++");
        } catch (IOException e) {
            try {
                Log.d(TAG,""  + e.getMessage() + ".");
                Log.d(TAG, "CLOSING SOCKET AT ONRESUME");
                btSocket.close();
            } catch (IOException e2) {
                errorExit("Fatal Error", "In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
            }
        }

        Log.d(TAG, "+++ onResume: Creating Socket +++");

        mConnectedThread = new ConnectedThread(btSocket);
        mConnectedThread.start();

        previousTime = System.currentTimeMillis();
    }

    public void onPause() {
        super.onPause();

        Log.d(TAG, "...In onPause()...");

        try     {
            btSocket.close();
        } catch (IOException e2) {
            errorExit("Fatal Error", "In onPause() and failed to close socket." + e2.getMessage() + ".");
        }
    }
    // Function to check if BT is enabled on device.
    private void checkBTState() {
        if (mBluetoothAdapter == null){
            errorExit("Fatal Error", "Bluetooth not supported on this device");
        } else {
            if (mBluetoothAdapter.isEnabled()){
                Log.d(TAG, "+++ CheckBtState : Bluetooth Is On  +++");
            } else {
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent, 1);
            }
        }
    }

    //The BroadcastReceiver that listens for bluetooth broadcasts
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equalsIgnoreCase("android.bluetooth.BluetoothDevice.ACTION_ACL_CONNECTED")) {
                Log.d("TAG","Bluetooth connect");
            }
        }
    };

    public void exeButton() {
        btScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBluetoothAdapter.startDiscovery();
                Toast.makeText(getApplicationContext(), "Scanning for devices.", Toast.LENGTH_LONG).show();
                IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                registerReceiver(mReceiver, intentFilter);

            }
        });

        btShow.setOnClickListener(new View.OnClickListener() {
            private final String Tag = "BT";

            @Override
            public void onClick(View v) {

                Set<BluetoothDevice> bt = mBluetoothAdapter.getBondedDevices();
                String[] strings = new String[bt.size()];
                int index = 0;

                if (bt.size()>0){
                    for (BluetoothDevice device : bt){
                        strings[index] = device.getName();
                        Log.d(Tag, device.getName());
                        index++;
                    }

                    ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, strings);
                    listView.setAdapter(arrayAdapter);
                }
            }
        });
    }



    private void bluetoothOffMethod() {
        btOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBluetoothAdapter.isEnabled()){
                    mBluetoothAdapter.disable();
                    Toast.makeText(getApplicationContext(), "Bluetooth is disabled.", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void bluetoothOnMethod() {
        btOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBluetoothAdapter == null){
                    // Device doesnt support BT.
                    Toast.makeText(getApplicationContext(), "This device doesn't support Bluetooth.", Toast.LENGTH_LONG).show();
                } else {
                    if (!mBluetoothAdapter.isEnabled()){
                        // If device BT is enabled.
                        startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BT);
                    }
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(getApplicationContext(), "Bluetooth is enabled.", Toast.LENGTH_LONG).show();
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(getApplicationContext(), "Bluetooth is enabling is cancelled.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

//    @Override
//    protected void onPostResume() {
//        super.onPostResume();
//
//        Log.d("A", "+++ onPostResume: Resuming +++");
//        BluetoothDevice btDevice = mBluetoothAdapter.getRemoteDevice(address);
//
//        try {
//            btSocket = createBluetoothSocket(btDevice);
//            Log.d(TAG, "+++ onPostResume: Connection OK");
//        } catch (IOException e) {
//
//                errorExit("Fatal Error", "In onResume() and socket create failed: " + e.getMessage() + ".");
//            }
//        }
//
//        mBluetoothAdapter.cancelDiscovery();
//
//        Log.d(TAG, "+++ onPostResume: Connecting+++");
//
//        try {
//            btSocket.connect();
//            Log.d(TAG, "+++ onPostResume: Connection OK+++");
//        } catch (IOException e) {
//            try {
//                btSocket.close();
//            } catch (IOException e2) {
//                errorExit("Fatal Error", "In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
//            }
//        }
//
//        Log.d(TAG, "+++ onPostResume: Create Socket+++");
//
//        mConnectedThread = new ConnectedThread(btSocket);
//        mConnectedThread.start();
//
//        previousTime = System.currentTimeMillis();
//
//    }

    // Function for toasting errormessages to screen.
    private void errorExit(String title, String message){
        Toast.makeText(getBaseContext(), title + " - " + message, Toast.LENGTH_LONG).show();
        finish();
    }

    // Function to connect and get inputsteam.
    private class ConnectedThread extends Thread {
        private final InputStream mInputStream;

        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            try{
                tmpIn = socket.getInputStream();
            }
            catch (IOException e){}
            mInputStream = tmpIn;
        }

        public void Run (){
            byte[] buffer = new byte[256];
            int bytes;

            while (true){
                if (System.currentTimeMillis( ) - previousTime > 10000) {
                    previousTime = System.currentTimeMillis();
                }
                    try {
                        bytes = mInputStream.read(buffer);
                        mHandler.obtainMessage(RECEIVE_MESSAGE, bytes, -1, buffer).sendToTarget();
                    }
                    catch (IOException e){
                        break;
                    }
            }
        }
    }

    // Function to connect and send DATA
    private class connect_and_send extends AsyncTask<Void, Void, Void>{

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }



        @Override
        protected Void doInBackground(Void... voids) {

            try {
                Log.d(TAG, "doInBackground: closing socket ");
                btSocket.close();
                Log.d(TAG, "... ALERT: SOCKET CLOSED...");
            } catch (IOException e) {
                errorExit("FATAL ERROR:", "In onResume() and unable to close socket during connection failure" + e.getMessage() + ".");
                Log.d(TAG, "Impossible to close the socket");
            }

            Log.d(TAG, ".. Onresume trying to connect..");

            // Pointer to BT device
            BluetoothDevice btDevice = mBluetoothAdapter.getRemoteDevice(address);

            try {
                btSocket = createBluetoothSocket(btDevice);
            } catch (IOException e) {
                errorExit("Fatal Error", "In onResume() and socket create failed: " + e.getMessage() + ".");
            }

            // Cancel Discovery when it isn't needed.
            mBluetoothAdapter.cancelDiscovery();

            // Establish the connection.  This will block until it connects.
            Log.d(TAG, "...Connecting...");
            try {
                btSocket.connect();
                Log.d(TAG, "....Connection ok...");
            } catch (IOException e) {
                try {
                    Log.d(TAG, "CLOSING SOCKET AT 2nd thingy");
                    btSocket.close();
                } catch (IOException e2) {
                    errorExit("Fatal Error", "In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
                }
            }

            // Create a data stream so we can talk to server.
            Log.d(TAG, "...Create Socket...");

            mConnectedThread = new ConnectedThread(btSocket);
            mConnectedThread.start();


            previousTime = System.currentTimeMillis();
            return null;




        }
        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }
}
