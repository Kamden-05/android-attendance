package com.example.androidattendanceapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends AppCompatActivity
{
    private static final String TAG = "MainActivity";
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothConnectionService mBluetoothConnection;
    private Button btnEnable;
    private Button btnDiscover;
    private Button btnStartConnection;
    private TextView discoverText;
    private EditText scannerText;
    private static final UUID mUUID = UUID.fromString("e0cbf06c-cd8b-4647-bb8a-263b43f0f974");
    private BluetoothDevice mBTDevice;
    private String inputMessage = "";
    public ArrayList<BluetoothDevice> mBTDevices = new ArrayList<>();

    // Create a BroadcastReceiver for ACTION_STATE_CHANGED
    private final BroadcastReceiver mBroadcastReceiver1 = new BroadcastReceiver()
    {
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED))
            {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

                switch(state)
                {
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "onReceive: STATE OFF");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING OFF");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "mBroadcastReceiver1: STATE ON");
                        btnEnable.setVisibility(Button.INVISIBLE);
                        btnDiscover.setVisibility(Button.VISIBLE);
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING ON");
                        break;
                }
            }
        }
    };

    /**
     * Broadcast Receiver for listing devices that are not yet paired
     * -Executed by btnDiscover() method.
     */
    private BroadcastReceiver mBroadcastReceiver2 = new BroadcastReceiver()
    {
        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent)
        {
            final String action = intent.getAction();

            if(action.equals(BluetoothDevice.ACTION_FOUND))
            {
                BluetoothDevice device = intent.getParcelableExtra (BluetoothDevice.EXTRA_DEVICE);
                Log.d(TAG, "Device found: " + device.getName() + ": " + device.getAddress());
                mBTDevices.add(device);
            }
            else if(action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED))
            {
                Log.d(TAG, "ACTION_DISCOVERY_FINISHED");
                if(!mBTDevices.isEmpty())
                {
                    BluetoothDevice device = mBTDevices.remove(0);
                    boolean result = device.fetchUuidsWithSdp();
                }
            }
            else if(action.equals(BluetoothDevice.ACTION_UUID))
            {
                BluetoothDevice deviceExtra = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Parcelable[] uuidExtra = intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID);
                Log.d(TAG, "Device Name and Address: " + deviceExtra.getName() + " " + deviceExtra.getAddress());
                boolean isFound = false;

                if(uuidExtra != null)
                {
                    for(Parcelable p : uuidExtra)
                    {
                        Log.d(TAG, "uuidExtra - " + p);
                        ParcelUuid uuid = (ParcelUuid) p;
                        if(uuid.getUuid().equals(mUUID))
                        {
                            isFound = true;
                            Log.d(TAG, "FOUND THE UUID");
                            deviceExtra.createBond();
                            mBluetoothAdapter.cancelDiscovery();
                            break;
                        }
                    }
                }
                else
                {
                    Log.d(TAG, "uuidExtra is still null");
                }

                if(mBTDevices.isEmpty())
                {
                    Log.d(TAG, "Device list is empty, target UUID not found");
                    discoverText.setVisibility(Button.INVISIBLE);
                    btnDiscover.setVisibility(Button.VISIBLE);
                }
                else if(!mBTDevices.isEmpty() && !isFound)
                {
                    Log.d(TAG, "Reached the end of loop");
                    BluetoothDevice device = mBTDevices.remove(0);
                    boolean result = device.fetchUuidsWithSdp();
                }
            }
        }
    };

    /**
     * Broadcast Receiver that detects bond state changes (Pairing status changes)
     */
    private final BroadcastReceiver mBroadcastReceiver3 = new BroadcastReceiver()
    {
        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent)
        {
            final String action = intent.getAction();

            if(action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED))
            {
                BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                // devices paired/bonded
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDED)
                {
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDED.");
                    mBTDevice = mDevice;
                    mBluetoothConnection = new BluetoothConnectionService(MainActivity.this);
                    discoverText.setVisibility(Button.INVISIBLE);
                    btnStartConnection.setVisibility(Button.VISIBLE);
                }
                // creating a bond between devices
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDING)
                {
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDING.");
                }
                // bond is broken/does not exist
                if (mDevice.getBondState() == BluetoothDevice.BOND_NONE)
                {
                    Log.d(TAG, "BroadcastReceiver: BOND_NONE.");
                }
            }
        }
    };

    // This handles whenever the other device sends information to the app device
    BroadcastReceiver inputReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String text = intent.getStringExtra("theMessage");
            inputMessage += text;
        }
    };

    @Override
    protected void onDestroy()
    {
        Log.d(TAG, "onDestroy: called.");
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver1);
        unregisterReceiver(mBroadcastReceiver2);
        unregisterReceiver(mBroadcastReceiver3);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnEnable = findViewById(R.id.btnEnable);
        btnDiscover = findViewById(R.id.btnDiscoverUnpairedDevices);
        mBTDevices = new ArrayList<>();
        btnStartConnection = findViewById(R.id.btnStartConnection);
        scannerText = findViewById(R.id.editText);
        discoverText = findViewById(R.id.discoverText);

        LocalBroadcastManager.getInstance(this).registerReceiver(inputReceiver, new IntentFilter("incomingMessage"));

        HandlerThread scannerThread = new HandlerThread("scannerThread");
        scannerThread.start();

        // start of bluetooth implementation
        // request permissions if needed
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH}, 1);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_ADMIN}, 1);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        }

        //Broadcasts when bond state changes (ie:pairing)
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mBroadcastReceiver3, filter);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        btnEnable.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                Log.d(TAG, "onClick: enabling/disabling bluetooth.");
                enableDisableBT();
            }
        });

        btnDiscover.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                Log.d(TAG, "onClick: Discover devices");
                btnDiscover();
            }
        });

        btnStartConnection.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                startBTConnection(mBTDevice, mUUID);
            }
        });

        Handler handler = new Handler(scannerThread.getLooper())
        {
            @Override
            public void handleMessage(@NonNull Message msg)
            {
                String inputText = (String) msg.obj;
                if(inputMessage != null && !inputMessage.isEmpty())
                {
                    String[] list = inputMessage.split("\n");
                    for(String s : list)
                    {
                        if(inputText.contains(s))
                        {
                            Toast.makeText(getApplicationContext(), "UTD Comet Card is valid", Toast.LENGTH_LONG).show();
                            mBluetoothConnection.write(s.getBytes());
                            break;
                        }
                    }
                }
                else
                {
                    Log.d(TAG, "List does not exist, connection is not working");
                }
            }
        };

        scannerText.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2)
            {
                // nothing needed here
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2)
            {
                // nothing needed here
            }

            @Override
            public void afterTextChanged(Editable editable)
            {
                scannerText.removeTextChangedListener(this);

                String input = editable.toString();
                System.out.println("This is what was captured:\n" + input);
                scannerText.setText("");
                Message message = handler.obtainMessage();
                message.obj = input;
                handler.sendMessage(message);

                scannerText.addTextChangedListener(this);
            }
        });
    }

    /**
     * Method that starts connection between devices
     */
    public void startBTConnection(BluetoothDevice device, UUID uuid)
    {

        Log.d(TAG, "startBTConnection: Initializing RFCOMM Bluetooth Connection.");
        btnStartConnection.setVisibility(Button.INVISIBLE);
        mBluetoothConnection.startClient(device,uuid);
        Handler handler = new Handler();
        Runnable runnable = new Runnable()
        {
            @Override
            public void run()
            {
                btnStartConnection.setVisibility(Button.INVISIBLE);
                scannerText.setVisibility(Button.VISIBLE);
                //scannerText.setInputType(InputType.TYPE_NULL);
                byte[] bytes = "*ID*".getBytes();
                mBluetoothConnection.write(bytes);
            }
        };
        handler.postDelayed(runnable, 5000);
    }

    @SuppressLint("MissingPermission")
    public void enableDisableBT()
    {
        if(mBluetoothAdapter == null)
        {
            Log.d(TAG, "enableDisableBT: Does not have BT capabilities.");
            finish();
        }
        else if(!mBluetoothAdapter.isEnabled())
        {
            Log.d(TAG, "enableDisableBT: enabling BT.");
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBTIntent);

            IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mBroadcastReceiver1, BTIntent);
        }
        else if(mBluetoothAdapter.isEnabled())
        {
            Log.d(TAG, "enableDisableBT: disabling BT.");
            mBluetoothAdapter.disable();

            IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mBroadcastReceiver1, BTIntent);
        }

    }

    @SuppressLint("MissingPermission")
    public void btnDiscover()
    {
        btnDiscover.setVisibility(Button.INVISIBLE);
        discoverText.setVisibility(Button.VISIBLE);
        Set<BluetoothDevice> bondedDevices = mBluetoothAdapter.getBondedDevices();
        if(bondedDevices.size() > 0)
        {
            Log.d(TAG, "Bonded Devices exist");
            for(BluetoothDevice device : bondedDevices)
            {
                if(device.getUuids() != null)
                {
                    for(ParcelUuid uuid : device.getUuids())
                    {
                        if(uuid.getUuid().equals(mUUID))
                        {
                            mBTDevice = device;
                            Log.d(TAG, "Device with UUID: " + mBTDevice.getName());
                            mBluetoothConnection = new BluetoothConnectionService(MainActivity.this);
                            mBluetoothAdapter.cancelDiscovery();
                            discoverText.setVisibility(Button.INVISIBLE);
                            btnStartConnection.setVisibility(Button.VISIBLE);
                            break;
                        }
                    }
                }
            }
        }
        else
        {
            Log.d(TAG, "btnDiscover: Looking for unpaired devices.");

            if (mBluetoothAdapter.isDiscovering())
            {
                mBluetoothAdapter.cancelDiscovery();
                Log.d(TAG, "btnDiscover: Canceling discovery.");
                checkBTPermission();

                mBluetoothAdapter.startDiscovery();
                IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                registerReceiver(mBroadcastReceiver2, discoverDevicesIntent);
            }
            else
            {
                checkBTPermission();
                mBluetoothAdapter.startDiscovery();
                IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                registerReceiver(mBroadcastReceiver2, discoverDevicesIntent);
            }

            IntentFilter discoverFinished = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            registerReceiver(mBroadcastReceiver2, discoverFinished);

            IntentFilter actionUUID = new IntentFilter(BluetoothDevice.ACTION_UUID);
            registerReceiver(mBroadcastReceiver2, actionUUID);
        }
    }

    private void checkBTPermission()
    {
        int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
        permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
        if(permissionCheck != 0)
        {
            this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001);
        }
    }

}
