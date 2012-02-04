package org.zakky.smart_tag_app;

import java.io.FileNotFoundException;
import java.io.InputStream;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

/**
 * 画像アップローダー<br>
 * twiccaプラグインを利用して画像をアップロードし、URLを返します。
 * 
 * @author mstssk
 */
public class MediaUtils {

	private static final String ACTION_UPLOAD = "jp.r246.twicca.ACTION_UPLOAD";

	/**
	 * アップロードのIntentを作成
	 * 
	 * @param contentUri
	 * @return intent
	 */
	public static Intent createUploadIntent(Uri contentUri) {
		Intent intent = new Intent(ACTION_UPLOAD);
		intent.addCategory(Intent.CATEGORY_DEFAULT);
		intent.setData(contentUri);
		return intent;
	}

	/**
	 * UriからBitmapを取得
	 * 
	 * @param context
	 * @param contentUri
	 * @return Bitmap
	 */
	public static Bitmap loadBitmapFromuri(Context context, Uri contentUri) {
		InputStream is = null;
		Bitmap bitmap = null;
		try {
			is = context.getContentResolver().openInputStream(contentUri);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		bitmap = BitmapFactory.decodeStream(is);
		return bitmap;
	}

}
