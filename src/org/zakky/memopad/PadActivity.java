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
     * 選択可能な色のラベル配列
     */
    private String[] mBgColorLabels;
    /**
     * 選択可能な色の値。 {@code mBgColorLabels} と、インデックスで対応づけされる。
     */
    private int[] mBgColorValues;

    /**
     * 現在のペンカラーのインデックス。{@code mBgColorLabels} と {@code mBgColorValues}用。
     */
    private int mPenColorIndex;
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
         * 背景色一覧
         */
        mBgColorLabels = resources.getStringArray(R.array.bg_color_label_list);
        mBgColorValues = resources.getIntArray(R.array.bg_color_value_list);

        mPaintView = (PaintView) findViewById(R.id.canvas);
        /*
         * １つ目の色をデフォルトの色として選択。
         * メニューラベル更新の都合があるので、反映は #onStart() で行います。
         */
        mPenColorIndex = 0;
        mBgColorIndex = 0;
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

    /**
     * ペンの色を、次の色に変更します。
     */
    private void setNextPenColor() {
        mPenColorIndex++;
        mPenColorIndex %= mPenColorValues.length;
        setPenColor();
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
        mPenColorMenuItem.setTitle(mPenColorMenuLabelBase + mPenColorLabels[mPenColorIndex]);
    }

    /**
     * {@link #mBgColorIndex} が示す背景色を反映させます。
     */
    private void setBgColor() {
        mPaintView.setBackgroundColor(mBgColorValues[mBgColorIndex]);
        mBgColorMenuItem.setTitle(mBgColorMenuLabelBase + mBgColorLabels[mBgColorIndex]);
    }

    /**
     * 現在の画像をファイルに保存し、 {@link Intent#ACTION_SEND ACTION_SEND} なインテントを
     * 飛ばします。
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

}
