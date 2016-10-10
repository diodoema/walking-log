package id.makeithappen.walkinglog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener{

    SwipeRefreshLayout swipeRefreshLayout;
    TextView tvStep, tvPercent, tvDuration, tvDistance, tvCalorie, tvFrequency;
    FloatingActionButton fabMapView;
    ToggleButton tbStartStop;

    Context context = this;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Set default value for preference if user never open the setting menu
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setColorSchemeColors(Color.MAGENTA);
        swipeRefreshLayout.setDistanceToTriggerSync(500);
        swipeRefreshLayout.setSize(SwipeRefreshLayout.DEFAULT);
        swipeRefreshLayout.setOnRefreshListener(this);

        tvStep = (TextView) findViewById(R.id.tvStep);
        tvPercent = (TextView) findViewById(R.id.tvPercent);
        tvDuration = (TextView) findViewById(R.id.tvDuration);
        tvDistance = (TextView) findViewById(R.id.tvDistance);
        tvCalorie = (TextView) findViewById(R.id.tvCalorie);
        tvFrequency = (TextView) findViewById(R.id.tvFrequency);

        fabMapView = (FloatingActionButton) findViewById(R.id.fabMapView);

        tbStartStop = (ToggleButton) findViewById(R.id.tbStartStop);

        SharedPreferences sp = getSharedPreferences("id.makeithappen.walkinglog", Context.MODE_PRIVATE);
        tbStartStop.setChecked(sp.getBoolean("pref", false));
    }

    @Override
    public void onResume() {
        super.onResume();
        registerReceiver(broadcastReceiver,  new IntentFilter(BackgroundService.BROADCAST_ACTION));
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(broadcastReceiver);
    }

    @Override
    public void onRefresh() {
        if(BackgroundService.isRunning) {
            new AlertDialog.Builder(context)
                    .setTitle("Caution")
                    .setMessage("Are you sure want to restart?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            BackgroundService.isRestart = true;
                            stopService(new Intent(context, BackgroundService.class));
                            startService(new Intent(context, BackgroundService.class));
                        }
                    })
                    .setNegativeButton("No", null)
                    .show();
        }
        swipeRefreshLayout.setRefreshing(false);
    }

    /**
     * This method would be call when user press START & STOP button
     */
    public void serviceOnOff(View view){
        if(isFeatureAvailable()){
            if(!BackgroundService.isRunning){
                Intent startIntent = new Intent(this, BackgroundService.class);
                startService(startIntent);

                SharedPreferences.Editor editor = getSharedPreferences("id.makeithappen.walkinglog", Context.MODE_PRIVATE).edit();
                editor.putBoolean("pref", true);
                editor.apply();
            }
            else{
                Intent stopIntent = new Intent(this, BackgroundService.class);
                stopService(stopIntent);

                SharedPreferences.Editor editor = getSharedPreferences("id.makeithappen.walkinglog", Context.MODE_PRIVATE).edit();
                editor.putBoolean("pref", false);
                editor.apply();
            }
        }
        else{
            tbStartStop.setChecked(false);
            Toast.makeText(this, "Sorry, your phone not support this app", Toast.LENGTH_LONG).show();
        }
    }

    // Check if user smartphone support this app
    private boolean isFeatureAvailable() {
        boolean result;
        PackageManager pm = getPackageManager();
        int APIversion = Build.VERSION.SDK_INT;
        result = APIversion >= 19 && pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_STEP_COUNTER);
        return result;
    }

    // Broadcast Receiver
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            updateUI(intent);
        }
    };

    // Update value on main screen
    private void updateUI(Intent intent){
        String step = intent.getStringExtra("step");
        String percentage = intent.getStringExtra("percentage");
        String duration_hour = intent.getStringExtra("duration_hour");
        String duration_min = intent.getStringExtra("duration_min");
        String duration_sec = intent.getStringExtra("duration_sec");
        String distance = intent.getStringExtra("distance");
        String calorie = intent.getStringExtra("calorie");
        String frequency = intent.getStringExtra("frequency");

        tvStep.setText(step);
        tvPercent.setText("Target: "+ percentage + "%");
        tvDuration.setText("Duration\n" + duration_hour + ":" + duration_min + ":" + duration_sec + " sec");
        tvDistance.setText("Distance\n" + distance + " km");
        tvCalorie.setText("Calorie Burn\n" + calorie + " cal");
        tvFrequency.setText("Frequency\n" + frequency + " step/hr");
    }

    /**
     * This method would be call when user press TRACK button
     */
    public void trackView(View view) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        if (pref.getBoolean("pref_key_track", false)) {
            Intent i = new Intent(this, TrackActivity.class);
            startActivity(i);
        } else {
            Toast.makeText(this, "You must enable 'Show Track' on Setting", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent intent1 = new Intent(this, SettingActivity.class);
                this.startActivity(intent1);
                return true;
            case R.id.action_history:
                Intent intent2 = new Intent(this, HistoryActivity.class);
                this.startActivity(intent2);
                return true;
            case R.id.action_about:
                Intent intent3 = new Intent(this, AboutActivity.class);
                this.startActivity(intent3);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
