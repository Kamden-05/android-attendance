package com.example.androidattendanceapp;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.UUID;

public class BluetoothConnectionService
{
    private static final String TAG = "BluetoothConnectionService";
//    private static final String appName = "Android Attendance App";
//    private static final UUID MY_UUID_INSECURE = UUID.fromString("90302c06-3c92-4b60-9b43-08ca2a66ab3b");
//    private AcceptThread mInsecureAcceptThread;
    private final BluetoothAdapter mBluetoothAdapter;
    Context mContext;
    private ConnectThread mConnectThread;
    private BluetoothDevice mmDevice;
    private UUID deviceUUID;

    private ConnectedThread mConnectedThread;

    public BluetoothConnectionService(Context context)
    {
        mContext = context;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        start();
    }

//    /**
//     * This thread runs while listening for incoming connections. It behaves
//     * like a server-side client. It runs until a connection is accepted
//     * (or until cancelled).
//     */
//    private class AcceptThread extends Thread
//    {
//
//        // The local server socket
//        private final BluetoothServerSocket mmServerSocket;
//
//        @SuppressLint("MissingPermission")
//        public AcceptThread()
//        {
//            BluetoothServerSocket tmp = null;
//
//            // Create a new listening server socket
//            try
//            {
//                tmp = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(appName, MY_UUID_INSECURE);
//
//                Log.d(TAG, "AcceptThread: Setting up Server using: " + MY_UUID_INSECURE);
//            }
//            catch (IOException e)
//            {
//                Log.e(TAG, "AcceptThread: IOException: " + e.getMessage() );
//            }
//
//            mmServerSocket = tmp;
//        }
//
//        public void run()
//        {
//            Log.d(TAG, "run: AcceptThread Running.");
//
//            BluetoothSocket socket = null;
//
//            try
//            {
//                // This is a blocking call and will only return on a
//                // successful connection or an exception
//                Log.d(TAG, "run: RFCOM server socket start.....");
//
//                socket = mmServerSocket.accept();
//
//                Log.d(TAG, "run: RFCOM server socket accepted connection.");
//
//            }
//            catch (IOException e)
//            {
//                Log.e(TAG, "AcceptThread: IOException: " + e.getMessage());
//            }
//
//            //talk about this is in the 3rd
//            if(socket != null)
//            {
//                connected(socket);
//            }
//
//            Log.i(TAG, "END mAcceptThread ");
//        }
//
//        public void cancel()
//        {
//            Log.d(TAG, "cancel: Canceling AcceptThread.");
//            try
//            {
//                mmServerSocket.close();
//            }
//            catch (IOException e)
//            {
//                Log.e(TAG, "cancel: Close of AcceptThread ServerSocket failed. " + e.getMessage() );
//            }
//        }
//
//    }

    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread
    {
        private BluetoothSocket mmSocket;

        public ConnectThread(BluetoothDevice device, UUID uuid)
        {
            Log.d(TAG, "ConnectThread: started.");
            mmDevice = device;
            deviceUUID = uuid;
        }

        @SuppressLint("MissingPermission")
        public void run()
        {
            BluetoothSocket tmp = null;
            Log.i(TAG, "RUN mConnectThread ");

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try
            {
                Log.d(TAG, "ConnectThread: Trying to create InsecureRfcommSocket using UUID: " + deviceUUID.toString());
                tmp = mmDevice.createRfcommSocketToServiceRecord(deviceUUID);
            }
            catch (IOException e)
            {
                Log.e(TAG, "ConnectThread: Could not create InsecureRfcommSocket " + e.getMessage());
            }

            mmSocket = tmp;
            if(mmSocket == null)
            {
                Log.d(TAG,"Socket does not exist");
            }
            else
            {
                Log.d(TAG, "Socket exists: " + mmSocket.toString());
            }

            // Always cancel discovery because it will slow down a connection
            mBluetoothAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try
            {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();

                Log.d(TAG, "run: ConnectThread connected.");
            }
            catch (IOException e)
            {
                // Close the socket
                e.printStackTrace();
                try
                {
                    mmSocket.close();
                    Log.d(TAG, "run: Closed Socket.");
                }
                catch (IOException e1)
                {
                    Log.e(TAG, "mConnectThread: run: Unable to close connection in socket " + e1.getMessage());
                }
                Log.d(TAG, "run: ConnectThread: Could not connect to UUID: " + deviceUUID);
            }

            connected(mmSocket);
        }
        public void cancel()
        {
            try
            {
                Log.d(TAG, "cancel: Closing client Socket.");
                mmSocket.close();
            }
            catch (IOException e)
            {
                Log.e(TAG, "cancel: close() of mmSocket in ConnectThread failed. " + e.getMessage());
            }
        }
    }

    /**
     * Start the connection process, clears out any previously made connectThread and connectedThread
     */
    public synchronized void start()
    {
        Log.d(TAG, "Starting connection process");

        if (mConnectThread != null)
        {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if(mConnectedThread != null)
        {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

//        if (mInsecureAcceptThread == null)
//        {
//            mInsecureAcceptThread = new AcceptThread();
//            mInsecureAcceptThread.start();
//        }
    }

    /**
     Method to instantiate and start connectThread
     ConnectThread starts and attempts to make a connection with the other device.
     **/

    public void startClient(BluetoothDevice device,UUID uuid)
    {
        Log.d(TAG, "startClient: Started.");

        mConnectThread = new ConnectThread(device, uuid);
        mConnectThread.start();
    }

    /**
     Finally the ConnectedThread which is responsible for maintaining the BTConnection, Sending the data, and
     receiving incoming data through input/output streams respectively.
     **/
    private class ConnectedThread extends Thread
    {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket)
        {
            Log.d(TAG, "ConnectedThread: Starting.");

            mmSocket = socket;
            InputStream tempIn = null;
            OutputStream tempOut = null;

            try
            {
                tempIn = mmSocket.getInputStream();
                tempOut = mmSocket.getOutputStream();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

            mmInStream = tempIn;
            mmOutStream = tempOut;
        }

        public void run()
        {
            byte[] buffer = new byte[1024];  // buffer store for the stream

            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true)
            {
                // Read from the InputStream
                try
                {
                    bytes = mmInStream.read(buffer);
                    String incomingMessage = new String(buffer, 0, bytes);
                    Log.d(TAG, "InputStream: " + incomingMessage);

                    // this is to capture the message to be accessed by MainActivity
                    Intent incomingMessageIntent = new Intent("incomingMessage");
                    incomingMessageIntent.putExtra("theMessage", incomingMessage);
                    LocalBroadcastManager.getInstance(mContext).sendBroadcast(incomingMessageIntent);
                }
                catch (IOException e)
                {
                    Log.e(TAG, "write: Error reading Input Stream. " + e.getMessage() );
                    break;
                }
            }
        }

        //Call this from the main activity to send data to the remote device
        public void write(byte[] bytes)
        {
            String text = new String(bytes, Charset.defaultCharset());
            Log.d(TAG, "write: Writing to output stream: " + text);
            try
            {
                mmOutStream.write(bytes);
            } catch (IOException e)
            {
                Log.e(TAG, "write: Error writing to output stream. " + e.getMessage() );
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel()
        {
            try
            {
                mmSocket.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    private void connected(BluetoothSocket mmSocket)
    {
        Log.d(TAG, "connected: Starting.");

        // Start the thread to manage the connection and perform input/output transitions
        mConnectedThread = new ConnectedThread(mmSocket);
        mConnectedThread.start();
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     *
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out)
    {
        // Create temporary object
        ConnectedThread r;

        // Synchronize a copy of the ConnectedThread
        Log.d(TAG, "write: Write Called.");
        //perform the write
        mConnectedThread.write(out);
    }

}
























