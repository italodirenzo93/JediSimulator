package com.idirenzo.mobiledev.jedisimulator;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    // Constants
    private static final float SWING_THRESHOLD = 14f;
    private static final float HIT_THREASHOLD = 26f;

    private static final int RAND_MAX = 100;
    private static final int WILHELM_CHANCE = 5;

    private static final int SENSOR_DELAY = SensorManager.SENSOR_DELAY_FASTEST;

    private static final float PULSE_VOLUME = 0.7f;
    private static final float ON_OFF_VOLUME = 1.0f;
    private static final float SWING_HIT_VOLUME = 0.75f;
    private static final float WILHELM_VOLUME = 1.0f;

    // Saber colours
    private List<String> saberColours = new ArrayList<>();
    private String currentSaberColour;

    // Sound IDs
    private SoundPool soundPool;
    private MediaPlayer saberPulse;
    private int sidLightSaberOn;
    private int sidLightSaberOff;
    private int sidWilhelm;
    private int[] sidLightSaberSwing = new int[4];
    private int[] sidLightSaberHit = new int[4];

    // Sensor stuff
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private float acceleration;
    private float lastAcceleration;
    private float currentAcceleration;

    // UI controls
    private Button buttonSaber;

    // Additional stuff
    private Random rand = new Random();
    private boolean lightSaberOn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Keep the screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Set up sensors
        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, accelerometer, SENSOR_DELAY);

        lastAcceleration = SensorManager.GRAVITY_EARTH;
        currentAcceleration = SensorManager.GRAVITY_EARTH;

        // Set up audio
        saberPulse = MediaPlayer.create(this, R.raw.lightsaberpulse);
        saberPulse.setVolume(PULSE_VOLUME, PULSE_VOLUME);
        saberPulse.setLooping(true);

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(12)
                .setAudioAttributes(audioAttributes)
                .build();

        sidLightSaberOn = soundPool.load(this, R.raw.ltsaberon01, 1);
        sidLightSaberOff = soundPool.load(this, R.raw.ltsaberoff01, 1);
        sidWilhelm = soundPool.load(this, R.raw.wilhelm, 1);

        sidLightSaberSwing[0] = soundPool.load(this, R.raw.ltsaberswing01, 1);
        sidLightSaberSwing[1] = soundPool.load(this, R.raw.ltsaberswing02, 1);
        sidLightSaberSwing[2] = soundPool.load(this, R.raw.ltsaberswing03, 1);
        sidLightSaberSwing[3] = soundPool.load(this, R.raw.ltsaberswing04, 1);

        sidLightSaberHit[0] = soundPool.load(this, R.raw.ltsaberhit01, 1);
        sidLightSaberHit[1] = soundPool.load(this, R.raw.ltsaberhit02, 1);
        sidLightSaberHit[2] = soundPool.load(this, R.raw.ltsaberhit03, 1);
        sidLightSaberHit[3] = soundPool.load(this, R.raw.ltsaberhit15, 1);

        // Set up saber colours
        saberColours.add("Red");
        saberColours.add("Green");
        saberColours.add("Blue");

        // Get the saved saber colour of default to Blue
        SharedPreferences preferences = getPreferences(Context.MODE_PRIVATE);
        currentSaberColour = preferences.getString("SaberColour", "Blue");

        // UI
        buttonSaber = (Button)findViewById(R.id.buttonSaber);
        updateSaberImage();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        // Utilize "Immersive Mode", new in Android 4.4 KitKat
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
        if (lightSaberOn) {
            saberPulse.pause();
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        sensorManager.registerListener(this, accelerometer, SENSOR_DELAY);
        if (lightSaberOn) {
            saberPulse.start();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        float x = sensorEvent.values[0];
        float y = sensorEvent.values[1];
        float z = sensorEvent.values[2];
        lastAcceleration = currentAcceleration;
        currentAcceleration = (float)Math.sqrt((double)x*x + y*y + z*z);
        float delta = currentAcceleration - lastAcceleration;
        acceleration = acceleration * 0.9f + delta;

        // Check for lightsaber swing
        if (lightSaberOn && acceleration > SWING_THRESHOLD) {
            // If the phone was swung hard enough, play a hit effect
            if (acceleration > HIT_THREASHOLD) {
                int soundId = rand.nextInt(sidLightSaberHit.length);
                soundPool.play(sidLightSaberHit[soundId], SWING_HIT_VOLUME, SWING_HIT_VOLUME, 0, 0, 1);

                // What action scene can't be made better with a "Wilhelm Scream"?
                if (rand.nextInt(RAND_MAX) < WILHELM_CHANCE) {
                    soundPool.play(sidWilhelm, WILHELM_VOLUME, WILHELM_VOLUME, 1, 0, 1);
                }
            } else {    // Play a swing effect
                int soundId = rand.nextInt(sidLightSaberSwing.length);
                soundPool.play(sidLightSaberSwing[soundId], SWING_HIT_VOLUME, SWING_HIT_VOLUME, 0, 0, 1);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {}

    // Click handlers
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.buttonHelpAbout:
                startActivity(new Intent(this, HelpAboutActivity.class));
                break;
            case R.id.buttonSaveColour:
                SharedPreferences preferences = getPreferences(Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString("SaberColour", currentSaberColour);
                editor.apply();
                Toast.makeText(this, "Colour Saved", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    public void toggleLightSaber(View view) {
        if (!lightSaberOn) {
            soundPool.play(sidLightSaberOn, ON_OFF_VOLUME, ON_OFF_VOLUME, 0, 0, 1);
            saberPulse.start();
            lightSaberOn = true;
        } else {
            soundPool.play(sidLightSaberOff, ON_OFF_VOLUME, ON_OFF_VOLUME, 0, 0, 1);
            saberPulse.stop();
            try {
                saberPulse.prepare();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error Re-Initializing Saber Pulse Sound", Toast.LENGTH_SHORT).show();
            }
            lightSaberOn = false;
        }
        updateSaberImage();
    }

    public void cycleSaberColour(View view) {
        int pos = saberColours.indexOf(currentSaberColour);
        if (view.getId() == R.id.buttonNextSaber) {
            // Next saber
            if (pos + 1 >= saberColours.size()) {
                currentSaberColour = saberColours.get(0);
            } else {
                currentSaberColour = saberColours.get(pos + 1);
            }
        } else {
            // Previous saber
            if (pos - 1 < 0) {
                currentSaberColour = saberColours.get(saberColours.size() - 1);
            } else {
                currentSaberColour = saberColours.get(pos - 1);
            }
        }
        updateSaberImage();
    }

    // Helpers
    private void updateSaberImage() {
        switch (currentSaberColour) {
            case "Red":
                if (lightSaberOn) {
                    buttonSaber.setBackground(getDrawable(R.drawable.lightsaber_red));
                } else {
                    buttonSaber.setBackground(getDrawable(R.drawable.lightsaber_red_hilt));
                }
                break;
            case "Green":
                if (lightSaberOn) {
                    buttonSaber.setBackground(getDrawable(R.drawable.lightsaber_green));
                } else {
                    buttonSaber.setBackground(getDrawable(R.drawable.lightsaber_green_hilt));
                }
                break;
            case "Blue":
                if (lightSaberOn) {
                    buttonSaber.setBackground(getDrawable(R.drawable.lightsaber_blue));
                } else {
                    buttonSaber.setBackground(getDrawable(R.drawable.lightsaber_blue_hilt));
                }
                break;
        }
    }
}
