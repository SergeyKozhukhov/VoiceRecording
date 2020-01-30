package ru.sergeykozhukhov.voicerecording.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ru.sergeykozhukhov.voicerecording.MainActivity;
import ru.sergeykozhukhov.voicerecording.R;

/**
 * Сервис для взаимодействия с файлами
 */
public class FileManagerService extends Service {

    /**
     * Идентификатор канала
     */
    private static final String CHANNEL_ID = "CHANNEL_ID_1";

    /**
     * Идентификатор уведомлений
     */
    private static final int NOTIFICATION_ID = 1;

    /**
     * Класс, позволяющий получить ссылку на FileManagerService
     */
    private LocalFilerManagerServiceBinder mLocalFilerManagerServiceBinder;

    /**
     * Директория голосовых записей
     */
    private File directoryAudioFiles;

    @Override
    public void onCreate() {
        super.onCreate();
        mLocalFilerManagerServiceBinder = new LocalFilerManagerServiceBinder();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        startForeground(NOTIFICATION_ID, createNotification());

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {

        return mLocalFilerManagerServiceBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {

        return super.onUnbind(intent);
    }

    private Notification createNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID);

        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        builder.setContentTitle(getString(R.string.file_service_notif_title))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentText(getString(R.string.file_service_notif_text))
                .setOnlyAlertOnce(true)
                .setContentIntent(pendingIntent);

        return builder.build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.file_service_channel_name);
            String description = getString(R.string.file_service_channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager =
                    getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void updateNotification(@NonNull Notification notification) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    public File getDirectoryAudioFiles() {
        return directoryAudioFiles;
    }

    public void setDirectoryAudioFiles(File directoryAudioFiles) {
        this.directoryAudioFiles = directoryAudioFiles;
    }

    /**
     * Получение списка файлов установленной директории
     * @return список файлов директории
     */
    public List<File> getFiles() {

        if (directoryAudioFiles != null) {
            updateNotification(createNotification());
            File[] files = directoryAudioFiles.listFiles();
            if (files == null)
                return null;
            return new ArrayList<>(Arrays.asList(files));
        }
        return null;
    }

    /**
     * Удаление всех файлов установленной директории
     * @return true - если все файлы успешно удалены или папка изначально была пустая
     *         false - если минимум один из файлов не удален или директория не является папкой
     */
    public boolean deleteFromExternalStorage() {
        if (directoryAudioFiles.isDirectory()) {
            File[] files = directoryAudioFiles.listFiles();
            if (files != null){
                for (File child : files) {
                    boolean success = child.delete();
                    Log.e("FileManagerService", "delete result: " + success);
                    if (!success) {
                        return false;
                    }
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Класс, позволяющий получить ссылку на сервис FileManagerService
     */
    public class LocalFilerManagerServiceBinder extends Binder {
        public FileManagerService getFileManagerService() {
            return FileManagerService.this;
        }
    }
}
