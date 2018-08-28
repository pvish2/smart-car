/*
 * Copyright 2016, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.piyushdroid.androidthings.smartcar;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.things.contrib.driver.button.Button;
import com.google.android.things.contrib.driver.button.ButtonInputDriver;
import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;
import com.google.android.things.pio.PeripheralManager;
import com.google.android.things.pio.SpiDevice;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;


/**
 *  activity that capture a picture from an Android Things
 * camera on a button press and post it to Firebase and Google Cloud
 * Vision API.
 * Read RFID Tags
 * Read PIR sensor Data
 * Trigger button clicks
 * Identify driver state
 */
public class SmartCarMainActivity extends Activity implements SensorEventListener{
    private static final String TAG = SmartCarMainActivity.class.getSimpleName();

    private FirebaseDatabase mDatabase;
    private FirebaseStorage mStorage;
    private CarCamera mCamera;


    LocationManager myLocationManager;
    String PROVIDER = LocationManager.NETWORK_PROVIDER;

    String mLatitude,mLongitude,mTemprature;
    /**
     * Driver for the car button;
     */
    private ButtonInputDriver mButtonInputDriver;

    /**
     * A {@link Handler} for running Camera tasks in the background.
     */
    private Handler mCameraHandler;

    /**
     * An additional thread for running Camera tasks that shouldn't block the UI.
     */
    private HandlerThread mCameraThread;

    /**
     * A {@link Handler} for running Cloud tasks in the background.
     */
    private Handler mCloudHandler;

    /**
     * An additional thread for running Cloud tasks that shouldn't block the UI.
     */
    private HandlerThread mCloudThread;



    //FOR RFID
   TextView textView1;
    //END HERE



    @Override
    public void onSensorChanged(SensorEvent event) {
        Log.i(TAG, "sensor changed: " + event.values[0]);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.i(TAG, "sensor accuracy changed: " + accuracy);
    }





    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "Activity created.");
        setContentView(R.layout.activity_main);
        initCameraAndFirebase();




        //LOCATION
        myLocationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);

        //get last known location, if available
        Location location = myLocationManager.getLastKnownLocation(PROVIDER);
        getExactLoaction(location);




        //RFID
        textView1 = (TextView)findViewById(R.id.textView1);

        mRfidTask = new RfidTask(mRc522);
        mRfidTask.execute();
        textView1.setText(R.string.reading);



        PeripheralManager pioService = PeripheralManager.getInstance();
        try {
            spiDevice = pioService.openSpiDevice(SPI_PORT);
            gpioReset = pioService.openGpio(PIN_RESET);
            mRc522 = new Rc522(spiDevice, gpioReset);
            mRc522.setDebugging(true);
        } catch (IOException e) {
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }



        //PIR ///
    try {
            // set PIR sensor as button for LED
            // Create GPIO connection.
            mPirGpio = pioService.openGpio(PIR_PIN);
            // Configure as an input.
            mPirGpio.setDirection(Gpio.DIRECTION_IN);
            // Enable edge trigger events for both falling and rising edges. This will make it a toggle button.
            mPirGpio.setEdgeTriggerType(Gpio.EDGE_BOTH);
            // Register an event callback.
            mPirGpio.registerGpioCallback(mSetLEDCallback);

            // set LED as output
            // Create GPIO connection.
            mLedGpio = pioService.openGpio(LED_PIN);
            // Configure as an output.
            mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);

        } catch (IOException e) {
            Log.e(TAG, "Error on PeripheralIO API", e);
        }

    }


private void initCameraAndFirebase(){

    // We need permission to access the camera
    if (checkSelfPermission(Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
        // A problem occurred auto-granting the permission
        Log.e(TAG, "No permission");
        return;
    }

    mDatabase = FirebaseDatabase.getInstance();
    mStorage = FirebaseStorage.getInstance();

    // Creates new handlers and associated threads for camera and networking operations.
    mCameraThread = new HandlerThread("CameraBackground");
    mCameraThread.start();
    mCameraHandler = new Handler(mCameraThread.getLooper());

    mCloudThread = new HandlerThread("CloudThread");
    mCloudThread.start();
    mCloudHandler = new Handler(mCloudThread.getLooper());

    // Initialize the car button driver
    initPIO();

    // Camera code is complicated, so we've shoved it all in this closet class for you.
    mCamera = CarCamera.getInstance();
    mCamera.initializeCamera(this, mCameraHandler, mOnImageAvailableListener);

}

    private void initPIO() {
        try {
            mButtonInputDriver = new ButtonInputDriver(
                    BoardDefaults.getGPIOForButton(),
                    Button.LogicState.PRESSED_WHEN_LOW,
                    KeyEvent.KEYCODE_ENTER);
            mButtonInputDriver.register();
        } catch (IOException e) {
            mButtonInputDriver = null;
            Log.w(TAG, "Could not open GPIO pins", e);
        }


        handler.postDelayed(new Runnable(){
            public void run(){
                checkDriverStatus();
                //do something
                handler.postDelayed(this, delay);
            }
        }, delay);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCamera.shutDown();

        mCameraThread.quitSafely();
        mCloudThread.quitSafely();
        try {
            mButtonInputDriver.close();
        } catch (IOException e) {
            Log.e(TAG, "button driver error", e);
        }

        //PIR//

        // Close the resource
        if (mPirGpio != null) {
            mPirGpio.unregisterGpioCallback(mSetLEDCallback);
            try {
                mPirGpio.close();
            } catch (IOException e) {
                Log.e(TAG, "Error on PeripheralIO API", e);
            }
        }
        if (mLedGpio != null) {
            try {
                mLedGpio.close();
            } catch (IOException e) {
                Log.e(TAG, "Error on PeripheralIO API", e);
            }
        }


    }




    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_ENTER) {
            // Push Button clicked//Accident happened!
            Log.d(TAG, "button pressed");
            mCamera.takePicture();
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    /**
     * Listener for new camera images.
     */
    private ImageReader.OnImageAvailableListener mOnImageAvailableListener =
            new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = reader.acquireLatestImage();
            // get image bytes
            ByteBuffer imageBuf = image.getPlanes()[0].getBuffer();
            final byte[] imageBytes = new byte[imageBuf.remaining()];
            imageBuf.get(imageBytes);
            image.close();

            onPictureTaken(imageBytes,null);
        }
    };

    /**
     * Upload image data to Firebase as a car event.
     */
    private void onPictureTaken(final byte[] imageBytes,final String message ) {
        if (imageBytes != null) {
           startUploadtask(imageBytes,message,false);
        }
    }

    private void startUploadtask(final byte[] imageBytes,final String message,final boolean isDriver){

        final DatabaseReference log = mDatabase.getReference("logs").push();
        final StorageReference imageRef = mStorage.getReference().child(log.getKey());
        // Create file metadata including the content type
        StorageMetadata metadata = new StorageMetadata.Builder()
                .setContentType("image/jpg")
                .setCustomMetadata("latitude", "latitude7798").setCustomMetadata("longitude","longitude3213")
                .setCustomMetadata("message",message)
                .setCustomMetadata("userState", userState)
                .build();


        // upload image to storage
        UploadTask task = imageRef.putBytes(imageBytes,metadata);

        task.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Uri downloadUrl = taskSnapshot.getDownloadUrl();
                // mark image in the database
                Log.i(TAG, "Image upload successful");
                log.child("timestamp").setValue(ServerValue.TIMESTAMP);
                log.child("image").setValue(downloadUrl.toString());
                log.child("latitude").setValue("Latitude "+mLatitude);
                log.child("longitude").setValue("Longtitude "+mLongitude);
                log.child("temprature").setValue("Temprature "+mTemprature);
                log.child("message").setValue(" "+message);
                log.child("isDriver").setValue(isDriver);
                log.child("userState").setValue(userState);
                // process image annotations
                annotateImage(log, imageBytes);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                // clean up this entry
                Log.w(TAG, "Unable to upload image to Firebase");
                log.removeValue();
            }
        });
    }

    /**
     * Process image contents with Cloud Vision.
     */
    private void annotateImage(final DatabaseReference ref, final byte[] imageBytes) {
        mCloudHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "sending image to cloud vision");
                // annotate image by uploading to Cloud Vision API
                try {
                    Map<String, Float> annotations = CloudVisionUtils.annotateImage(imageBytes);
                    Log.d(TAG, "cloud vision annotations:" + annotations);
                    if (annotations != null) {
                        ref.child("annotations").setValue(annotations);
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Cloud Vison API error: ", e);
                }
            }
        });
    }

    @Override
    protected void onPause() {
        myLocationManager.removeUpdates(myLocationListener);
        super.onPause();
    }

    @Override
    protected void onResume() {
        myLocationManager.requestLocationUpdates(
                PROVIDER, //provider
                0, //minTime
                0, //minDistance
                myLocationListener); //LocationListener
        super.onResume();
    }



    private LocationListener myLocationListener
            = new LocationListener(){

        @Override
        public void onLocationChanged(Location location) {
            getExactLoaction(location);
        }

        @Override
        public void onProviderDisabled(String provider) {
// TODO Auto-generated method stub

        }

        @Override
        public void onProviderEnabled(String provider) {
// TODO Auto-generated method stub

        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
// TODO Auto-generated method stub

        }};

    private void getExactLoaction(Location l) {
        if(l == null){
        Log.e("No Locatio","No Location");
        }else{
            mLatitude ="Latitude: " + l.getLatitude();
            mLongitude="Longitude: " + l.getLongitude();
        }

    }
    ////RFID//



    private Rc522 mRc522;
    RfidTask mRfidTask;

    private SpiDevice spiDevice;
    private Gpio gpioReset;

    private static final String SPI_PORT = "SPI0.0";
    private static final String PIN_RESET = "BCM25";

    String resultsText = "";
    private class RfidTask extends AsyncTask<Object, Object, Boolean> {
        private static final String TAG = "RfidTask";
        private Rc522 rc522;
        String resultS;

        RfidTask(Rc522 rc522){
            this.rc522 = rc522;
        }

        @Override
        protected void onPreExecute() {
            resultsText = "";
        }

        @Override
        protected Boolean doInBackground(Object... params) {
            if(rc522==null){
                return null;
            }
            rc522.stopCrypto();
            while(true){
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return null;
                }
                //Check if a RFID tag has been found
                if(!rc522.request()){
                    continue;
                }
                //Check for collision errors
                if(!rc522.antiCollisionDetect()){
                    continue;
                }
                byte[] uuid = rc522.getUid();
                return rc522.selectTag(uuid);
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if(rc522==null || success ==null || !success){
                textView1.setText(R.string.unknown_error);
                Log.e(TAG,"unknown_error");
                resume();
                return;
            }
            // Try to avoid doing any non RC522 operations until you're done communicating with it.
            byte address = Rc522.getBlockAddress(2,1);
            // Mifare's card default key A and key B, the key may have been changed previously
            byte[] key = {(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF};
            // Each sector holds 16 bytes
            // Data that will be written to sector 2, block 1
          // byte[] newData = {0x0F,0x0E,0x0D,0x0C,0x0B,0x0A,0x09,0x08,0x07,0x06,0x05,0x04,0x03,0x02,0x01,0x40};
          //  byte[] newData = {0x0F,0x0E,0x0D,0x0C,0x0B,0x0A,0x09,0x08,0x07,0x06,0x05,0x04,0x03,0x02,0x02,0x20};
            // In this case, Rc522.AUTH_A or Rc522.AUTH_B can be used
            try {
                //We need to authenticate the card, each sector can have a different key
                boolean result = rc522.authenticateCard(Rc522.AUTH_A, address, key);
                if (!result) {
                    textView1.setText(R.string.authetication_error);
                    Log.e(TAG,"authetication_error");

                    return;
                }
              //  result = rc522.writeBlock(address, newData);
                if(!result){
                    textView1.setText(R.string.write_error);

                    Log.e(TAG,"write_error");

                    return;
                }
                resultsText += "Sector written successfully";
                byte[] buffer = new byte[16];
                //Since we're still using the same block, we don't need to authenticate again
                result = rc522.readBlock(address, buffer);
                if(!result){
                    textView1.setText(R.string.read_error);
                    Log.e(TAG,"read_error");

                    return;
                }
                resultS = Rc522.dataToHexString(buffer).trim().replaceAll(" ","");
                resultsText += "\nSector read successfully: "+ Rc522.dataToHexString(buffer);
                rc522.stopCrypto();
               textView1.setText(resultsText);
                Log.e(TAG,resultsText);

            }finally{
                if(rc522!=null){


                String aspect ="";
                String speedLimit = "";
                String distance = "";
                if(rc522.getUidString().equals("67-236-59-39-179")){
                    aspect ="SCHOOL";
                }else if(rc522.getUidString().equals("227-29-80-115-221")){
                    aspect ="ACCIDENT PRONE AREA";
                }


                if(resultS!=null){
                    speedLimit = resultS.substring(resultS.length()-2);
                    distance = resultS.substring(resultS.length()-3,resultS.length()-2);
                    String printMessage = aspect+","+distance+" km ahead and speed limit of road is "+speedLimit +"km/hr";
                    Log.e(TAG,printMessage);


                    final byte[] imageBytes = printMessage.getBytes(StandardCharsets.UTF_8);

                    startUploadtask(imageBytes,printMessage,true);
                    //textView1.setText( aspect+","+distance+" km ahead and speed limit of road is "+speedLimit +"km/hr");
                }
                }
               resume();
            }
        }


        private void resume(){
            textView1.setText("READING");
            mRfidTask.cancel(true);
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mRfidTask = new RfidTask(mRc522);
            mRfidTask.execute();
        }
    }



    ///PR Sensor///

    public static final String PIR_PIN = "BCM17"; //physical pin #11
    public static final String LED_PIN = "BCM13"; //physical pin #33

    private Gpio mPirGpio;
    private Gpio mLedGpio;

    // Register an event callback.
    private GpioCallback mSetLEDCallback = new GpioCallback() {
        @Override
        public boolean onGpioEdge(Gpio gpio) {
            Log.i(TAG, "GPIO callback ------------");
            if (mLedGpio == null) {
                return true;
            }
            try {
                Log.i(TAG, "GPIO " + gpio.getValue());
                // set the LED state to opposite of input state
                mLedGpio.setValue(gpio.getValue());
                if(gpio.getValue()){
                    prevEventTime = System.currentTimeMillis();
                }
            } catch (IOException e) {
                Log.e(TAG, "Error on PeripheralIO API", e);
            }
            // Return true to keep callback active.
            return true;
        }
    };

    long prevEventTime ;
    String userState = "Active";

    Handler handler = new Handler();
    int delay = 5000; //milliseconds

    private void  checkDriverStatus(){

        if(prevEventTime!=0){
            //check , diver have flickered his/her eyes with in 4 seconds
            if ( Math.abs(prevEventTime-System.currentTimeMillis())>4000){
                //flickered
                userState = "Active";

            } else {
                //not flickered
                userState ="InActive";
                String printMessage ="Are you sleeping?";
                Log.e(TAG,printMessage);


                final byte[] imageBytes = printMessage.getBytes(StandardCharsets.UTF_8);

                startUploadtask(imageBytes,printMessage,true);
            }
        }

    }


}
