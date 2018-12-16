# accelerometer-android-app

The aim behind this app is to send data read from the accelerometer
of one Android device via Bluetooth to another Android device 
that displays the data as a timeseries plot. 

This Android app works using two mobile devices. Before testing the application, 
turn on the Bluetooth on both devices and pair them, 
as this application requires both devices to be paired.

To test it, please follow the steps below:
1- Connect the two Android mobile devices to your computer & run the code on both of them.
2- On only one of the two devices, let us name it D1, tap on "LISTEN". 
This would allow any other paired device to connect with this device.
3- On the other device, D2, tap on "LIST DEVICES" to show you the previous device in 
its list of paired devices (and any other device it is paired with).
4- On D2, tap on the name of D1 in the shown list of paired devices. 
Now both devices are connected and the acceloremeter data read from D1 will be 
displayed as a timeseries plot on D2. 


References used:
1- https://developer.android.com/guide/topics/sensors/sensors_motion
2- https://developer.android.com/guide/topics/connectivity/bluetooth
3- https://www.youtube.com/playlist?list=PLFh8wpMiEi8_I3ujcYY3-OaaYyLudI_qi
4- https://www.youtube.com/watch?v=0LrMBEO7sb8
5- https://github.com/jjoe64/GraphView
