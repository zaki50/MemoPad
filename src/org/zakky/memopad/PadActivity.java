
package org.zakky.memopad;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class PadActivity extends Activity {

    private PaintView mPaintView;
    private String[] mColorLabels;
    private int[] mColorValues;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        final Resources resources = getResources();
        mColorLabels = resources.getStringArray(R.array.color_label_list);
        mColorValues = resources.getIntArray(R.array.color_value_list);

        // １つ目の色をデフォルトの背景色として選択
        mPaintView = (PaintView) findViewById(R.id.canvas);
        mPaintView.setBackgroundColor(mColorValues[0]);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.pad, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
            /* 消去メニュー */
            case R.id.menu_clear:
                mPaintView.clearCanvas();
                return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }


}
