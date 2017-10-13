package com.idirenzo.mobiledev.jedisimulator;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;

import java.io.IOException;
import java.util.Random;

/**
 * Created by Italo on 2017-08-23.
 */

public class SoundEngine {
    private static final float PULSE_VOLUME = 0.7f;
    private static final float ON_OFF_VOLUME = 1.0f;
    private static final float SWING_HIT_VOLUME = 0.75f;
    private static final float WILHELM_VOLUME = 1.0f;

    // Sound IDs
    private SoundPool soundPool;
    private MediaPlayer saberPulse;
    private int sidLightSaberOn;
    private int sidLightSaberOff;
    private int sidWilhelm;
    private int[] sidLightSaberSwing;
    private int[] sidLightSaberHit;

    private Random rand = new Random();
    
    public SoundEngine(Context context) {
        // Set up audio
        saberPulse = MediaPlayer.create(context, R.raw.lightsaberpulse);
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

        sidLightSaberOn = soundPool.load(context, R.raw.ltsaberon01, 1);
        sidLightSaberOff = soundPool.load(context, R.raw.ltsaberoff01, 1);
        sidWilhelm = soundPool.load(context, R.raw.wilhelm, 1);

        sidLightSaberSwing = new int[4];
        sidLightSaberSwing[0] = soundPool.load(context, R.raw.ltsaberswing01, 1);
        sidLightSaberSwing[1] = soundPool.load(context, R.raw.ltsaberswing02, 1);
        sidLightSaberSwing[2] = soundPool.load(context, R.raw.ltsaberswing03, 1);
        sidLightSaberSwing[3] = soundPool.load(context, R.raw.ltsaberswing04, 1);

        sidLightSaberHit = new int[4];
        sidLightSaberHit[0] = soundPool.load(context, R.raw.ltsaberhit01, 1);
        sidLightSaberHit[1] = soundPool.load(context, R.raw.ltsaberhit02, 1);
        sidLightSaberHit[2] = soundPool.load(context, R.raw.ltsaberhit03, 1);
        sidLightSaberHit[3] = soundPool.load(context, R.raw.ltsaberhit15, 1);
    }

    public void startSaberPulse() {
        saberPulse.start();
    }

    public void pauseSaberPulse() {
        saberPulse.pause();
    }

    public void stopSaberPulse() {
        saberPulse.stop();
        try {
            saberPulse.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void playSaberOn() {
        soundPool.play(sidLightSaberOn, ON_OFF_VOLUME, ON_OFF_VOLUME, 0, 0, 1);
    }

    public void playSaberOff() {
        soundPool.play(sidLightSaberOff, ON_OFF_VOLUME, ON_OFF_VOLUME, 0, 0, 1);
    }

    public void playSaberSwing() {
        int soundId = rand.nextInt(sidLightSaberSwing.length);
        soundPool.play(sidLightSaberSwing[soundId], SWING_HIT_VOLUME, SWING_HIT_VOLUME, 0, 0, 1);
    }

    public void playSaberHit() {
        int soundId = rand.nextInt(sidLightSaberHit.length);
        soundPool.play(sidLightSaberHit[soundId], SWING_HIT_VOLUME, SWING_HIT_VOLUME, 0, 0, 1);
    }

    public void playWilhelmScream() {
        soundPool.play(sidWilhelm, WILHELM_VOLUME, WILHELM_VOLUME, 1, 0, 1);
    }
}
