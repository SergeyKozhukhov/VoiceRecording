package ru.sergeykozhukhov.voicerecording;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.MediaRecorder;

import androidx.core.content.ContextCompat;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class Dictaphone {

    private MediaRecorder mediaRecorder;
    private MediaPlayer mediaPlayer;
    private String filePath;
    private String fileName;


    public Dictaphone(Context context) {
        filePath = ContextCompat.getDataDir(context).getPath();
    }

    public void recordStart() {
        try {
            releaseRecorder();

            File outFile = new File(filePath+newFileNameByDate());
            if (outFile.exists()) {
                outFile.delete();
            }

            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setOutputFile(filePath+fileName);
            mediaRecorder.prepare();
            mediaRecorder.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void recordStop() {
        if (mediaRecorder != null) {
            mediaRecorder.stop();
        }
    }

    public void playStart() {
        try {
            releasePlayer();
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(filePath+fileName);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void playStop() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }
    }

    private void releaseRecorder() {
        if (mediaRecorder != null) {
            mediaRecorder.release();
            mediaRecorder = null;
        }
    }

    private void releasePlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private String newFileNameByDate(){
        Date date = new Date();
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(date);
        String fileName = "/"+
                + calendar.get(Calendar.YEAR)+"_"
                + calendar.get(Calendar.MONTH) + "_"
                + calendar.get(Calendar.DAY_OF_MONTH) + "_"
                + calendar.get(Calendar.HOUR) + "_"
                + calendar.get(Calendar.MINUTE) + "_"
                + calendar.get(Calendar.SECOND) + ".3gpp";
        this.fileName = fileName;
        return fileName;
    }


    public MediaRecorder getMediaRecorder() {
        return mediaRecorder;
    }

    public void setMediaRecorder(MediaRecorder mediaRecorder) {
        this.mediaRecorder = mediaRecorder;
    }

    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    public void setMediaPlayer(MediaPlayer mediaPlayer) {
        this.mediaPlayer = mediaPlayer;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}
