package org.zakky.smart_tag_app;

import java.io.InputStream;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcF;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import com.aioisystems.imaging.DisplayPainter;
import com.aioisystems.smarttagsample.Common;
import com.aioisystems.smarttagsample.R;
import com.aioisystems.smarttagsample.SmartTag;

public class SmartTagAppActivity extends Activity {
    /** Called when the activity is first created. */
    
    private NfcAdapter mAdapter = null;
    private PendingIntent mPendingIntent;
    private IntentFilter[] mFilters;
    private String[][] mTechLists;
    
    private SmartTagTask mTagTask;
    
    private static final SmartTag mSmartTag = new SmartTag();
    
    private ImageView imageView;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        imageView = (ImageView)findViewById(R.id.imageview);
        mSmartTag.setFunctionNo(SmartTag.FN_SHOW_STATUS);
        
        try {
            mAdapter = NfcAdapter.getDefaultAdapter(this);
            if (mAdapter != null) {
                mPendingIntent = PendingIntent.getActivity(this, 0,
                        new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
                IntentFilter filter = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
                filter.addDataType("*/*");
                
                mFilters = new IntentFilter[]{
                        filter,
                    };
                mTechLists = new String[][]{new String[] { NfcF.class.getName() }};
            }
        } catch (Exception e) {
            
        }
        
        
        if (getIntent().getAction().equals(Intent.ACTION_SEND)) {
            Uri uri = Uri.parse(getIntent().getExtras().get("android.intent.extra.STREAM").toString());
            if (uri != null) {
                Bitmap bitmap = null;
                BitmapFactory.Options mOptions = new BitmapFactory.Options();
                mOptions.inSampleSize = 4;

                try {
                    InputStream is = getContentResolver().openInputStream(uri);
                    bitmap = BitmapFactory.decodeStream(is, null, mOptions);
                    is.close();
                } catch (Exception e) {
                    
                }
                
                
                //Bitmap bitmap = MediaUtils.loadBitmapFromuri(this, uri);
                
                int width = bitmap.getWidth();
                int height = bitmap.getHeight();
                //my resize
                int newWidth = 96;
                int newHeight = 200;
               
                // calculate the scale - in this case = 0.4f
                float scaleWidth = ((float) newWidth) / width;
                float scaleHeight = ((float) newHeight) / height;
//                scaleWidth = newWidth;
//                scaleHeight = newHeight;
               
                // createa matrix for the manipulation
                Matrix matrix = new Matrix();
                // resize the bit map
                matrix.postScale(scaleWidth, scaleHeight);
                // rotate the Bitmap
                matrix.postRotate(90);

                // recreate the new Bitmap
                Bitmap resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0,
                                  width, height, matrix, true);
                
                
                imageView.setImageBitmap(resizedBitmap);
                //Bitmap bitmap = MediaUtils.loadBitmapFromuri(this, uri);
                mSmartTag.setFunctionNo(mSmartTag.FN_DRAW_CAMERA_IMAGE);
                DisplayPainter painter = new DisplayPainter();
                painter.putImage(bitmap, 0, 0, true);
                bitmap.recycle();
                bitmap = painter.getPreviewImage();
                mSmartTag.setCameraImage(resizedBitmap);
                
                ///imageView.setImageBitmap(bitmap);
                
//                try {
//                    Intent uploadIntent = MediaUtils.createUploadIntent(uri);
//                    startActivityForResult(uploadIntent, 1);
//                } catch(Exception e){
//                    Log.v("Error",e.getMessage());
//                }
            }
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK){
            Log.v("TEST","OKだよ！");
        } else {
            Log.v("TEST","だめだたよ！");
        }
        if (requestCode == 1) {
            if (data != null) {
                String imgUrl = data.getData().toString();
                Log.v("TEST","URL:"+imgUrl);
            }
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();

        Common.addLogi("start adapter");
        if(mAdapter != null){
            mAdapter.enableForegroundDispatch(
                    this, mPendingIntent, mFilters, mTechLists);
        }
    }
    
    @Override
    protected void onNewIntent(Intent intent){
        Log.v("TEST","onNewIntent");
        Tag tag = (Tag)intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        byte[] idm = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID);

        mSmartTag.selectTarget(idm, tag);
        
        //非同期処理クラス初期化
        mTagTask = new SmartTagTask(this, mSmartTag);
        
        mTagTask.execute();
    }
}