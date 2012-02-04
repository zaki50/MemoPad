package org.zakky.smart_tag_app;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.aioisystems.smarttagsample.SmartTag;

public class SmartTagTask extends AsyncTask<Void, Void, Void> {
    
    private static SmartTag mSmartTag;
    private static Context mContext;
    
    public SmartTagTask(Context context, SmartTag smartTag){
        mContext = context;
        mSmartTag = smartTag;
    }
    
    @Override
    protected void onPreExecute() {
    }

    @Override
    protected Void doInBackground(Void... params) {
        Log.v("TEST","SmartTagTask doInBackground");
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
            if(function == SmartTag.FN_CHANGE_LAYOUT){
                //次の番号に切り替える
            }else if(function == SmartTag.FN_READ_DATA){
                //URLを開く
                //openUrl(mSmartTag.getReadText());
                //showAlert(mSmartTag.getReadText());
            }


        }
        Log.v("TEST","onPostExecute");
        Toast.makeText(mContext, "そうしんしたよ！",
                Toast.LENGTH_LONG).show();
    }
}
