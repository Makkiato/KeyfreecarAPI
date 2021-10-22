package kr.co.keyfreecar.apitest;

import android.Manifest;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import kr.co.keyfreecar.api.KeyfreecarBluetoothManager;
import kr.co.keyfreecar.api.KeyfreecarConnector;


public class MainActivity extends AppCompatActivity {

    private final String Logtag = "apitest.MainActivity";

    private BottomNavigationView bottomNavigationView;


    static KeyfreecarBluetoothManager kbm;
    static KeyfreecarConnector connected;


    LinearLayout mainContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 1200);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.mainBottomNav);
        mainContent = findViewById(R.id.mainContent);





        bottomNavigationView.setOnItemSelectedListener(item -> {
            FragmentManager fm = getSupportFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();
            switch(item.getItemId()){
                case R.id.remoteAndSearch:


                    fm.popBackStack("remote",0);
                    ft.addToBackStack("remote");
                    ft.replace(R.id.mainContent,new searchAndRemote()).commit();
                    getLayoutInflater();

                    break;
                case R.id.status:
                    if(connected != null) {
                        fm.popBackStack("settings", 0);
                        ft.addToBackStack("settings");
                        getSupportFragmentManager().beginTransaction().replace(R.id.mainContent, new Settings()).commit();

                    }
                    break;
            }

            return false;
        });
        bottomNavigationView.setOnItemReselectedListener(null);

        bottomNavigationView.setSelectedItemId(R.id.remoteAndSearch);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

}