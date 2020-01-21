package ru.sergeykozhukhov.voicerecording;

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

public class RecordingListAdapeter extends RecyclerView.Adapter<RecordingListAdapeter.FileHolder> {

    /**
     * Обработчик нажатия на элементы
     */
    // private OnItemFileClickListener onItemFileClickListener;

    /**
     * Список файлов, названия которых предполагаются к отображению
     */
    private List<File> files = new ArrayList<>();

    /*public FileAdapter(OnItemFileClickListener onItemFileClickListener) {
        this.onItemFileClickListener = onItemFileClickListener;
    }*/

    public RecordingListAdapeter() {

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

        return new FileHolder(view);
        //return new FileHolder(view, onItemFileClickListener);
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
         * Изображение для идентификации файллов
         */
        private Drawable file_drawable;

        /**
         * Изображение для идентификации папок
         */
        private Drawable directory_drawable;


        FileHolder(@NonNull View itemView) {
            super(itemView);
            fileName_text_view = itemView.findViewById(android.R.id.text1);

            file_drawable = itemView.getResources().getDrawable(R.drawable.ic_launcher_background);
            directory_drawable = itemView.getResources().getDrawable(R.drawable.ic_launcher_foreground);

            /*
             * Предстоящий процесс отрисовки будет происходить в квадратах заданных размеров
             * */
            directory_drawable.setBounds(0, 0, 50, 50);
            file_drawable.setBounds(0, 0, 50, 50);

            fileName_text_view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // onItemFileClickListener.onItemClick(files.get(getAdapterPosition()));
                }
            });
        }
    }
}
