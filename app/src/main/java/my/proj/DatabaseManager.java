package my.proj;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by Shaahin Sh on 4/13/2015.
 */
// http://www.codeproject.com/Articles/783073/A-Simple-Android-SQLite-Example

public class DatabaseManager {
    static SQLiteDatabase db;
    static int TABLE_MAX_CAP = 20;

    public DatabaseManager(Context context) {

        db = context.openOrCreateDatabase("DrumPatternDB", context.MODE_PRIVATE, null);
        // db.execSQL("DROP TABLE IF EXISTS DrumPatternTable");
        db.execSQL("CREATE TABLE IF NOT EXISTS " +
                        "DrumPatternTable(name VARCHAR(30), tempo INT, patternlength INT, soundbank INT, dmgroup0 VARCHAR, dminst0 VARCHAR, dmgroup1 VARCHAR, dminst1 VARCHAR" +
                        ");"
        );
    }

    public static int addDrumPattern(String name){
        try {
            String sql = "SELECT COUNT(*) FROM DrumPatternTable";
            SQLiteStatement statement = db.compileStatement(sql);
            long count = statement.simpleQueryForLong();
            Log.d("COUNT", count + "");

            if (count >= TABLE_MAX_CAP)
                return 1; // max limit reached

            db.execSQL("INSERT INTO DrumPatternTable VALUES('" + name + "','" + DrumPatternCore.tempo + "','" + DrumPatternCore.length + "','" + DrumPatternCore.drum_type +
                    "','" + (Arrays.toString(DrumPatternCore.group[0])) + "','" + (Arrays.toString(DrumPatternCore.instrument[0])) +
                    "','" + (Arrays.toString(DrumPatternCore.group[1])) + "','" + (Arrays.toString(DrumPatternCore.instrument[1])) + "');");

            Log.d("pattern saved", Arrays.toString(DrumPatternCore.instrument[0]));
            return 0; // success
        }
        catch (Exception ex) {
            Log.d("Exception occured", ex.toString());
            return 2; // other error
        }

    }

    public static void getDrumPattern(int rowid) {

        Cursor c=db.rawQuery("SELECT rowid, * FROM DrumPatternTable WHERE rowid = " + rowid, null);
        if (c.moveToNext()) {
            // Fetch time!
            DrumPatternCore.tempo = c.getInt(2);
            DrumPatternCore.length = c.getInt(3);
            DrumPatternCore.drum_type = c.getInt(4);
            stringToArrayInt(DrumPatternCore.group[0], c.getString(5));
            stringToArrayInt(DrumPatternCore.instrument[0], c.getString(6));
            stringToArrayInt(DrumPatternCore.group[1], c.getString(7));
            stringToArrayInt(DrumPatternCore.instrument[1], c.getString(8));

        }
        Log.d("pattern loaded", Arrays.toString(DrumPatternCore.instrument[0]));
        // close the cursor
        c.close();
    }

    public static String [] [] getDrumList() {

        Cursor c=db.rawQuery("SELECT rowid, name FROM DrumPatternTable", null);
        ArrayList<String> temp_list =new ArrayList<String>();
        ArrayList<String> temp_index =new ArrayList<String>();

        // AlertDialog seems to only accept arrays, so we convert
        // ArrayList to array later here.
        while(c.moveToNext())
        {
            temp_index.add(c.getString(0));
            temp_list.add(c.getString(1));
        }
        c.close();
        // String [0][] keeps rowIDs. String [1][] keeps the titles for patterns.
        return new String [] [] {
                temp_index.toArray(new String[temp_index.size()]),
                temp_list.toArray(new String[temp_list.size()])
        };
    }

    public static void deleteDrumPattern(String string_rowid) {
        int rowid = Integer.parseInt(string_rowid);
        db.execSQL("DELETE from DrumPatternTable WHERE rowid IS " + rowid);
    }

    public static void stringToArrayInt(int[] int_array, String raw_string){
        String [] temparray;
        temparray = raw_string.substring(1, raw_string.length() - 1).split(","); // gets rid of accolade chars
        Log.d("temparray ", Arrays.toString(temparray));
        for (int i = 0; i < temparray.length; i++) {
            try {
                int_array[i] = Integer.parseInt(temparray[i].replaceAll("\\s",""));
                //Log.d("Drum/temp", int_array[i] + " " + temparray[i] );
            } catch (NumberFormatException nfe) {
                Log.d("ERROR NumberFormatEx.", nfe.getMessage());
            };
        }
    }
}
