package hu.infokristaly.powerchargedetector;

import static androidx.core.content.IntentCompat.getSerializableExtra;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import hu.infokristaly.powerchargedetector.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    ActivityMainBinding binding;

    ActivityResultLauncher<Intent> MySettingstLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
//                if (result.getResultCode() == RESULT_OK) {
                    //Intent data = result.getData();
                    updateMQTTConnesctoin();
//                }
            });

    private void updateMQTTConnesctoin() {
        stopMyService();
        startMyService();
    }

    private void stopMyService() {
        Context context = getApplicationContext();
        Intent i = new Intent(context, MyService.class);
        stopService(i);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        binding.tvMessage.setText("Power change monitor service");

        Intent intent = new Intent(this,SettingsActivity.class);
        binding.btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MySettingstLauncher.launch(intent);
            }
        });
        startMyService();
    }

    private void startMyService() {
        Context context = getApplicationContext();
        Intent i = new Intent(context, MyService.class);

        boolean startForeground = false;
        if (startForeground && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(i);
        } else {
            startService(i);
        }
    }

}