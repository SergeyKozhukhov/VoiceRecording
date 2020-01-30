package ru.sergeykozhukhov.voicerecording;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

import ru.sergeykozhukhov.voicerecording.adapter.OnItemAudioFileClickListener;
import ru.sergeykozhukhov.voicerecording.adapter.AudioFilesAdapter;
import ru.sergeykozhukhov.voicerecording.services.FileManagerService;
import ru.sergeykozhukhov.voicerecording.services.PlayerService;

import ru.sergeykozhukhov.recorderservice.IRecorderServiceAIDL;

public class MainActivity extends AppCompatActivity{

    /**
     * Объект сервиса по управлению файлами
     */
    private FileManagerService fileMangerService;

    /**
     * Соединение с RecorderService
     */
    private RecorderServiceConnection recorderServiceConnection;


    /**
     * Соединение с FileManagerService
     */
    private FileMangerServiceConnection fileMangerServiceConnection;

    /**
     * Соединение с PlayerService
     */
    private ServiceConnection playerServiceConnection;

    /**
     * Механизм взаимодействия PlayerService с MainActivity посредством обмена сообщений
     * MainActivity - передатчик, PlayerService - приемник сообщений
     */
    private Messenger playerServiceMessenger;

    /**
     * Механизм взаимодействия с PlayerService посредством обмена сообщений
     * MainActivity - приемник сообщений, PlayerService - передатчик
     */
    private Messenger mainActivityMessenger;

    /**
     * Есть ли подключение к PlayerService
     */
    private boolean boundPlayerService = false;

    /**
     * Сообщение сервису для воспроизведения звука
     */
    public static final int MSG_PLAY_PLAYER = 1;

    /**
     * Сообщение сервису для остановки звука
     */
    public static final int MSG_STOP_PLAYER = 2;

    /**
     * Сообщение сервису для паузы
     */
    public static final int MSG_PAUSE_PLAYER = 3;

    /**
     * Идентификатор для получения/передачи файла на воспроизведение от activity
     */
    public static final String BUNDLE_KEY_FILE = "BUNDLE_KEY_FILE";

    /**
     * Список звуковых файлов
     */
    private RecyclerView audioFilesRecyclerView;

    /**
     * Адаптер для списка звуковых файлов
     */
    private AudioFilesAdapter audioFilesAdapter;

    /**
     * Запись/остановка записи нового звукового файла
     */
    private ImageButton recordRecordServiceImageButton;

    /**
     * Pause/resume звукового файла
     */
    private ImageButton pausePlayerServiceImageButton;

    /**
     * Остановка воспроизведения звукового файла
     */
    private ImageButton stopPlayerServiceImageButton;

    /**
     * Кнопка для обновления списка файлов
     */
    private ImageButton updateListImageButton;
    /**
     * Кнопка для удаления всех записанных файлов из директории
     */
    private ImageButton deleteDirectoryImageButton;

    /**
     * Текущее время воспроизвдения файла
     */
    private SeekBar indicatorProgressSeekBar;

    /**
     * Текущее время воспроизвдения файла
     */
    private TextView indicatorProgressTextView;

    /**
     * Обработчик нажатий на список звуковых файлов
     */
    private OnItemAudioFileClickListener onItemAudioFileClickListener;

    /**
     * Директория для сохранения файлов
     */
    private File directoryAudioFiles;


    /**
     * Длительность воспроизводимого звукового файла
     */
    private long audioFileDuration;

    /**
     * Производится ли в данный момент запись файлов
     */
    private boolean isRecord = false;

    /**
     * Было ли произведено воспроизвдение файла (false - если проигрыватель не начинал запись или уже остановлен)
     */
    private boolean isStartPlayer = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkPermission();
        getDirectoryAudioFiles();
        initMessengers();
        initPlayerServiceConnection();
        initViews();
        initListeners();
        initData();
    }

    @Override
    protected void onStart() {
        super.onStart();
        bindFileManagerService();
        bindPlayerService();
    }

    @Override
    protected void onStop() {
        unbindPlayerService();
        unbindFileManagerService();
        super.onStop();
    }

    /**
     * Запрос на разрешение записи файлов на карту памяти
     */
    private void requestPermission(){
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                1);
    }

    /**
     * Проверка имеется ли разрешение на запись файлов на карту памяти
     * Если отсутствует, запрашивает без обработки ответа от пользователя
     * @return true если разрешение есть
     *         false если нет
     */
    private boolean checkPermission(){
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            requestPermission();
            return false;
        }
        return true;
    }

    /**
     * Получении директории в которую, будут писаться звуковые файлы
     * Если есть доступ к карте памяти, то директории подготавливается на ней
     * В ином случае, она создается в корне пакета, но сервис по записи, не сможет создавать в ней объекты
     */
    private void getDirectoryAudioFiles(){
        File directory;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())){
            directory = Environment.getExternalStorageDirectory();
        }else {
            directory = ContextCompat.getDataDir(MainActivity.this);
        }

        directoryAudioFiles = new File(directory.getPath() + "/recordsTest/");
        if (!directoryAudioFiles.exists()){
            if(!directoryAudioFiles.mkdir()) {
                directoryAudioFiles = directory;
            }
        }
    }

    private void initPlayerServiceConnection(){
        recorderServiceConnection = new RecorderServiceConnection();
        fileMangerServiceConnection = new FileMangerServiceConnection();

        playerServiceConnection = new ServiceConnection(){

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                playerServiceMessenger = new Messenger(service);
                boundPlayerService = true;
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                playerServiceMessenger = null;
                boundPlayerService = false;
            }
        };
    }

    private void initMessengers(){
        playerServiceMessenger = null;
        mainActivityMessenger = new Messenger(new IncomingHandlerMainActivity());
    }

    /**
     * Инициализация Views
     */
    private void initViews() {

        audioFilesRecyclerView = findViewById(R.id.recording_list_recycler_view);

        recordRecordServiceImageButton = findViewById(R.id.start_record_service_button);
        pausePlayerServiceImageButton = findViewById(R.id.pause_player_service_button);
        stopPlayerServiceImageButton = findViewById(R.id.stop_player_service_button);

        updateListImageButton = findViewById(R.id.update_list_image_button);
        deleteDirectoryImageButton = findViewById(R.id.delete_directory_image_button);

        indicatorProgressSeekBar = findViewById(R.id.indicator_seek_bar);
        indicatorProgressTextView = findViewById(R.id.indicator_text_view);
    }

    /**
     * Инициализация обработчиков
     */
    private void initListeners(){
        recordRecordServiceImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isRecord)
                {
                    bindRecorderService();
                    isRecord = true;
                    recordRecordServiceImageButton.setImageDrawable(getDrawable(R.drawable.ic_stop));
                }
                else {
                    unbindRecorderService();
                    isRecord = false;
                    recordRecordServiceImageButton.setImageDrawable(getDrawable(R.drawable.ic_record_voice));
                }
            }
        });

        pausePlayerServiceImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isStartPlayer){
                    Message msg = Message.obtain(null, MSG_PAUSE_PLAYER, 0, 0);
                    try{
                        playerServiceMessenger.send(msg);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        stopPlayerServiceImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isStartPlayer) {
                    Message msg = Message.obtain(null, MSG_STOP_PLAYER, 0, 0);
                    try {
                        playerServiceMessenger.send(msg);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        updateListImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isStartPlayer){
                    Toast.makeText(MainActivity.this, getString(R.string.message_toast_stop_player), Toast.LENGTH_SHORT).show();
                }
                else if (isRecord){
                    Toast.makeText(MainActivity.this, getString(R.string.message_toast_stop_recorder), Toast.LENGTH_SHORT).show();
                }
                else{
                    audioFilesAdapter.setFiles(fileMangerService.getFiles());
                    Toast.makeText(MainActivity.this, getString(R.string.message_toast_list_updated), Toast.LENGTH_SHORT).show();
                }
            }
        });

        deleteDirectoryImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fileMangerService.deleteFromExternalStorage();
            }
        });

        onItemAudioFileClickListener = new OnItemAudioFileClickListener() {
            @Override
            public void onPlayAudioFile(@NonNull File file) {
                if (boundPlayerService && !isRecord){
                    Message msg = Message.obtain(null, MSG_PLAY_PLAYER, 0, 0);

                    Bundle bundle = new Bundle();
                    bundle.putSerializable(BUNDLE_KEY_FILE, file);

                    msg.setData(bundle);
                    msg.replyTo = mainActivityMessenger;

                    try{
                        playerServiceMessenger.send(msg);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }

                }
            }
        };
    }

    /**
     * Инициализация данных
     */
    private void initData(){

        audioFilesRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        audioFilesAdapter = new AudioFilesAdapter(onItemAudioFileClickListener);
        audioFilesRecyclerView.setAdapter(audioFilesAdapter);
    }

    /**
     * Подключение к FileManagerService
     */
    private void bindFileManagerService() {
        Intent intent = new Intent(this, FileManagerService.class);
        bindService(intent, fileMangerServiceConnection, BIND_AUTO_CREATE);
    }

    /**
     * Подключение к PlayerService
     */
    private void bindPlayerService(){
        Intent intent = new Intent(this, PlayerService.class);
        bindService(intent, playerServiceConnection, Context.BIND_AUTO_CREATE);
    }

    /**
     * Подключение к RecorderService
     */
    private void bindRecorderService(){
        ComponentName componentName = new ComponentName("ru.sergeykozhukhov.recorderservice",
                "ru.sergeykozhukhov.recorderservice.RecorderService");
        Intent intentR = new Intent();
        intentR.setComponent(componentName);

        bindService(intentR, recorderServiceConnection, BIND_AUTO_CREATE);
    }

    /**
     * Отключение от FileManagerService
     */
    private void unbindFileManagerService(){
        unbindService(fileMangerServiceConnection);
        fileMangerService = null;
    }

    /**
     * Отключение от PlayerService
     */
    private void unbindPlayerService(){
        if (boundPlayerService){
            unbindService(playerServiceConnection);
            boundPlayerService = false;
        }
    }

    /**
     * Отключение от RecorderService
     */
    private void unbindRecorderService(){
        unbindService(recorderServiceConnection);
    }

    /**
     * Преобразование из милисекунд в секунды
     * @param time - время в милисекундах
     * @return время в секундах
     */
    private long millsToSeconds(long time) {
        return time / 1000L;
    }

    /**
     * Преобразование из секунд в процент пройденного времени
     * @param sec время в секундах
     * @return процент пройденного времени
     */
    private int secondToPercent(long sec){
        if (audioFileDuration < 1000L)
            return 1;
        Log.d("MAIN_ACTIVITY", "Duration (seconds to percent): "+audioFileDuration);
        return (int)((millsToSeconds(audioFileDuration)-sec)*100/millsToSeconds(audioFileDuration));
    }

    /**
     * Механизм взаимодействия с PlayerService посредством обмена сообщений
     * MainActivity - приемник сообщений, PlayerService - передатчик
     */
    public class IncomingHandlerMainActivity extends Handler {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case PlayerService.MSG_PLAYER_TICK:
                    long currentSecond = msg.getData().getLong(PlayerService.BUNDLE_CURRENT_PROGRESS);
                    long secondFromStart = millsToSeconds(audioFileDuration) - currentSecond;
                    indicatorProgressTextView.setText(String.format(getString(R.string.indicator_time_text_view), secondFromStart, secondToPercent(currentSecond)));
                    indicatorProgressSeekBar.setProgress(secondToPercent(currentSecond));
                    break;
                case PlayerService.MSG_PLAYER_GOT_DURATION:
                    audioFileDuration = msg.getData().getLong(PlayerService.BUNDLE_FILE_DURATION);
                    break;
                case PlayerService.MSG_PLAYER_STARTED_PLAY:
                    isStartPlayer = true;
                    recordRecordServiceImageButton.setVisibility(View.GONE);
                    indicatorProgressTextView.setVisibility(View.VISIBLE);
                    indicatorProgressSeekBar.setVisibility(View.VISIBLE);
                    pausePlayerServiceImageButton.setVisibility(View.VISIBLE);
                    stopPlayerServiceImageButton.setVisibility(View.VISIBLE);
                     break;
                case PlayerService.MSG_PLAYER_STOPPED:
                    isStartPlayer = false;
                    recordRecordServiceImageButton.setVisibility(View.VISIBLE);
                    indicatorProgressTextView.setVisibility(View.GONE);
                    indicatorProgressSeekBar.setVisibility(View.GONE);
                    pausePlayerServiceImageButton.setVisibility(View.GONE);
                    stopPlayerServiceImageButton.setVisibility(View.GONE);
                    Log.d("MAIN_ACTIVITY", "stop");
                default:
            }
        }
    }

    /**
     * Соединение с сервисом RecorderService
     * ServiceConnection - интерфейс, определеяющий состояние сервиса
     */
    private class RecorderServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            IRecorderServiceAIDL recorderServiceAIDL = IRecorderServiceAIDL.Stub.asInterface(service);
            try {
                recorderServiceAIDL.setDirectory(directoryAudioFiles.getPath());
                recorderServiceAIDL.startRecord();

            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    }

    /**
     * Соединение с сервисом FileManagerService
     * ServiceConnection - интерфейс, определеяющий состояние сервиса
     */
    private class FileMangerServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            checkPermission();
            fileMangerService = ((FileManagerService.LocalFilerManagerServiceBinder) service).getFileManagerService();
            fileMangerService.setDirectoryAudioFiles(directoryAudioFiles);
            audioFilesAdapter.setFiles( fileMangerService.getFiles());
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    }


}
