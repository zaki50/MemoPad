
package org.zakky.memopad;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.MaskFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.net.Uri;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

public class PaintView extends View {

    private static final String TAG = PaintView.class.getSimpleName();

    /*
     * for stroke
     */
    private static final float TOUCH_TOLERANCE = 4;
    private static final int DEFAULT_PEN_COLOR = Color.BLACK;
    private final Paint mPaintForPen;
    private final Path mPath;
    private float mPrevX = Float.NaN;
    private float mPrevY = Float.NaN;

    /*
     * for off-screen
     */
    private final Paint mOffScreenPaint;
    private Bitmap mOffScreenBitmap;
    private Canvas mOffScreenCanvas;
    private int mBgColor;

    public PaintView(Context c, AttributeSet attrs) {
        super(c, attrs);

        mPaintForPen = new Paint();
        mPaintForPen.setColor(Color.BLACK);
        mPaintForPen.setAntiAlias(true);
        mPaintForPen.setDither(true);
        mPaintForPen.setColor(DEFAULT_PEN_COLOR);
        mPaintForPen.setStyle(Paint.Style.STROKE);
        mPaintForPen.setStrokeJoin(Paint.Join.ROUND);
        mPaintForPen.setStrokeCap(Paint.Cap.ROUND);
        mPaintForPen.setStrokeWidth(12.0F);

        final MaskFilter blur = new BlurMaskFilter(1,
                BlurMaskFilter.Blur.NORMAL);
        mPaintForPen.setMaskFilter(blur);

        mOffScreenPaint = new Paint(Paint.DITHER_FLAG);
        mOffScreenBitmap = null;
        mOffScreenCanvas = null;

        mPath = new Path();
        clearPath();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mOffScreenBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        mOffScreenCanvas = new Canvas(mOffScreenBitmap);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawBitmap(mOffScreenBitmap, 0.0F, 0.0F, mOffScreenPaint);
        canvas.drawPath(mPath, mPaintForPen);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);

        float currentX = event.getX();
        float currentY = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                handleTouchStart(currentX, currentY);
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                assert !Float.isNaN(mPrevX) && !Float.isNaN(mPrevY);
                for (int i = 0; i < event.getHistorySize(); i++) {
                    handleTouchMove(event.getHistoricalX(i), event.getHistoricalY(i));
                }
                handleTouchMove(currentX, currentY);
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
                assert !Float.isNaN(mPrevX) && !Float.isNaN(mPrevY);
                for (int i = 0; i < event.getHistorySize(); i++) {
                    handleTouchMove(event.getHistoricalX(i), event.getHistoricalY(i));
                }
                handleTouchEnd(currentX, currentY);
                invalidate();
                return true;
            default:
                return false;
        }
    }

    public void setPenColor(int argb) {
        mPaintForPen.setColor(argb);
    }

    @Override
    public void setBackgroundColor(int argb) {
        mBgColor = argb;
        super.setBackgroundColor(argb);
    }

    public void clearCanvas() {
        final int w = mOffScreenBitmap.getWidth();
        final int h = mOffScreenBitmap.getHeight();
        mOffScreenBitmap.recycle();

        mOffScreenBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        mOffScreenCanvas = new Canvas(mOffScreenBitmap);
        clearPath();
        invalidate();
    }

    private void handleTouchStart(float x, float y) {
        clearPath();
        mPath.moveTo(x, y);
        // タッチしただけで点が描かれるようにとりあえず１ドット線をひく
        mPath.lineTo(x + 1, y);
        mPrevX = x;
        mPrevY = y;
    }

    private void handleTouchMove(float x, float y) {
        if (Math.abs(x - mPrevX) < TOUCH_TOLERANCE
                && Math.abs(y - mPrevY) < TOUCH_TOLERANCE) {
            return;
        }
        mPath.quadTo(mPrevX, mPrevY, (mPrevX + x) / 2, (mPrevY + y) / 2);
        mPrevX = x;
        mPrevY = y;
    }

    private void handleTouchEnd(float x, float y) {
        mPath.lineTo(x, y);

        // オフスクリーンにコミットしてパスをクリア
        mOffScreenCanvas.drawPath(mPath, mPaintForPen);
        clearPath();
    }

    private void clearPath() {
        mPath.reset();
        mPrevX = Float.NaN;
        mPrevY = Float.NaN;
    }

    private File prepareImageBaseDir() {
        final String appName = getResources().getString(R.string.app_name);
        final File baseDir = new File(Environment.getExternalStorageDirectory(), appName);
        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }
        if (!baseDir.isDirectory()) {
            Log.e(TAG, "not a directory: " + baseDir.getPath());
            return null;
        }
        return baseDir;
    }

    private File createImageFileForNew(File baseDir, String extention) {
        boolean interrupted = false;
        File imageFile = null;
        do {
            if (imageFile != null) {
                // ２回目以降は少し待つ
                try {
                    TimeUnit.MILLISECONDS.sleep(10L);
                } catch (InterruptedException e) {
                    interrupted = true;
                }
            }
            imageFile = new File(baseDir, "image-" + System.currentTimeMillis() + "." + extention);
        } while (imageFile.exists());

        if (interrupted) {
            Thread.currentThread().interrupt();
        }
        return imageFile;
    }

    private FileOutputStream openImageFile(File f) {
        try {
            return new FileOutputStream(f);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "failed to create image file: " + f.getPath(), e);
            return null;
        }
    }

    public Uri saveImageAsPng() {
        final File baseDir = prepareImageBaseDir();
        if (baseDir == null) {
            return null;
        }
        final File imageFile = createImageFileForNew(baseDir, "png");
        final OutputStream os = openImageFile(imageFile);
        if (os == null) {
            return null;
        }
        try {
            final Bitmap bitmap = Bitmap.createBitmap(mOffScreenBitmap.getWidth(),
                    mOffScreenBitmap.getHeight(), Config.ARGB_8888);
            try {
                bitmap.eraseColor(mBgColor);
                final Canvas canvas = new Canvas(bitmap);
                canvas.drawBitmap(mOffScreenBitmap, 0.0f, 0.0f, mOffScreenPaint);
                if (!bitmap.compress(CompressFormat.PNG, 100, os)) {
                    Log.e(TAG, "failed to create image file: " + imageFile.getPath());
                    return null;
                }
                return Uri.fromFile(imageFile);
            } finally {
                bitmap.recycle();
            }
        } finally {
            try {
                os.close();
            } catch (IOException e) {
                Log.e(TAG, "failed to create image file: " + imageFile.getPath());
                return null;
            }
        }
    }
}
