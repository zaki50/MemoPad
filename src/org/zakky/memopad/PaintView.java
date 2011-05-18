
package org.zakky.memopad;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.MaskFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class PaintView extends View {

    /*
     * for stroke
     */
    private static final float TOUCH_TOLERANCE = 4;
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

    public PaintView(Context c, AttributeSet attrs) {
        super(c, attrs);

        mPaintForPen = new Paint();
        mPaintForPen.setColor(Color.BLACK);
        mPaintForPen.setAntiAlias(true);
        mPaintForPen.setDither(true);
        mPaintForPen.setColor(Color.BLACK);
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

}
