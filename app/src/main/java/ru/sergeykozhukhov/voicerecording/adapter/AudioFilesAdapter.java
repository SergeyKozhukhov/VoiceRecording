package ru.sergeykozhukhov.voicerecording.adapter;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import ru.sergeykozhukhov.voicerecording.R;

public class AudioFilesAdapter extends RecyclerView.Adapter<AudioFilesAdapter.FileHolder> {

    /**
     * Обработчик нажатия на элементы
     */
    private OnItemAudioFileClickListener onItemAudioFileClickListener;

    /**
     * Список файлов, названия которых предполагаются к отображению
     */
    private List<File> files = new ArrayList<>();

    public AudioFilesAdapter(OnItemAudioFileClickListener onItemAudioFileClickListener) {
        this.onItemAudioFileClickListener = onItemAudioFileClickListener;
    }


    public void setFiles(List<File> files) {
        this.files = files;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FileHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());

        View view = layoutInflater.inflate(android.R.layout.simple_list_item_1, parent, false);

        return new FileHolder(view, onItemAudioFileClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull FileHolder holder, int position) {
        File file = files.get(position);

        holder.fileName_text_view.setText(file.getName());

        /*
         * isDirectory - проверка, является ли данный объект папкой
         * */
        if (file.isDirectory()) {
            holder.fileName_text_view.setCompoundDrawables(holder.directory_drawable, null, null, null);
        } else {
            holder.fileName_text_view.setCompoundDrawables(holder.file_drawable, null, null, null);
        }

    }


    @Override
    public int getItemCount() {
        return files.size();
    }

    public class FileHolder extends RecyclerView.ViewHolder {

        /**
         * Текстовое описание файла/директории
         */
        private TextView fileName_text_view;

        /**
         * Изображение для идентификации файлов
         */
        private Drawable file_drawable;

        /**
         * Изображение для идентификации папок
         */
        private Drawable directory_drawable;


        FileHolder(@NonNull View itemView, final OnItemAudioFileClickListener onItemAudioFileClickListener) {
            super(itemView);
            fileName_text_view = itemView.findViewById(android.R.id.text1);

            file_drawable = itemView.getResources().getDrawable(R.drawable.ic_music);
            directory_drawable = itemView.getResources().getDrawable(R.drawable.ic_launcher_foreground);

            /*
             * Предстоящий процесс отрисовки будет происходить в квадратах заданных размеров
             * */
            directory_drawable.setBounds(0, 0, 50, 50);
            file_drawable.setBounds(0, 0, 50, 50);

            fileName_text_view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onItemAudioFileClickListener.onPlayAudioFile(files.get(getAdapterPosition()));
                }
            });
        }
    }
}
