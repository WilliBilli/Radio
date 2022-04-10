package vip.mannheim.radio;

import android.content.ContentResolver;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

public class Splash extends AppCompatActivity {

    //Variable to store brightness value
    public int brightness;
    //Content resolver used as a handle to the system's settings
    private ContentResolver cResolver;
    //Window object, that will store a reference to the current window
    private Window window;

    VideoView videoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle arguments = getIntent().getExtras();
        final String power = (String) arguments.get("power");

        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        // Set the content to appear under the system bars so that the
                        // content doesn't resize when the system bars hide and show.
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        // Hide the nav bar and status bar
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);

        setContentView(R.layout.activity_splash);
        getSupportActionBar().hide();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(getApplicationContext())) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 200);
            } else {
                //Get the content resolver
                cResolver = getContentResolver();

                //Get the current window
                window = getWindow();

                try
                {
                    // To handle the auto
                    Settings.System.putInt(cResolver,
                            Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
                    //Get the current system brightness
                    brightness = Settings.System.getInt(cResolver, Settings.System.SCREEN_BRIGHTNESS);
                }
                catch (Settings.SettingNotFoundException e)
                {
                    //Throw an error case it couldn't be retrieved
                    Log.e("Error", "Cannot access system brightness");
                    e.printStackTrace();
                }

                if(power.equals("on")){
                    Settings.System.putInt(this.getContentResolver(),
                            Settings.System.SCREEN_BRIGHTNESS, 255);
                }else if(power.equals("off")){
                    Settings.System.putInt(this.getContentResolver(),
                            Settings.System.SCREEN_BRIGHTNESS, 0);
                }
            }
        }

        videoView = (VideoView) findViewById(R.id.videoView);

        if(power.equals("on")) {
            Uri video = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.video_in);
            videoView.setVideoURI(video);
        }else if(power.equals("off")) {
            Uri video = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.video_out);
            videoView.setVideoURI(video);
        }

        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                if (isFinishing())
                    return;
                if(power.equals("on")){

                    startActivity(new Intent(Splash.this, MainActivity.class));
                    overridePendingTransition(R.anim.activity_anim_in, R.anim.activity_anim_out);
                    finish();
                }else{
                    videoView.setVisibility(View.INVISIBLE);
                }
            }
        });
        videoView.start();
    }
}
