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
package conversion;

import org.apache.log4j.Logger;

import com.sun.speech.freetts.*;
//import javax.speech.*;
//import javax.speech.synthesis.Synthesizer;
//import javax.speech.synthesis.SynthesizerModeDesc;
//import javax.speech.synthesis.Voice; 

public class Text2Speech {
	
	// TextFileWriter logger creation
	static Logger logger = Logger.getLogger(Text2Speech.class);
	
	String speaktext; 
	public Voice voice;
	
	public void dospeak(String speak,String voicename, float speed, float volume)    
	{    
	    speaktext=speak;    
	    String voiceName =voicename;    
	    try    
	    {    
	    	System.setProperty("freetts.voices",  
	    	        "com.sun.speech.freetts.en.us.cmu_us_kal.KevinVoiceDirectory");  
//	        SynthesizerModeDesc desc = new SynthesizerModeDesc(null,"general",  Locale.US,null,null);    
//	        Synthesizer synthesizer =  Central.createSynthesizer(desc);    
	        //synthesizer.allocate();    
	        //synthesizer.resume();     
	        //desc = (SynthesizerModeDesc)  synthesizer.getEngineModeDesc();  
	        VoiceManager managerVoice = VoiceManager.getInstance();
	        Voice[] voices = managerVoice.getVoices();      
	        voice = null;
	        for (int i = 0; i < voices.length; i++)    
	        {    
	        	System.out.println(voices[i].getDescription());
	            if (voices[i].getName().equals(voiceName))    
	            {    
	                voice = voices[i];    
	                break;     
	            }     
	        }
	        // default voice
	        if(voice == null) {
	        	voice = managerVoice.getVoice("Kevin");
	        }
	        voice.setDurationStretch(speed);
//	        synthesizer.getSynthesizerProperties().setVoice(voice);    
//	        System.out.print("Speaking : "+speaktext);    
//	        synthesizer.speakPlainText(speaktext, null);    
//	        synthesizer.waitEngineState(Synthesizer.QUEUE_EMPTY);    
//	        synthesizer.deallocate();    
	        voice.setPitch(95F);
	        voice.allocate();
	        voice.setVolume(volume);
	        logger.debug(" Voice is speaking");
	        voice.speak(speaktext);
	        voice.deallocate();
	    }    
	    catch (Exception e)   
	    {    
	        String message = " missing speech.properties in " + System.getProperty("user.home") + "\n";    
	        logger.error(message+e.getMessage());
	    } 
	    
    } 
	
	public Voice getVoice() {
    	return this.voice;
    }
}
