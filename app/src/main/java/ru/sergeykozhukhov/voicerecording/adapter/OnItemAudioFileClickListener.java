package ru.sergeykozhukhov.voicerecording.adapter;

import androidx.annotation.NonNull;

import java.io.File;

/**
 * Обработчик нажатий на ячейку списка файлов
 */
public interface OnItemAudioFileClickListener {
    /**
     * Воспроизведение выбранного файла
     * @param file - файл для воспроизведения
     */
    void onPlayAudioFile(@NonNull File file);
}
