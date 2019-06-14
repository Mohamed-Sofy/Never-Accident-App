package com.example.pc.alertcar;

import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Vibrator;
import android.preference.DialogPreference;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.logging.Handler;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {


    private GoogleMap mMap;
    private GPStracker gpStracker;
    private Location mlocal;
    double latidu , longtidu;
    int PROXIMITY_RADIUS=10000;

    //Object dataTransfer[] = new Object[4];

    BluetoothAdapter mBluetoothAdapter;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    OutputStream mmOutputStream;
    InputStream mmInputStream;
    Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    int counter;
    volatile boolean stopWorker;
    String data;

/////////////////////////////////////////

    Vibrator vibrator;
    android.hardware.Camera camera;
    android.hardware.Camera.Parameters parameters;
    boolean isflash = false;
    boolean ison = false;


TextView text;
    String message;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        gpStracker = new GPStracker(getApplicationContext());
        mlocal = gpStracker.getLocation();
        latidu = mlocal.getLatitude();
        longtidu = mlocal.getLongitude();

        message = latidu +" : " +longtidu +"ارجو ارسال سياره اسعاف الى هذا العنوان";
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        text =(TextView)findViewById(R.id.test) ;


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.

    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(latidu , longtidu);
        mMap.addMarker(new MarkerOptions().position(sydney).title("موقعى")).showInfoWindow();
        mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sydney,16));

    }



    @NonNull
    private String getUrl(double latitude , double longitude , String nearbyPlace)
    {

        StringBuilder googlePlaceUrl = new StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json?");
        googlePlaceUrl.append("location="+latitude+","+longitude);
        googlePlaceUrl.append("&radius="+PROXIMITY_RADIUS);
        googlePlaceUrl.append("&type="+nearbyPlace);
        googlePlaceUrl.append("&sensor=true");
        googlePlaceUrl.append("&key="+"AIzaSyDFXod2FiuC9mJvOfqJa_0_CbAA0Klguak");

        //AIzaSyDFXod2FiuC9mJvOfqJa_0_CbAA0Klguak
        Log.d("MapsActivity", "url = "+googlePlaceUrl.toString());

        return googlePlaceUrl.toString();
    }

    public void hospital_l(View view) {

        GetNearbyPlacesData getNearbyPlacesData=new GetNearbyPlacesData();
        Object dataTransfer[] = new Object[2];
       // mMap.clear();
        String hospital = "hospital";
        String url = getUrl(latidu, longtidu, hospital);
        dataTransfer[0] = mMap;
        dataTransfer[1] =  url;
        getNearbyPlacesData.execute(dataTransfer);
        Toast.makeText(this, "Showing Nearby Hospitals", Toast.LENGTH_SHORT).show();



    }


    public void con(View v)
    {
        try
        {
            findBT();
            openBT();
        }
        catch (IOException ex) {
            Toast.makeText(this, "Bluetooth error", Toast.LENGTH_SHORT).show();
        }
    }

    void findBT()
    {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null)
        {
            // myLabel.setText("No bluetooth adapter available");
            Toast.makeText(this, "No bluetooth adapter available", Toast.LENGTH_SHORT).show();
        }

        if(!mBluetoothAdapter.isEnabled())
        {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if(pairedDevices.size() > 0)
        {
            for(BluetoothDevice device : pairedDevices)
            {
                if(device.getName().equals("HC-05"))
                {
                    mmDevice = device;
                    break;
                }
            }
        }
        //myLabel.setText("Bluetooth Device Found");
        Toast.makeText(this, "Bluetooth Device Found", Toast.LENGTH_SHORT).show();
    }

    void openBT() throws IOException
    {
      //  myLabel.setText(" Opened");
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); //Standard SerialPortService ID
        mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
        mmSocket.connect();
        mmOutputStream = mmSocket.getOutputStream();
        mmInputStream = mmSocket.getInputStream();

        beginListenForData();

    }


    void beginListenForData()
    {
        final android.os.Handler handler = new android.os.Handler();
        final byte delimiter = 10; //This is the ASCII code for a newline character

        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        workerThread = new Thread(new Runnable()
        {
            public void run()
            {
                while(!Thread.currentThread().isInterrupted() && !stopWorker)
                {
                    try
                    {
                        int bytesAvailable = mmInputStream.available();
                        if(bytesAvailable > 0)
                        {
                            byte[] packetBytes = new byte[bytesAvailable];
                            mmInputStream.read(packetBytes);
                            for(int i=0;i<bytesAvailable;i++)
                            {
                                byte b = packetBytes[i];
                                if(b == delimiter)
                                {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;


                                    handler.post(new Runnable() {
                                        public void run() {
                                            //  myLabel.setText(data);
                                            if (data.length() == 2) {
                                                //  Toast.makeText(MapsActivity.this, data, Toast.LENGTH_SHORT).show();
                                                vibrator.vibrate(1000);
                                                flash_on();
                                            } else if (data.length() == 3) {
                                                final Timer t = new Timer();


                                                Toast.makeText(MapsActivity.this, data, Toast.LENGTH_SHORT).show();
                                                AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
                                                builder.setMessage("ارسال رساله استغاسه الى اقرب مستشفى")
                                                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialog, int which) {

                                                                try {
                                                                    send_sms("01022711357", message);
                                                                    Toast.makeText(MapsActivity.this, "تم ارسال الرساله", Toast.LENGTH_SHORT).show();
                                                                } catch (Exception e) {
                                                                    Toast.makeText(MapsActivity.this, "حصل خطأ فى ارسال الرساله", Toast.LENGTH_SHORT).show();
                                                                }


                                                            }
                                                        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        t.cancel();
                                                    }
                                                });
                                                final AlertDialog alert = builder.create();
                                                alert.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                                                alert.show();


                                                t.schedule(new TimerTask() {
                                                    @Override
                                                    public void run() {
                                                        alert.dismiss();
                                                        send_sms("01022711357", message);
                                                        Toast.makeText(MapsActivity.this, "تم ارسال الرساله", Toast.LENGTH_SHORT).show();
                                                        t.cancel();
                                                    }
                                                }, 60000);

                                            } else
                                                off();
                                        }
                                    });


                                }
                                else
                                {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    }
                    catch (IOException ex)
                    {
                        stopWorker = true;
                    }
                }
            }
        });

        workerThread.start();
    }

    public void flash_on() {

        if(getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            camera = android.hardware.Camera.open();
            parameters = camera.getParameters();
            isflash = true;
            parameters.setFlashMode(android.hardware.Camera.Parameters.FLASH_MODE_TORCH);
            camera.setParameters(parameters);
            camera.startPreview();
            ison = true;

        }}


    public void off() {
        parameters.setFlashMode(android.hardware.Camera.Parameters.FLASH_MODE_OFF);
        camera.setParameters(parameters);
        camera.stopPreview();
        ison = false;

    }

    public void send_sms(String tel , String msg)
    {
        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(tel,null,msg,null,null);

    }

    public void call() {

        String phone ="01118198641";
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:"+phone));
        if(intent.resolveActivity(getPackageManager())!=null)
        {
            startActivity(intent);
        }
    }

}
