package com.traffic.locationremind.baidu.location.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.view.WindowManager;
import android.webkit.*;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.traffic.location.remind.R;
import com.traffic.locationremind.baidu.location.activity.MainActivity;
import com.traffic.locationremind.baidu.location.activity.WebMainActivity;
import com.traffic.locationremind.baidu.location.dialog.AutoManagerHintDialog;
import com.traffic.locationremind.baidu.location.dialog.SearchLoadingDialog;
import com.traffic.locationremind.baidu.location.utils.UWhiteListSettingUtils;
import com.traffic.locationremind.baidu.location.utils.Utils;
import com.traffic.locationremind.baidu.location.view.FullMapView;
import com.traffic.locationremind.common.util.*;
import com.traffic.locationremind.manager.bean.CityInfo;
import com.traffic.locationremind.manager.database.DataManager;
import com.traffic.locationremind.share.SharePlatformActivity;
import com.umeng.analytics.MobclickAgent;
import com.umeng.message.inapp.InAppMessageManager;

import java.util.List;

public class FullMapFragment extends Fragment {
    private static final String TAG = "FullMapFragment";
    private View rootView;
    //private FullMapView mFullMapView;
    private int screenWidth ,screenHeight;
    private DataManager mDataManager;
    private TextView text;
    private WebView webView;
    private ImageView share;
    private ImageView autoManger;
    private String currentCity;
//https://stavinli.github.io/the-subway-of-china/dest/index.html?cityCode=131
    private final String URL = "https://stavinli.github.io/the-subway-of-china/dest/index.html?cityCode=";
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG,"onCreateView");
        if (null != rootView) {
            ViewGroup parent = (ViewGroup) rootView.getParent();
            if (null != parent) {
                parent.removeView(rootView);
            }
        } else {
            rootView = inflater.inflate(R.layout.full_map_layout,container,false);
            WindowManager wm = (WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE);
            DisplayMetrics dm = new DisplayMetrics();
            wm.getDefaultDisplay().getMetrics(dm);
            screenWidth = dm.widthPixels;         // 屏幕宽度（像素）
            screenHeight = dm.heightPixels-(int)getResources().getDimension(R.dimen.full_map_top_bottom_height);       // 屏幕高度（像素）
            initView(rootView);// 控件初始化
        }
        return rootView;
    }

    private void initView(View rootView){
        //mFullMapView = (FullMapView)rootView.findViewById(R.id.map);
        mDataManager = ((MainActivity) getActivity()).getDataManager();
        webView = (WebView) rootView.findViewById(R.id.web_wv);
        text = (TextView) rootView.findViewById(R.id.text);
        share = (ImageView) rootView.findViewById(R.id.share_btn);
        autoManger = (ImageView) rootView.findViewById(R.id.auto_manger);
        share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(FullMapFragment.this.getActivity(),SharePlatformActivity.class);
                startActivity(intent);
            }
        });

        autoManger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity)FullMapFragment.this.getActivity()).showAutoManagerDialog();
                //UWhiteListSettingUtils.enterWhiteListSetting(FullMapFragment.this.getActivity());
            }
        });
        initData();
    }

    public class JsInterface {
        @JavascriptInterface
        public void log(final String msg) {
            webView.post(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "JsInterface");
                    //webView.loadUrl("javascript:callJS(" + "'" + msg + "'" + ")");
                }
            });
        }
    }

    public void initData(){
        String shpno = CommonFuction.getSharedPreferencesValue(getContext(), CityInfo.CITYNAME);
        List<CityInfo> cityList = mDataManager.getDataHelper().QueryCityByCityNo(shpno);
        if(cityList != null && cityList.size() > 0){
            int id = FileUtil.getResIconId(getContext(),cityList.get(0).getPingying());
            //Bitmap bitmap2 = BitmapFactory.decodeResource(getResources(), id);
            //mFullMapView.setBitmap(bitmap2,screenWidth,screenHeight);
            WebSettings webSettings = webView.getSettings();
            // 将JavaScript设置为可用，这一句话是必须的，不然所做一切都是徒劳的
            webSettings.setJavaScriptEnabled(true);
            // 给webview添加JavaScript接口
            //String data = StreamUtils.get(getContext(),R.raw.json);

            webView.addJavascriptInterface(new JsInterface(), "show");
            String cityName = cityList.get(0).getPingying()+".json";
            Log.d(TAG,"initData "+" cityName = "+cityName);
            String url = "file:///android_asset/src/index.html?cityCode="+cityName;
            webView.loadUrl(url);
            webView.setWebChromeClient(new MyWebChromeClient());
            if(NetWorkUtils.isGPSEnabled(getActivity()) && NetWorkUtils.isNetworkConnected(getActivity())){
                text.setVisibility(View.GONE);
            }
        }
    }

    public void updateCity(){
        String shpno = "";
        if(getContext() != null) {
            shpno = CommonFuction.getSharedPreferencesValue(getContext(), CityInfo.CITYNAME);
        }
        if (TextUtils.isEmpty(shpno)) {
            shpno = IDef.DEFAULTCITY;
        }
        /*if(!TextUtils.isEmpty(currentCity) && currentCity.equals(shpno)){
            return;
        }*/
        currentCity= shpno;
        if(mDataManager != null && mDataManager.getDataHelper() != null) {
            List<CityInfo> cityList = mDataManager.getDataHelper().QueryCityByCityNo(shpno);
            if (cityList != null && cityList.size() > 0) {
                String cityName = cityList.get(0).getPingying() + ".json";
                Log.d(TAG, "updateCity " + " cityName = " + cityName);
                String url = "file:///android_asset/src/index.html?cityCode=" + cityName;
                webView.loadUrl(url);
            }
        }
    }

    final class MyWebChromeClient extends WebChromeClient {
        @Override
        public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
            Log.d(TAG, message);
            result.confirm();
            return true;
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onResume() {
        super.onResume();
        webView.getSettings().setJavaScriptEnabled(true);
        webView.onResume();
        MobclickAgent.onPageStart("FullMapFragment"); //统计页面("MainScreen"为页面名称，可自定义)

        //弹屏幕字体广告
       InAppMessageManager.getInstance(getContext()).setPlainTextSize( (int)getContext().getResources().getDimension(R.dimen.notif_station_size),
                (int) getContext().getResources().getDimension(R.dimen.notif_station_size),
                (int)getContext().getResources().getDimension(R.dimen.notif_station_size));
    }

    @Override
    public void onPause() {
        super.onPause();
        webView.onPause();
        MobclickAgent.onPageEnd("FullMapFragment");
    }

    @Override
    public void onStop() {
        super.onStop();
        //挂在后台  资源释放
        webView.getSettings().setJavaScriptEnabled(false);
    }

    @Override
    public void onDestroy() {
        webView.setVisibility(View.GONE);
        webView.destroy();
        super.onDestroy();
    }
}
