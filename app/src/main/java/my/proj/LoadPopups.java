package my.proj;

import android.content.ActivityNotFoundException;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.text.InputFilter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Shaahin Sh on 4/13/2015.
 */

public class LoadPopups {
    String result = "";
    AlertDialog.Builder about_me_diag;
    Main context;
    LoadPopups(Main context){
        this.context = context;
    }

    public void showAbout(Resources res, String app_ver) {

        about_me_diag = new AlertDialog.Builder(context);
        final TextView showAboutView = new TextView(context);
        showAboutView.setText(R.string.aboutapp);
        showAboutView.setTextColor(res.getColor(android.R.color.black));
        showAboutView.setTextSize(12);
        about_me_diag.setTitle("Erratic Drummer");
        about_me_diag.setMessage("Version " + app_ver + "\nshaahin@gmail.com");
        about_me_diag.setInverseBackgroundForced(true);
        about_me_diag.setIcon(R.drawable.ic_appicon);
        about_me_diag.setView(showAboutView);
        showAboutView.setPadding(15, 15, 15, 15);
        about_me_diag.setNeutralButton("Dismiss", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dlg, int sumthin) {

            }
        });
        about_me_diag.setPositiveButton("Rate", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dlg, int sumthin) {
                loadRateit();
            }
        });
        about_me_diag.show();

        /*
        AlertDialog.Builder alertadd = new AlertDialog.Builder(context);
        LayoutInflater factory = LayoutInflater.from(context);
        final View view = factory.inflate(R.layout.aboutme, null);
        alertadd.setView(view);
        view.setPadding(20, 20, 20, 20);
        alertadd.setNeutralButton("Dismiss", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dlg, int sumthin) {

            }
        });

        alertadd.show();
        */
    }

    public void loadRateit() {
        Uri uri = Uri.parse("market://details?id=" + context.getPackageName());
        Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
        try {
            context.startActivity(goToMarket);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, "Market could not be launched!", Toast.LENGTH_LONG).show();
        }
    }

    public void loadDrumList() {

        Log.d("ERRATIC DRUMMER", "Play:OnLoad");
        AlertDialog.Builder dialog = new AlertDialog.Builder(context);

        dialog.setTitle("Saved drum patterns:");
        // solution for finding the chosen item:
        // http://stackoverflow.com/questions/2494171/how-to-get-selected-item-of-a-singlechoice-alert-dialog
        dialog.setNeutralButton("Cancel", null);

        dialog.setPositiveButton("Load", new
                DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        ListView lv = ((AlertDialog)dialog).getListView();
                        Integer selected = (Integer)lv.getTag();
                        if(selected != null) {
                            dialog.cancel();
                            context.fetchSavedDrum(
                                    Integer.parseInt( DatabaseManager.getDrumList()[0][selected]
                                    ) );
                        }

                    }
                });
        dialog.setNegativeButton("Delete", new
                DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        ListView lv = ((AlertDialog)dialog).getListView();
                        Integer selected = (Integer)lv.getTag();
                        if(selected != null) {
                            DatabaseManager.deleteDrumPattern(DatabaseManager.getDrumList()[0][selected]);
                        }
                    }
                });

        dialog.setSingleChoiceItems(
                DatabaseManager.getDrumList()[1]
                , -1, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        ListView lv = ((AlertDialog) dialog).getListView();
                        lv.setTag(new Integer(which));
                    }
                });
        dialog.create().show();

    }

    public void initFetchSavedDrum(int which) {
        context.fetchSavedDrum(which);
    }

    public void saveDrumDialog() {
        Log.d("ERRATIC DRUMMER", "Play:OnSave");
        AlertDialog.Builder dialog = new AlertDialog.Builder(context);
        final EditText patternName = new EditText(context);
        patternName.setText(currentDateProvider());
        patternName.setSelection(patternName.getText().length()); // brings cursor to the end of text

        InputFilter[] FilterArray = new InputFilter[1];
        FilterArray[0] = new InputFilter.LengthFilter(30); // max text length
        patternName.setFilters(FilterArray);
        patternName.setSingleLine();

        dialog.setTitle("Save drum pattern as:");
        dialog.setView(patternName);
        dialog.setPositiveButton("Save", new
                DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // insert current drum pattern to database
                        int result = DatabaseManager.addDrumPattern(patternName.getText().toString());
                        context.loadButton.setVisibility(View.VISIBLE);
                        if (result == 0)
                            Toast.makeText(context, "Saved!", Toast.LENGTH_SHORT).show();
                        if (result == 1)
                            Toast.makeText(context, "Cannot save! Maximum capacity reached.", Toast.LENGTH_LONG).show();
                        if (result == 2)
                            Toast.makeText(context, "An error occured!", Toast.LENGTH_LONG).show();
                        dialog.cancel();
                    }
                });
        dialog.setNegativeButton("Cancel", new
                DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        dialog.show();
        return;
    }

    public String currentDateProvider() {
        DateFormat dateFormat = new SimpleDateFormat("MM/dd HH:mm");
        Date date = new Date();
        return (dateFormat.format(date)); //2014/08/06 15:59:48
    }
}
