package com.example.salmazaki.myapplication;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.w3c.dom.Text;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final String TAG = "MainActivity";
    private SensorManager mSensorManager;
    Sensor accelerometer;

    //Bluetooth
    Button listen, listDevices;
    ListView listView;
    TextView text;

    BluetoothAdapter btAdapter;
    BluetoothDevice [] btArray;
    int enableRequestCode = 1;

    private static final UUID myUUID = UUID.fromString("c6ef2f10-21ee-42a9-8764-38bb4a7c5fe1");
    private static final String appName = "accelerometer";

    SendReceive sendReceive;
    byte [] b;

    int MESSAGE_RECEIVED = 1;

    private LineGraphSeries<DataPoint> series;
    private int lastX = 0;
    double rootMeanSquare;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "onCreate is now called - Initialising Sensor Services.");
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if(accelerometer!=null)
            mSensorManager.registerListener(MainActivity.this,accelerometer,SensorManager.SENSOR_DELAY_NORMAL);
        Log.d(TAG, "Accelerometer has been registered.");

        //Bluetooth
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if(!btAdapter.isEnabled())
        {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, enableRequestCode);
        }

        findViewbyIds();
        implementListeners();

        GraphView graph = (GraphView) findViewById(R.id.graph);
        series = new LineGraphSeries<DataPoint>();
        graph.addSeries(series);

        Viewport viewport = graph.getViewport();
        viewport.setXAxisBoundsManual(true);
        viewport.setMinX(0);
        viewport.setMaxX(10);
        viewport.setScrollable(true);
    }

    private void addEntry() {
            series.appendData(new DataPoint(lastX++, rootMeanSquare), true, 10);
    }
    private void implementListeners() {

        listDevices.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                Set<BluetoothDevice> bt = btAdapter.getBondedDevices(); //get all the paired devices (must pair first to see the devices in the list)
                btArray = new BluetoothDevice[bt.size()];
                String [] deviceNames = new String [bt.size()];
                int i =0;
                if(bt.size()>0)
                {
                    for(BluetoothDevice dev : bt)
                    {
                        btArray[i]=dev;
                        deviceNames[i] = dev.getName();
                        i++;
                    }
                    ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getApplicationContext(),android.R.layout.simple_list_item_1,deviceNames);
                    listView.setAdapter(arrayAdapter);
                }
            }
        });

        listen.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                Server server = new Server(); //the listening device is set to be the Server
                server.start();
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Client client = new Client(btArray[position]); //getting the bluetooth device corresponding to the item in the list.
                client.start();
                new Thread(new Runnable() {

                    @Override
                    public void run() {
                        while(true){
                            runOnUiThread(new Runnable() {

                                @Override
                                public void run() {
                                    addEntry();
                                }
                            });
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                            }
                        }
                    }
                }).start();
            }
        });
    }

    //handler to receive the text from the SendReceive thread and manipulate it in the View.
    Handler handler = new Handler(new Handler.Callback(){

        @Override
        public boolean handleMessage(Message msg) {
            if(msg.what == MESSAGE_RECEIVED)
            {
                byte [] readBuff = (byte[])msg.obj;
                String tmpMsg = new String(readBuff,0,msg.arg1);
                String tmp0 = tmpMsg.substring(0, tmpMsg.indexOf("/"));
                tmpMsg = tmpMsg.substring(tmpMsg.indexOf("/")+1, tmpMsg.length());
                String tmp1 = tmpMsg.substring(0, tmpMsg.indexOf("/"));
                tmpMsg = tmpMsg.substring(tmpMsg.indexOf("/")+1, tmpMsg.length());
                String tmp2 = tmpMsg;
                float values0 = Float.parseFloat(tmp0);
                float values1 = Float.parseFloat(tmp1);
                float values2 = Float.parseFloat(tmp2);
                rootMeanSquare= Math.sqrt((((values0*values0) + (values1*values1) + (values2*values2))/3));
            }
            return false;
        }
    });

    private void findViewbyIds() {
        listen = (Button)findViewById(R.id.listen);
        listDevices = (Button)findViewById(R.id.listDevices);
        listView = (ListView) findViewById(R.id.listView);
        text = (TextView)findViewById(R.id.text);
    }

    private class Server extends Thread {
        private BluetoothServerSocket serverSocket;

        public Server(){
            try {
                serverSocket = btAdapter.listenUsingRfcommWithServiceRecord(appName,myUUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run()
        {
            BluetoothSocket socket =  null;

            while(socket == null)
            {
                try {
                    Log.e(TAG, "accepting............");
                    socket = serverSocket.accept(); //accept the connection with the client
                } catch (IOException e) {
                    //Log.e(TAG, "socket is null");
                    e.printStackTrace();
                }
                if(socket!=null)
                {
                    //Log.d(TAG, "socket is not null.");
                    sendReceive = new SendReceive(socket);
                    sendReceive.start();
                    break;
                }
            }

            while(true)
            {
                //Log.d(TAG, "writing: "+ b.toString());
                sendReceive.write(b); //send the accelerometer data
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class Client extends Thread {
        private final BluetoothDevice device;
        private final BluetoothSocket socket;

        public Client(BluetoothDevice device1) {
            device = device1;
            BluetoothSocket tmp = null;
            try {
                tmp = device.createRfcommSocketToServiceRecord(myUUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
            socket = tmp;
        }

        public void run() {
            try {
                socket.connect();
            } catch (IOException e) {
                try {
                    Log.e(TAG, "Unable to connect.", e);
                    socket.close();
                } catch (IOException e1) {
                    Log.e(TAG, "Could not close the client socket", e1);
                    e1.printStackTrace();
                }
                return;
            }
            sendReceive = new SendReceive(socket);
            sendReceive.start();
        }
    }

    private class SendReceive extends Thread {
        private final BluetoothSocket bluetoothSocket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public SendReceive (BluetoothSocket socket)
        {
            bluetoothSocket = socket;
            InputStream tempIn = null;
            OutputStream tempOut = null;
            try {
                tempIn = bluetoothSocket.getInputStream();
                tempOut = bluetoothSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            inputStream = tempIn;
            outputStream = tempOut;
        }

        public void run()
        {
            byte [] buffer = new byte[1024]; //buffer which stores the read data.
            int bytes;

            while(true)
            {
                try {
                    bytes = inputStream.read(buffer); //bytes is the no. of bytes.
                    handler.obtainMessage(MESSAGE_RECEIVED,bytes,-1,buffer).sendToTarget();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void write (byte[]bytes)
        {
            try {
                outputStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float v0 = event.values[0];
        float v1 = event.values[1];
        float v2 = event.values[2];

        String str = Float.toString(v0);
        str+="/";
        str+=Float.toString(v1);
        str+="/";
        str+=Float.toString(v2);
        b = str.getBytes();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
