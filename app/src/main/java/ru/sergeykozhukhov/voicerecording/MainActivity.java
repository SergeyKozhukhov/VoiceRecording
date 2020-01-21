package ru.sergeykozhukhov.voicerecording;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";

    private RecyclerView recordingList;


    private Button startDictaphone;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.start_service_button).setOnClickListener(this);
        findViewById(R.id.stop_service_button).setOnClickListener(this);

        requestPermission();
        init();
    }

    private void init(){
        recordingList = findViewById(R.id.recording_list_recycler_view);

        File directory = ContextCompat.getDataDir(this);

        recordingList.setLayoutManager(new LinearLayoutManager(this));

        RecordingListAdapeter recordingListAdapeter = new RecordingListAdapeter();
        recordingListAdapeter.setFiles(getFiles(directory));
        recordingList.setAdapter(recordingListAdapeter);

        // dictaphone = new Dictaphone(this);
        // startDictaphone = findViewById(R.id.start_dictaphone_button);
        // startDictaphone.setOnClickListener(new View.OnClickListener() {
        /*    @Override
            public void onClick(View v) {
                dictaphone.recordStart();
            }
        });*/
    }

    private void requestPermission(){
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO},
                11);
    }

    public List<File> getFiles(File currentDirectory){

        File[] files = currentDirectory.listFiles();
        if(files == null)
            return null;
        return new ArrayList<>(Arrays.asList(files));
    }




    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.start_service_button:
                Log.d(TAG, "start_service_button clicked");

                startService();
                break;

            case R.id.stop_service_button:
                Log.d(TAG, "stop_service_button clicked");
                // dictaphone.recordStop();
                // dictaphone.playStart();

                stopService();
            default:

                break;
        }
    }

    private void startService() {
        Intent intent = new Intent(this, TimerService.class);
        long timeCountdown = 1000 * 10L;
        intent.putExtra(TimerService.SETTED_TIME_COUNDOWN, timeCountdown);
        intent.putExtra(TimerService.FILE_PATH, ContextCompat.getDataDir(this));
        startService(intent);
    }

    private void stopService() {
//        Intent intent = new Intent(this, TimerService.class);
//        stopService(intent);

        Intent intent = new Intent(this, TimerService.class);
        intent.setAction(TimerService.ACTION_CLOSE);

        startService(intent);
    }

}
