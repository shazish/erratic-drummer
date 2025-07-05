package my.proj;

import java.util.Arrays;
import java.util.Random;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

public class SoundMaker implements Runnable {
	protected Main context;
	int[][] loadedSoundIds; // Store Oboe sound IDs instead of SoundPool IDs
	int loadedMuteSound;
	String[] loadedSoundPoolNames;
	static int currentPatternRepeats; // Number of times pattern has been repeated so far
	int instGroupPrioritySum;
	static int[] instGroupPriority; // Currently 0~2
	float[] vol = {0.3f, 0.45f, 0.6f, 0.75f, 0.9f};
	float finalVolume;
	static int tempoAfterSettingsImplemented;
	int totalPatternRepeats;
	static int patternLengthAfterSettingsImplemented;
	int densityOfSequence;

	// Playback properties
	static boolean delaylessTransition = false;
	static boolean variableVolume = false;
	static boolean randomLength = false;
	static boolean randomTempo = false;

	boolean running = true;
	boolean justInitialized; // Becomes false upon first pattern generation
	static boolean playKeyPressed = false;
	static boolean lockKeyPressed = false;
	static boolean fwdKeyPressed = false;
	static int currentSoundBank = 0;
	int temp_priority_randomizer;
	int temp_priority_sum_keeper = 0;
	Random rand;
	AssetManager assets;
	boolean randomMode;
	public static final int effectDelayMid = 100;

	// Native methods for Oboe
	private native boolean initOboe(AssetManager assetManager);
	private native int loadSound(String assetPath);
	private native void playSound(int soundId, float volume);
	private native void stopAllSounds();
	private native void releaseOboe();

	// Load the native library
	static {
		System.loadLibrary("native-lib");
	}

	SoundMaker(Main _context) {
		Log.d("SoundMaker", "Constructor");
		this.context = _context;
		rand = new Random();
		assets = context.getAssets();
		instGroupPriority = new int[]{1, 1, 1, 1};
		instGroupPrioritySum = 4;
		
		// Initialize with default Jazz sound bank names to prevent null pointer
		loadedSoundPoolNames = new String[]{
			"Snare.Rim.Tom",
			"Kick", 
			"Hat",
			"Perc"
		};
		
		// Initialize empty sound IDs arrays to prevent null pointer during generateSequence
		loadedSoundIds = new int[4][];
		for (int i = 0; i < 4; i++) {
			loadedSoundIds[i] = new int[0]; // Empty arrays initially
		}
		
		generateSequence();
		
		// Initialize audio engine and load sounds on background thread
		context.initializeAudioAsync();
	}

	public void updateCounterUI(final int i, final int alpha) {
		context.runOnUiThread(() -> context.repeatCounterDot[i].setAlpha(alpha));
	}

	public void resetCounterUI() {
		context.runOnUiThread(() -> {
			if (!lockKeyPressed)
				context.patternRepeatCounterAnimator(1);
		});
	}

	public void playSequence() {
		Log.d("SoundMaker", "playSequence()");
		currentPatternRepeats = 0;

		outerloop:
		while (currentPatternRepeats <= totalPatternRepeats) {
			if (lockKeyPressed) {
				currentPatternRepeats = totalPatternRepeats - 1;
			}
			if (randomLength)
				DrumPatternCore.length = patternLengthAfterSettingsImplemented;
			for (int i = 0; i < DrumPatternCore.length; i++) {
				if (!playKeyPressed || fwdKeyPressed)
					break outerloop;
				try {
					for (int j = 0; j < DrumPatternCore.MAX_SIMULTANEOUS_INST; j++) {
						if (variableVolume) {
							finalVolume = vol[rand.nextInt(5)];
						} else {
							finalVolume = 0.9f;
						}
						if (DrumPatternCore.group[j][i] != -1 && DrumPatternCore.instrument[j][i] != -1) {
							int soundId = loadedSoundIds[DrumPatternCore.group[j][i]][DrumPatternCore.instrument[j][i]];
							Log.d("SoundMaker", "Playing NATIVE sound ID: " + soundId + " at volume: " + finalVolume);
							playSound(soundId, finalVolume);
						}
					}
					if (randomTempo)
						DrumPatternCore.tempo = tempoAfterSettingsImplemented;
					Thread.sleep(15000 / DrumPatternCore.tempo);
				} catch (Exception e) {
					Log.d("SoundMaker Exception", e.toString());
				}
			}
			if (lockKeyPressed) {
				currentPatternRepeats = totalPatternRepeats - 1;
			}
			Log.d("playing Tempo/length:", DrumPatternCore.tempo + "/" + DrumPatternCore.length);
			Log.d("inst[0] playing", Arrays.toString(DrumPatternCore.instrument[0]));
			Log.d("inst[1] playing", Arrays.toString(DrumPatternCore.instrument[1]));
			updateCounterUI(currentPatternRepeats, 100);
			currentPatternRepeats++;
		}
		if (playKeyPressed)
			resetCounterUI();
	}

	public void generateSequence() {
		Log.d("SoundMaker", "generateSequence()");
		
		// Debug: Log sound array lengths
		for (int g = 0; g < 4; g++) {
			if (loadedSoundIds != null && loadedSoundIds[g] != null) {
				Log.d("SoundMaker", "Group " + g + " has " + loadedSoundIds[g].length + " sounds");
			}
		}
		
		if (randomLength) {
			patternLengthAfterSettingsImplemented =
					DrumPatternCore.MIN_RIFF_LENGTH + rand.nextInt(DrumPatternCore.MAX_RIFF_LENGTH - DrumPatternCore.MIN_RIFF_LENGTH);
		}
		if (randomTempo) {
			tempoAfterSettingsImplemented = 80 + rand.nextInt(50);
		}
		for (int m = 0; m < DrumPatternCore.MAX_RIFF_LENGTH; m++) {
			for (int i = 0; i < DrumPatternCore.MAX_SIMULTANEOUS_INST; i++) {
				DrumPatternCore.group[i][m] = -1;
				DrumPatternCore.instrument[i][m] = -1;
			}
		}
		
		int assignedInstruments = 0;
		for (int m = 0; m < DrumPatternCore.MAX_RIFF_LENGTH; m++) {
			for (int i = 0; i < DrumPatternCore.MAX_SIMULTANEOUS_INST; i++) {
				if (rand.nextInt(100) > (15 * densityOfSequence + 35))
					continue;
				temp_priority_randomizer = rand.nextInt(instGroupPrioritySum);
				temp_priority_sum_keeper = 0;
				for (int x = 0; x < 4; x++) {
					temp_priority_sum_keeper += instGroupPriority[x];
					if (temp_priority_randomizer < temp_priority_sum_keeper) {
						DrumPatternCore.group[i][m] = x;
						break;
					}
				}
				if (loadedSoundIds[DrumPatternCore.group[i][m]].length != 0) {
					DrumPatternCore.instrument[i][m] = rand.nextInt(loadedSoundIds[DrumPatternCore.group[i][m]].length);
					assignedInstruments++;
				}
			}
		}
		Log.d("SoundMaker", "Generated pattern with " + assignedInstruments + " assigned instruments");
	}

	public static String[] exportDrumPatterns() {
		String[] dmgroup = new String[2];
		String[] dminst = new String[2];
		Arrays.fill(dmgroup, "");
		Arrays.fill(dminst, "");
		for (int m = 0; m < DrumPatternCore.MAX_SIMULTANEOUS_INST; m++) {
			for (int i = 0; i < DrumPatternCore.MAX_RIFF_LENGTH; i++) {
				dmgroup[m] += DrumPatternCore.group[m][i] + ",";
				dminst[m] += DrumPatternCore.instrument[m][i] + ",";
			}
		}
		return new String[]{dmgroup[0], dminst[0], dmgroup[1], dminst[1]};
	}

	public void separatorOfSequences() {
		if (!delaylessTransition) {
			try {
				Thread.sleep(750);
				Log.d("SoundMaker", "separator - Of - Sequences");
			} catch (Exception e) {
				// Handle exception
			}
		}
	}

	public void run() {
		Log.d("SoundMaker", "run()");
		while (running) {
			if (playKeyPressed) {
				playSequence();
				assignInstrumentGroupPriority();
				if (instGroupPrioritySum == 0)
					continue;
				if (playKeyPressed)
					generateSequence();
				separatorOfSequences();
				if (fwdKeyPressed)
					fwdKeyPressed = !fwdKeyPressed;
			} else {
				try {
					Thread.sleep(50);
				} catch (Exception e) {
					Log.d("SoundMaker", "runerror: " + e.toString());
				}
			}
		}
	}

	public void initializeSoundPool(int bankID, boolean init) {
		Log.d("SoundMaker", "initializeSoundPool() starting for bank " + bankID);
		if (!initOboe(assets)) { // Initialize Oboe with AssetManager
			Log.e("SoundMaker", "Failed to initialize Oboe audio engine");
			return;
		}
		Log.d("SoundMaker", "Oboe initialized successfully");
		
		try {
			loadedMuteSound = loadSound("mute1000.ogg");
			if (loadedMuteSound == -1) {
				Log.e("SoundMaker", "Failed to load mute sound");
			} else {
				Log.d("SoundMaker", "Mute sound loaded with ID: " + loadedMuteSound);
			}
			if (bankID == 0) {
				loadedSoundPoolNames = new String[]{
						"Snare.Rim.Tom",
						"Kick",
						"Hat",
						"Perc",
				};
				loadedSoundIds = new int[4][];
				// Load Snare.Rim.Tom group
				loadedSoundIds[0] = new int[]{
						loadSound("jazzy/439__tictacshutup__prac-snare-2.ogg"),
						loadSound("jazzy/440__tictacshutup__prac-snare-off.ogg"),
						loadSound("jazzy/441__tictacshutup__prac-snare-rim.ogg"),
						loadSound("jazzy/442__tictacshutup__prac-snare-rimshot-2.ogg"),
						loadSound("jazzy/443__tictacshutup__prac-snare-rimshot.ogg"),
						loadSound("jazzy/444__tictacshutup__prac-snare-roll-short-2.ogg"),
						loadSound("jazzy/445__tictacshutup__prac-snare-roll-short.ogg"),
						loadSound("jazzy/446__tictacshutup__prac-snare-roll.ogg"),
						loadSound("jazzy/447__tictacshutup__prac-snare.ogg"),
						loadSound("jazzy/448__tictacshutup__prac-tom-light.ogg"),
						loadSound("jazzy/449__tictacshutup__prac-tom.ogg")
				};
				// Check for failed loads
				for (int i = 0; i < loadedSoundIds[0].length; i++) {
					if (loadedSoundIds[0][i] == -1) {
						Log.e("SoundMaker", "Failed to load snare sound at index " + i);
					}
				}
				// Load Kick group
				loadedSoundIds[1] = new int[]{
						loadSound("jazzy/427__tictacshutup__prac-kick-light.ogg"),
						loadSound("jazzy/428__tictacshutup__prac-kick.ogg")
				};
				for (int i = 0; i < loadedSoundIds[1].length; i++) {
					if (loadedSoundIds[1][i] == -1) {
						Log.e("SoundMaker", "Failed to load kick sound at index " + i);
					}
				}
				// Load Hat group
				loadedSoundIds[2] = new int[]{
						loadSound("jazzy/421__tictacshutup__prac-hat-2.ogg"),
						loadSound("jazzy/422__tictacshutup__prac-hat-3.ogg"),
						loadSound("jazzy/423__tictacshutup__prac-hat-open-2.ogg"),
						loadSound("jazzy/424__tictacshutup__prac-hat-open-3.ogg"),
						loadSound("jazzy/426__tictacshutup__prac-hat.ogg"),
						loadSound("jazzy/425__tictacshutup__prac-hat-open.ogg"),
						loadSound("jazzy/434__tictacshutup__prac-ride-bell-loud.ogg"),
						loadSound("jazzy/435__tictacshutup__prac-ride-bell.ogg"),
						loadSound("jazzy/436__tictacshutup__prac-ride.ogg")
				};
				for (int i = 0; i < loadedSoundIds[2].length; i++) {
					if (loadedSoundIds[2][i] == -1) {
						Log.e("SoundMaker", "Failed to load hat sound at index " + i);
					}
				}
				// Load Perc group
				loadedSoundIds[3] = new int[]{
						loadSound("jazzy/429__tictacshutup__prac-perc-1.ogg"),
						loadSound("jazzy/430__tictacshutup__prac-perc-2.ogg"),
						loadSound("jazzy/431__tictacshutup__prac-perc-3.ogg"),
						loadSound("jazzy/432__tictacshutup__prac-perc-4.ogg"),
						loadSound("jazzy/433__tictacshutup__prac-perc-5.ogg"),
						loadSound("jazzy/437__tictacshutup__prac-sidestick-2.ogg"),
						loadSound("jazzy/438__tictacshutup__prac-sidestick.ogg")
				};
				for (int i = 0; i < loadedSoundIds[3].length; i++) {
					if (loadedSoundIds[3][i] == -1) {
						Log.e("SoundMaker", "Failed to load perc sound at index " + i);
					}
				}
			} else if (bankID == 2) {
				loadedSoundPoolNames = new String[]{
						"Dumbek",
						"Tabla",
						"Gourd",
						"Sticks",
				};
				loadedSoundIds = new int[][]{
						{
								loadSound("exotic/DUMBEK_1.ogg"),
								loadSound("exotic/DUMBEK_2.ogg"),
								loadSound("exotic/DUMBEK_3.ogg"),
								loadSound("exotic/DUMBEK_4.ogg"),
								loadSound("exotic/DUMBEK_5.ogg")
						},
						{
								loadSound("exotic/TABLA1-1.ogg"),
								loadSound("exotic/TABLA1-2.ogg"),
								loadSound("exotic/TABLA1-3.ogg"),
								loadSound("exotic/TABLA1-4.ogg"),
								loadSound("exotic/TABLA1-5.ogg"),
								loadSound("exotic/TABLA1-6.ogg"),
								loadSound("exotic/TABLA1-7.ogg"),
								loadSound("exotic/TABLA2-1.ogg"),
								loadSound("exotic/TABLA2-2.ogg"),
								loadSound("exotic/TABLA2-3.ogg"),
								loadSound("exotic/TABLA2-4.ogg"),
								loadSound("exotic/TABLA2-5.ogg"),
								loadSound("exotic/TABLA2-6.ogg"),
								loadSound("exotic/TABLA2-7.ogg"),
								loadSound("exotic/TABLA2-8.ogg"),
								loadSound("exotic/TABLA2-9.ogg"),
								loadSound("exotic/TABLA2-10.ogg"),
								loadSound("exotic/TABLA2-11.ogg"),
								loadSound("exotic/TABLA2-12.ogg"),
								loadSound("exotic/TABLA2-13.ogg"),
								loadSound("exotic/TABLA2-14.ogg"),
								loadSound("exotic/TABLA2-15.ogg"),
								loadSound("exotic/TABLA2-16.ogg")
						},
						{
								loadSound("exotic/GOURD_1.ogg"),
								loadSound("exotic/GOURD_2.ogg"),
								loadSound("exotic/GOURD_3.ogg"),
								loadSound("exotic/GOURD_4.ogg"),
								loadSound("exotic/GOURD_5.ogg"),
								loadSound("exotic/GOURD_6.ogg"),
								loadSound("exotic/GOURD_7.ogg"),
								loadSound("exotic/GOURD_8.ogg"),
								loadSound("exotic/MARACA.ogg")
						},
						{
								loadSound("exotic/STICKS_1.ogg"),
								loadSound("exotic/STICKS_2.ogg"),
								loadSound("exotic/STICKS_3.ogg"),
								loadSound("exotic/STICKS_4.ogg"),
								loadSound("exotic/STICKS_5.ogg"),
								loadSound("exotic/STICKS_6.ogg"),
								loadSound("exotic/STICKS_7.ogg"),
								loadSound("exotic/LOG_DRUM.ogg"),
								loadSound("exotic/LOGDRUM2.ogg"),
								loadSound("exotic/LOGDRUM3.ogg")
						}
				};
			} else if (bankID == 3) {
				loadedSoundPoolNames = new String[]{
						"Snare",
						"Kick",
						"Hat.Crash",
						"Rim.Tom",
				};
				loadedSoundIds = new int[][]{
						{
								loadSound("metal/Rodgers_DynLH10.ogg"),
								loadSound("metal/Rodgers_HrdRH05.ogg")
						},
						{
								loadSound("metal/CLudwigKick-Dyn08.ogg"),
								loadSound("metal/CLudwigKick-Dyn04.ogg")
						},
						{
								loadSound("metal/SabHHXEvo20_Bell04.ogg"),
								loadSound("metal/SabHHXEvo20_Bell08.ogg"),
								loadSound("metal/ZildjinCrsh 1-Dyn06.ogg"),
								loadSound("metal/ZildjinCrsh 2-Dyn07.ogg"),
								loadSound("metal/ZildMstrsnd-DynClsdLH10.ogg"),
								loadSound("metal/ZildMstrsnd-DynClsdLH12.ogg"),
								loadSound("metal/ZildMstrsnd-DynPed08.ogg"),
								loadSound("metal/ZildMstrsnd-DynSmiOpn11.ogg")
						},
						{
								loadSound("metal/Rodgers_RimClck06.ogg"),
								loadSound("metal/CLudwigTom1-DynLH10.ogg"),
								loadSound("metal/CLudwigTom2-DynLH10.ogg")
						}
				};
			}
			playSound(loadedMuteSound, 0.0f); // Play mute sound for sync
		} catch (Exception e) {
			Log.d("SoundMaker", "initializeSoundPoolError" + e.toString());
		}
	}

	public void assignInstrumentGroupPriority() {
		instGroupPrioritySum = instGroupPriority[0] + instGroupPriority[1] + instGroupPriority[2] + instGroupPriority[3];
		Log.d("SoundMaker ", instGroupPrioritySum + "");
	}

	public void disposeSoundpool() {
		releaseOboe();
		loadedSoundIds = null;
	}
}