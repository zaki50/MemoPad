
package org.zakky.memopad;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pad);

        mCanvas = (ImageView) findViewById(R.id.canvas);
        mCanvas.setOnTouchListener(new OnTouchListener() {
            private float mPrevX;
            private float mPrevY;

            private int mCount = 0;

            {
                clear();
            }

            public boolean onTouch(View v, MotionEvent event) {
                float currentX = event.getX();
                float currentY = event.getY();

                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    mPrevX = currentX;
                    mPrevY = currentY;
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    clear();
                    return true;
                } else if (event.getAction() != MotionEvent.ACTION_MOVE) {
                    return false;
                }

                // 念のため前回の位置がない場合は DOWN として扱う
                if (Float.isNaN(mPrevX) || Float.isNaN(mPrevY)) {
                    mPrevX = currentX;
                    mPrevY = currentY;
                    return true;
                }

                final Canvas c;
                if (mCanvasBitmap == null) {
                    mCanvasBitmap = Bitmap.createBitmap(mCanvas.getWidth(), mCanvas.getHeight(),
                            Config.ARGB_4444);
                    c = new Canvas(mCanvasBitmap);
                    final Paint paint;
                    paint = new Paint();
                    paint.setColor(Color.WHITE);
                    c.drawRGB(0xff, 0xff, 0xff);
                } else {
                    c = new Canvas(mCanvasBitmap);
                }

                final Paint paint = new Paint();
                paint.setColor(Color.BLACK);
                paint.setStrokeWidth(4.0f);
                paint.setAntiAlias(true);
                for (int i = 0; i < event.getHistorySize(); i++) {
                    c.drawLine(mPrevX, mPrevY, event.getHistoricalX(i), event.getHistoricalY(i),
                            paint);
                    mPrevX = event.getHistoricalX(i);
                    mPrevY = event.getHistoricalY(i);
                }

                c.drawLine(mPrevX, mPrevY, currentX, currentY, paint);
                mPrevX = currentX;
                mPrevY = currentY;
                mCanvas.setBackgroundDrawable(new BitmapDrawable(mCanvasBitmap));
                mCount++;
                Log.i("Pad", mCount + ": " + event.getHistorySize());
                return true; // consumed!
            }

            private void clear() {
                mPrevX = Float.NaN;
                mPrevY = Float.NaN;
            }
        });
    }
}
