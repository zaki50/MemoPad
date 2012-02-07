package org.zakky.smart_tag_app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcF;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.aioisystems.smarttagsample.Common;
import com.aioisystems.smarttagsample.R;
import com.aioisystems.smarttagsample.SmartTag;

public class SmartTagAppActivity extends Activity {
    
    private NfcAdapter mAdapter = null;
    private PendingIntent mPendingIntent;
    private IntentFilter[] mFilters;
    private String[][] mTechLists;
    
    private SmartTagTask mTagTask;
    
    //スマートタグ出力の向き
    private int ORIENTATION = 0;
    
    //アップロードした画像URL
    private String UPLOADED_IMG_URL = null;
    
    private static final SmartTag mSmartTag = new SmartTag();
    private static final SmartTag mSmartTag2 = new SmartTag();
    private static final int REQUEST_CROP_PICK = 1;
    private static final int REQUEST_UPLOAD = 2;
    
    private ImageView imageView;
    private TextView imgUrlView;
    private Uri CONTENT_URI;
    private Intent cropIntent = new Intent("com.android.camera.action.CROP");
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        imageView = (ImageView)findViewById(R.id.imageview);
        imgUrlView = (TextView)findViewById(R.id.imgUrlView);
        
        //スマートタグ設定
        mSmartTag.setFunctionNo(SmartTag.FN_DRAW_CAMERA_IMAGE);
        mSmartTag2.setFunctionNo(SmartTag.FN_WRITE_DATA);
        
        //NFC機能初期設定
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
            Log.v("ERROR",e.getMessage());
        }
        
        if (getIntent().getAction().equals(Intent.ACTION_SEND)) {
            Uri uri = Uri.parse(getIntent().getExtras().get("android.intent.extra.STREAM").toString());
            CONTENT_URI = uri;
            if (uri != null) {
                cropIntent.setData(uri);
                
                //縦か横か、画像を切り抜く
                String[] dialogItem = new String[]{"Cutout Vertical","Cutout Horizontal"};
                AlertDialog.Builder opDialog = new AlertDialog.Builder(this);
                opDialog.setTitle("Option");
                opDialog.setItems(dialogItem, dialogListener).create().show();
            }
        }
    }
    
    private DialogInterface.OnClickListener dialogListener = 
        new DialogInterface.OnClickListener(){

            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                case 0:
                    cropIntent.putExtra("outputX", 96);
                    cropIntent.putExtra("outputY", 200);
                    cropIntent.putExtra("aspectX", 100);
                    cropIntent.putExtra("aspectY", 208);
                    ORIENTATION = 1;
                    
                    break;
                case 1:
                    cropIntent.putExtra("outputX", 200);
                    cropIntent.putExtra("outputY", 96);
                    cropIntent.putExtra("aspectX", 208);
                    cropIntent.putExtra("aspectY", 100);
                    ORIENTATION = 0;
                    
                    break;
                }
                cropIntent.putExtra("scale", true);
                cropIntent.putExtra("return-data", true);
                startActivityForResult(cropIntent, REQUEST_CROP_PICK);
            }
    };
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK){
            if (requestCode == REQUEST_CROP_PICK) {
                if (data != null) {
                    //画像表示
                    Bitmap viewBitmap = data.getExtras().getParcelable("data");
                    imageView.setImageBitmap(viewBitmap);
                    
                    //タグ書き込み用を作成
                    Bitmap tagBitmap = data.getExtras().getParcelable("data");
                    mSmartTag.setCameraImage(
                            MediaUtils.editBitmapForTag(tagBitmap, ORIENTATION));
                    
                    //twiccaプラグインで画像アップロード
                    try {
                        Intent uploadIntent = MediaUtils.createUploadIntent(CONTENT_URI);
                        startActivityForResult(uploadIntent, REQUEST_UPLOAD);
                    } catch(Exception e){
                        Log.v("ERROR",e.getMessage());
                    }
                }
            } else if (requestCode == REQUEST_UPLOAD) {
                UPLOADED_IMG_URL = data.getData().toString();
                imgUrlView.setText(UPLOADED_IMG_URL);
                mSmartTag2.setWriteText(UPLOADED_IMG_URL);
            }
        } else {
            Log.v("ERROR","result error:" + String.valueOf(requestCode));
            //finish();
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
        Tag tag = (Tag)intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        byte[] idm = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID);
        mSmartTag.selectTarget(idm, tag);
        mSmartTag2.selectTarget(idm, tag);
        
        //非同期処理クラス初期化
        mTagTask = new SmartTagTask(this, this, mSmartTag);
        mTagTask.execute();
    }
    
    public void writeUrlTag(){
        if (UPLOADED_IMG_URL != null) {
            SmartTagTask tagTask = new SmartTagTask(this, this, mSmartTag2);
            tagTask.execute();
        }
    }
}