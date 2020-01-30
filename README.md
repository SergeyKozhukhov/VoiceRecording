# VoiceRecording

### [VoiceRecording]

Приложение, позволяющее записывать звуковые файлы и воспроизводить их.
На основе этого приложения демонстрируется работа с сервисами, созданными в текущем процессе и работающими в других процессах.

### [ФУНКЦИИ]

Возможности:

1. запись звука;
2. воспроизведение звука;
3. удаление всех записанных звуковых файлов;
4. обновление списка файлов.

### [ТЕХНИЧЕСКАЯ ЧАСТЬ]

Использование:

- сервисы (для работы с файлами, записи и воспроизведения);
- bindService (для подключения к сервису работы с файлами);
- Messenger (для работы с сервисом воспроизведения файлов);
- AIDL (для работы с сервисом записи звуковых файлов);
- Notifications (для оповещения пользователя и реализации Foreground Service);
- MediaPlayer (для работы со звуковыми файлами).

### [ИНТЕРФЕЙС]

На экране имеется поле со списком звуковых файлов, по нажатию на файл производится воспроизведение.
В правом верхнем углу две кнопки: обновление списка файлов и удаление всех файлов.
В левом верхнем углу в соответствии с текущим состоянием находятся кнопки: запись звука, остановка записи звука, паузка проигрывания звукового файла и его остановка.
Также имеется вывод текущего состояние воспроизведения в виде секунд и процентов.

[МОДЕРНИЗАЦИЯ]

В программе следует добавить:
- обратную коммуникаю с сервисом записи;
- обработку ситуаций, когда записанный файл меньше секунды.

### [ПРИМЕР РАБОТЫ ПРИЛОЖЕНИЯ]

1. Воспроизведение звукового файла.

![Image alt](/scr/01_01.jpg)
