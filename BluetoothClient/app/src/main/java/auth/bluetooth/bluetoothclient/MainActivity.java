package auth.bluetooth.bluetoothclient;


import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarMenu;
import com.google.android.material.navigation.NavigationBarView;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNavView = findViewById(R.id.bottom_nav_menu);

        bottomNavView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                if (item.getTitle().equals("unlock")){
                    getSupportFragmentManager()
                            .beginTransaction()
                            .disallowAddToBackStack()
                            .replace(R.id.frag_container, Connect.class, null, "UNLOCK")
                            .commit();
                }
                if (item.getTitle().equals("settings")){
                    getSupportFragmentManager()
                            .beginTransaction()
                            .disallowAddToBackStack()
                            .replace(R.id.frag_container, UserConfiguration.class, null)
                            .commit();
                }
                return true;
            }
        });
    }

}