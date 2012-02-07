package org.zakky.smart_tag_app;

import java.io.InputStream;

import com.aioisystems.imaging.DisplayPainter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.net.Uri;
import android.util.Log;

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
		intent.setType("image/jpeg");
		intent.setData(contentUri);
		intent.putExtra("jp.r246.twicca.USER_SCREEN_NAME", "");
		return intent;
	}
	
	/**
     * スマートタグタメのBitmapを作成
     * 
     * @param bitmap
     * @return orientation
     */
    public static Bitmap editBitmapForTag(Bitmap bitmap, int orientation) {
        Bitmap newBitmap = bitmap;
        if (orientation == 1) {
            Matrix matrix = new Matrix();
            matrix.postRotate(-90);
            newBitmap = Bitmap.createBitmap(bitmap, 0, 0,
                    bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        }
        DisplayPainter painter = new DisplayPainter();
        painter.putImage(newBitmap, 0, 0, true);
        newBitmap.recycle();
        newBitmap = painter.getPreviewImage();
        return newBitmap;
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
			bitmap = BitmapFactory.decodeStream(is);
			is.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return bitmap;
	}

	/**
	 * SmartTag用にBitmapサイズ修正
	 * 
	 * @param src
	 * @return
	 */
	public static Bitmap resizeBitmapForSmartTag(Bitmap src) {
		return resizeBitamp(src, 200, 96, true);
	}

	/**
	 * Bitmapサイズ修正
	 * 
	 * @param src
	 * @param width
	 * @param height
	 * @return
	 */
	public static Bitmap resizeBitamp(Bitmap src, int width, int height,
			boolean autoRotate) {

		int srcWidth = src.getWidth(); // 元画像のwidth
		int srcHeight = src.getHeight(); // 元画像のheight

		// 画面サイズを取得する
		Matrix matrix = new Matrix();

		float widthScale = width / srcWidth;
		float heightScale = height / srcHeight;
		if (widthScale > heightScale) {
			matrix.postScale(heightScale, heightScale);
		} else {
			matrix.postScale(widthScale, widthScale);
		}

		// 回転
		if (autoRotate && (srcHeight > srcWidth && width > height)
				|| (srcWidth > srcHeight && height > width)) {
			matrix.postRotate(90);
		}

		// リサイズ
		Bitmap result = Bitmap.createBitmap(width, height, null);
		Canvas canvas = new Canvas(result);
		canvas.drawBitmap(src, matrix, null);

		return result;
	}

}
