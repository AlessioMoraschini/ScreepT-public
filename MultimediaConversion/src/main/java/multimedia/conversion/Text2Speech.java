/**
 *
 * =========================================================================================
 *  Copyright (C) 2019-2020
 *
 *  AM-Design-Development - (Alessio Moraschini)
 * =========================================================================================
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package multimedia.conversion;

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
