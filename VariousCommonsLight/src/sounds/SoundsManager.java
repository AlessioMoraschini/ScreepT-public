/**
 *
 * =========================================================================================
 *  Copyright (C) 2019-2020
 *
 *  AM-Design-Development - (Alessio Moraschini) - All Rights Reserved
 * =========================================================================================
 *
 * You should have received a copy of the license with this file.
 * If not, please write to: info@am-design-development.com, or visit : https://www.am-design-development.com
 */
package sounds;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;

public class SoundsManager {
	
	public static final float DEFAULT_VOLUME = 0.8f;
	public static float volume = DEFAULT_VOLUME;
	
	public static volatile boolean MUTED = false;

	// Preset static volumes
	public static final float MAX = 1.0f;
	public static final float HIGH = 0.8f;
	public static final float MED_HIGH = 0.6f;
	public static final float MED = 0.5f;
	public static final float MED_LOW = 0.4f;
	public static final float LOW = 0.3f;
	public static final float MIN = 0.2f;
	public static final float MUTE = 0f;

	public static AudioInputStream openStream = null;
	public static AudioInputStream openStreamInstance = null;
	
	public float volumeInstance = DEFAULT_VOLUME;
	private int lastFrame;
	public Clip clip;
	
	public static void playSound(String pathToResource, Float volum){
		
		if(MUTED)
			return;
		
		setGeneralVolume(volum);
		File audioFile = new File(pathToResource);
		if(audioFile == null || !audioFile.exists()) {
			return;
		}
		
		AudioInputStream inputStream = null;
		try {
			Clip clip = AudioSystem.getClip();
	        inputStream = AudioSystem.getAudioInputStream(audioFile);
	        clip.open(inputStream);
	        applyVolume(clip, volume);
	        clip.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if(inputStream != null) {
			try {
				inputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void play(String pathToResource, Float volum){
		if(volum != null) {
			setVolume(volum);
		}else {
			setVolume(DEFAULT_VOLUME);
		}
		File audioFile = new File(pathToResource);
		if(audioFile == null || !audioFile.exists()) {
			return;
		}
		
		AudioInputStream inputStream = null;
		try {
			clip = AudioSystem.getClip();
			inputStream = AudioSystem.getAudioInputStream(audioFile);
			clip.open(inputStream);
			applyVolume(clip, volumeInstance);
			clip.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if(inputStream != null) {
			try {
				inputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void stop() {
		if(clip != null) {
			clip.stop();
		}
	}

	public void loop(String pathToResource, Float volum){
		if(volum != null) {
			setVolume(volum);
		}else {
			setVolume(DEFAULT_VOLUME);
		}
		
		File audioFile = new File(pathToResource);
		if(audioFile == null || !audioFile.exists()) {
			return;
		}
		
		AudioInputStream inputStream = null;
		try {
			clip = AudioSystem.getClip();
			inputStream = AudioSystem.getAudioInputStream(audioFile);
			clip.open(inputStream);
			applyVolume(clip, volumeInstance);
			clip.loop(Clip.LOOP_CONTINUOUSLY);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if(inputStream != null) {
			try {
				inputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void applyVolume(Clip clip, float volume) {
        FloatControl volumeManager = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
        float range = volumeManager.getMaximum() - volumeManager.getMinimum();
        float calculatedVolume = volumeManager.getMinimum() + (range * volume);
        volumeManager.setValue(calculatedVolume);
	}

	public void applyVolume() {
		FloatControl volumeManager = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
		float range = volumeManager.getMaximum() - volumeManager.getMinimum();
		float calculatedVolume = volumeManager.getMinimum() + (range * volumeInstance);
		volumeManager.setValue(calculatedVolume);
	}
	
	public static void mute(Clip clip) {
		FloatControl volumeManager = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
        volumeManager.setValue(volumeManager.getMinimum());
	}

	public void pause() {

        if (clip != null && clip.isRunning()) {
            lastFrame = clip.getFramePosition();
            clip.stop();
        }
    }

    public void resumeLoop() {

        if (clip != null && !clip.isRunning()) {
            // Make sure we haven't passed the end of the file...
            if (lastFrame < clip.getFrameLength()) {
                clip.setFramePosition(lastFrame);
            } else {
                clip.setFramePosition(0);
            }
            
            clip.loop(Clip.LOOP_CONTINUOUSLY);
        }
    }

	public static float getGeneralVolume() {
		return volume;
	}
	
	public static float adaptVolume(float volume) {
		volume = volume* (1.0f + volume);
		
		volume = (volume > 1)? 1.0f : volume;
		volume = (volume < 0)? 0.0f : volume;
		
		return volume;
	}

	public static void setGeneralVolume(float volume) {
		SoundsManager.volume = adaptVolume(volume);
	}

	public void setVolume(float volume) {
		this.volumeInstance = adaptVolume(volume);
	}
}
