package my.proj;

import android.R.style;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentTransaction;
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

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.ActionBar.TabListener;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
// import com.mhgwlminzr.tzlliwgeem158145.ApSmartWall;

public class Main extends SherlockActivity implements TabListener {
    /** Called when the activity is first created. */
// 	ApSmartWall apSmartWall;
	AlertDialog.Builder troubleshooterDialog;
	SharedPreferences prefs;
	ViewFlipper viewFlip;
	String app_ver;
	
	static SoundMaker mSoundMaker;
	static Thread mSoundMakerThread;
	
	int currentSoundBank = 0;
	
	SharedPreferences mainPreferences;
	//public final static String PREFS_NAME = "erratic_drummer_prefs";
	public final static int REQUEST_CODE_PREFERENCES = 1;
    

	SeekBar inst1Bar, inst2Bar, inst3Bar, inst4Bar;
	TextView inst1Text, inst2Text, inst3Text, inst4Text;
	String tab1, tab2;
	
	SeekBar tempoBar;
	SeekBar beatBar;
	SeekBar repeatBar;
	TextView tempoText;
	TextView beatText;
	TextView repeatText;
	
	ImageView [] repeatCounterDot = new ImageView [8];
	LinearLayout repeatCounter;
	Animation fadein, fadeout, slidein, slideout, buttonflash;
	 
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

//        if (apSmartWall == null)
//        	apSmartWall = new ApSmartWall(Main.this, null);
        
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
    	
        final Button playButton = (Button) findViewById(R.id.play_button);
        final Button lockButton = (Button) findViewById(R.id.lock_button);
        final Button fwdButton = (Button) findViewById(R.id.fwd_button);
        //final Button wrenchButton = (Button) findViewById(R.id.wrench_button);
        //final Button backButton = (Button) findViewById(R.id.back_button);
        

        
        repeatCounter = (LinearLayout) findViewById(R.id.linearLayoutRepeatCounter);
        
        
        tempoBar = (SeekBar) findViewById(R.id.tempoBar);
        beatBar = (SeekBar) findViewById(R.id.beatBar);
        repeatBar = (SeekBar) findViewById(R.id.repeatBar);      
        tempoText = (TextView) findViewById(R.id.tempoText);
        beatText = (TextView) findViewById(R.id.beatText);
        repeatText = (TextView) findViewById(R.id.repeatText);
        
        tempoBar.setMax(SoundMaker.MAX_TEMPO - SoundMaker.MIN_TEMPO);
        tempoBar.setProgress(SoundMaker.START_TEMPO - SoundMaker.MIN_TEMPO);
        repeatBar.setMax(3);
        repeatBar.setProgress(2);
        beatBar.setMax(SoundMaker.MAX_RIFF_LENGTH - SoundMaker.MIN_RIFF_LENGTH);
        beatBar.setProgress(SoundMaker.START_RIFF_LENGTH - SoundMaker.MIN_RIFF_LENGTH);      


        repeatText.setText( repeatBarAssignValue(repeatBar.getProgress()) + "x ");
        beatText.setText((beatBar.getProgress() + SoundMaker.MIN_RIFF_LENGTH) + " ");
        tempoText.setText(SoundMaker.START_TEMPO + " ");
        //densityText.setText("Normal");
                    
        mSoundMaker.tempo = tempoBar.getProgress() + SoundMaker.MIN_TEMPO;
        mSoundMaker.totalPatternRepeats = repeatBarAssignValue(repeatBar.getProgress());
        mSoundMaker.patternLength = beatBar.getProgress() + SoundMaker.MIN_RIFF_LENGTH;
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
        

        
        playButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.d("ERRATIC DRUMMER", "Play:OnClick");
                
            	SoundMaker.playKeyPressed = (!SoundMaker.playKeyPressed);
            	
            	if (SoundMaker.playKeyPressed) {
            		playButton.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.button_play_on, 0, 0);
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
            		playButton.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.button_play_off, 0, 0);
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
            		lockButton.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.button_lock_on, 0, 0);
            		repeatBar.setEnabled(false);
            		repeatText.setText("Inf. ");
            		if (SoundMaker.playKeyPressed) {
            			patternRepeatCounterAnimator(2);
            		}
            	}
            	else {
            		lockButton.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.button_lock_off, 0, 0);
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
        
        fwdButton.setOnTouchListener(new View.OnTouchListener() {
			
			public boolean onTouch(View v, MotionEvent event) {
				if(event.getAction() == MotionEvent.ACTION_DOWN) {					
					fwdButton.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.button_next_on, 0, 0);
				}
				else if (event.getAction() == MotionEvent.ACTION_UP) {
					fwdButton.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.button_next_off, 0, 0);	                
	                if (SoundMaker.playKeyPressed == true) {
	                	SoundMaker.fwdKeyPressed = true;
	                	if (!SoundMaker.lockKeyPressed)
	                		patternRepeatCounterAnimator(1);
	                }
				}
				
				return false;
			}
									
		});

        tempoBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {				
				tempoText.setText( tempoBar.getProgress() + SoundMaker.MIN_TEMPO + " ");				
				mSoundMaker.tempo = tempoBar.getProgress() + SoundMaker.MIN_TEMPO;
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
				beatText.setText((beatBar.getProgress() + SoundMaker.MIN_RIFF_LENGTH) + " ");
				mSoundMaker.patternLength = beatBar.getProgress() + SoundMaker.MIN_RIFF_LENGTH;
				
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




        /**
        densityBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				mSoundMaker.densityOfSequence = densityBar.getProgress();
				switch(densityBar.getProgress()) {
				case 0:
					densityText.setText("Light");
					return;
				case 1:
					densityText.setText("Normal");
					return;
				case 2:
					densityText.setText("Dense");
					return;
				}								
			}

			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				
			}

			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				
			}
        	
        });  **/
        
        
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
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu1) {
			changeSoundBank(0);
			return true;
		} else if (item.getItemId() == R.id.menu2) {
			changeSoundBank(3);
			return true;
		} else if (item.getItemId() == R.id.menu3) {
			changeSoundBank(2);
			return true;
		} else if (item.getItemId() == R.id.about) {
			showAbout();
			return true;
		} else if (item.getItemId() == R.id.preferences) {
			loadPreferencesPage();
			return true;
		} else if (item.getItemId() == R.id.rateit) {
			loadRateit();
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
    }
   
    
    public void showAbout() {
    	final TextView showAboutView = new TextView(getApplicationContext());
    	showAboutView.setText("Erratic Drummer is created for progressive musicians and ear-trained music enthusiasts. It helps your musical ideas flow " +
    			"by generating customizable drum riffs based on your taste, while keeping the controls simple and intuitive.\n\n" +
    			"Either plug your guitar in, lock on a cool drum pattern and start jamming, or put your headphones on " +
    			"and let the non-stop flow of diverse drum riffs give you joy or even inspiration for writing mindblowing progressive drum riffs!" +
    			"\n\nIf you like the app, please spare a few seconds and rate it! As a freelance developer, I greatly depend on your ratings and word-of-mouth. Thank you!\n");
    	showAboutView.setTextSize(12);
    	//showAboutView.setBackgroundColor(R.drawable.transparent);
    	showAboutView.setPadding(10, 15, 10, 15);
    	
    	troubleshooterDialog.setTitle("Erratic Drummer");
    	troubleshooterDialog.setInverseBackgroundForced(true);
    	troubleshooterDialog.setMessage("Version: " + app_ver + "\nAuthor: Shaahin Shahbazi\nshaahin@gmail.com");
    	troubleshooterDialog.setIcon(R.drawable.ic_appicon);
    	troubleshooterDialog.setView(showAboutView);
    	
    	troubleshooterDialog.show();
    }
    
    public void loadPreferencesPage() {
    	startActivityForResult((new Intent(this, Preferences.class)), REQUEST_CODE_PREFERENCES);
    	
    }
    
    public void loadRateit() {
        Uri uri = Uri.parse("market://details?id=" + getPackageName());
        Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
        try {
            startActivity(goToMarket);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Market could not be launched!", Toast.LENGTH_LONG).show();
        }
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
        
		//troubleshooterDialog.setMessage("" + SoundMaker.delaylessTransition + ", " + SoundMaker.variableVolume + ", " + SoundMaker.randomLength + ", " + SoundMaker.randomTempo);
		//troubleshooterDialog.show();
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
    		beatText.setText(beatBar.getProgress() + SoundMaker.MIN_RIFF_LENGTH + " ");
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
    
    public void changeSoundBank(int requestedSoundBank){
    	if (currentSoundBank == requestedSoundBank)
    		return;
    	boolean tempStateKeeper = SoundMaker.playKeyPressed ;
    	SoundMaker.playKeyPressed = false;
    	mSoundMaker.disposeSoundpool();
    	mSoundMaker.initializeSoundPool(requestedSoundBank);
    	currentSoundBank = requestedSoundBank;
    	SoundMaker.playKeyPressed = tempStateKeeper;
    	setInstrumentSeekBarStatus(); // sets the names associated to each instrument group
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
    
	public void onTabSelected(Tab tab, FragmentTransaction ft) {
		String selectedtab = tab.getText().toString();
		if(selectedtab.equalsIgnoreCase(tab2)) {
			viewFlip.showNext();			
		}

	}

	public void onTabUnselected(Tab tab, FragmentTransaction ft) {
		String selectedtab = tab.getText().toString();
		if(selectedtab.equalsIgnoreCase(tab2)) {
			viewFlip.showPrevious();			
		}
	}

	public void onTabReselected(Tab tab, FragmentTransaction ft) {
		

	}
}