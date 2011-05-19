
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
     * ペンカラーのメニューラベルのベース部分
     */
    private CharSequence mPenColorMenuLabelBase;
    /**
     * 背景色変更用のメニューアイテム
     */
    private MenuItem mBgColorMenuItem;
    /**
     * 背景色のメニューラベルのベース部分
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
        mPenColorLabels = resources.getStringArray(R.array.pen_color_label_list);
        mPenColorValues = resources.getIntArray(R.array.pen_color_value_list);
        mBgColorLabels = resources.getStringArray(R.array.bg_color_label_list);
        mBgColorValues = resources.getIntArray(R.array.bg_color_value_list);

        // １つ目の色をデフォルトの背景色として選択
        mPaintView = (PaintView) findViewById(R.id.canvas);
        mPenColorIndex = 0;
        mBgColorIndex = 0;
    }

    private void setNextPenColor() {
        mPenColorIndex++;
        mPenColorIndex %= mPenColorValues.length;
        setPenColor();
    }

    private void setNextBgColor() {
        mBgColorIndex++;
        mBgColorIndex %= mBgColorValues.length;
        setBgColor();
    }

    private void setPenColor() {
        mPaintView.setPenColor(mPenColorValues[mPenColorIndex]);
        mPenColorMenuItem.setTitle(mPenColorMenuLabelBase + mPenColorLabels[mPenColorIndex]);
    }

    private void setBgColor() {
        mPaintView.setBackgroundColor(mBgColorValues[mBgColorIndex]);
        mBgColorMenuItem.setTitle(mBgColorMenuLabelBase + mBgColorLabels[mBgColorIndex]);
    }

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.pad, menu);
        mPenColorMenuItem = menu.findItem(R.id.menu_pen_color);
        mPenColorMenuLabelBase = mPenColorMenuItem.getTitle();
        setPenColor();
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

}
