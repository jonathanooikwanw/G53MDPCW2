package com.example.cw2;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteCallbackList;
import android.util.Log;

import androidx.core.app.NotificationCompat;

public class MP3Service extends Service {

    private final IBinder binder = new MyBinder();
    MP3Player mp3player;
    private final String CHANNEL_ID = "100";
    int NOTIFICATION_ID = 001;
    RemoteCallbackList<MyBinder> remoteCallbackList = new RemoteCallbackList<MyBinder>();  //maintains a list of remote interfaces - used to perform callbacks for the progress of the song

    @Override
    public void onCreate()
    {
        super.onCreate();
        mp3player = new MP3Player(); //creates a mp3 object when the service is started

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE); //notification
         // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            CharSequence name = "channel name";
            String description = "channel description";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            notificationManager.createNotificationChannel(channel);

            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this,
                    CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_background)
                    .setContentTitle("MP3 Player")
                    .setContentText("MP3 Player is on.")
                    .setContentIntent(pendingIntent)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);

            notificationManager.notify(NOTIFICATION_ID, mBuilder.build());
            startForeground(NOTIFICATION_ID, mBuilder.build());
        }
        Log.d("mp3service", "has been created");
    }

    @Override
    public IBinder onBind(Intent arg0) //returns an IBinder object that clients can call

    {
        return binder;
    }


    public void doCallback(int progress) { //callback for getting the current progress of the song
        final int n = remoteCallbackList.beginBroadcast();
        for (int i=0; i<n; i++) {
            remoteCallbackList.getBroadcastItem(i).callback.progress(progress);
        }
        remoteCallbackList.finishBroadcast();
    }


    public class currentProg extends Thread implements Runnable //seperate class for the current progress as java does not support multiple inheritance
    {
        public currentProg()
        {
            this.start();
        } //starts counting

        @Override
        public void run() {
            while ((mp3player.getState().toString().equals("PLAYING") )|| (mp3player.getState().toString().equals("PAUSED") )){ //while the mp3player is not in a stopped state, the method will keep track of the progress of the song

                doCallback(mp3player.getProgress());
            }

            if ((mp3player.getState().toString().equals("STOPPED"))){ //if the mp3player is stopped, the current progress will reset to 0
                doCallback(0);
            }
        }

    }

    public class MyBinder extends Binder implements IInterface
    {
        @Override
        public IBinder asBinder() {
            return this;
        } //retrieve the binder object

        MP3Player mp3Player()
        {
          return MP3Service.this.mp3player; //returns the mp3player object - this is to get the current duration of the song
        }


        void play() //mp3player play
        {
            mp3player.play();
        }

        void pause() //mp3player pause
        {
            mp3player.pause();
        }

        void stop() //mp3player stopped
        {
            mp3player.stop();

        }

        void load(String path) //mp3player load
        {
         mp3player.load(path);
         currentProg currentProgress = new currentProg(); //creates a object of the callback - this is to show the current progress of the song when a song is loaded


        }

        public void registerCallback(Callback callback) { //registers the callback when the service is connected
            this.callback = callback;
            remoteCallbackList.register(MyBinder.this);
        }

        public void unregisterCallback(Callback callback) { //unregisters the service when the service is disconnected
            remoteCallbackList.unregister(MyBinder.this);
        }

        Callback callback;

    }


}
