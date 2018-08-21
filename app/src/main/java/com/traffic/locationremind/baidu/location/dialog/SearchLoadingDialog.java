package com.traffic.locationremind.baidu.location.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import com.traffic.location.remind.R;
import com.traffic.locationremind.common.util.CommonFuction;
import com.traffic.locationremind.manager.database.DataManager;


public class SearchLoadingDialog extends Dialog{
    public SearchLoadingDialog(Context context, int theme){
        super(context, theme);

    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCanceledOnTouchOutside(false);
    }

    @Override
    public void dismiss() {
        super.dismiss();
    }
}