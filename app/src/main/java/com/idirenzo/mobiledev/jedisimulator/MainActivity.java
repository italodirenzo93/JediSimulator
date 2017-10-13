package com.idirenzo.mobiledev.jedisimulator;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
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
    private static final float HIT_THRESHOLD = 26f;

    private static final int RAND_MAX = 100;
    private static final int WILHELM_CHANCE = 5;

    private static final int SENSOR_DELAY = SensorManager.SENSOR_DELAY_FASTEST;

    // Saber colours
    private static final int SABER_COLOUR_BLUE = 0;
    private static final int SABER_COLOUR_GREEN = 1;
    private static final int SABER_COLOUR_RED = 2;
    private int currentSaberColour;

    // Sensor stuff
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private float acceleration;
    private float lastAcceleration;
    private float currentAcceleration;

    // Sounds
    private SoundEngine soundEngine;

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

        // Set up sound
        soundEngine = new SoundEngine(this);

        // Get the saved saber colour of default to Blue
        SharedPreferences preferences = getPreferences(Context.MODE_PRIVATE);
        currentSaberColour = preferences.getInt("SaberColour", SABER_COLOUR_BLUE);

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
            soundEngine.pauseSaberPulse();
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        sensorManager.registerListener(this, accelerometer, SENSOR_DELAY);
        if (lightSaberOn) {
            soundEngine.startSaberPulse();
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
            if (acceleration > HIT_THRESHOLD) {
                soundEngine.playSaberHit();

                // What action scene can't be made better with a "Wilhelm Scream"?
                if (rand.nextInt(RAND_MAX) < WILHELM_CHANCE) {
                    soundEngine.playWilhelmScream();
                }
            } else {    // Play a swing effect
                soundEngine.playSaberSwing();
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
                editor.putInt("SaberColour", currentSaberColour);
                editor.apply();
                Toast.makeText(this, "Colour Saved", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    public void toggleLightSaber(View view) {
        if (!lightSaberOn) {
            soundEngine.playSaberOn();
            soundEngine.startSaberPulse();
            lightSaberOn = true;
        } else {
            soundEngine.playSaberOff();
            soundEngine.stopSaberPulse();
            lightSaberOn = false;
        }
        updateSaberImage();
    }

    public void cycleSaberColour(View view) {
        if (view.getId() == R.id.buttonNextSaber) {
            // Next saber
            switch (currentSaberColour) {
                default:
                case SABER_COLOUR_RED:
                    currentSaberColour = SABER_COLOUR_GREEN;
                    break;
                case SABER_COLOUR_GREEN:
                    currentSaberColour = SABER_COLOUR_BLUE;
                    break;
                case SABER_COLOUR_BLUE:
                    currentSaberColour = SABER_COLOUR_RED;
                    break;
            }
        } else {
            // Previous saber
            switch (currentSaberColour) {
                default:
                case SABER_COLOUR_RED:
                    currentSaberColour = SABER_COLOUR_BLUE;
                    break;
                case SABER_COLOUR_GREEN:
                    currentSaberColour = SABER_COLOUR_GREEN;
                    break;
                case SABER_COLOUR_BLUE:
                    currentSaberColour = SABER_COLOUR_RED;
                    break;
            }
        }
        updateSaberImage();
    }

    // Helpers
    private void updateSaberImage() {
        switch (currentSaberColour) {
            case SABER_COLOUR_RED:
                if (lightSaberOn) {
                    buttonSaber.setBackground(getDrawable(R.drawable.lightsaber_red));
                } else {
                    buttonSaber.setBackground(getDrawable(R.drawable.lightsaber_red_hilt));
                }
                break;
            case SABER_COLOUR_GREEN:
                if (lightSaberOn) {
                    buttonSaber.setBackground(getDrawable(R.drawable.lightsaber_green));
                } else {
                    buttonSaber.setBackground(getDrawable(R.drawable.lightsaber_green_hilt));
                }
                break;
            case SABER_COLOUR_BLUE:
                if (lightSaberOn) {
                    buttonSaber.setBackground(getDrawable(R.drawable.lightsaber_blue));
                } else {
                    buttonSaber.setBackground(getDrawable(R.drawable.lightsaber_blue_hilt));
                }
                break;
        }
    }
}
