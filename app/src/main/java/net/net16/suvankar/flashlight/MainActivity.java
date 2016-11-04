package net.net16.suvankar.flashlight;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Camera;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    private static Camera camera = null;
    private final int MY_PERMISSIONS_REQUEST_CAMERA = 1;
    private Animation animFadein;
    private Animation animFadeout;
    private boolean lightOn = false;
    private ImageView ring;
    private ImageView ring2;
    private ImageView ring3;
    private ImageView ring4;

    private AdView mAdView;
    private AdRequest adRequest;
    private IntentFilter ifilter;
    private Intent batteryStatus;
    private Timer timer = new Timer();
    private TimerTask timerTask;
    private Switch aSwitch;

    private InterstitialAd mInterstitialAd;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Only potrait mode
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        //Remove notification bar
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);

        //battery level

        final TextView batteryTextView = (TextView) findViewById(R.id.battery);
        timerTask = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
                        batteryStatus = registerReceiver(null, ifilter);

                        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                        if (batteryTextView != null) {
                            batteryTextView.setText("Battery level "+level+"%");
                        }
                        if(level >= 50){
                            batteryTextView.setTextColor(Color.GREEN);
                        }
                        else if(level>=30 && level < 50){
                            batteryTextView.setTextColor(Color.YELLOW);
                        }
                        else if(level <30){
                            Log.d("battery",level+"");
                            batteryTextView.setTextColor(Color.RED);
                        }
                        batteryTextView.setTextColor(batteryTextView.getTextColors().withAlpha(150));
                    }
                });
            }
        };
        timer.schedule(timerTask, 0, 10000);

        //for interstitial ad on turning the switch off
        mInterstitialAd = new InterstitialAd(MainActivity.this);
        mInterstitialAd.setAdUnitId("ca-app-pub-1300395620912543/4131906111");
        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                if(!lightOn) {
                    mInterstitialAd.show();
                }
            }
        });
        TimerTask adTimerTask = new TimerTask() {
            @Override
            public void run() {
                Log.d("TAG","***********************");
                if(lightOn == false && mInterstitialAd!=null) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(!mInterstitialAd.isLoaded()) {
                                AdRequest adRequest = new AdRequest.Builder().build();
                                mInterstitialAd.loadAd(adRequest);
                            }
                        }
                    });
                }
            }
        };
        Timer adTimer = new Timer();
        adTimer.schedule(adTimerTask,1000, 10000);

        // load the animation
        animFadein = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        animFadein.setStartOffset(100);

        animFadeout = AnimationUtils.loadAnimation(this, R.anim.fade_out);
        animFadeout.setStartOffset(500);

        ring = (ImageView) findViewById(R.id.ring);
        ring2 = (ImageView) findViewById(R.id.ring2);
        ring3 = (ImageView) findViewById(R.id.ring3);
        ring4 = (ImageView) findViewById(R.id.ring4);

        ring.setVisibility(View.INVISIBLE);
        ring2.setVisibility(View.INVISIBLE);
        ring3.setVisibility(View.INVISIBLE);
        ring4.setVisibility(View.INVISIBLE);

        animFadein.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if(!lightOn)
                    return;
                ring.startAnimation(animFadeout);
                ring2.startAnimation(animFadeout);
                ring3.startAnimation(animFadeout);
                ring4.startAnimation(animFadeout);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        animFadeout.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if(!lightOn)
                    return;

                ring.startAnimation(animFadein);
                ring2.startAnimation(animFadein);
                ring3.startAnimation(animFadein);
                ring4.startAnimation(animFadein);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        if(ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_PHONE_STATE)) {
                Toast.makeText(this,"We need camera permission to switch on flashlight!", Toast.LENGTH_SHORT).show();
            }
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.CAMERA},MY_PERMISSIONS_REQUEST_CAMERA);
        }
        else {
            flashOnOffController();
        }

        aSwitch =(Switch)findViewById(R.id.switch1);
        final Intent intent = new Intent(this, ScreenIlluminateActivity.class);
        aSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    startActivity(intent);
                }
                aSwitch.setChecked(false);
            }
        });

        //AdMob
        MobileAds.initialize(getApplicationContext(), "ca-app-pub-1300395620912543~9725845314");
        mAdView = (AdView) findViewById(R.id.adView);
        adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_CAMERA: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    flashOnOffController();

                } else {

                    Toast.makeText(this,"We need camera permission to switch on flashlight!", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void flashOnOffController(){
        final ImageButton power = (ImageButton) findViewById(R.id.button);
        final boolean hasFlash = getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
        if (power == null) throw new AssertionError();
        if (hasFlash) {
            power.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!lightOn) {
                        flashOn();
                    } else if (lightOn) {
                        flashOff();
                    }
                }


            });
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        flashOff();
        if(mInterstitialAd!=null)
            mInterstitialAd.setAdListener(null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //turn on the flash
        //flashOn();
        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                if(!lightOn) {
                    mInterstitialAd.show();
                }
            }
        });
    }

    protected void flashOn(){
        //turn on the flash
        camera = Camera.open();
        Camera.Parameters parameters = camera.getParameters();
        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        camera.setParameters(parameters);
        camera.startPreview();
        ring.setVisibility(View.VISIBLE);
        ring2.setVisibility(View.VISIBLE);
        ring3.setVisibility(View.VISIBLE);
        ring4.setVisibility(View.VISIBLE);

        //start the animation
        ring.startAnimation(animFadein);
        ring.startAnimation(animFadeout);
        ring2.startAnimation(animFadein);
        ring2.startAnimation(animFadeout);

        ring3.startAnimation(animFadein);
        ring3.startAnimation(animFadeout);

        ring4.startAnimation(animFadein);
        ring4.startAnimation(animFadeout);
        lightOn = true;


    }

    protected  void flashOff() {
        lightOn = false;
        if(camera!=null) {
            camera.stopPreview();
            camera.release();
        }
        camera = null;
        ring.clearAnimation();
        ring2.clearAnimation();
        ring3.clearAnimation();
        ring4.clearAnimation();

        ring.setVisibility(View.INVISIBLE);
        ring2.setVisibility(View.INVISIBLE);
        ring3.setVisibility(View.INVISIBLE);
        ring4.setVisibility(View.INVISIBLE);
    }
}

