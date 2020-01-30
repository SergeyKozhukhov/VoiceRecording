package ru.sergeykozhukhov.voicerecording.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.io.File;

import ru.sergeykozhukhov.voicerecording.audio_tools.AudioPlayer;
import ru.sergeykozhukhov.voicerecording.MainActivity;
import ru.sergeykozhukhov.voicerecording.R;

import static ru.sergeykozhukhov.voicerecording.MainActivity.BUNDLE_KEY_FILE;
import static ru.sergeykozhukhov.voicerecording.MainActivity.MSG_PAUSE_PLAYER;
import static ru.sergeykozhukhov.voicerecording.MainActivity.MSG_PLAY_PLAYER;
import static ru.sergeykozhukhov.voicerecording.MainActivity.MSG_STOP_PLAYER;

/**
 * Сервис воспроизведения записей
 */
public class PlayerService extends Service {

    private static final String LOG = "Player Service: ";

    /**
     * Идентификатор канала уведомлений
     */
    private static final String CHANNEL_ID = "CHANNEL_ID_2";

    /**
     * Шаг таймера
     */
    private static final long TIMER_PERIOD = 1000L;

    /**
     * Идентификатор уведомления
     */
    private static final int NOTIFICATION_ID = 2;

    /**
     * Действие, по которому производится остановка проигрывателя (при нажатии кнопки в уведомлении)
     */
    public static final String ACTION_STOP = "PLAYER_SERVICE_ACTION_STOP";

    /**
     * Действие, по которому производится resume/pause проигрывателя (при нажатии кнопки в уведомлении)
     */
    public static final String ACTION_PAUSE = "PLAYER_SERVICE_ACTION_PAUSE";

    /**
     * Сообщение activity на каждую секунду работы звука
     */
    public static final int MSG_PLAYER_TICK = 4;

    /**
     * Сообщение activity на получение длительности аудиофайла
     */
    public static final int MSG_PLAYER_GOT_DURATION = 5;

    /**
     * Сообщение activity об остановки звука
     */
    public static final int MSG_PLAYER_STOPPED = 6;

    /**
     * Сообщение activity о воспроизведении звука
     */
    public static final int MSG_PLAYER_STARTED_PLAY = 7;

    /**
     * Идентификатор для получения/передачи в activity оставшихся секунд до окончания таймера
     */
    public static final String BUNDLE_CURRENT_PROGRESS = "BUNDLE_CURRENT_PROGRESS";

    /**
     * Идентификатор для получения/передачи в activity длительности звукого файла
     */
    public static final String BUNDLE_FILE_DURATION = "BUNDLE_FILE_DURATION";

    /**
     * Обработчик поступающих сообщений от activity
     */
    private Messenger mServiceMessenger = new Messenger(new MessengerHandler());

    /**
     * Обработчик отправляемых сообщений в activity
     */
    private Messenger mainActivityMessenger;

    /**
     * Аудиоплеер
     */
    private AudioPlayer audioPlayer;

    /**
     * Прололжительность таймера
     */
    private long audioFileDuration;

    /**
     * Счетчик обратного отсчета (таймер)
     */
    private CountDownTimer mCountDownTimer;

    /**
     * Находится ли аудиоплеер в состоянии паузы
     */
    private boolean isPause = false;

    /**
     * Находится ли на данный момент афдиофайл в состоянии воспроизведения или паузы
     */
    private boolean isStartPlayer = false;

    /**
     * Оставшееся время таймера
     */
    private long leftTime;

    @Override
    public void onCreate() {
        super.onCreate();

        createNotificationChannel();
        initData();
    }

    private void initData(){
        audioPlayer = new AudioPlayer();
    }

    /**
     * Запуск серсива с уведомлением или завершение его в случае отмены пользователем.
     *
     * @param intent - интент сервиса
     * @param flags - флаги старта сервиса
     * @param startId - идентификатор
     * @return START_NOT_STICKY – сервис не будет перезапущен после того, как был убит системой
     *
     * startForeground(...) - назначение сервиса foreground.
     * Параметры: идентификатор уведомления и само уведомление.
     *
     * stopSelf() - аналог stopService(Intent)
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (ACTION_STOP.equals(intent.getAction())) {
            stopAudioFile();
        } else if(ACTION_PAUSE.equals(intent.getAction())) {
            pauseAudioFile();
        }
        else{
            Log.d(LOG, "OTHER");
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(LOG, "ON_DESTROY");
        stopAudioFile();

        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(LOG, "ON_BIND");
        return mServiceMessenger.getBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(LOG, "ON_UNBIND");
        return super.onUnbind(intent);
    }

    /**
     * Функция, создающая Notification.
     * @param currentTime - текущий показатель времени, отображаемый в тексте.
     * @return сконфигурированный объект Notification.
     *
     * ### Notification ###
     *
     * Notification - класс, хранящий информацию о виде уведомления, которое предоставляется пользователю.
     * E.g. значок, расширенное сообщение и дополнительные параметры настройки (звук и др.)
     *
     * ### Notification.Builder ###
     *
     * NotificationCompat.Builder - класс для создания объектов NotificationCompat.
     * Позволяет легко контролировать все флаги и легче создавать уведомления.
     * Параметры его конструктора: контекст для создания своего макета под уведомления и идентификатор канала.
     *
     * - setOnlyAlertOnce(true) - установка флага для вызова звукого, вибрационного оповещения только,
     * если notification еще не показывалось
     * - setAction - добавление action к notification.
     * Обычно отображается в виде кнопки. В этом случае по нажатию, производится остановка сервиса.
     * Параметры: иконка, текст, PendingIntent, который выполняется по нажатию на кнопку.
     * - setContentIntent - добавления PendingIntent, которое выполняется по нажатию на само уведомление.
     * В данном случае производится открытие MainActivity.
     * - builder.build() - установка всех обозначенных параметров и возвращение объекта Notification
     *
     * ### PendingIntent ###
     *
     * PendingIntent позволяет стороннему приложению (в которое его передали) запустить хранящийся внутри него Intent,
     * от имени того приложения (и теми же с полномочиями ), которое передало этот PendingIntent.
     *
     * - getActivity определяет тип activity, который будет вызван с помощью intent
     * - requestCode - идентификатор PendingIntent
     * - flags - флаги, влияющие на создание и поведение PendingIntent
     *
     * - setAction(ACTION_STOP) - установка действия на закрытие запущенного сервиса
     *
     * - PendingIntent.getService - получение PendingIntent, который позволяет запустить службу, подобно вызову startService()
     * Аргументы для сервиса получаются путем извлечения данных из переданного intent
     */
    private Notification createNotification(long currentTime) {

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID);

        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        Intent intentStopService = new Intent(this, PlayerService.class);
        intentStopService.setAction(ACTION_STOP);
        PendingIntent pendingIntentStopService = PendingIntent.getService(this, 0, intentStopService, 0);

        Intent intentPauseService = new Intent(this, PlayerService.class);
        intentPauseService.setAction(ACTION_PAUSE);
        PendingIntent pendingIntentPauseService = PendingIntent.getService(this, 0, intentPauseService, 0);

        int percentTime = secondsToPercent(currentTime);

        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.custom_notification);
        remoteViews.setOnClickPendingIntent(R.id.notif_stop_player_button, pendingIntentStopService);
        remoteViews.setOnClickPendingIntent(R.id.notif_pause_player_button, pendingIntentPauseService);
        remoteViews.setProgressBar(R.id.notif_indicator_progress_bar,100, percentTime,false);
        remoteViews.setTextViewText(R.id.notif_time_text_view, String.valueOf(currentTime));


        builder.setContentTitle(getString(R.string.player_service_notif_titile))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setCustomContentView(remoteViews)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setOnlyAlertOnce(true)
                .setContentIntent(pendingIntent);
        return builder.build();
    }

    /**
     * Создание канала для уведомлений
     *
     * Каналы доступны, начиная с версии android oreo (api 26)
     *
     * ### NotificationChannel ###
     *
     * NotificationChannel создается для набора однотипных уведомлений.
     * Позволяет пользователю для данного набора самостоятельно устанавливать настройки.
     * Параметры конструктора: идентификатор, имя, видимое пользователю, приоритет.
     *
     * - setDescription(...) - установка описания канала, видимое пользователю.
     *
     * ### NotificationManager ###
     *
     * NotificationManager — системный сервис Android, который управляет всеми уведомлениями.
     * IMPORTANCE_DEFAULT - стандартный приоритет уведомления. Производится звуковое оповещение и отображается сверху.
     *
     * - getSystemService(NotificationManager.class) - получение системного сервиса NotificationManager.
     *
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.player_service_channel_name);
            String description = getString(R.string.player_service_channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager =
                    getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    /**
     * Запуск таймера.
     *
     * Производится обновление уведомлений с учетом времени на каждом шаге таймера.
     *
     * @param time - время таймера
     * @param period - шаг отсчета
     *
     * ### CountDownTimer ###
     *
     * Параметры конструктора: продолжительность таймера и шаг отсчета.
     * OnTick(...) - действия каждый шаг отсчета.
     * OnFinish() - действие по завершению таймера.
     *
     *
     */
    private void startCountdownTimer(long time, long period) {
        mCountDownTimer = new CountDownTimer(time, period) {
            @Override
            public void onTick(long millisUntilFinished) {
                updateNotification(createNotification(millsToSeconds(millisUntilFinished)));
                sendCurrentSecond(millsToSeconds(millisUntilFinished));
                leftTime = millisUntilFinished;
                Log.d(LOG, "TIMER_TICK: ["+millsToSeconds(millisUntilFinished)+"] -> {"+millisUntilFinished+"}");
            }
            @Override
            public void onFinish() {
                stopAudioFile();
                Log.d(LOG, "TIMER_FINISH");
            }
        };

        mCountDownTimer.start();
        sendFileDuration();
    }

    /**
     * Отправление уведомления пользователю.
     * @param notification - уведомление для отображения.
     *
     * NotificationManagerCompat.from(Context context) - получение NotificationManager из библиотеки совместимости
     * для указанного контекста.
     *
     * notificationManager.notify(...) - отправление уведомления для показа пользователю.
     * Параметры: идентификатор уведомления и само уведомление.
     */
    private void updateNotification(@NonNull Notification notification) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    /**
     * Остановка таймера.
     */
    private void stopCountdownTimer() {
        if (mCountDownTimer != null && isStartPlayer) {
            mCountDownTimer.cancel();
            mCountDownTimer = null;
        }
    }

    /**
     * Передача сигнала о начале воспроизведения файла
     */
    private void sendPlayAudioFile(){
        Message message = Message.obtain(null, MSG_PLAYER_STARTED_PLAY);

        if (mainActivityMessenger != null){
            try {
                mainActivityMessenger.send(message);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Передача сигнала об окончании работы проигрывателя
     */
    private void sendStopAudioFile(){
        Message message = Message.obtain(null, MSG_PLAYER_STOPPED);

        if (mainActivityMessenger != null){
            try {
                mainActivityMessenger.send(message);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Воспроизведение звукового файла
     * @param file - аудио файл
     */
    private void startAudioFile(File file){
        if(isStartPlayer){
            Log.d(LOG, "isStartPlayer: "+String.valueOf(isStartPlayer));
            stopAudioFile();
        }

        audioPlayer.setAudioFile(file);
        audioPlayer.start();
        audioFileDuration = audioPlayer.getMediaPlayer().getDuration();
        Log.d(LOG, "Duration: "+String.valueOf(audioFileDuration));

        startCountdownTimer(audioFileDuration, TIMER_PERIOD);
        startForeground(NOTIFICATION_ID, createNotification(1000));
        isStartPlayer = true;
        sendPlayAudioFile();
    }

    /**
     * Пауза работы проигрывателя, если он воспроизведен и
     * возобновление его работы, если приостановлен
     */
    private void pauseAudioFile(){
        Log.d(LOG, "PAUSE");
        if (!isPause)
        {
            isPause = true;
            stopCountdownTimer();
            Log.d(LOG, "PAUSE - false");

        }else {
            isPause = false;
            startCountdownTimer(leftTime, TIMER_PERIOD);
            Log.d(LOG, "PAUSE - true");
        }
        audioPlayer.pause();
    }

    /**
     * Остановка проигрывателя
     */
    private void stopAudioFile(){

        audioPlayer.stop();
        stopCountdownTimer();

        isStartPlayer = false;
        if (isPause) {
            isPause = false;
        }

        sendStopAudioFile();
        stopForeground( true );
        Log.d(LOG, "STOP");
    }

    /**
     * Перевод миллисекунд в секунды.
     * @param time - время в милиссекундах.
     * @return время в секундах.
     */
    private long millsToSeconds(long time) {
        return time / 1000L;
    }

    /**
     * Преобразование из оставшихся секунд в процент выполнения таймера.
     *
     * @param sec - колличество секунд до финиша таймера.
     * @return процент выполнения таймера
     */
    private int secondsToPercent(long sec){
        Log.d(LOG, String.valueOf((int)((millsToSeconds(audioFileDuration)-sec)*100/millsToSeconds(audioFileDuration))));
        return (int)((millsToSeconds(audioFileDuration)-sec)*100/millsToSeconds(audioFileDuration));
    }

    /**
     * Передача секунд, оставшихся до окончания таймера
     * @param time - оставшееся время в секундах
     */
    private void sendCurrentSecond(long time) {
        Message message = Message.obtain(null, MSG_PLAYER_TICK);

        Bundle bundle = new Bundle();
        bundle.putLong(BUNDLE_CURRENT_PROGRESS, time);

        message.setData(bundle);

        if (mainActivityMessenger != null) {
            try {
                mainActivityMessenger.send(message);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Передача длительности установленного звукового файла
     */
    private void sendFileDuration(){
        Message message = Message.obtain(null, MSG_PLAYER_GOT_DURATION);

        Bundle bundle = new Bundle();
        bundle.putLong(BUNDLE_FILE_DURATION, audioFileDuration);

        message.setData(bundle);

        if (mainActivityMessenger != null){
            try {
                mainActivityMessenger.send(message);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Класс для обработки сообщений от activity
     */
    public class MessengerHandler extends Handler {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case MSG_PLAY_PLAYER:
                    Log.d(LOG, "START_WORK_MSG_PLAY");

                    File file = (File)msg.getData().getSerializable(BUNDLE_KEY_FILE);
                    mainActivityMessenger = msg.replyTo;
                    startAudioFile(file);

                    Log.d(LOG, "STOP_WORK_MSG_PLAY");
                    break;
                case MSG_PAUSE_PLAYER:
                    pauseAudioFile();
                    break;
                case MSG_STOP_PLAYER:
                    stopAudioFile();
                    break;
                default:
            }
        }
    }
}
