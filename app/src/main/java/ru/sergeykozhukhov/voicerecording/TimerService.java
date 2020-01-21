package ru.sergeykozhukhov.voicerecording;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.os.Build;
import android.os.CountDownTimer;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class TimerService extends Service {

    /**
     * Тэг для логов.
     */
    private static final String TAG = "TimerService";

    /**
     * Идентификатор канала уведомлений.
     */
    private static final String CHANNEL_ID = "channel_id_2";

    /**
     * Шаг таймера.
     */
    private static final long TIMER_PERIOD = 1000L;

    /**
     * Идентификатор уведомления.
     */
    private static final int NOTIFICATION_ID = 1;

    private static int sNotificationID = 0;

    /**
     * Действие интента, по которому производится остановка сервиса.
     */
    public static final String ACTION_CLOSE = "TIMER_SERVICE_ACTION_CLOSE";

    public static final String SETTED_TIME_COUNDOWN = "time_countdown";

    public static final String FILE_PATH = "file_path";

    private Dictaphone dictaphone;


    /**
     * Прололжительность таймера.
     */
    private long timeCountdown;

    /**
     * Счетчик обратного отсчета (таймер)
     */
    private CountDownTimer mCountDownTimer;

    @Override
    public void onCreate() {
        super.onCreate();

        createNotificationChannel();
        initDictaphone();

        Log.d(TAG, "onCreate() called");
    }

    private void initDictaphone(){
        dictaphone = new Dictaphone(this);
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
        Log.d(TAG, "onStartCommand() called with: intent = [" + intent + "], flags = [" + flags + "], startId = [" + startId + "]");

        if (ACTION_CLOSE.equals(intent.getAction())) {
            stopSelf();
            dictaphone.recordStop();
        } else {

            timeCountdown = intent.getLongExtra(SETTED_TIME_COUNDOWN, 0);
            startCountdownTimer(timeCountdown, TIMER_PERIOD);

            startForeground(NOTIFICATION_ID, createNotification(1000));
            dictaphone.recordStart();
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.d(TAG, "onDestroy() called");

        stopCountdownTimer();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
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
     * - setAction(ACTION_CLOSE) - установка действия на закрытие запущенного сервиса
     *
     * - PendingIntent.getService - получение PendingIntent, который позволяет запустить службу, подобно вызову startService()
     * Аргументы для сервиса получаются путем извлечения данных из переданного intent
     */
    private Notification createNotification(long currentTime) {


        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID);

        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        Intent intentCloseService = new Intent(this, TimerService.class);
        intentCloseService.setAction(ACTION_CLOSE);
        PendingIntent pendingIntentCloseService = PendingIntent.getService(this, 0, intentCloseService, 0);

        int percentTime = secondsToPercent(currentTime);

        builder.setContentTitle(getString(R.string.timer_service_content_title))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setProgress(100, percentTime , false)
                .setContentText(getString(R.string.timer_service_content_description) + currentTime + " (" + percentTime+"%)")
                .setOnlyAlertOnce(true)
                .addAction(0, getString(R.string.button_stop_service), pendingIntentCloseService)
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
            CharSequence name = getString(R.string.notification_channel_name);
            String description = getString(R.string.notification_channel_description);
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
                Log.d(TAG, "onTick() called with: millisUntilFinished = [" + millsToSeconds(millisUntilFinished) + "]");

//                startForeground(NOTIFICATION_ID, createNotification(millsToSeconds(millisUntilFinished)));
                updateNotification(createNotification(millsToSeconds(millisUntilFinished)));
            }

            @Override
            public void onFinish() {
                Log.d(TAG, "onFinish() called");

                stopSelf();
            }
        };

        mCountDownTimer.start();
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
        if (mCountDownTimer != null) {
            mCountDownTimer.cancel();
            mCountDownTimer = null;
        }
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
        return (int)((millsToSeconds(timeCountdown)-sec)*100/millsToSeconds(timeCountdown));
    }
}
