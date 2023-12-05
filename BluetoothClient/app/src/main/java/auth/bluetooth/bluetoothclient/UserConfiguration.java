package auth.bluetooth.bluetoothclient;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;

import android.os.Environment;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link UserConfiguration#newInstance} factory method to
 * create an instance of this fragment.
 */
public class UserConfiguration extends Fragment {

    SharedPreferences sharedPref;

    public UserConfiguration() {
        // Required empty public constructor
    }
    public static UserConfiguration newInstance(String param1, String param2) {
        UserConfiguration fragment = new UserConfiguration();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
    } // onCreate END

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_user_configuration, container, false);

        String user = sharedPref.getString("username", "");

        Button keyGenButton = view.findViewById(R.id.key_gen_button);
        EditText enter_username = view.findViewById(R.id.enter_username);
        TextView username_display = view.findViewById(R.id.username_display);
        Button set_username = view.findViewById(R.id.set_username);

        if (!user.equals("")){
            username_display.setText(user);
            enter_username.setHint(user);
        }

        set_username.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String username = enter_username.getText().toString();

                username_display.setText(username);
                enter_username.setHint(username);

                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString("username", username);
                editor.commit();
            }
        });

        keyGenButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setMessage("Are you sure?").setPositiveButton("Yes", dialogClickListener)
                        .setNegativeButton("No", dialogClickListener).show();
            }
        });

        return view;
    } //onCreateView END

    private void genKey(){
        try {
            // Generate key pair
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            KeyPair kp = kpg.generateKeyPair();
            PrivateKey privateKey = kp.getPrivate();
            PublicKey publicKey = kp.getPublic();
            Log.i("PUBKEY FORMAT", publicKey.getFormat());

            // strings that hold keys for storage in preferences
            String privateString = "";
            String publicString = "";


            byte[] privateBytes = privateKey.getEncoded();
            byte[] publicBytes = publicKey.getEncoded();

            privateString = new BigInteger(1, privateBytes).toString(16);
            publicString = new BigInteger(1, publicBytes).toString(16);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("privKey", privateString);
            editor.putString("pubKey", publicString);
            editor.commit();

            String username = sharedPref.getString("username", "");

            if (!username.equals("")) {

                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.putExtra(Intent.EXTRA_TITLE, username + ".pub");
                intent.setType("application/pkcs8");

                // Optionally, specify a URI for the directory that should be opened in
                // the system file picker when your app creates the document.
                intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI,
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS));

                activityLauncher.launch(intent);

            }else{
                Toast toast = new Toast(getContext());
                toast.setText("CREATE USERNAME FIRST");
                toast.show();
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    } // genKey END

    ActivityResultLauncher<Intent> activityLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Log.i("SUCCESS", "FILE CREATED");
                        Intent data = result.getData();
                        try {
                            OutputStream outputStream = getActivity()
                                    .getContentResolver()
                                    .openOutputStream(data.getData());
                            String pubKey = sharedPref.getString("pubKey", "");
                            byte[] bytes = new byte[pubKey.length()/2];
                            for (int i = 0; i < bytes.length; i++){
                                String subString = pubKey.substring(i*2, i*2+2);
                                Log.i("byte string", subString);
                                bytes[i] = new BigInteger(subString, 16).byteValue();
                            }
                            outputStream.write(bytes);
                            outputStream.flush();
                        }catch(Exception e){
                            Log.e("FILE ERROR", e.toString());
                        }
                    }else{
                        Log.i("FAILURE", "FILE NOT CREATED");
                    }
                }
            });

    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which){
                case DialogInterface.BUTTON_POSITIVE:
                    genKey();
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    Toast toast_no = new Toast(getContext());
                    toast_no.setText("Key Generation Aborted");
                    toast_no.show();
                    break;
            }
        }
    };


}