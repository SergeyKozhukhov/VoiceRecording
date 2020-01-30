package ru.sergeykozhukhov.voicerecording.audio_tools;

import android.media.MediaPlayer;
import java.io.File;

/**
 * Класс для воспроизведения звуковых файлов
 */
public class AudioPlayer {

    /**
     * Проигрывать файлов
     */
    private MediaPlayer mediaPlayer;

    /**
     * Текущий файл
     */
    private File audioFile;


    public AudioPlayer() {
    }

    /**
     * Воспроизведение файла
     */
    public void start() {
        try {
            releasePlayer();
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(audioFile.getPath());
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Завершение проигрывания файла
     */
    public void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }
    }

    /**
     * Пауза в проигрывании файла, если на текущий момент он проигрывается,
     * воспроизведение с приостановленного места, если он был остановлен
     */
    public void pause() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying())
                mediaPlayer.pause();
            else {
                mediaPlayer.start();
            }
        }
    }

    /**
     * Освобождение ресурсов проигрывателя
     */
    private void releasePlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    public File getAudioFile() {
        return audioFile;
    }

    public void setAudioFile(File audioFile) {
        this.audioFile = audioFile;
    }

    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }


}
