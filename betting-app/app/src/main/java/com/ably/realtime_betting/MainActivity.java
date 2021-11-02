package com.ably.realtime_betting;

import android.os.Bundle;
import android.view.View;
import android.view.Menu;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import com.ably.realtime_betting.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);
        binding.appBarMain.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    private AblyRealtime ablyRealtime;
    private TextView rollingLogs;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        rollingLogs = findViewById(R.id.rolling_logs);
        try {
            initAblyRuntime();
        } catch (AblyException e) {
            logMessage("AblyException " + e.getMessage());
        }
    }
    /**
     * Step 1: Initialize Ably Runtime
     *
     * @throws AblyException
     */
    private void initAblyRuntime() throws AblyException {
        ClientOptions options = new ClientOptions();
        options.key = "YOUR_API_KEY";
        options.clientId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        ablyRealtime = new AblyRealtime(options);
        ablyRealtime.setAndroidContext(getApplicationContext());
        ablyRealtime.connect();
        LocalBroadcastManager.getInstance(this).registerReceiver(pushReceiver, new IntentFilter("io.ably.broadcast.PUSH_ACTIVATE"));
        LocalBroadcastManager.getInstance(this).registerReceiver(pushReceiver, new IntentFilter(AblyPushMessagingService.PUSH_NOTIFICATION_ACTION));
        ablyRealtime.connection.on(new ConnectionStateListener() {
            @Override
            public void onConnectionStateChanged(ConnectionStateChange state) {
                logMessage("Connection state changed to : " + state.current.name());
                switch (state.current) {
                    case connected:
                        logMessage("Connected to Ably with clientId " + ablyRealtime.auth.clientId);
                        break;
                }
            }
        });
    }
    private void logMessage(String message) {
        Log.i(MainActivity.class.getSimpleName(), message);
        rollingLogs.append(message);
        rollingLogs.append("\n");
    }

    public void activatePush(View view) {
        try {
            ablyRealtime.push.activate();
        } catch (AblyException e) {
            logMessage("AblyException activating push: " + e.getMessage());
        }
    }

    public void deactivatePush(View view) {
        try {
            logMessage("Deactivating Push on device");
            ablyRealtime.push.deactivate();
        } catch (AblyException e){
            logMessage("AblyException deactivating push: " + e.getMessage());
        }
    }

    private BroadcastReceiver pushReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("io.ably.broadcast.PUSH_ACTIVATE".equalsIgnoreCase(intent.getAction())) {
                ErrorInfo error = IntentUtils.getErrorInfo(intent);
                if (error!=null) {
                    logMessage("Error activating push service: " + error);
                    return;
                }
                try {
                    logMessage("Device is now registered for push with deviceId " + deviceId());
                    subscribeChannels();
                } catch(AblyException e) {
                    logMessage("AblyException getting deviceId: " + e);
                }
                return;
            }
            if (AblyPushMessagingService.PUSH_NOTIFICATION_ACTION.equalsIgnoreCase(intent.getAction())) {
                logMessage("Received Push message");
            }
        }
    };
    private String deviceId() throws AblyException {
        return ablyRealtime.device().id;
    }
    private void subscribeChannels() {
        ablyRealtime.channels.get("push:test_push_channel").push.subscribeClientAsync(new CompletionListener() {
            @Override
            public void onSuccess() {
                logMessage("Subscribed to push for the channel");
            }
            @Override
            public void onError(ErrorInfo reason) {
                logMessage("Error subscribing to push channel " + reason.message);
                logMessage("Visit link for more details: " + reason.href);
            }
        });
    }
}