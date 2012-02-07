package org.zakky.smart_tag_app;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.aioisystems.smarttagsample.SmartTag;

public class SmartTagTask extends AsyncTask<Void, Void, Void> {
    
    private static SmartTag mSmartTag;
    private static Context mContext;
    private static SmartTagAppActivity mActivity;
    
    public SmartTagTask(SmartTagAppActivity activity,Context context, SmartTag smartTag){
        mActivity = activity;
        mContext = context;
        mSmartTag = smartTag;
    }
    
    @Override
    protected void onPreExecute() {
    }

    @Override
    protected Void doInBackground(Void... params) {
        mSmartTag.startSession();
        return null;
    }

    @Override
    protected void onProgressUpdate(Void... values) {
        super.onProgressUpdate(values);
    }

    @Override
    protected void onPostExecute(Void result) {

        Exception error = mSmartTag.getLastError();
        if(error != null){
            Log.v("TEST","SmartTag Error!!!");
        }else{

            int function = mSmartTag.getFunctionNo();
            
            if (function == SmartTag.FN_DRAW_CAMERA_IMAGE) {
                mActivity.writeUrlTag();
                Toast.makeText(mContext, "画像を書き込んだお",
                        Toast.LENGTH_SHORT).show();
            } else if (function == SmartTag.FN_WRITE_DATA) {
                Toast.makeText(mContext, "URLを書き込んだお",
                        Toast.LENGTH_LONG).show();
            }

        }
    }
}
