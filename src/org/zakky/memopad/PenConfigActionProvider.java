
package org.zakky.memopad;

import android.content.Context;
import android.view.ActionProvider;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.PopupWindow;

public class PenConfigActionProvider extends ActionProvider {
    @SuppressWarnings("unused")
    private final PenConfigActionProvider self = this;

    public interface OnPenConfigChangedListener {
        public void onColorChanged(int index);

        public void onWidthChanged(float width);
    }

    private final Context mContext;

    private final LayoutInflater mLayoutInflater;

    private PopupWindow mPopupWindow;

    private OnPenConfigChangedListener mListener;

    public PenConfigActionProvider(Context context) {
        super(context);
        mContext = context;

        mLayoutInflater = LayoutInflater.from(context);

        View v = mLayoutInflater.inflate(R.layout.color_popup, null, false);

        //        mColorPicker = (HsvColorPickerView) v.findViewById(R.id.color_picker);
        //        mColorPicker.showPreview(true);
        //        mColorPicker.setIniticalColor(mInitialColor);
        //        mColorPicker.setOnColorChangedListener(new HsvColorPickerView.OnColorChangedListener() {
        //
        //            @Override
        //            public void onColorChanged(int color, float[] hsv) {
        //                if (mListener != null) {
        //                    mListener.onColorChanged(color);
        //                }
        //            }
        //        });

        v.findViewById(R.id.white).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPopupWindow.dismiss();
                if (mListener != null) {
                    mListener.onColorChanged(0);
                }
            }
        });

        v.findViewById(R.id.black).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPopupWindow.dismiss();
                if (mListener != null) {
                    mListener.onColorChanged(1);
                }
            }
        });

        v.findViewById(R.id.red).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPopupWindow.dismiss();
                if (mListener != null) {
                    mListener.onColorChanged(2);
                }
            }
        });

        v.findViewById(R.id.orange).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPopupWindow.dismiss();
                if (mListener != null) {
                    mListener.onColorChanged(3);
                }
            }
        });

        v.findViewById(R.id.yellow).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPopupWindow.dismiss();
                if (mListener != null) {
                    mListener.onColorChanged(4);
                }
            }
        });

        v.findViewById(R.id.green).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPopupWindow.dismiss();
                if (mListener != null) {
                    mListener.onColorChanged(5);
                }
            }
        });

        v.findViewById(R.id.blue).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPopupWindow.dismiss();
                if (mListener != null) {
                    mListener.onColorChanged(6);
                }
            }
        });

        v.findViewById(R.id.purple).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPopupWindow.dismiss();
                if (mListener != null) {
                    mListener.onColorChanged(7);
                }
            }
        });

        mPopupWindow = new PopupWindow(v);
        mPopupWindow
                .setBackgroundDrawable(mContext.getResources().getDrawable(R.drawable.panel_bg));
        mPopupWindow.setWidth(WindowManager.LayoutParams.MATCH_PARENT);
        mPopupWindow.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public View onCreateActionView() {
        LayoutInflater layoutInflater = LayoutInflater.from(mContext);

        final View actionItem = layoutInflater.inflate(R.layout.pen_config_action_provider, null);

        ImageButton button = (ImageButton) actionItem.findViewById(R.id.pen_config_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!mPopupWindow.isShowing()) {
                    //                            mColorPicker.setIniticalColor(mInitialColor);
                    mPopupWindow.showAsDropDown(actionItem);
                } else {
                    mPopupWindow.dismiss();
                }
            }
        });

        return actionItem;
    }

    public void setOnColorChangedListener(OnPenConfigChangedListener l) {
        mListener = l;
    }

}
