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

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
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
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

/**
 * お絵かき用のアクティビティです。
 */
public class PadActivity extends FragmentActivity implements CanvasListener {

    private static final String FG_TAG_CANVAS = "canvas";

    private CanvasFragment[] mCanvases;

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mCanvases = new CanvasFragment[2];
        for (int i = 0; i < mCanvases.length; i++) {
            mCanvases[i] = new CanvasFragment();
        }

        fixOrientation();

        setContentView(R.layout.placeholder);

        final FragmentManager fm = getSupportFragmentManager();
        final FragmentTransaction tx = fm.beginTransaction();
        try {
            final Fragment old = fm.findFragmentByTag(FG_TAG_CANVAS);
            if (old != null) {
                tx.remove(old);
            }
            tx.add(R.id.container, getCurrentCanvas(), FG_TAG_CANVAS);
        } finally {
            tx.commit();
        }

        if (MyDialogFragment.showAtStartup(this)) {
            new MyDialogFragment().show(fm.beginTransaction(), "dialog");
        }

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
    }

    @Override
    protected void onStart() {
        super.onStart();
        getCurrentCanvas().applyPenColor();
        getCurrentCanvas().applyBgColor();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.pad, menu);

        /*
         * ペンカラー変更メニュー項目
         */
        mPenColorMenuItem = menu.findItem(R.id.menu_pen_color);
        mPenColorMenuLabelBase = mPenColorMenuItem.getTitle();
        getCurrentCanvas().applyPenColor();

        /*
         * ペンサイズ変更メニュー項目
         */
        mPenSizeMenuItem = menu.findItem(R.id.menu_pen_size);
        getCurrentCanvas().applyPenSize();

        /*
         * 背景色変更メニュー項目
         */
        mBgColorMenuItem = menu.findItem(R.id.menu_bg_color);
        mBgColorMenuLabelBase = mBgColorMenuItem.getTitle();
        getCurrentCanvas().applyBgColor();

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_swap: /* SWAPメニュー */
                swapCanvas();
                return true;
            case R.id.menu_pen_size: /* ペンサイズメニュー */
                getCurrentCanvas().setNextPenSize(mPenSizeValues.length);
                return true;
            case R.id.menu_pen_color: /* ペン色メニュー */
                getCurrentCanvas().setNextPenColor(mPenColorValues.length);
                return true;
            case R.id.menu_bg_color: /* 背景色メニュー */
                getCurrentCanvas().setNextBgColor(mBgColorValues.length);
                return true;
            case R.id.menu_share: /* 共有メニュー */
                shareImage();
                return true;
            case R.id.menu_clear: /* 消去メニュー */
                clearCanvas();
                return true;
            default:
                return super.onMenuItemSelected(featureId, item);
        }
    }

    private CanvasFragment getCurrentCanvas() {
        return mCanvases[0];
    }

    private void swapCanvas() {
        final FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        try {
            tx.replace(R.id.container, mCanvases[1], FG_TAG_CANVAS);
            tx.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);

            final CanvasFragment prevCurrent = mCanvases[0];
            mCanvases[0] = mCanvases[1];
            mCanvases[1] = prevCurrent;
        } finally {
            tx.commit();
        }
        getSupportFragmentManager().executePendingTransactions();

        final CanvasFragment currentCanvas = getCurrentCanvas();
        currentCanvas.applyPenColor();
        currentCanvas.applyPenSize();
        currentCanvas.applyBgColor();
        currentCanvas.invalidate();
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
     * {@code penColorIndex} が示すペンカラーを反映させます。
     * @param penColorIndex
     */
    public int penColorChanged(int penColorIndex) {
        if (mPenColorMenuItem != null) {
            mPenColorMenuItem.setTitle(mPenColorMenuLabelBase + mPenColorLabels[penColorIndex]);
        }
        final int argb = mPenColorValues[penColorIndex];
        return argb;
    }

    /**
     * {@code penSizeIndex} が示すペンサイズを反映させます。
     * @param penSizeIndex
     */
    public float penSizeChanged(int penSizeIndex) {
        if (mPenSizeMenuItem != null) {
            mPenSizeMenuItem.setIcon(mPenSizeImages[penSizeIndex]);
        }
        final float penSize = mPenSizeValues[penSizeIndex];
        return penSize;
    }

    /**
     * {@code bgColorIndex} が示す背景色を反映させます。
     * @param bgColorIndex
     */
    public int bgColorChanged(int bgColorIndex) {
        if (mBgColorMenuItem != null) {
            mBgColorMenuItem.setTitle(mBgColorMenuLabelBase + mBgColorLabels[bgColorIndex]);
        }
        final int bgArgb = mBgColorValues[bgColorIndex];
        return bgArgb;
    }

    /**
     * 現在の画像をファイルに保存し、 {@link Intent#ACTION_SEND ACTION_SEND} なインテントを 飛ばします。
     */
    private void shareImage() {
        final Uri imageFile = getCurrentCanvas().saveImageAsPng();
        if (imageFile == null) {
            Toast.makeText(this, R.string.failed_to_save_image, Toast.LENGTH_LONG).show();
            return;
        }
        final Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("image/png");
        intent.putExtra(Intent.EXTRA_STREAM, imageFile);
        startActivity(intent);
    }

    private void clearCanvas() {
        getCurrentCanvas().clearCanvas();
    }

    private void fixOrientation() {
        final Configuration config = getResources().getConfiguration();
        if (config.orientation == Configuration.ORIENTATION_PORTRAIT) {
            // TODO SCREEN_ORIENTATION_REVERSE_PORTRAIT の判定
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            // TODO SCREEN_ORIENTATION_REVERSE_LADSCAPE の判定
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
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
