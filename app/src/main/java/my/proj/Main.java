package my.proj;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import androidx.fragment.app.FragmentTransaction;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.ActionBar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class Main extends AppCompatActivity implements ActionBar.TabListener {
    /** Called when the activity is first created. */

	AlertDialog.Builder troubleshooterDialog;
	SharedPreferences prefs;
	ViewFlipper viewFlip;
	String app_ver;
    private static ProgressDialog pd;

	static SoundMaker mSoundMaker;
	static Thread mSoundMakerThread;
	
	SharedPreferences mainPreferences;
	//public final static String PREFS_NAME = "erratic_drummer_prefs";
	public final static int REQUEST_CODE_PREFERENCES = 1;


	static SeekBar inst1Bar, inst2Bar, inst3Bar, inst4Bar, tempoBar, beatBar, repeatBar;
	static TextView inst1Text, inst2Text, inst3Text, inst4Text, tempoText, beatText, repeatText ;
	String tab1, tab2;
	
	ImageView [] repeatCounterDot = new ImageView [8];
	LinearLayout repeatCounter;
	Animation fadein, fadeout, slidein, slideout, buttonflash;

    Button loadButton, saveButton;
	ImageView playButton,  fwdButton, lockButton;

    LoadPopups showPopups = new LoadPopups(Main.this);
    DatabaseManager dbmanager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("ERRATIC DRUMMER", "OnCreate");
        //requestWindowFeature(Window.FEATURE_NO_TITLE); // enable or disable action bar!
        if (getResources().getBoolean(R.bool.has_two_panes)) {
        	setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        else {
        	setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR); 
        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN , 
        //        WindowManager.LayoutParams. FLAG_FULLSCREEN ); 
                
        setContentView(R.layout.main);
        dbmanager = new DatabaseManager(Main.this);
        troubleshooterDialog = new AlertDialog.Builder(this);
                      
        viewFlip = (ViewFlipper) findViewById(R.id.flipper); // used for drumset customizer page
        final Animation s_in  = AnimationUtils.loadAnimation(this, R.anim.fadein);
        final Animation s_in_out  = AnimationUtils.loadAnimation(this, R.anim.fadeinout);
        
    	//Animation out = AnimationUtils.makeInAnimation(this, true);
    	fadein = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
    	fadeout = AnimationUtils.loadAnimation(this, android.R.anim.fade_out);
    	buttonflash = AnimationUtils.loadAnimation(this, R.anim.fadeinout);
    	slidein = AnimationUtils.makeInAnimation(this, true);
    	slideout = AnimationUtils.makeOutAnimation(this, true);
    	
        //this.viewFlip.setInAnimation(s_in);


    	
        // SRC: http://android-journey.blogspot.com/2009/12/android-viewflipper-and-slidingdrawer.html
        // SRC: http://stackoverflow.com/questions/2597329/how-to-do-a-fadein-of-an-image-on-an-android-activity-screen
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

                
        // Initialize spinner
        final Spinner spinner = (Spinner) findViewById(R.id.density_spinner);
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(this, 
                R.array.density_array, android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerAdapter);
        spinner.setSelection(1);
        
        
        //final SeekBar densityBar = (SeekBar) findViewById(R.id.densityBar); 
        //final TextView densityText = (TextView) findViewById(R.id.densityText);
        inst1Bar = (SeekBar) findViewById(R.id.seekBarInst1);
        inst2Bar = (SeekBar) findViewById(R.id.seekBarInst2);
        inst3Bar = (SeekBar) findViewById(R.id.seekBarInst3);
        inst4Bar = (SeekBar) findViewById(R.id.seekBarInst4);       
        inst1Text = (TextView) findViewById(R.id.inst1);
        inst2Text = (TextView) findViewById(R.id.inst2);
        inst3Text = (TextView) findViewById(R.id.inst3);
        inst4Text = (TextView) findViewById(R.id.inst4);

        tab1 = getResources().getString(R.string.tabname1);
        tab2 = getResources().getString(R.string.tabname2);
        
        
        inst1Bar.setMax(4);
        inst2Bar.setMax(4);
        inst3Bar.setMax(4);
        inst4Bar.setMax(4);
        
        //densityBar.setMax(4);
        //densityBar.setProgress(2);               
		
        
        mSoundMaker = new SoundMaker(this);
    	
        playButton = (ImageView) findViewById(R.id.play_button);
        lockButton = (ImageView) findViewById(R.id.lock_button);
        fwdButton  = (ImageView) findViewById(R.id.fwd_button);
        loadButton = (Button) findViewById(R.id.load_button);
        saveButton = (Button) findViewById(R.id.save_button);
        saveButton.setVisibility(View.INVISIBLE); // won't be visible until a pattern is generated
        repeatCounter = (LinearLayout) findViewById(R.id.linearLayoutRepeatCounter);
        
        
        tempoBar = (SeekBar) findViewById(R.id.tempoBar);
        beatBar = (SeekBar) findViewById(R.id.beatBar);
        repeatBar = (SeekBar) findViewById(R.id.repeatBar);      
        tempoText = (TextView) findViewById(R.id.tempoText);
        beatText = (TextView) findViewById(R.id.beatText);
        repeatText = (TextView) findViewById(R.id.repeatText);
        
        tempoBar.setMax(DrumPatternCore.MAX_TEMPO - DrumPatternCore.MIN_TEMPO);
        tempoBar.setProgress(DrumPatternCore.START_TEMPO - DrumPatternCore.MIN_TEMPO);
        repeatBar.setMax(3);
        repeatBar.setProgress(2);
        beatBar.setMax(DrumPatternCore.MAX_RIFF_LENGTH - DrumPatternCore.MIN_RIFF_LENGTH);
        beatBar.setProgress(DrumPatternCore.START_RIFF_LENGTH - DrumPatternCore.MIN_RIFF_LENGTH);


        repeatText.setText( repeatBarAssignValue(repeatBar.getProgress()) + "x ");
        beatText.setText((beatBar.getProgress() + DrumPatternCore.MIN_RIFF_LENGTH) + " ");
        tempoText.setText(DrumPatternCore.START_TEMPO + " ");
        //densityText.setText("Normal");
                    
        DrumPatternCore.tempo = tempoBar.getProgress() + DrumPatternCore.MIN_TEMPO;
        mSoundMaker.totalPatternRepeats = repeatBarAssignValue(repeatBar.getProgress());
        DrumPatternCore.length = beatBar.getProgress() + DrumPatternCore.MIN_RIFF_LENGTH;
        // mSoundMaker.densityOfSequence = densityBar.getProgress();
        mSoundMaker.densityOfSequence = spinner.getSelectedItemPosition();


        // SET UP REPEAT COUNTER
        for (int i = 0; i < 8; i++) {
        	repeatCounterDot[i] = new ImageView(this);
        	repeatCounterDot[i].setImageResource(R.drawable.bluedot);        
        }
        repeatCounter.addView(repeatCounterDot[0]);
        repeatCounter.setVisibility(View.INVISIBLE);
        
        
        // SET UP TABS              
        if (getResources().getBoolean(R.bool.has_two_panes) == false) {
        	final ActionBar actionBar = getSupportActionBar();
	        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
	
	        actionBar.addTab(actionBar.newTab().setText(tab1)
	            .setTabListener(this));
	        actionBar.addTab(actionBar.newTab().setText(tab2)
	            .setTabListener(this));
        }
              
        setInstrumentSeekBarStatus();	
        implementPreferences();
        
        startActivityForResult((new Intent(this, Preferences.class).putExtra("init", true)), 2);

        try
        {
            app_ver = this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionName;
        }
        catch (NameNotFoundException e)
        {
            Log.d("versionfetcherror", e.getMessage());
        }

    	
    	// new CounterAsync().execute(); // Initiate the AsyncTask class at the end of initialization

        
        // ----------------------------------------- //
        // -------- END OF INITIALIZATION ---------- //
        // ----------------------------------------- //


        loadButton.setOnClickListener(new View.OnClickListener() {
              public void onClick(View v){
                  fetchSavedDrumsList();
              }
         });

        saveButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showPopups.saveDrumDialog();
            };
        });


        playButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.d("ERRATIC DRUMMER", "Play:OnClick");
                
            	SoundMaker.playKeyPressed = (!SoundMaker.playKeyPressed);
                saveButton.setVisibility(View.VISIBLE);

                if (SoundMaker.playKeyPressed) {
					playButton.setImageDrawable(getResources().getDrawable(R.drawable.button_play_on));
            		playButton.startAnimation(buttonflash);
					if (!SoundMaker.lockKeyPressed)
            			patternRepeatCounterAnimator(0); // slide in the counter
                	mSoundMakerThread = new Thread(mSoundMaker);
                    mSoundMakerThread.start();                
                	//nullSoundMakerThread = new Thread(nullSoundMaker);
                	//nullSoundMakerThread.start();      
                	mSoundMaker.running = true;
                	//nullSoundMaker.running = true;
            	}
            	else {
					playButton.setImageDrawable(getResources().getDrawable(R.drawable.button_play_off));
					playButton.clearAnimation();
            		if (!SoundMaker.lockKeyPressed)
            			patternRepeatCounterAnimator(2); // slide out the counter
            		
            		mSoundMaker.running = false;
            		
            	}
            }
        });        
        
        lockButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.d("ERRATIC DRUMMER", "Lock:OnClick");
                
            	SoundMaker.lockKeyPressed = (!SoundMaker.lockKeyPressed);
            	
            	if (SoundMaker.lockKeyPressed) {
            		lockButton.setImageDrawable(getResources().getDrawable(R.drawable.button_lock_on));
            		repeatBar.setEnabled(false);
            		repeatText.setText("Inf. ");
            		if (SoundMaker.playKeyPressed) {
            			patternRepeatCounterAnimator(2);
            		}
            	}
            	else {
					lockButton.setImageDrawable(getResources().getDrawable(R.drawable.button_lock_off));
            		repeatBar.setEnabled(true);
            		repeatText.setText( repeatBarAssignValue(repeatBar.getProgress()) + "x ");
            		if (SoundMaker.playKeyPressed) {
            			patternRepeatCounterAnimator(0);
            		}
            	}
            }
        });

        
        /**
        fwdButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.d("ERRATIC DRUMMER", "fwd:OnClick");
                patternRepeatCounterAnimator(1);
            	SoundMaker.fwdKeyPressed = true;           	
            }                
        });
        **/
        
        fwdButton.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {

				if (SoundMaker.playKeyPressed == true) {
					SoundMaker.fwdKeyPressed = true;
					if (!SoundMaker.lockKeyPressed)
						patternRepeatCounterAnimator(1);
				}
			}
		});

        tempoBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {				
				tempoText.setText( tempoBar.getProgress() + DrumPatternCore.MIN_TEMPO + " ");
				DrumPatternCore.tempo = tempoBar.getProgress() + DrumPatternCore.MIN_TEMPO;
			}

			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				
			}

			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				
			}
		});

        
        beatBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				beatText.setText((beatBar.getProgress() + DrumPatternCore.MIN_RIFF_LENGTH) + " ");
				DrumPatternCore.length = beatBar.getProgress() + DrumPatternCore.MIN_RIFF_LENGTH;
				
			}

			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				
			}

			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				
			}
        	
        });
        
        	
        repeatBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {	
					

				repeatText.setText( repeatBarAssignValue (repeatBar.getProgress()) + "x " );
				mSoundMaker.totalPatternRepeats = repeatBarAssignValue(repeatBar.getProgress());
				
			}

			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				
			}

			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				
			}
        	
        });
        
        spinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				// TODO Auto-generated method stub
				mSoundMaker.densityOfSequence = arg0.getSelectedItemPosition();
				((TextView) arg0.getChildAt(0)).setTextColor(Color.WHITE);
				((TextView) arg0.getChildAt(0)).setTypeface(null, Typeface.BOLD);
				((TextView) arg0.getChildAt(0)).setMaxWidth(150);
				 
			}

			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub
				
			}
        	
        });
        
        
        // * * * * * * * * *
        // Instrument Bars        
        // * * * * * * * * *
   
        
        inst1Bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
	
				mSoundMaker.instGroupPriority[0] = inst1Bar.getProgress();
				
			}

			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				
			}

			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				
			}
		});
        
        inst2Bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
	
				mSoundMaker.instGroupPriority[1] = inst2Bar.getProgress();
				
			}

			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				
			}

			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				
			}
		});

        inst3Bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
	
				mSoundMaker.instGroupPriority[2] = inst3Bar.getProgress();
				
			}

			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				
			}

			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				
			}
		});
        
        inst4Bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
	
				mSoundMaker.instGroupPriority[3] = inst4Bar.getProgress();
				
			}

			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				
			}

			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				
			}
		});
                       
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu1) {
			changeSoundBank(0, true);
			return true;
		} else if (item.getItemId() == R.id.menu2) {
			changeSoundBank(3, true);
			return true;
		} else if (item.getItemId() == R.id.menu3) {
			changeSoundBank(2, true);
			return true;
		} else if (item.getItemId() == R.id.about) {
            showPopups.showAbout(getResources(), app_ver);
			return true;
		} else if (item.getItemId() == R.id.preferences) {
			loadPreferencesPage();
			return true;
		} else if (item.getItemId() == R.id.rateit) {
			showPopups.loadRateit();
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
    }

    public void loadPreferencesPage() {
    	startActivityForResult((new Intent(this, Preferences.class)), REQUEST_CODE_PREFERENCES);
    	
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        grabPreferences();
      	implementPreferences();
    }

    public void grabPreferences() {
        SoundMaker.delaylessTransition = Preferences.KEY_DELAYLESS_TRANSITION;
        SoundMaker.variableVolume = Preferences.KEY_VARIABLE_VOLUME;
        SoundMaker.randomLength = Preferences.KEY_RANDOM_LENGTH;
        SoundMaker.randomTempo = Preferences.KEY_RANDOM_TEMPO;
        
		//about_me_diag.setMessage("" + SoundMaker.delaylessTransition + ", " + SoundMaker.variableVolume + ", " + SoundMaker.randomLength + ", " + SoundMaker.randomTempo);
		//about_me_diag.show();
    }
    
    public void implementPreferences(){
    	
    	// Enable or disable beatBar based on randomLength variable
    	if (SoundMaker.randomLength == true) {
    		beatBar.setEnabled(false);
    		beatText.setText("Random ");
    		//beatText.setTextColor(Color.BLUE);
    	}
    	else {
    		beatBar.setEnabled(true);
    		beatText.setText(beatBar.getProgress() + DrumPatternCore.MIN_RIFF_LENGTH + " ");
    		//beatText.setTextColor(Color.WHITE);
    	}
    	// Enable or disable tempoBar based on randomTempo variable
    	if (SoundMaker.randomTempo == true) {
    		tempoBar.setEnabled(false);
    		tempoText.setText("Random ");    		
    	}
    	else {
    			//
    	}
    	    	
    }

    public void fetchSavedDrumsList() {
        // once the list has been fetched
        showPopups.loadDrumList();
    }

    public void fetchSavedDrum(int rowid){
        // Stop playing
        final int rowid_final = rowid; // needed?
        final boolean tempStateKeeper = SoundMaker.playKeyPressed ;
        SoundMaker.playKeyPressed = false;

        // Show progress spinner, load patterns separately. UI related activities must not be in doInBackground
        // http://stackoverflow.com/questions/13660160/asynctask-trouble-an-error-occured-while-executing-doinbackground
        // http://www.emmbimobile.com/blog/2013/07/25/create-loading-spinner-progress-dialog-android/
        AsyncTask<Void, Void, Void> fetchPatternCore = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                DatabaseManager.getDrumPattern(rowid_final); // fetches AND sets the saved pattern
                return null;
            };
            @Override
            protected void onPostExecute(Void result) {
                pd.dismiss();
                changeSoundBank(DrumPatternCore.drum_type, false);
                mSoundMaker.separatorOfSequences();
                tempoBarManualChange( DrumPatternCore.tempo );
                beatBarManualChange( DrumPatternCore.length );
                // Retrieve previous play status
                SoundMaker.playKeyPressed = tempStateKeeper;

                // lock pattern and change UI
                if (!SoundMaker.lockKeyPressed)
                    lockButton.performClick();

                //Toast.makeText(this, "Pattern loaded.", Toast.LENGTH_LONG).show();
            };

        };
        pd = ProgressDialog.show(this, "Please Wait", "Loading from database...", true, true);
        fetchPatternCore.execute();
    }

    
    public void patternRepeatCounterAnimator(int code) {
		switch (code) {
			
	    	case 0:
	    		repeatCounter.removeAllViews();	    		
		    	for (int i = 0; i < mSoundMaker.totalPatternRepeats; i++) {
					repeatCounter.addView(repeatCounterDot[i]);	
					repeatCounterDot[i].setAlpha(200);
		    	}
		    	repeatCounter.setVisibility(View.VISIBLE);
		    	repeatCounter.startAnimation(slidein);    		
				break;		    	
	    	case 1:
	    		repeatCounter.startAnimation(fadeout);
	    		repeatCounter.setVisibility(View.INVISIBLE);
	    		
	    		repeatCounter.postDelayed(new Runnable() {	    		    
	    		    public void run() {	    			    
			    		repeatCounter.removeAllViews();
				    	for (int i = 0; i < mSoundMaker.totalPatternRepeats; i++) {
							repeatCounter.addView(repeatCounterDot[i]);	
							repeatCounterDot[i].setAlpha(200);
				    	}
				    	repeatCounter.clearAnimation();
				    	repeatCounter.setVisibility(View.VISIBLE);	
				    	repeatCounter.startAnimation(fadein);
	    		    }
	    		}, 500);	    		
		    	break;		    
	    	case 2:
	    		repeatCounter.startAnimation(slideout);
	    		repeatCounter.setVisibility(View.INVISIBLE);	  
	    		break;
		}		
    }
    
    public void changeSoundBank(int requestedSoundBank, boolean init){
        final int requestedSoundBank_final = requestedSoundBank;
        final boolean init_final = init;

    	if (SoundMaker.currentSoundBank == requestedSoundBank)
    		return;
    	final boolean tempStateKeeper = SoundMaker.playKeyPressed ;
        SoundMaker.playKeyPressed = false;

        AsyncTask<Void, Void, Void> fetchBankCore = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                mSoundMaker.disposeSoundpool();
                mSoundMaker.initializeSoundPool(requestedSoundBank_final, init_final);
                DrumPatternCore.drum_type = requestedSoundBank_final;
                return null;
            }
            @Override
            protected void onPostExecute(Void result) {
                SoundMaker.playKeyPressed = tempStateKeeper;
                setInstrumentSeekBarStatus(); // sets the names associated to each instrument group
                SoundMaker.currentSoundBank = requestedSoundBank_final;
                //Log.d("changesound", requestedSoundBank + " ");]
                pd.dismiss();
            }
        };
        pd = ProgressDialog.show(this, "Please Wait", "Loading sound bank...", true, true);
        fetchBankCore.execute();
    }
    
    public int repeatBarAssignValue(int temp) {
    	switch (temp) {
    		case 0:
    			return 1;
    		case 1:
    			return 2;
    		case 2:
    			return 3;
    		case 3:
    			return 7;
    	}
    	return 0;
    }

    public static void initializeRetrievedPattern() {

    }

    public static void tempoBarManualChange(int tempo) {
        tempoBar.setProgress( tempo - DrumPatternCore.MIN_TEMPO );
        tempoText.setText( tempo + " " );
    }

    public static void beatBarManualChange(int length){
        beatBar.setProgress(length - DrumPatternCore.MIN_RIFF_LENGTH);
        beatText.setText(length + " ");
    }

    
    public void setInstrumentSeekBarStatus(){
    	inst1Text.setText(mSoundMaker.loadedSoundPoolNames[0]);
    	inst2Text.setText(mSoundMaker.loadedSoundPoolNames[1]);
    	inst3Text.setText(mSoundMaker.loadedSoundPoolNames[2]);
    	inst4Text.setText(mSoundMaker.loadedSoundPoolNames[3]);
    	inst1Bar.setEnabled(true);
    	inst2Bar.setEnabled(true);
    	inst3Bar.setEnabled(true);
    	inst4Bar.setEnabled(true);
    	SoundMaker.instGroupPriority[0] = 3;
    	SoundMaker.instGroupPriority[1] = 3;
    	SoundMaker.instGroupPriority[2] = 3;
    	SoundMaker.instGroupPriority[3] = 3;
    	inst1Bar.setProgress(3);
    	inst2Bar.setProgress(3);
    	inst3Bar.setProgress(3);
    	inst4Bar.setProgress(3);
    	
    	/** USED IF A DRUMSET HAS LESS THAN 4 INSTS - NOT NECESSARY FOR NOW
    	if (mSoundMaker.loadedSoundPool[0].length == 0) {
    		inst1Bar.setEnabled(false);
    		SoundMaker.instGroupPriority[0] = 0;
    	}
    	if (mSoundMaker.loadedSoundPool[1].length == 0) {
    		inst2Bar.setEnabled(false);
    		SoundMaker.instGroupPriority[1] = 0;
    	}
    	if (mSoundMaker.loadedSoundPool[2].length == 0) {
    		inst3Bar.setEnabled(false);
    		SoundMaker.instGroupPriority[2] = 0;
    	}	
    	if (mSoundMaker.loadedSoundPool[3].length == 0) {
    		inst4Bar.setEnabled(false);
    		SoundMaker.instGroupPriority[3] = 0;
    	}
    	*/
    	
    	mSoundMaker.assignInstrumentGroupPriority();    		
    }


    @Override
    public void onBackPressed() {

    	super.onBackPressed();
    }

    protected void onPause(){
    	super.onPause(); 
    	Log.d("ERRATIC DRUMMER", "onPause");
    }
    
    protected void onResume(){
    	super.onResume(); 
    	Log.d("ERRATIC DRUMMER", "onResume");
    }

	@Override
	public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
		String selectedtab = tab.getText().toString();
		if(selectedtab.equalsIgnoreCase(tab2)) {
			viewFlip.showNext();
		}
	}

	@Override
	public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
		String selectedtab = tab.getText().toString();
		if(selectedtab.equalsIgnoreCase(tab2)) {
			viewFlip.showPrevious();
		}
	}

	@Override
	public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {

	}
}