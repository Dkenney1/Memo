package edu.illinois.cs.cs125.thotpatrol.verbalmemoapp;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.media.MediaRecorder;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private boolean canWriteToPublicStorage = false;
    private boolean canRecordAudio = false;
    private static final int REQUEST_WRITE_STORAGE = 112;
    private static final int REQUEST_MICROPHONE_ACCESS = 110;
    private static final int READ_REQUEST_CODE = 42;
    private static final int AUDIO_CAPTURE_REQUEST_CODE = 1;
    private static String fileName = null;

    MediaRecorder soundRecorder = null;
    MediaPlayer soundPlayer = new MediaPlayer();

    private File currentMemoFile = null;

    private void onRecord(boolean start) {
        if (start) {
            startRecordAudio();
        } else {
            stopRecordAudio();
        }
    }
    private void onPlay(boolean start) {
        if (start) {
            startPlaying();
        } else {
            stopPlaying();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fileName = getExternalCacheDir().getAbsolutePath();
        fileName += "/memoFiles";
        fileName += "/" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                .format(new Date());
        fileName += ".3gp";
        /**
         * TODO Implement file selection
         * Currently can only play the file made upon app start up.
         * Can also only record over the file made upon app start up.
         */
        canWriteToPublicStorage = (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
        canRecordAudio = (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED);
        if (!canWriteToPublicStorage || !canRecordAudio) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO},
                    REQUEST_WRITE_STORAGE);
        }

        final ImageButton recordAudio = findViewById(R.id.recordButton);
        recordAudio.setOnClickListener(new View.OnClickListener() {
            boolean beginRecording = true;
            @Override
            public void onClick(final View v) {
                onRecord(beginRecording);
                if (beginRecording) {
                    beginRecording = false;
                } else {
                    beginRecording = true;
                }
            }
        });
        final ImageButton playAudio = findViewById(R.id.playButton);
        playAudio.setOnClickListener(new View.OnClickListener() {
            boolean beginPlaying = true;
            @Override
            public void onClick(final View v) {
                onPlay(beginPlaying);
                if (beginPlaying) {
                    beginPlaying = false;
                } else {
                    beginPlaying = true;
                }
            }
        });
        final ImageButton openFile = findViewById(R.id.openFileButton);
        openFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                startOpenFile();
            }
        });

        ProgressBar progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.INVISIBLE);
        progressBar.getIndeterminateDrawable()
                .setColorFilter(getResources()
                        .getColor(R.color.colorPrimaryDark), PorterDuff.Mode.SRC_IN);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    public void onActivityResult(final int requestCode, final int resultCode,
                                 final Intent resultData) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        Uri currentAudioURI;
        if (requestCode == READ_REQUEST_CODE) {
            currentAudioURI = resultData.getData();
            fileName = currentAudioURI.getPath();
        } else if (requestCode == AUDIO_CAPTURE_REQUEST_CODE) {
            currentAudioURI = Uri.fromFile(currentMemoFile);
            if (canWriteToPublicStorage) {
                addAudioToGallery(currentAudioURI);
            }
        } else {
            return;
        }
    }

    private void startOpenFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setType("audio/*");
        startActivityForResult(intent, READ_REQUEST_CODE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Begins the process of recording audio.
     */
    private void startRecordAudio() {
        currentMemoFile = getSaveFilename();
        fileName = getSaveFilename().toString();

        /*Intent recordIntent = new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION);


        recordIntent.putExtra(MediaStore.EXTRA_OUTPUT, audioURI);
        startActivityForResult(recordIntent, AUDIO_CAPTURE_REQUEST_CODE);
        */

        soundRecorder = new MediaRecorder();
        soundRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        soundRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        soundRecorder.setOutputFile(fileName);
        soundRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try{
            soundRecorder.prepare();
        } catch (Exception e) {
            System.out.println("Crap");
        }
        soundRecorder.start();
    }
    private void stopRecordAudio() {
        soundRecorder.stop();
        /*Uri audioURI = FileProvider.getUriForFile(this,
                "com.github.Dkenny1.Memo.fileprovider", currentMemoFile);
        addAudioToGallery(audioURI);*/
        soundRecorder.release();
        soundRecorder = null;
    }
    private void startPlaying() {
        soundPlayer = new MediaPlayer();
        try {
            soundPlayer.setDataSource(fileName);
            soundPlayer.prepare();
            soundPlayer.start();
        } catch (IOException e) {
            System.out.println("Crap");
        }
    }

    private void stopPlaying() {
        soundPlayer.release();
        soundPlayer = null;
    }
    void addAudioToGallery(final Uri toAdd) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(toAdd);
        this.sendBroadcast(mediaScanIntent);
    }
    File getSaveFilename() {
        String imageFileName = "Memo_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                .format(new Date());
        File storageDir;
        if (canWriteToPublicStorage) {
            storageDir = Environment
                    .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        } else {
            storageDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        }
        try {
            return File.createTempFile(imageFileName, ".3gp", storageDir);
        } catch (IOException e) {
            return null;
        }
    }
    @Override
    public void onStop() {
        super.onStop();
        if (soundRecorder != null) {
            soundRecorder.release();
            soundRecorder = null;
        }
        if (soundPlayer != null) {
            soundPlayer.release();
            soundPlayer = null;
        }
    }
}
