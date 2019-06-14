package com.example.pc.alertcar;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

/**
 * Created by PC on 9/22/2017.
 */
public class GPStracker extends Service implements LocationListener {

    private final Context context;
    boolean isGPSEnable =false;
    boolean isNetworkEnable = false;
    boolean canGetLocation =false;

    Location location;
    protected LocationManager locationManager;




    public GPStracker (Context context)
    {
        this.context = context;
    }

    // method to get location
    public Location getLocation()
    {
     try {

         locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
         isGPSEnable = locationManager.isProviderEnabled(locationManager.GPS_PROVIDER);
         isNetworkEnable = locationManager.isProviderEnabled(locationManager.NETWORK_PROVIDER);

         if(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION ) == PackageManager.PERMISSION_GRANTED

                 ||ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION ) == PackageManager.PERMISSION_GRANTED )
         {
             if(isGPSEnable)
             {
                 if(location == null)
                     locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,10000,10,this);
                 if(locationManager !=null)
                 {
                     location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                 }
             }

             if(location ==null)
             {
                 if(isNetworkEnable)
                 {
                     if(location == null)
                         locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,10000,10,this);
                     if(locationManager !=null)
                     {
                         location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

                     }
                 }
             }


         }




     }catch (Exception e)
     {
         Toast.makeText(this,"error",Toast.LENGTH_LONG).show();

     }
        return location;
    }




    public void onLocationChanged(Location location)
    {

    }

    public void onStatusChanged(String provider, int status , Bundle extras)
    {

    }
    public void onProviderEnabled(String provider){

    }
    public void onProviderDisabled(String provider){

    }
    public IBinder onBind(Intent arg0){
        return null;
    }
}
