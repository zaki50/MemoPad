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

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

/**
 * お絵かき用のアクティビティです。
 */
public class PadActivity extends Activity {

    /**
     * お絵かき用の {@link View} です。
     */
    private PaintView mPaintView;

    /*
     * 動的に ActionBar 上のメニューを更新するためのメニューアイテム
     */

    /**
     * ペンカラー変更用のメニューアイテム
     */
    private MenuItem mPenColorMenuItem;
    /**
     * ペンサイズ変更用のメニューアイテム
     */
    private MenuItem mPenSizeMenuItem;
    /**
     * ペンカラーのメニューラベルのベース部分。後ろに現在のペンカラーを表す文字列を連結して使用します。
     */
    private CharSequence mPenColorMenuLabelBase;
    /**
     * 背景色変更用のメニューアイテム
     */
    private MenuItem mBgColorMenuItem;
    /**
     * 背景色のメニューラベルのベース部分。後ろに現在の背景色を表す文字列を連結して使用します。
     */
    private CharSequence mBgColorMenuLabelBase;

    /**
     * 選択可能な色のラベル配列
     */
    private String[] mPenColorLabels;
    /**
     * 選択可能な色の値。 {@code mPenColorLabels} と、インデックスで対応づけされる。
     */
    private int[] mPenColorValues;
    /**
     * 選択可能なペンサイズを表現するアイコン。
     */
    private Drawable[] mPenSizeImages;
    /**
     * 選択可能なペンサイズの値。
     */
    private float[] mPenSizeValues;
    /**
     * 選択可能な色のラベル配列
     */
    private String[] mBgColorLabels;
    /**
     * 選択可能な色の値。 {@code mBgColorLabels} と、インデックスで対応づけされる。
     */
    private int[] mBgColorValues;

    /**
     * 現在のペンカラーのインデックス。{@code mPecColorLabels} と {@code mPenColorValues}用。
     */
    private int mPenColorIndex;
    /**
     * 現在のペンサイズのインデックス。{@code mPenSizeImages} と {@code mPenSizeValues}用。
     */
    private int mPenSizeIndex;
    /**
     * 現在の背景色のインデックス。{@code mBgColorLabels} と {@code mBgColorValues}用。
     */
    private int mBgColorIndex;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        final Resources resources = getResources();
        /*
         * ペンの色一覧
         */
        mPenColorLabels = resources.getStringArray(R.array.pen_color_label_list);
        mPenColorValues = resources.getIntArray(R.array.pen_color_value_list);
        /*
         * ペンの太さ一覧
         */
        mPenSizeValues = toFloatArray(resources.getIntArray(R.array.pen_size_value_list));
        mPenSizeImages = buildPenSizeDrawables(mPenSizeValues);

        /*
         * 背景色一覧
         */
        mBgColorLabels = resources.getStringArray(R.array.bg_color_label_list);
        mBgColorValues = resources.getIntArray(R.array.bg_color_value_list);

        mPaintView = (PaintView) findViewById(R.id.canvas);
        /*
         * １つ目の色をデフォルトの色として選択。 メニューラベル更新の都合があるので、反映は #onStart() で行います。
         */
        mPenColorIndex = 0;
        mPenSizeIndex = 0;
        mBgColorIndex = 0;
    }

    @Override
    protected void onStart() {
        super.onStart();
        setPenColor();
        setBgColor();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.pad, menu);

        /*
         * ペンカラー変更メニュー項目
         */
        mPenColorMenuItem = menu.findItem(R.id.menu_pen_color);
        mPenColorMenuLabelBase = mPenColorMenuItem.getTitle();
        setPenColor();

        /*
         * ペンサイズ変更メニュー項目
         */
        mPenSizeMenuItem = menu.findItem(R.id.menu_pen_size);
        setPenSize();

        /*
         * 背景色変更メニュー項目
         */
        mBgColorMenuItem = menu.findItem(R.id.menu_bg_color);
        mBgColorMenuLabelBase = mBgColorMenuItem.getTitle();
        setBgColor();

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_pen_color: /* ペン色メニュー */
                setNextPenColor();
                return true;
            case R.id.menu_pen_size: /* ペンサイズメニュー */
                setNextPenSize();
                return true;
            case R.id.menu_bg_color: /* 背景色メニュー */
                setNextBgColor();
                return true;
            case R.id.menu_share: /* 共有メニュー */
                shareImage();
                return true;
            case R.id.menu_clear: /* 消去メニュー */
                mPaintView.clearCanvas();
                return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    private Drawable[] buildPenSizeDrawables(float[] sizeArray) {
        final BitmapDrawable[] result = new BitmapDrawable[sizeArray.length];
        final int width = 30;
        final int height = 30;
        final Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setStyle(Paint.Style.FILL);
//        paint.setStrokeJoin(Paint.Join.ROUND);
//        paint.setStrokeCap(Paint.Cap.ROUND);
//        paint.setStrokeWidth(12.0F);
        for (int i = 0; i < sizeArray.length; i++) {
            final float size = sizeArray[i];
            final Bitmap bitmap = Bitmap.createBitmap(width, height, Config.ARGB_4444);
            final Canvas c = new Canvas(bitmap);
            c.drawCircle(width / 2, height / 2, size == 0.0 ? 1.0f : size / 2, paint);
            final BitmapDrawable drawable = new BitmapDrawable(bitmap);
            result[i] = drawable;
        }
        return result;
    }

    /**
     * ペンの色を、次の色に変更します。
     */
    private void setNextPenColor() {
        mPenColorIndex++;
        mPenColorIndex %= mPenColorValues.length;
        setPenColor();
    }

    /**
     * ペンのサイズを、次のサイズに変更します。
     */
    private void setNextPenSize() {
        mPenSizeIndex++;
        mPenSizeIndex %= mPenSizeValues.length;
        setPenSize();
    }

    /**
     * 背景の色を次の色に変更します。
     */
    private void setNextBgColor() {
        mBgColorIndex++;
        mBgColorIndex %= mBgColorValues.length;
        setBgColor();
    }

    /**
     * {@link #mPenColorIndex} が示すペンカラーを反映させます。
     */
    private void setPenColor() {
        mPaintView.setPenColor(mPenColorValues[mPenColorIndex]);
        if (mPenColorMenuItem != null) {
            mPenColorMenuItem.setTitle(mPenColorMenuLabelBase + mPenColorLabels[mPenColorIndex]);
        }
    }

    /**
     * {@link #mPenSizeIndex} が示すペンサイズを反映させます。
     */
    private void setPenSize() {
        mPaintView.setPenSize(mPenSizeValues[mPenSizeIndex]);
        if (mPenSizeMenuItem != null) {
            mPenSizeMenuItem.setIcon(mPenSizeImages[mPenSizeIndex]);
        }
    }

    /**
     * {@link #mBgColorIndex} が示す背景色を反映させます。
     */
    private void setBgColor() {
        mPaintView.setBackgroundColor(mBgColorValues[mBgColorIndex]);
        if (mBgColorMenuItem != null) {
            mBgColorMenuItem.setTitle(mBgColorMenuLabelBase + mBgColorLabels[mBgColorIndex]);
        }
    }

    /**
     * 現在の画像をファイルに保存し、 {@link Intent#ACTION_SEND ACTION_SEND} なインテントを 飛ばします。
     */
    private void shareImage() {
        final Uri imageFile = mPaintView.saveImageAsPng();
        if (imageFile == null) {
            Toast.makeText(this, R.string.failed_to_save_image, Toast.LENGTH_LONG).show();
            return;
        }
        final Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("image/png");
        intent.putExtra(Intent.EXTRA_STREAM, imageFile);
        startActivity(intent);
    }

    private static float[] toFloatArray(int[] intArray) {
        if (intArray == null) {
            return null;
        }
        final float[] result = new float[intArray.length];
        for (int i = 0; i < intArray.length; i++) {
            final int value = intArray[i];
            result[i] = (float) value;
        }
        return result;
    }
}
