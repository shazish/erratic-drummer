package my.proj;

import java.util.Arrays;
import java.util.Random;

import android.content.Context;
import android.content.res.AssetManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.util.Log;

public class SoundMaker implements Runnable {
    //DrumPatternCore pattern;
	protected Main context;
	int [][] loadedSoundPool; 
	int loadedMuteSound;
	String []   loadedSoundPoolNames;
	static int currentPatternRepeats; // Number of times pattern has been repeated so far 
	
	int instGroupPrioritySum;
	static int [] instGroupPriority; // currently 0~2	
	// static int [] priorityKeeper;

	float [] vol = {0.3f, 0.45f, 0.6f, 0.75f, 0.9f};
	float finalVolume;
	
	static int tempoAfterSettingsImplemented;
	int totalPatternRepeats;
	static int patternLengthAfterSettingsImplemented;
	int densityOfSequence;

    // playback properties
    static boolean delaylessTransition = false;
    static boolean variableVolume = false;
    static boolean randomLength = false;
    static boolean randomTempo = false;
    
	boolean running = true;
	boolean justInitialized; // becomes False upon first time a pattern is generated/loaded
	static boolean playKeyPressed = false;
	static boolean lockKeyPressed = false;
	static boolean fwdKeyPressed = false;
	static int currentSoundBank = 0;
	int temp_priority_randomizer;
	int temp_priority_sum_keeper = 0;
	Random rand;
	SoundPool soundPool, soundPoolMute;
	AssetManager assets;
	boolean randomMode;
	public static final int effectDelayMid = 100;
	//public static final int MAX_SIMULTANEOUS_INST = 2;
	//public static final int MAX_RIFF_LENGTH = 16;
	//public static final int MIN_RIFF_LENGTH = 4;
	//public static final int START_RIFF_LENGTH = 8;
	
	//public static final int MAX_TEMPO = 130;
	//public static final int MIN_TEMPO = 80;
	//public static final int START_TEMPO = 100;

	
	SoundMaker (Main _context) {
		Log.d("SoundMaker", "Constructor");	
		this.context = _context;
		rand = new Random();
		assets = context.getAssets();
        //pattern = new DrumPatternCore();
		//setVolumeControlStream(AudioManager.STREAM_MUSIC);
		instGroupPriority = new int []{1 , 1 , 1 , 1};
	 	initializeSoundPool(0, true);
		// Randomize and populate the first pattern
		instGroupPrioritySum = 4;

		generateSequence();
	}
	
	
	public void updateCounterUI(final int i, final int alpha) {
		context.runOnUiThread(new Runnable() {	               
            public void run() {
            	context.repeatCounterDot[i].setAlpha(alpha);             	
            }
        });	
	}
	
	public void resetCounterUI() {
		context.runOnUiThread(new Runnable() {	               
            public void run() {
            	if (!lockKeyPressed)
            		context.patternRepeatCounterAnimator(1);             	
            }
        });	
	}
	
	public void playSequence(){
		Log.d("SoundMaker", "playSequence()");
		currentPatternRepeats = 0;

		outerloop:

		while (currentPatternRepeats <= totalPatternRepeats)	{								
			
			if (lockKeyPressed) { // Checks LockKey status at the beginning and end of each loop
				currentPatternRepeats = totalPatternRepeats - 1;
			}
			if (randomLength)
				DrumPatternCore.length = patternLengthAfterSettingsImplemented;
			for (int i = 0 ; i < DrumPatternCore.length ; i++ ){
				if (!playKeyPressed | fwdKeyPressed)
					break outerloop;
					try {
						for (int j = 0 ; j < DrumPatternCore.MAX_SIMULTANEOUS_INST; j++) {
							//Log.d("Time:", "");
							if (variableVolume) {
								finalVolume = vol[rand.nextInt(5)];
							} else {
								finalVolume = 0.9f;
							}
							if (DrumPatternCore.group[j][i]!= -1 & DrumPatternCore.instrument[j][i]!= -1 ) {
									// float vol = rand.nextFloat() / 2; // possible lag for weaker CPUs?
									soundPool.play(loadedSoundPool
											[ DrumPatternCore.group[j][i] ][ DrumPatternCore.instrument[j][i] ]
											, finalVolume, finalVolume, 1, 0, 1);
							}									
					}
						if (randomTempo) 
							DrumPatternCore.tempo = tempoAfterSettingsImplemented;
						Thread.sleep(15000/DrumPatternCore.tempo);
					} catch (Exception e) { 
						Log.d("SoundMaker Exception", e.toString());
					}
			}
			if (lockKeyPressed) { // Checks LockKey status at the beginning and end of each loop
				currentPatternRepeats = totalPatternRepeats - 1;
			}
            Log.d("playing Tempo/length:", DrumPatternCore.tempo +"/" + DrumPatternCore.length);
            Log.d("inst[0] playing", Arrays.toString( DrumPatternCore.instrument[0] ));
            Log.d("inst[1] playing", Arrays.toString( DrumPatternCore.instrument[1] ));
			updateCounterUI(currentPatternRepeats, 100); // ALPHA = 100
			currentPatternRepeats++;
		}
		if (playKeyPressed)
			resetCounterUI(); // End of the outer loop. Reset the counter.
	}
	
	public void generateSequence(){
		Log.d("SoundMaker", "generateSequence()");
		
		if (randomLength) {
			patternLengthAfterSettingsImplemented = 
				DrumPatternCore.MIN_RIFF_LENGTH + rand.nextInt(DrumPatternCore.MAX_RIFF_LENGTH - DrumPatternCore.MIN_RIFF_LENGTH);
		}

		
		if (randomTempo) {
			tempoAfterSettingsImplemented = 80 + rand.nextInt(50);
		}		
		
		for (int m = 0 ; m < DrumPatternCore.MAX_RIFF_LENGTH ; m++ ) {
            // Clean up previous composition
            for (int i = 0; i < DrumPatternCore.MAX_SIMULTANEOUS_INST; i++) {
                DrumPatternCore.group[i][m] = -1;
                DrumPatternCore.instrument[i][m] = -1;
            }
        }

        for (int m = 0 ; m < DrumPatternCore.MAX_RIFF_LENGTH ; m++ ) {
			// Sets a number of instruments as true, per beat
			for (int i = 0 ; i < DrumPatternCore.MAX_SIMULTANEOUS_INST ; i++) {

				// makes it a 35-50-65 chance depending on value of densityOfSequence
				if ( rand.nextInt(100) > (15 * densityOfSequence + 35) ) continue;
				
				// chooses one of the instrument-types based on values of instGroupPriority.
				temp_priority_randomizer = rand.nextInt(instGroupPrioritySum);
				temp_priority_sum_keeper = 0;
				for (int x = 0; x < 4; x++) {
					temp_priority_sum_keeper += instGroupPriority[x];
					if ( temp_priority_randomizer < temp_priority_sum_keeper ) {
						DrumPatternCore.group[i][m] = x;
						break;
					}					
				}
				// chooses one of the instruments based on the length of the arrow that correlates to the chosen instrument-type
				if ( loadedSoundPool [DrumPatternCore.group[i][m]].length != 0 )
                    DrumPatternCore.instrument[i][m] = rand.nextInt( loadedSoundPool [ DrumPatternCore.group[i][m] ].length );

			}
			 //Log.d("Group:Instrument chosen", DrumPatternCore.group[0][m] + " : " + DrumPatternCore.instrument[0][m] + " -- "
			//		+ DrumPatternCore.group[1][m] + " : " + DrumPatternCore.instrument[1][m] );
		}



        //Log.d("DRUMLINE0", exportDrumPatterns()[0]);
        //Log.d("DRUMLINE1", exportDrumPatterns()[1] );
        //Log.d("TEMPO", DrumPatternCore.tempo + " ");

	}

    public static String [] exportDrumPatterns() {
        String dmgroup[] = new String [2];
        String dminst[] = new String [2];
        Arrays.fill(dmgroup, "");
        Arrays.fill(dminst, "");
        for (int m = 0 ; m < DrumPatternCore.MAX_SIMULTANEOUS_INST ; m++) {
            for (int i = 0; i < DrumPatternCore.MAX_RIFF_LENGTH; i++) {
                dmgroup[m] += DrumPatternCore.group[m][i] + ",";
                dminst[m] += DrumPatternCore.instrument[m][i] + ",";
            }
        }

        return new String[]{ dmgroup[0], dminst[0], dmgroup[1], dminst[1] };
    }

	public void separatorOfSequences(){
        if (delaylessTransition == false)
            try {
                Thread.sleep(750);
                Log.d("SoundMaker", "separator - Of - Sequences");
            } catch (Exception e) {
                //
            }
	}
		

	public void run() {
		Log.d("SoundMaker", "run()");
		while(running)
			if (playKeyPressed) {

				playSequence();
				assignInstrumentGroupPriority();
				if (instGroupPrioritySum == 0)
					continue;
				if (playKeyPressed) generateSequence();
				separatorOfSequences();

				if (fwdKeyPressed) 
					fwdKeyPressed = !fwdKeyPressed;					
			} 
			else 
			{
				try {
					Thread.sleep(50);
				} catch (Exception e) {
					Log.d("SoundMaker", "runerror: " + e.toString());
				}
			}
	}
	
     
    
	public void initializeSoundPool(int bankID, boolean init){
        //pattern = new DrumPatternCore();
		soundPool = new SoundPool (25, AudioManager.STREAM_MUSIC, 0); // MAX_SIMULTANEOUS_INST * 2 is enough
		// soundPoolMute = new SoundPool (1, AudioManager.STREAM_MUSIC, 0); // MAX_SIMULTANEOUS_INST * 2 is enough	

		try {
				loadedMuteSound = soundPool.load(assets.openFd("mute1000.ogg"), 1);
	
				if (bankID == 0) {	
					loadedSoundPoolNames = new String [] {
							"Snare.Rim.Tom",
							"Kick",
							"Hat",
							"Perc",
					};
				loadedSoundPool = new int [][] {
					{
						soundPool.load(assets.openFd("jazzy/439__tictacshutup__prac-snare-2.ogg"), 1),
						soundPool.load(assets.openFd("jazzy/440__tictacshutup__prac-snare-off.ogg"), 1),
						soundPool.load(assets.openFd("jazzy/441__tictacshutup__prac-snare-rim.ogg"), 1),
						soundPool.load(assets.openFd("jazzy/442__tictacshutup__prac-snare-rimshot-2.ogg"), 1),
						soundPool.load(assets.openFd("jazzy/443__tictacshutup__prac-snare-rimshot.ogg"), 1),
						soundPool.load(assets.openFd("jazzy/444__tictacshutup__prac-snare-roll-short-2.ogg"), 1),
						soundPool.load(assets.openFd("jazzy/445__tictacshutup__prac-snare-roll-short.ogg"), 1),
						soundPool.load(assets.openFd("jazzy/446__tictacshutup__prac-snare-roll.ogg"), 1),
						soundPool.load(assets.openFd("jazzy/447__tictacshutup__prac-snare.ogg"), 1),
						soundPool.load(assets.openFd("jazzy/448__tictacshutup__prac-tom-light.ogg"), 1),
						soundPool.load(assets.openFd("jazzy/449__tictacshutup__prac-tom.ogg"), 1)
					}, {					
						soundPool.load(assets.openFd("jazzy/427__tictacshutup__prac-kick-light.ogg"), 1),
						soundPool.load(assets.openFd("jazzy/428__tictacshutup__prac-kick.ogg"), 1)
					}, {
						soundPool.load(assets.openFd("jazzy/421__tictacshutup__prac-hat-2.ogg"), 1),
						soundPool.load(assets.openFd("jazzy/422__tictacshutup__prac-hat-3.ogg"), 1),
						soundPool.load(assets.openFd("jazzy/423__tictacshutup__prac-hat-open-2.ogg"), 1),
						soundPool.load(assets.openFd("jazzy/424__tictacshutup__prac-hat-open-3.ogg"), 1),
						soundPool.load(assets.openFd("jazzy/426__tictacshutup__prac-hat.ogg"), 1),
						soundPool.load(assets.openFd("jazzy/425__tictacshutup__prac-hat-open.ogg"), 1),
						soundPool.load(assets.openFd("jazzy/434__tictacshutup__prac-ride-bell-loud.ogg"), 1),
						soundPool.load(assets.openFd("jazzy/435__tictacshutup__prac-ride-bell.ogg"), 1),
						soundPool.load(assets.openFd("jazzy/436__tictacshutup__prac-ride.ogg"), 1),
					}, {
						soundPool.load(assets.openFd("jazzy/429__tictacshutup__prac-perc-1.ogg"), 1),
						soundPool.load(assets.openFd("jazzy/430__tictacshutup__prac-perc-2.ogg"), 1),
						soundPool.load(assets.openFd("jazzy/431__tictacshutup__prac-perc-3.ogg"), 1),
						soundPool.load(assets.openFd("jazzy/432__tictacshutup__prac-perc-4.ogg"), 1),
						soundPool.load(assets.openFd("jazzy/433__tictacshutup__prac-perc-5.ogg"), 1),
						soundPool.load(assets.openFd("jazzy/437__tictacshutup__prac-sidestick-2.ogg"), 1),
						soundPool.load(assets.openFd("jazzy/438__tictacshutup__prac-sidestick.ogg"), 1),
					}
				};
			}

			/**
			if (bankID == 1) {
				loadedSoundPoolNames = new String [] {
						"Snare",
						"Kick",
						"Hat",
						"Clap",
				};

				loadedSoundPool = new int [][] {
					{
						soundPool.load(assets.openFd("noisecollector/snare.ogg"), 1),
					 	soundPool.load(assets.openFd("noisecollector/snare2.ogg"), 1)
					}, {			 	
						soundPool.load(assets.openFd("noisecollector/kick.ogg"), 1),
						soundPool.load(assets.openFd("noisecollector/kick2.ogg"), 1),
						soundPool.load(assets.openFd("noisecollector/kick3.ogg"), 1),			 	
						soundPool.load(assets.openFd("noisecollector/kicklong.ogg"), 1)
					}, {			 	
						soundPool.load(assets.openFd("noisecollector/hat.ogg"), 1),
						soundPool.load(assets.openFd("noisecollector/hat2.ogg"), 1),
						soundPool.load(assets.openFd("noisecollector/hatopen.ogg"), 1),
						soundPool.load(assets.openFd("noisecollector/hatopen2.ogg"), 1),
					}, {			 	
			 		 	soundPool.load(assets.openFd("noisecollector/clap.ogg"), 1),
			 		 //	soundPool.load(assets.openFd("hardpcm.ogg"), 1),
			 		 	soundPool.load(assets.openFd("45533__jesuswaffle__bassblast.ogg"), 1),
					}
				};
			}
			
				**/
				if (bankID == 2) {
					loadedSoundPoolNames = new String [] {
							"Dumbek",
							"Tabla",
							"Gourd",
							"Sticks",
					};
					loadedSoundPool = new int [][] {
						{						
							soundPool.load(assets.openFd("exotic/DUMBEK_1.ogg"), 1),
							soundPool.load(assets.openFd("exotic/DUMBEK_2.ogg"), 1),
							soundPool.load(assets.openFd("exotic/DUMBEK_3.ogg"), 1),
							soundPool.load(assets.openFd("exotic/DUMBEK_4.ogg"), 1),
							soundPool.load(assets.openFd("exotic/DUMBEK_5.ogg"), 1),
						}, {	
							soundPool.load(assets.openFd("exotic/TABLA1-1.ogg"), 1),
							soundPool.load(assets.openFd("exotic/TABLA1-2.ogg"), 1),
							soundPool.load(assets.openFd("exotic/TABLA1-3.ogg"), 1),
							soundPool.load(assets.openFd("exotic/TABLA1-4.ogg"), 1),
							soundPool.load(assets.openFd("exotic/TABLA1-5.ogg"), 1),
							soundPool.load(assets.openFd("exotic/TABLA1-6.ogg"), 1),
							soundPool.load(assets.openFd("exotic/TABLA1-7.ogg"), 1),
							soundPool.load(assets.openFd("exotic/TABLA2-1.ogg"), 1),
							soundPool.load(assets.openFd("exotic/TABLA2-2.ogg"), 1),
							soundPool.load(assets.openFd("exotic/TABLA2-3.ogg"), 1),
							soundPool.load(assets.openFd("exotic/TABLA2-4.ogg"), 1),
							soundPool.load(assets.openFd("exotic/TABLA2-5.ogg"), 1),
							soundPool.load(assets.openFd("exotic/TABLA2-6.ogg"), 1),
							soundPool.load(assets.openFd("exotic/TABLA2-7.ogg"), 1),
							soundPool.load(assets.openFd("exotic/TABLA2-8.ogg"), 1),
							soundPool.load(assets.openFd("exotic/TABLA2-9.ogg"), 1),
							soundPool.load(assets.openFd("exotic/TABLA210.ogg"), 1),
							soundPool.load(assets.openFd("exotic/TABLA211.ogg"), 1),
							soundPool.load(assets.openFd("exotic/TABLA212.ogg"), 1),
							soundPool.load(assets.openFd("exotic/TABLA213.ogg"), 1),
							soundPool.load(assets.openFd("exotic/TABLA214.ogg"), 1),
							soundPool.load(assets.openFd("exotic/TABLA215.ogg"), 1),
							soundPool.load(assets.openFd("exotic/TABLA216.ogg"), 1),

						}, {	

							soundPool.load(assets.openFd("exotic/GOURD_1.ogg"), 1),
							soundPool.load(assets.openFd("exotic/GOURD_2.ogg"), 1),
							soundPool.load(assets.openFd("exotic/GOURD_3.ogg"), 1),
							soundPool.load(assets.openFd("exotic/GOURD_4.ogg"), 1),
							soundPool.load(assets.openFd("exotic/GOURD_5.ogg"), 1),
							soundPool.load(assets.openFd("exotic/GOURD_6.ogg"), 1),
							soundPool.load(assets.openFd("exotic/GOURD_7.ogg"), 1),
							soundPool.load(assets.openFd("exotic/GOURD_8.ogg"), 1),
							soundPool.load(assets.openFd("exotic/MARACA.ogg"), 1),

						}, {	
							soundPool.load(assets.openFd("exotic/STICKS_1.ogg"), 1),
							soundPool.load(assets.openFd("exotic/STICKS_2.ogg"), 1),
							soundPool.load(assets.openFd("exotic/STICKS_3.ogg"), 1),
							soundPool.load(assets.openFd("exotic/STICKS_4.ogg"), 1),
							soundPool.load(assets.openFd("exotic/STICKS_5.ogg"), 1),
							soundPool.load(assets.openFd("exotic/STICKS_6.ogg"), 1),
							soundPool.load(assets.openFd("exotic/STICKS_7.ogg"), 1),
							soundPool.load(assets.openFd("exotic/LOG_DRUM.ogg"), 1),
							soundPool.load(assets.openFd("exotic/LOGDRUM2.ogg"), 1),
							soundPool.load(assets.openFd("exotic/LOGDRUM3.ogg"), 1),


						}
					};				 	
				}

				if (bankID == 3) {
					loadedSoundPoolNames = new String [] {
							"Snare",
							"Kick",
							"Hat.Crash",
							"Rim.Tom",
					};
					loadedSoundPool = new int [][] {
							{					
								soundPool.load(assets.openFd("metal/Rodgers_DynLH10.ogg"), 1),
								soundPool.load(assets.openFd("metal/Rodgers_HrdRH05.ogg"), 1),								
								//soundPool.load(assets.openFd(".ogg"), 1),
							}, {
								soundPool.load(assets.openFd("metal/CLudwigKick-Dyn08.ogg"), 1),
								soundPool.load(assets.openFd("metal/CLudwigKick-Dyn04.ogg"), 1),
							}, { // open hat/crash
								soundPool.load(assets.openFd("metal/SabHHXEvo20_Bell04.ogg"), 1),
								soundPool.load(assets.openFd("metal/SabHHXEvo20_Bell08.ogg"), 1),
								soundPool.load(assets.openFd("metal/ZildjinCrsh 1-Dyn06.ogg"), 1),
								soundPool.load(assets.openFd("metal/ZildjinCrsh 2-Dyn07.ogg"), 1),
								// closed hat
								soundPool.load(assets.openFd("metal/ZildMstrsnd-DynClsdLH10.ogg"), 1),
								soundPool.load(assets.openFd("metal/ZildMstrsnd-DynClsdLH12.ogg"), 1),
								soundPool.load(assets.openFd("metal/ZildMstrsnd-DynPed08.ogg"), 1),
								soundPool.load(assets.openFd("metal/ZildMstrsnd-DynSmiOpn11.ogg"), 1),

							}, {
								soundPool.load(assets.openFd("metal/Rodgers_RimClck06.ogg"), 1),
								soundPool.load(assets.openFd("metal/CLudwigTom1-DynLH10.ogg"), 1),
								soundPool.load(assets.openFd("metal/CLudwigTom2-DynLH10.ogg"), 1),
							}
					};
				}
				soundPool.play(loadedMuteSound, 0, 0, 1, -1, 1); // starts playing mute sound for sync purposes
				
		} catch (Exception e) {
			Log.d("SoundMaker", "initializeSoundPoolError" + e.toString());
		}
		
	}

    /*
	public void initializeSoundPoolVars() {
		
		drumMapSequenceGroup = new int [MAX_RIFF_LENGTH][MAX_SIMULTANEOUS_INST];
		drumMapSequenceInst = new int [MAX_RIFF_LENGTH][MAX_SIMULTANEOUS_INST];
	
		// assignInstrumentGroupPriority();
		
	}
	*/

	public void assignInstrumentGroupPriority() {
		instGroupPrioritySum = 
				instGroupPriority[0] + instGroupPriority[1] + instGroupPriority[2] + instGroupPriority[3];
		Log.d("SoundMaker ", instGroupPrioritySum +  "");					
		/**
		int temp_counter = 0;
		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < instGroupPriority[i]; j++) {
				priorityKeeper[temp_counter++] = i; 
			}				
		}
		**/		
	}
	
	
	public void disposeSoundpool(){
    	soundPool.release();
    	soundPool = null;
    	loadedSoundPool = null;
    	//instGroupPriority = null;
    	//priorityKeeper = null;

	}
    
}
