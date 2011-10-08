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

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.Media;
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

/**
 * お絵かき用の {@link View} です。 {@link View} がもともと持っている背景色、commit
 * されたストローク用のオフスクリーンビットマップ、 現在描いている途中のストロークを別々に保持し、描画時に({@link #onDraw(Canvas)}
 * で)合成します。
 */
public class PaintView extends View {

    private static final String TAG = PaintView.class.getSimpleName();

    /**
     * サポートする最大のポインタ数。
     */
    private static final int MAX_POINTERS = 20;

    /*
     * for stroke
     */
    private static final float TOUCH_TOLERANCE = 4;

    private static final int DEFAULT_PEN_COLOR = Color.BLACK;

    private final Paint mPaintForPen;

    private int mCurrentMaxPointerCount = 0;

    /**
     * パスの配列。
     *
     * <p>
     * 配列の長さは {@link #MAX_POINTERS} で初期化されます。
     * PointerId を配列のインデックスとして使用します。ストローク中の Pointer は
     * {@code non-null}, ストロークが無いときは {@code null} です。
     * </p>
     */
    private final Path[] mPath;

    /**
     * 各ポインタの前回の X 座標の値。ベジェ曲線を作成する際に使用します。
     *
     * <p>
     * 配列の長さは {@link #MAX_POINTERS} で初期化されます。
     * </p>
     */
    private float[] mPrevX;

    /**
     * 各ポインタの前回の Y 座標の値。ベジェ曲線を作成する際に使用します。
     *
     * <p>
     * 配列の長さは {@link #MAX_POINTERS} で初期化されます。
     * </p>
     */
    private float[] mPrevY;

    /*
     * for off-screen
     */
    private final Paint mOffScreenPaint;

    private Bitmap mOffScreenBitmap;

    private Canvas mOffScreenCanvas;

    /**
     * 背景色(AARRGGBB)
     */
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

        mOffScreenPaint = new Paint(Paint.DITHER_FLAG);
        mOffScreenBitmap = null;
        mOffScreenCanvas = null;

        mPath = new Path[MAX_POINTERS];
        mPrevX = new float[MAX_POINTERS];
        mPrevY = new float[MAX_POINTERS];
        clearAllPaths();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w <= 0 || h <= 0) {
            return;
        }
        mOffScreenBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        mOffScreenCanvas = new Canvas(mOffScreenBitmap);
    }

    /**
     * {@link View} の中身を描画します。親クラスで描画した背景の上にコミット済みのストローク画像を コピーし、最後に
     * {@link #mPath} が保持する未コミットのストロークを描画します。
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawBitmap(mOffScreenBitmap, 0.0F, 0.0F, mOffScreenPaint);
        for (int i = 0; i < mCurrentMaxPointerCount; i++) {
            final Path path = mPath[i];
            if (path == null) {
                continue;
            }
            canvas.drawPath(path, mPaintForPen);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);

        final int pointerCount = event.getPointerCount();
        for (int pIndex = 0; pIndex < pointerCount; pIndex++) {

            float currentX = event.getX(pIndex);
            float currentY = event.getY(pIndex);

            final int pointerId = event.getPointerId(pIndex);
            if (MAX_POINTERS <= pointerId) {
                Log.i(TAG, "too many pointers(PointerId = " + pointerId + ").");
                return true;
            }
            mCurrentMaxPointerCount = Math.max(mCurrentMaxPointerCount, pointerId + 1);

            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_POINTER_DOWN:
                    if (event.getActionIndex() != pIndex) {
                        continue;
                    }
                    // 現在の座標から描画開始
                    handleTouchStart(currentX, currentY, pointerId);
                    invalidate(); // 面倒なので View 全体を再描画要求
                    break;
                case MotionEvent.ACTION_MOVE:
                    assert !Float.isNaN(mPrevX[pointerId]) && !Float.isNaN(mPrevY[pointerId]);
                    for (int i = 0; i < event.getHistorySize(); i++) {
                        // 未処理の move イベントを反映させる。
                        handleTouchMove(event.getHistoricalX(pIndex, i),
                                event.getHistoricalY(pIndex, i), pointerId);
                    }
                    // 現在の座標を move として反映する。
                    handleTouchMove(currentX, currentY, pointerId);
                    invalidate(); // 面倒なので View 全体を再描画要求
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                    if (event.getActionIndex() != pIndex) {
                        continue;
                    }
                    assert !Float.isNaN(mPrevX[pointerId]) && !Float.isNaN(mPrevY[pointerId]);
                    // 現在の座標をストローク完了として反映する。
                    handleTouchEnd(currentX, currentY, pointerId);
                    invalidate(); // 面倒なので View 全体を再描画要求
                    break;
                default:
                    return false;
            }
        }
        return true;
    }

    /**
     * ペンの色をセットします。
     * 
     * @param argb ペンの色(AARRGGBB)。
     */
    public void setPenColor(int argb) {
        mPaintForPen.setColor(argb);
    }

    /**
     * ペンのサイズをセットします。
     *
     * @param size ペンのサイズ。
     */
    public void setPenSize(float size) {
        mPaintForPen.setStrokeWidth(size);
    }

    /**
     * 背景色をセットします。
     * 
     * @param argb 背景色(AARRGGBB)。
     */
    @Override
    public void setBackgroundColor(int argb) {
        mBgColor = argb;
        super.setBackgroundColor(argb);
    }

    /**
     * すべてのストロークを消去します。
     */
    public void clearCanvas() {
        mOffScreenBitmap.eraseColor(0); // 透明に戻す
        clearAllPaths();
        invalidate();
    }

    /**
     * 現在の画像を PNG ファイルとして書き出し、書きだしたファイルを {@link Uri} で返します。
     * 
     * @return 書きだしたファイルの Uri。書き出しが正常に行えなかった場合は {@code null} を返します。
     */
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
                updateMediaDatabase(imageFile);
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

    private void handleTouchStart(float x, float y, int pointerId) {
        preparePath(pointerId);
        assert mPath[pointerId] != null;
        mPath[pointerId].moveTo(x, y);
        // タッチしただけで点が描かれるようにとりあえず１ドット線をひく
        mPath[pointerId].lineTo(x + 1, y);
        mPrevX[pointerId] = x;
        mPrevY[pointerId] = y;
    }

    private void handleTouchMove(float x, float y, int pointerId) {
        final float prevX = mPrevX[pointerId];
        final float prevY = mPrevY[pointerId];
        if (Math.abs(x - prevX) < TOUCH_TOLERANCE && Math.abs(y - prevY) < TOUCH_TOLERANCE) {
            return;
        }
        mPath[pointerId].quadTo(prevX, prevY, (prevX + x) / 2, (prevY + y) / 2);
        mPrevX[pointerId] = x;
        mPrevY[pointerId] = y;
    }

    private void handleTouchEnd(float x, float y, int pointerId) {
        if (mPath[pointerId] == null) {
            return;
        }
        mPath[pointerId].lineTo(x, y);
        // オフスクリーンにコミットしてパスをクリア
        mOffScreenCanvas.drawPath(mPath[pointerId], mPaintForPen);

        mPath[pointerId].close();
        mPath[pointerId] = null;
    }

    private void clearAllPaths() {
        for (int i = 0; i < MAX_POINTERS; i++) {
            if (mPath[i] != null) {
                mPath[i].close();
            }
            mPath[i] = null;
            mPrevX[i] = Float.NaN;
            mPrevY[i] = Float.NaN;
        }
    }

    private void preparePath(int pointerId) {
        if (mPath[pointerId] == null) {
            mPath[pointerId] = new Path();
        }
        mPath[pointerId].reset();
        mPrevX[pointerId] = Float.NaN;
        mPrevY[pointerId] = Float.NaN;
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

    /**
     * 画像ファイルがギャラリーに表示されるようにするため、データベースに追加します。
     * 
     * @param imageFile イメージファイル。
     */
    private void updateMediaDatabase(File imageFile) {
        final ContentValues values = new ContentValues();
        ContentResolver contentResolver = getContext().getContentResolver();
        values.put(Images.Media.MIME_TYPE, "image/jpeg");
        values.put(Images.Media.TITLE, imageFile.getName());
        values.put("_data", imageFile.getAbsolutePath());
        contentResolver.insert(Media.EXTERNAL_CONTENT_URI, values);
    }

}
