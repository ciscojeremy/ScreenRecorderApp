package com.deephost.screenrecorderapp;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.media.CamcorderProfile;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.support.v4.app.NotificationCompat.Action;
import android.support.v4.app.NotificationCompat;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetView;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;

import static android.app.Notification.GROUP_ALERT_SUMMARY;


public class C extends AppCompatActivity {

    private static final String TAG = "C";
    private static final int REQUEST_CODE = 1000;
    private int cms;
    private MediaProjectionManager mMediaProjectionManager;
    private static int cw = 720;
    private static int ch = 1280;
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private MediaProjectionCallback mMediaProjectionCallback;
    private MediaRecorder mMediaRecorder;
    private static final SparseIntArray mSparseIntArray = new SparseIntArray();
    private static final int REQUEST_RUNTIME_CODE = 10;

    String cf;
    List<A> lists;
    ListView mListView;
    String cxy = "";
    FloatingActionButton mFloatingActionButton;
    public static List<String> cmy;
    File file;
    File clt;
    long Time;
    private static final String YES_ACTION = "YES_ACTION";
    private NotificationManager notificationManager;
    private InterstitialAd mInterstitialAd;
    private AdView mAdView;

    static {
        mSparseIntArray.append(Surface.ROTATION_0, 90);
        mSparseIntArray.append(Surface.ROTATION_90, 0);
        mSparseIntArray.append(Surface.ROTATION_180, 270);
        mSparseIntArray.append(Surface.ROTATION_270, 180);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MobileAds.initialize(this, getString(R.string.app_id));
        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId( getString(R.string.interstitial_id));
        mInterstitialAd.loadAd(new AdRequest.Builder().build());
        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                // Load the next interstitial.
                mInterstitialAd.loadAd(new AdRequest.Builder().build());
            }

        });

        mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
        lists = new ArrayList<>();

        mListView = (ListView) findViewById(R.id.cmy1);
        mFloatingActionButton = (FloatingActionButton) findViewById(R.id.fav);
        cxy = "s";

        //creating the adapter
        //B adapter = new B(this, R.layout.custom_listview, lists);

        File mediaStorageDir = new File(Environment.getExternalStorageDirectory(), "Screen Recording");
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                mediaStorageDir.mkdirs();
            }
        }

        RequestRunTimePermission();

        GetPhoneScreenDisplay();

        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        boolean isFirstStart = prefs.getBoolean("isFirstStart", true);
        if (isFirstStart){
            ShowTapTarget();
        }

        mFloatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (cxy.equals("s"))
                {
                    //Request Permission To Start Recording If Not Requested Already
                    RequestPermissionToStartRecording();
                }
                else if (cxy.equals("t"))
                {
                    notificationManager.cancel(1);
                    cxy = "s";
                    mFloatingActionButton.setImageResource(R.drawable.ic_recode);
                    StopMediaRecorder();
                    SaveRecordedVideoFile();
                    ReloadInterstitialAd();
                }
            }
        });
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        cms = metrics.densityDpi;
        mMediaRecorder = new MediaRecorder();
        mMediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        GetActionNotificationCancelled(getIntent());


    }

    private void ShowTapTarget() {
        TapTargetView.showFor(this,
                TapTarget.forView(findViewById(R.id.fav),
                        "Initiate Screen Capture",
                        "Use this button to start screen recording")
                        .transparentTarget(false)
                        .tintTarget(false));


        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("isFirstStart", false);
        editor.apply();
    }

    private void GetPhoneScreenDisplay() {

        Display display = ((WindowManager)getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        cw = display.getWidth();
        ch = display.getHeight();

    }

    private void RequestRunTimePermission(){
        TedPermission.with(this)
                .setPermissionListener(permissionlistener)
                .setPermissions(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.RECORD_AUDIO)
                .check();
    }

    PermissionListener permissionlistener = new PermissionListener() {
        @Override
        public void onPermissionGranted() {
            SaveRecordedVideoFile();
        }

        @Override
        public void onPermissionDenied(List<String> deniedPermissions) {
            Toast.makeText(C.this, "Permission Denied\n" + deniedPermissions.toString(), Toast.LENGTH_SHORT).show();
        }
    };

    public void SimpleDateFormatter(){
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.US);
        Date now = new Date();
        cf = formatter.format(now);
    }

    @SuppressLint("DefaultLocale")
    public static String VideoTime(long seconds) {
        return String.format("%02d:%02d:%02d",
                TimeUnit.MILLISECONDS.toHours(seconds),
                TimeUnit.MILLISECONDS.toMinutes(seconds) -
                        TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(seconds)), // The change is in this line
                TimeUnit.MILLISECONDS.toSeconds(seconds) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(seconds)));
    }

    public static String VideoSize(long size) {
        if (size <= 0)
            return "0";
        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    public void SaveRecordedVideoFile(){
        lists.clear();
        cmy = new ArrayList<String>();
        File directory = Environment.getExternalStorageDirectory();
        file = new File(directory + "/Screen Recording");
        File list[] = file.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String cf) {
                // TODO Auto-generated method stub
                if (cf.contains(".mp4")) {
                    return true;
                }
                return false;
            }
        });

        for (int i = 0; i < list.length; i++) {
            cmy.add(list[i].getName());
            String filepath = Environment.getExternalStorageDirectory() + "/Screen Recording/"+list[i].getName();
            File file = new File(filepath);
            long length = file.length();
            if (length < 1024){
                clt = new File(Environment.getExternalStorageDirectory() + "/Screen Recording/"+list[i].getName());
                clt.delete();
            } else {
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                retriever.setDataSource(getApplicationContext(), Uri.parse(Environment.getExternalStorageDirectory() + "/Screen Recording/"+list[i].getName()));
                String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                Time = Long.parseLong(time );
            }
            lists.add(new A("video.png", list[i].getName(), "" + VideoTime(Time),"Size : " + VideoSize(length)));

            B adapter = new B(this, R.layout.custom_listview, lists);

            if (adapter.getCount()==0)
            {
                //Set the emptyView to the ListView
                mListView.setEmptyView(findViewById(R.id.emptyElement));
            }
            else
            {
                //attaching adapter to the listview
                mListView.setAdapter(adapter);

                //Set the emptyView to the ListView
                mListView.setEmptyView(findViewById(R.id.emptyElement));
            }

        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1, cmy);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1,int position, long arg3)
            {

                //Intent intent = new Intent(Intent.ACTION_VIEW);
                Uri videoUri = Uri.parse(Environment.getExternalStorageDirectory() + "/Screen Recording/"+cmy.get(position));
                //intent.setDataAndType(uri, "video/*");
                //startActivity(intent);

                //Pass video file to another activity
                Intent videoIntent = new Intent(getBaseContext(), FullVideoActivity.class);
                videoIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                videoIntent.putExtra("videoUri", videoUri);
                startActivity(videoIntent);

            }
        });


    }

    private Intent ReturnToMainActivity() {
        Intent intent = new Intent(this, C.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return intent;
    }

    public void TriggerNotification() {

        Intent yesIntent = ReturnToMainActivity();
        yesIntent.setAction(YES_ACTION);
        notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        int notificationId = 1;
        String channelId = "channel-01";
        String channelName = "Channel Name";
        int importance = NotificationManager.IMPORTANCE_HIGH;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel(
                    channelId, channelName, importance);
            notificationManager.createNotificationChannel(mChannel);
        }

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getApplicationContext(), channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Screen Recorder App")
                .setContentText("Recording")
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(false)
                .setOngoing(true)
                .setUsesChronometer(true)
                .setVibrate(null)
                .setGroupAlertBehavior(GROUP_ALERT_SUMMARY)
                .setGroup("My group")
                .setGroupSummary(false)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .addAction(new Action(
                        R.drawable.ic_close,
                        "Cancel",
                        PendingIntent.getActivity(this, 0, yesIntent, PendingIntent.FLAG_UPDATE_CURRENT)));
        notificationManager.notify(notificationId, mBuilder.build());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        GetActionNotificationCancelled(intent);
        super.onNewIntent(intent);
    }

    private void GetActionNotificationCancelled(Intent intent) {
        if (intent.getAction() != null) {
            switch (intent.getAction()) {
                case YES_ACTION:
                    notificationManager.cancel(1);
                    cxy = "s";
                    mFloatingActionButton.setImageResource(R.drawable.ic_recode);
                    StopMediaRecorder();
                    SaveRecordedVideoFile();
                    ReloadInterstitialAd();
                    break;
            }
        }
    }
    
    public void RequestPermissionToStartRecording(){
        if (ContextCompat.checkSelfPermission(C.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) + ContextCompat
                .checkSelfPermission(C.this,
                        Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale
                    (C.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
                    ActivityCompat.shouldShowRequestPermissionRationale
                            (C.this, Manifest.permission.RECORD_AUDIO)) {
            } else {
                ActivityCompat.requestPermissions(C.this,
                        new String[]{Manifest.permission
                                .WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO},
                        REQUEST_RUNTIME_CODE);
            }
        } else {
            PrepareMediaRecorder();
            CreateMediaProjectionAndStartRecording();
        }
    }

    public void StopMediaRecorder(){
        mMediaRecorder.stop();
        mMediaRecorder.reset();
        Log.v(TAG, "Stopping Recording");
        ReleaseVirtualDisplay();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_CODE) {
            Log.e(TAG, "Unknown request code: " + requestCode);
            return;
        }
        if (resultCode != RESULT_OK) {
            Toast.makeText(this, "Screen Cast Permission Denied", Toast.LENGTH_SHORT).show();
            notificationManager.cancel(1);
            SaveRecordedVideoFile();
            cxy = "s";
            mFloatingActionButton.setImageResource(R.drawable.ic_recode);
            return;
        }else {
            cxy = "t";
            mFloatingActionButton.setImageResource(R.drawable.ic_clear);
            TriggerNotification();
            SendUserToPhoneHomeScreen();
            LoadInterstitialAd();
        }
        mMediaProjectionCallback = new MediaProjectionCallback();
        mMediaProjection = mMediaProjectionManager.getMediaProjection(resultCode, data);
        mMediaProjection.registerCallback(mMediaProjectionCallback, null);
        mVirtualDisplay = createVirtualDisplay();
        mMediaRecorder.start();
    }

    public void SendUserToPhoneHomeScreen() {
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startMain);
    }

    public void LoadInterstitialAd(){
        mInterstitialAd.loadAd(new AdRequest.Builder().build());
    }

    public void ReloadInterstitialAd(){
        if (mInterstitialAd.isLoaded()) {
            mInterstitialAd.show();
        }else {
            mInterstitialAd.loadAd(new AdRequest.Builder().build());
        }

    }

    // Request permission to start recording screen
    private void CreateMediaProjectionAndStartRecording() {
        if (mMediaProjection == null) {
            startActivityForResult(mMediaProjectionManager.createScreenCaptureIntent(), REQUEST_CODE);
            return;
        }
        mVirtualDisplay = createVirtualDisplay();
        mMediaRecorder.start();
    }

    private VirtualDisplay createVirtualDisplay() {
        return mMediaProjection.createVirtualDisplay("C",
                cw, ch, cms,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mMediaRecorder.getSurface(), null /*Callbacks*/, null
                /*Handler*/);
    }

    private void PrepareMediaRecorder() {

        SimpleDateFormatter();

        try {
            CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mMediaRecorder.setOutputFile(Environment.getExternalStorageDirectory()+"/Screen Recording" + "/Screen Recording "+cf+".mp4");
            mMediaRecorder.setVideoSize(cw, ch);
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mMediaRecorder.setVideoEncodingBitRate(profile.videoBitRate);
            mMediaRecorder.setCaptureRate(20);
            mMediaRecorder.setVideoFrameRate(20);
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            int orientation = mSparseIntArray.get(rotation + 90);
            mMediaRecorder.setOrientationHint(orientation);
            mMediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class MediaProjectionCallback extends MediaProjection.Callback {
        @Override
        public void onStop() {
            mMediaRecorder.stop();
            mMediaRecorder.reset();
            Log.v(TAG, "Recording Stopped");

            mMediaProjection = null;
            ReleaseVirtualDisplay();
        }
    }

    private void ReleaseVirtualDisplay() {
        if (mVirtualDisplay == null) {
            return;
        }
        mVirtualDisplay.release();
        UnregisterMediaProjectionCallback();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        UnregisterMediaProjectionCallback();
    }

    private void UnregisterMediaProjectionCallback() {
        if (mMediaProjection != null) {
            mMediaProjection.unregisterCallback(mMediaProjectionCallback);
            mMediaProjection.stop();
            mMediaProjection = null;
        }
        Log.i(TAG, "MediaProjection Stopped");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {

        switch (requestCode)
        {
            case REQUEST_RUNTIME_CODE: {

                if ((grantResults.length > 0) && (grantResults[0] + grantResults[1]) == PackageManager.PERMISSION_GRANTED) {

                    // if permission granted do what you want to do

                } else {

                    // request for runtime permissions supported by app
                    RequestRunTimePermission();

                }
                return;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_toolbar, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.share ){

            try {
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("text/plain");
                i.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
                String sAux = "\n "+getString(R.string.app_name)+" - Download Now\n\n";
                sAux = sAux + "https://play.google.com/store/apps/details?id="+getPackageName()+" \n\n";
                i.putExtra(Intent.EXTRA_TEXT, sAux);
                startActivity(Intent.createChooser(i, "choose one"));
            } catch(Exception e) {

            }
        }

        switch (item.getItemId())
        {
            case R.id.menu_about:

                Toast.makeText(this, "About", Toast.LENGTH_SHORT).show();
                return true;

            case R.id.menu_rate:

                Toast.makeText(this, "Rate", Toast.LENGTH_SHORT).show();
                return true;

            case R.id.menu_settings:

                Toast.makeText(this, "Settings", Toast.LENGTH_SHORT).show();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
