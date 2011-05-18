
package org.zakky.memopad;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;

public class PadActivity extends Activity {

    private ImageView mCanvas;
    private Bitmap mCanvasBitmap;
    private final Paint mPaintForPen;

    public PadActivity() {
        mPaintForPen = new Paint();
        mPaintForPen.setColor(Color.BLACK);
        mPaintForPen.setAntiAlias(true);
        mPaintForPen.setDither(true);
        mPaintForPen.setColor(Color.BLACK);
        mPaintForPen.setStyle(Paint.Style.STROKE);
        mPaintForPen.setStrokeJoin(Paint.Join.ROUND);
        mPaintForPen.setStrokeCap(Paint.Cap.ROUND);
        mPaintForPen.setStrokeWidth(12.0F);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pad);

        mCanvas = (ImageView) findViewById(R.id.canvas);
        mCanvas.setOnTouchListener(new OnTouchListener() {
            private final Path mPath = new Path();
            private float mPrevX = Float.NaN;
            private float mPrevY = Float.NaN;
            private static final float TOUCH_TOLERANCE = 4;

            private int mCount = 0;

            {
                clear();
            }

            public boolean onTouch(View v, MotionEvent event) {
                float currentX = event.getX();
                float currentY = event.getY();

                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    handleTouchStart(currentX, currentY);
                    return true;
                } else if (event.getAction() != MotionEvent.ACTION_MOVE
                        && event.getAction() != MotionEvent.ACTION_UP) {
                    return false;
                }

                // 念のため前回の位置がない場合は DOWN として扱う
                if (Float.isNaN(mPrevX) || Float.isNaN(mPrevY)) {
                    handleTouchStart(currentX, currentY);
                    return true;
                }

                final Canvas c;
                if (mCanvasBitmap == null) {
                    mCanvasBitmap = Bitmap.createBitmap(mCanvas.getWidth(), mCanvas.getHeight(),
                            Config.ARGB_8888);
                    c = new Canvas(mCanvasBitmap);
                    c.drawRGB(0xff, 0xff, 0xff);
                } else {
                    c = new Canvas(mCanvasBitmap);
                }

                for (int i = 0; i < event.getHistorySize(); i++) {
                    handleTouchMove(event.getHistoricalX(i), event.getHistoricalY(i));
                }
                if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    handleTouchMove(currentX, currentY);
                } else {
                    handleTouchEnd(currentX, currentY);
                }

                c.drawPath(mPath, mPaintForPen);
                mCanvas.setBackgroundDrawable(new BitmapDrawable(mCanvasBitmap));
                mCount++;
                Log.i("Pad", mCount + ": " + event.getHistorySize());

                return true; // consumed!
            }

            private void handleTouchStart(float x, float y) {
                mPath.reset();
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
                clear();
            }

            private void clear() {
                mPath.reset();
                mPrevX = Float.NaN;
                mPrevY = Float.NaN;
            }
        });
    }
}
