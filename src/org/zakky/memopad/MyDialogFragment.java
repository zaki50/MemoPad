
package org.zakky.memopad;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;

public class MyDialogFragment extends DialogFragment {
    private static final String PREF_FILE_NAME = "config";

    private static final String PREF_KEY_SHOW_AT_STARTUP = "show_at_startup";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, android.R.style.Theme_Holo_Dialog);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setContentView(R.layout.dialog);
        dialog.setTitle(R.string.app_name);

        dialog.findViewById(R.id.ok_button).setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                v.setEnabled(false);
                final CheckBox box = (CheckBox) dialog.findViewById(R.id.show_this_dialog_at_startup);

                final SharedPreferences pref = dialog.getContext().getSharedPreferences(
                        PREF_FILE_NAME, Context.MODE_PRIVATE);
                final Editor editor = pref.edit();
                try {
                    editor.putBoolean(PREF_KEY_SHOW_AT_STARTUP, box.isChecked());
                } finally {
                    editor.commit();
                }
                dialog.dismiss();
            }
        });
        return dialog;
    }

    public static boolean showAtStartup(Context context) {
        final SharedPreferences pref = context.getSharedPreferences(PREF_FILE_NAME,
                Context.MODE_PRIVATE);
        final boolean result = pref.getBoolean(PREF_KEY_SHOW_AT_STARTUP, true);
        return result;
    }
}
