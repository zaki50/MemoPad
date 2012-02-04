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

import org.zakky.memopad.BgConfigActionProvider.OnBgConfigChangedListener;
import org.zakky.memopad.PenConfigActionProvider.OnPenConfigChangedListener;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
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
    // private MenuItem mPenColorMenuItem;

    /**
     * 背景色変更用のメニューアイテム
     */
    private MenuItem mBgColorMenuItem;

    /**
     * 背景色のメニューラベルのベース部分。後ろに現在の背景色を表す文字列を連結して使用します。
     */
    private CharSequence mBgColorMenuLabelBase;

    /**
     * 選択可能な色の値。 {@code mPenColorLabels} と、インデックスで対応づけされる。
     */
    private int[] mPenColorValues;

    /**
     * 選択可能なペンサイズの値。
     */
    private float mCurrentPenSize;

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
        mPenColorValues = resources.getIntArray(R.array.pen_color_value_list);

        /*
         * 背景色一覧
         */
        mBgColorLabels = resources.getStringArray(R.array.bg_color_label_list);
        mBgColorValues = resources.getIntArray(R.array.bg_color_value_list);
    }

    @Override
    protected void onStart() {
        super.onStart();
        refresh();
    }

    @Override
    protected void onStop() {
        super.onStop();

        // save current canvas

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.pad, menu);

        /*
         * ペンカラー変更メニュー項目
         */
        MenuItem penColorMenuItem = menu.findItem(R.id.menu_pen_color);
        PenConfigActionProvider actionProvider = (PenConfigActionProvider) penColorMenuItem
                .getActionProvider();
        actionProvider.setOnColorChangedListener(new OnPenConfigChangedListener() {
            @Override
            public void onColorChanged(int index) {
                getCurrentCanvas().setPenColorIndex(index);
            }

            @Override
            public void onWidthChanged(float width) {
                mCurrentPenSize = width;
                getCurrentCanvas().setPenSize(width);
            }
        });
        // とりあえず2番目にしておく
        getCurrentCanvas().setNextPenColor(mPenColorValues.length);

        mCurrentPenSize = 20f;
        getCurrentCanvas().setPenSize(mCurrentPenSize);

        //        mPenColorMenuLabelBase = mPenColorMenuItem.getTitle();
        //        getCurrentCanvas().applyPenColor();

        /*
         * 背景色変更メニュー項目
         */
        //        mBgColorMenuItem = menu.findItem(R.id.menu_bg_color);
        //        mBgColorMenuLabelBase = mBgColorMenuItem.getTitle();
        //        getCurrentCanvas().applyBgColor();
        final MenuItem bgColorMenuItem = menu.findItem(R.id.menu_bg_color);
        final BgConfigActionProvider bgActionProvider = (BgConfigActionProvider) bgColorMenuItem
                .getActionProvider();
        bgActionProvider.setOnColorChangedListener(new OnBgConfigChangedListener() {
            @Override
            public void onColorChanged(int index) {
                getCurrentCanvas().setBgColorIndex(index);
            }
        });
        getCurrentCanvas().applyBgColor();

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_swap: /* SWAPメニュー */
                swapCanvas();
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

    private void refresh() {
        final CanvasFragment currentCanvas = getCurrentCanvas();
        currentCanvas.applyPenColor();
        currentCanvas.setPenSize(mCurrentPenSize);
        currentCanvas.applyBgColor();
        currentCanvas.invalidate();
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

        refresh();
    }

    /**
     * {@code penColorIndex} が示すペンカラーを反映させます。
     * @param penColorIndex
     */
    public int penColorChanged(int penColorIndex) {
        //        if (mPenColorMenuItem != null) {
        //            mPenColorMenuItem.setTitle(mPenColorMenuLabelBase + mPenColorLabels[penColorIndex]);
        //        }
        final int argb = mPenColorValues[penColorIndex];
        return argb;
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
}
