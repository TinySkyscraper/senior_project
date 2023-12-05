package auth.bluetooth.bluetoothclient;


import static android.content.ContentValues.TAG;
import static android.content.Context.MODE_PRIVATE;
import static androidx.core.content.ContextCompat.checkSelfPermission;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.UUID;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link Connect#newInstance} factory method to
 * create an instance of this fragment.
 */
public class Connect extends Fragment {

    private UUID mUuid = UUID.fromString("94f39d29-7d6d-437d-973b-fba39e49d4ee");
    private SharedPreferences sharedPref;
    BluetoothAdapter mBluetoothAdapter;
    String mAddress;
    Thread mConnectThread;
    BluetoothSocket mmSocket;
    InputStream is;
    OutputStream os;

    public Connect() {
        // Required empty public constructor
    }
    public static Connect newInstance() {
        Connect fragment = new Connect();
        return fragment;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                new ActivityResultCallback<Boolean>() {
                    @Override
                    public void onActivityResult(Boolean result) {
                        if (result) {
                            Log.i("REQUEST GRANTED", "YAY");
                        } else {
                            Log.i("REQUEST DENIED", "BOOOO");
                        }
                    }
                }
        );
        requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH);
        requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT);
        requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_ADMIN);
        requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_ADVERTISE);
        requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_SCAN);
        requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_PRIVILEGED);

        // initialize username for testing
        sharedPref = getActivity().getPreferences(MODE_PRIVATE);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBluetoothAdapter.startDiscovery();
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        ContextCompat.registerReceiver(getContext(),
                receiver,
                filter,
                ContextCompat.RECEIVER_EXPORTED);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_connect, container, false);

        return view;
    }

    @Override
    public void onDestroy(){
        try {
            if(is != null) {
                is.close();
            }
            if (os != null) {
                os.close();
            }
            if(mmSocket != null) {
                mmSocket.close();
            }
            getContext().unregisterReceiver(receiver);
        }catch(Exception e){
            Log.e("TRIED TO DESTROY", e.toString());
        }
        super.onDestroy();
    }

    @SuppressLint("MissingPermission")
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String name = device.getName();
                if (name != null) {
                    if (name.equals("CryptoLock")) {
                        Log.i("FOUND DEVICE", "FOUND DEVICE!!");
                        mAddress = device.getAddress();
                        mConnectThread = new ConnectThread(mBluetoothAdapter.getRemoteDevice(mAddress));
                        mConnectThread.start();
                    }
                }
            }
        }
    };

    private byte[] encryptData(byte[] data){
        // Initialize vars
        byte[] encryptedBytes = null;
        String privateString = sharedPref.getString("privKey", "");
        byte[] bytes = new byte[privateString.length()/2];
        for (int i = 0; i < bytes.length; i++){
            String subString = privateString.substring(i*2, i*2+2);
            bytes[i] = new BigInteger(subString, 16).byteValue();
        }

        // Encrypt data
        try {
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(bytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey privateKey1 = keyFactory.generatePrivate(spec);
            Signature signature = Signature.getInstance("SHA256withRSA/PSS");
            signature.initSign(privateKey1);
            signature.update(data);
            encryptedBytes = signature.sign();
        }catch(Exception e){
            // Failed to encrypt data
            Log.e("CRYPTO ERROR", e.toString());
            return encryptedBytes;
        }
        // Successfully encrypted data
        return encryptedBytes;
    }

    @SuppressLint("MissingPermission")
    public class ConnectThread extends Thread{
        private final BluetoothDevice mmDevice;
        private String mSocketType;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                tmp = device.createInsecureRfcommSocketToServiceRecord(mUuid);

            }catch (Exception e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "create() failed", e);
            }
            mmSocket = tmp;
            Log.i("CONNECTION TYPE", Boolean.toString(mmSocket.getConnectionType() == BluetoothSocket.TYPE_RFCOMM));
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread SocketType:" + mSocketType);
            setName("ConnectThread" + mSocketType);

            // Always cancel discovery because it will slow down a connection
            mBluetoothAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                Log.i("CHECKING BOND STATE", Integer.toString(mmDevice.getBondState()));
                // This is a blocking call and will only return on a
                // successful connection or an exception
                Log.i("CONNECT INFO", "Trying to connect . . .");

                ImageView lock = getView().findViewById(R.id.lock_view);
                lock.setImageResource(R.drawable.outline_autorenew_24);

                mmSocket.connect();
                ReadData readData = new ReadData();
                readData.start();
            } catch (IOException e) {
                // Close the socket
                Log.i("ENTERED CATCH", e.toString());
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() " + mSocketType +
                            " socket during connection failure", e2);
                }
            }
        }

        public void cancel() {
            try {
                mConnectThread.interrupt();
            } catch (Exception e) {
                Log.e(TAG, "close() of connect " + mSocketType + " socket failed", e);
            }
        }
    }

    public class ReadData extends Thread{
        public void run(){
            try {
                boolean wait = true;
                is = mmSocket.getInputStream();
                os = mmSocket.getOutputStream();

                while (true){
                    int availableBytes = is.available();
                    if (availableBytes > 0) {
                        // Get challenge message
                        byte[] data = new byte[availableBytes];
                        is.read(data);

                        Log.i("DATA DATA DATA", new String(data, StandardCharsets.UTF_8));
                        String string_data = new String(data, StandardCharsets.UTF_8);

                        // Check for session end
                        if(string_data.equals("VERIFIED")){
                            ImageView lock = getView().findViewById(R.id.lock_view);
                            lock.setImageResource(R.drawable.outline_lock_open_24);
                            break;
                        } else if(string_data.equals("DENIED")){
                            ImageView lock = getView().findViewById(R.id.lock_view);
                            lock.setImageResource(R.drawable.outline_lock_24);
                            break;
                        }

                        String username = sharedPref
                                .getString("username", "")
                                .concat("EoU");

                        Log.i("TOO FAR", "You went beyond the if statements");
                        byte[] usernameBytes = username.getBytes();
                        byte[] encryptedBytes = encryptData(data);
                        byte[] combinedBytes = new byte[usernameBytes.length
                                + encryptedBytes.length];

                        for (int i = 0; i < combinedBytes.length; ++i) {
                            combinedBytes[i] = i < usernameBytes.length ?
                                    usernameBytes[i] :
                                    encryptedBytes[i - usernameBytes.length];
                        }

                        os.write(combinedBytes);
                        os.flush();

                    }
                }
            }catch(Exception e){
                Log.e("FAILED READ", e.toString());
            }
        }
    }
}