package com.example.cw2;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;


public class MainActivity extends AppCompatActivity {

    private MP3Service.MyBinder myService = null;
    private TextView currentSong;
    private TextView currentDuration;
    private TextView currentProgress;
    File musicDir = new File(
            Environment.getExternalStorageDirectory().getPath() + "/Music/"); //gets the music directory
    File[] list = musicDir.listFiles(); //gets the list of the music


    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) { //when service is connected
            // TODO Auto-generated method stub
            Log.d("g53mdp", "MainActivity onServiceConnected");
            myService = (MP3Service.MyBinder) service;
            myService.registerCallback(callback); //callback is registered for the current progress

        }

        @Override
        public void onServiceDisconnected(ComponentName name) { //when service is disconnected
            // TODO Auto-generated method stub
            Log.d("g53mdp", "MainActivity onServiceDisconnected");
            myService.unregisterCallback(callback); //callback is unregistered
            myService = null;
        }
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.startService(new Intent(this, MP3Service.class)); //this is for when the activity is destroyed - the service will continue running and only stopped when explicitly stopped by the user
        this.bindService(new Intent(MainActivity.this, MP3Service.class), //binds to the service
                serviceConnection, Context.BIND_AUTO_CREATE);

        //shows the current song, duration and progress
        currentSong = findViewById(R.id.currentSong);
        currentProgress = findViewById(R.id.progressView);
        currentDuration = findViewById(R.id.durationView);

        final ListView lv = (ListView) findViewById(R.id.listView);
        // that is a reference to an built-in XML layout document that is part of the Android OS, rather than one of your own XML layouts.
        lv.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, list));
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            public void onItemClick (AdapterView <?> myAdapter, View myView, int myItemInt, long mylng) //when a song is clicked on the list
            {
                File selectedFromList = (File) (lv.getItemAtPosition(myItemInt));

                if (myService.mp3Player().getState().toString().equals("PLAYING")) //if a song is playing and a different song is selected
                {
                    myService.stop(); //stops the song
                    myService.load(selectedFromList.toString()); //loads the song
                    currentSong.setText(myService.mp3Player().getFilePath()); //shows the current song playing

                }
                else if (myService.mp3Player().getState().toString().equals("PAUSED")) //if song is paused and a different song is selected
                {
                    myService.stop();
                    myService.load(selectedFromList.toString());
                    currentSong.setText(myService.mp3Player().getFilePath());
                }

                if (myService.mp3Player().getState().toString().equals("STOPPED")) //if no song is playing currently
                {
                   myService.load(selectedFromList.toString());
                    currentSong.setText(myService.mp3Player().getFilePath());

               }

                currentDuration.setText(Double.toString(myService.mp3Player().getDuration()/1000)); //shows the duration of the entire song

            }

        });

    }



    public void onClickPlay(View v) //when the play button is clicked
    {
       myService.play();
    }

    public void onClickPause(View v) //when the pause button is clicked
    {
        myService.pause();

    }

    public void onClickStop(View v) //when the stopped button is clicked
    {
        myService.stop();
        currentSong.setText("No song is playing");

    }

    @Override
    protected void onDestroy() { //when the activity is destroyed
        super.onDestroy();


        if (myService.mp3Player().getState().toString().equals("STOPPED")){ //if no song is playing
            Intent myService = new Intent(MainActivity.this, MP3Service.class);
            stopService(myService); //the service is stopped
        }

        if(serviceConnection!=null) {
            unbindService(serviceConnection);
            serviceConnection = null;
        }
    }


    Callback callback = new Callback() { //callback to get the current progress
        @Override
        public void progress(final int progress) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    currentProgress.setText(Integer.toString(progress/1000)); //shows the current progress of the song
                }
            });
        }
    };


    @Override
    //when app is closed, it doesnt save, the way is to used shared preferences
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        double duration = myService.mp3Player().getDuration();
        String song = myService.mp3Player().getFilePath();
        outState.putDouble("duration", duration);
        outState.putString("song", song);

    }
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        double duration = savedInstanceState.getDouble("duration");
        String song = savedInstanceState.getString("song");
        currentDuration.setText(Double.toString(duration/1000));
        currentSong.setText(song);
    }
}
