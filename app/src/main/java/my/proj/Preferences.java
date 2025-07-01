package my.proj;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;
import android.util.Log;


public class Preferences extends PreferenceActivity {	

	CheckBoxPreference CHECKBOX_DELAYLESS_TRANSITION;
	CheckBoxPreference CHECKBOX_VARIABLE_VOLUME;
	CheckBoxPreference CHECKBOX_RANDOM_LENGTH;
	CheckBoxPreference CHECKBOX_RANDOM_TEMPO;
	CheckBoxPreference CHECKBOX_WAKELOCK;
    static boolean KEY_DELAYLESS_TRANSITION;
    static boolean KEY_VARIABLE_VOLUME;
    static boolean KEY_RANDOM_LENGTH ;
    static boolean KEY_RANDOM_TEMPO;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        
        CHECKBOX_DELAYLESS_TRANSITION = (CheckBoxPreference)getPreferenceScreen().findPreference("delaylessTransition");
        CHECKBOX_VARIABLE_VOLUME = (CheckBoxPreference)getPreferenceScreen().findPreference("variableVolume");
        CHECKBOX_RANDOM_LENGTH = (CheckBoxPreference)getPreferenceScreen().findPreference("randomLength");
        CHECKBOX_RANDOM_TEMPO = (CheckBoxPreference)getPreferenceScreen().findPreference("randomTempo");

      
       //  Log.d("PREFERENCES", "onCreate");
        
        boolean initCheck = getIntent().getBooleanExtra("init", false);
        if (initCheck == true)
        	onBackPressed();
    }
    
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

    }
    
    public void onBackPressed(){
    	super.onBackPressed();
    	
    	KEY_DELAYLESS_TRANSITION = CHECKBOX_DELAYLESS_TRANSITION.isChecked();
    	KEY_VARIABLE_VOLUME = CHECKBOX_VARIABLE_VOLUME.isChecked();
    	KEY_RANDOM_TEMPO = CHECKBOX_RANDOM_TEMPO.isChecked();
    	KEY_RANDOM_LENGTH = CHECKBOX_RANDOM_LENGTH.isChecked();
    	
    	// Log.d("PREFERENCES", "onBackPressed");

        if (getParent() == null) 
        	setResult(Activity.RESULT_OK);
        else
        	getParent().setResult(Activity.RESULT_OK);
        finish();
    }


// Read more: http://www.javacodegeeks.com/2011/01/android-quick-preferences-tutorial.html
}