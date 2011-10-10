/*
 * Copyright 2011 YAMAZAKI Makoto<makoto1975@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


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
