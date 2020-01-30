// IRecorderServiceAIDL.aidl
package ru.sergeykozhukhov.recorderservice;

interface IRecorderServiceAIDL {

    void setDirectory(String directory);
    void startRecord();
    void stopRecord();
}
