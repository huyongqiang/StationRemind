package com.traffic.locationremind.baidu.location.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.*;
import com.baidu.location.BDLocation;
import com.traffic.location.remind.R;
import com.traffic.locationremind.baidu.location.activity.MainActivity;
import com.traffic.locationremind.baidu.location.internal.Utils;
import com.traffic.locationremind.baidu.location.listener.ActivityListener;
import com.traffic.locationremind.baidu.location.listener.LocationChangerListener;
import com.traffic.locationremind.baidu.location.listener.RemindSetViewListener;
import com.traffic.locationremind.baidu.location.activity.AlarmActivity;
import com.traffic.locationremind.baidu.location.object.LineObject;
import com.traffic.locationremind.baidu.location.object.NotificationObject;
import com.traffic.locationremind.baidu.location.utils.NotificationUtils;
import com.traffic.locationremind.baidu.location.view.LineNodeView;
import com.traffic.locationremind.baidu.location.view.RemindLineColorView;
import com.traffic.locationremind.common.util.*;
import com.traffic.locationremind.manager.ScrollFavoriteManager;
import com.traffic.locationremind.manager.bean.LineInfo;
import com.traffic.locationremind.manager.bean.StationInfo;
import com.traffic.locationremind.manager.database.DataManager;
import com.umeng.analytics.MobclickAgent;
import com.umeng.message.inapp.IUmengInAppMsgCloseCallback;
import com.umeng.message.inapp.InAppMessageManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RemindFragment extends Fragment implements LocationChangerListener, View.OnClickListener, ActivityListener {

    private String TAG = "RemindFragment";
    public static final String STOP_LOCATION_ACTION = "com.traffic.location.remind.action.stop.remind";
    private LinearLayout linearlayout;
    private HorizontalScrollView horizontalScrollView;
    private List<StationInfo> list = null;
    private Map<String, LineNodeView> textViewList = new HashMap<>();
    private Map<Integer, String> lineDirection = new HashMap<>();
    private int lineNodeWidth = 0;
    private int lineNodeHeight = 0;
    private DataManager mDataManager;

    private View rootView;
    private LineNodeView currentStationView;

    private boolean isDefaultLine = false;

    private TextView hint_text;
    private TextView start_and_end;
    private TextView line_change_introduce;
    private RemindLineColorView line_color_view;
    private TextView current_info_text;
    private TextView collection_btn;
    private TextView favtor_btn;
    private TextView cancle_remind_btn;

    private RelativeLayout remind_root;
    private LineObject lineObject;

    private NotificationUtil mNotificationUtil;
    private StationInfo currentStation, nextStation;
    private RemindSetViewListener mRemindSetViewListener;

    private boolean isRemind = false;
    private boolean isPause = false;
    private MainActivity activity;
    private int pressColor = Color.BLUE;
    private int normalColor = Color.BLACK;

    private ScrollFavoriteManager mScrollFavoriteManager;
    private int drawableSize = 50;

    //起点，当前站，换乘点，终点
    private List<StationInfo> needChangeStationList = new ArrayList<>();
    private List<StationInfo> tempChangeStationList = new ArrayList<>();
    private List<StationInfo>  transferList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        drawableSize = (int)getContext().getResources().getDimension(R.dimen.drwable_size);
        pressColor = getContext().getResources().getColor(R.color.blue);
        normalColor = getContext().getResources().getColor(R.color.gray);
        normalColor = Color.BLACK;
        if (null != rootView) {
            ViewGroup parent = (ViewGroup) rootView.getParent();
            if (null != parent) {
                parent.removeView(rootView);
            }
        } else {
            registerLoginBroadcast();
            rootView = inflater.inflate(R.layout.remind_layout, container, false);
            initView(rootView);// 控件初始化
        }
        Drawable drawable = activity.getResources().getDrawable(R.drawable.favtor);
        if (CommonFuction.isEmptyColloctionFolder(getActivity())) {
            favtor_btn.setTextColor(normalColor);
            drawable = Utils.tint(drawable,normalColor);
            setCompoundDrawables(favtor_btn, drawable);
            initDefaultEmptyLine();
        } else {
            favtor_btn.setTextColor(pressColor);
            drawable = Utils.tint(drawable,pressColor);
            setCompoundDrawables(favtor_btn, drawable);
            List<LineObject> lineObjects = CommonFuction.getAllFavourite(getActivity(), mDataManager);
            Log.d(TAG,"initDefaultEmptyLine lineObjects.size = "+lineObjects.size());
            if (lineObjects.size() > 0) {
                initdata(lineObjects.get(0));
            }else{
                initDefaultEmptyLine();
            }
        }
        return rootView;
    }

    public void setData(LineObject lineObject) {
        this.lineObject = lineObject;
        isRemind = true;
        list = lineObject.stationList;
        transferList = CommonFuction.getTransFerList(lineObject);
        for (StationInfo stationInfo : list) {
            stationInfo.colorId = mDataManager.getLineInfoList().get(stationInfo.lineid).colorid;
        }
        createLine(list);
        if (getMainActivity() != null) {
            getMainActivity().setRemindState(true);
        }
        if(this.lineObject != null) {
            MobclickAgent.onEvent(activity, getResources().getString(R.string.event_startLocationLine), "开始定位");
        }
    }

    public DataManager getDataManager() {
        return mDataManager;
    }

    public void initdata(LineObject lineObject) {
        this.lineObject = lineObject;
        isRemind = false;
        list = lineObject.stationList;
        transferList = CommonFuction.getTransFerList(lineObject);
        if (getMainActivity() != null) {
            getMainActivity().setRemindState(false);
        }
        for (StationInfo stationInfo : list) {
            if(stationInfo != null) {
                stationInfo.colorId = mDataManager.getLineInfoList().get(stationInfo.lineid).colorid;
            }
        }
        createLine(list);
    }

    private MainActivity getMainActivity() {
        return (MainActivity) getActivity();
    }

    public void upadaData() {
        Log.d(TAG,"upadaData");
        if(getActivity() != null) {
            List<LineObject> lineObjects = CommonFuction.getAllFavourite(getActivity(), mDataManager);
            if (lineObjects.size() > 0) {
                initdata(lineObjects.get(0));
            } else {
                initDefaultEmptyLine();
            }
        }
    }

    public void createLine(final List<StationInfo> list) {
        if (list == null || list.size() <= 0) {
            return;
        }
        if(!NetWorkUtils.isGPSEnabled(activity) || !activity.getPersimmions()){
            NetWorkUtils.openGPS(activity);
            isRemind = false;
        }
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mScrollFavoriteManager.closeScrollView();
                needChangeStationList.clear();
                linearlayout.removeAllViews();
                Map<Integer, LineInfo> lineInfoMap = new HashMap<>();
                StationInfo preStationInfo = null;
                StringBuffer str = new StringBuffer();
                if(transferList != null) {
                    for (StationInfo stationInfo : transferList) {
                        str.append(stationInfo.getCname() + " ->");
                    }
                }
                Log.d("","createLine "+str.toString());
                for (int i = 0; i < list.size(); i++) {
                    StationInfo stationInfo = list.get(i);
                    //创建textview
                    LineNodeView textView = new LineNodeView(getActivity());
                    textView.setId(i + 1000);

                    textView.setStation(stationInfo);
                    LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(lineNodeWidth, lineNodeHeight);
                    linearlayout.addView(textView, layoutParams);
                    //添加到集合
                    textViewList.put(stationInfo.getCname(), textView);
                    if (i == 0) {//起点
                        currentStation = list.get(0);
                        int size = (int) getResources().getDimension(R.dimen.current_bitmap_siez);
                        Bitmap bitmap = CommonFuction.getbitmap(BitmapFactory.decodeResource(RemindFragment.this.getResources(), R.drawable.cm_main_map_pin_start), size, size);
                        textView.setStartBitMap(bitmap);
                    } else {
                        nextStation = list.get(1);
                    }
                    if(i == list.size() -1){
                        int size = (int) getResources().getDimension(R.dimen.current_bitmap_siez);
                        Bitmap bitmap = CommonFuction.getbitmap(BitmapFactory.decodeResource(RemindFragment.this.getResources(), R.drawable.cm_main_map_pin_end), size, size);
                        textView.setStartBitMap(bitmap);
                    }

                    if(CommonFuction.containTransfer(transferList,stationInfo)){
                        needChangeStationList.add(stationInfo);
                        if(i != list.size() - 1) {
                            int size = (int) getResources().getDimension(R.dimen.transfer_bitmap_size);
                            Bitmap bitmap = CommonFuction.getbitmap(BitmapFactory.decodeResource(RemindFragment.this.getResources(), R.drawable.cm_route_map_pin_dottransfer), size, size);
                            textView.setTransFerBitmap(bitmap);
                        }
                        if(preStationInfo != null) {
                            if (preStationInfo.pm < stationInfo.pm) {
                                lineDirection.put(preStationInfo.lineid, mDataManager.getLineInfoList().get(preStationInfo.lineid).getReverse());
                            } else {
                                lineDirection.put(preStationInfo.lineid, mDataManager.getLineInfoList().get(preStationInfo.lineid).getForwad());
                            }
                        }
                    }
                    int lineid = list.get(i).lineid;
                    lineInfoMap.put(lineid, mDataManager.getLineInfoList().get(lineid));
                    preStationInfo = stationInfo;
                }

                String start = list.get(0).getCname() + " -> " + list.get(list.size() - 1).getCname();
                start_and_end.setText(start);

                String stationNum = String.format(getResources().getString(R.string.station_number), list.size() + "");
                String changeNumstr = String.format(getResources().getString(R.string.change_number), lineInfoMap.size() + "");
                line_change_introduce.setText(changeNumstr + "  " + stationNum);

                String surplusNum = String.format(getResources().getString(R.string.surples_station), list.size() - 1 + "");
                String currenStr = getResources().getString(R.string.current_station) + list.get(0).getCname();
                String line = CommonFuction.getLineNo(DataManager.getInstance(getActivity()).getLineInfoList().get(list.get(0).lineid).linename)[0];
                //String.format(getResources().getString(R.string.line_tail),list.get(0).lineid+"") ;
                String direction = lineDirection.get(list.get(0).lineid) + getResources().getString(R.string.direction);
                current_info_text.setText(surplusNum + "   " + currenStr + "   " + line + "   " + direction);
                line_color_view.setLineInfoMap(lineInfoMap);
                if (isRemind)
                    cancle_remind_btn.setText(getResources().getString(R.string.cancle_remind));
                cancle_remind_btn.setEnabled(true);
                currentStation = list.get(0);
                nextStation = list.get(0);
                updateColloctionView();
                tempChangeStationList = new ArrayList<>(needChangeStationList);
                if (CommonFuction.isEmptyColloctionFolder(getActivity()) && !isDefaultLine) {
                    saveFavourite();
                }
                isDefaultLine = false;
            }
        });
        if (getMainActivity() != null) {
            getMainActivity().setRemindList(list, needChangeStationList, lineDirection);
        }
    }

    public void updateColloctionView() {
        String lineStr = CommonFuction.convertStationToString(lineObject);
        String allFavoriteLine = CommonFuction.getSharedPreferencesValue(activity, CommonFuction.FAVOURITE);
        Drawable drawable = activity.getResources().getDrawable(R.drawable.hwpush_no_collection);
        if (!TextUtils.isEmpty(lineStr) && allFavoriteLine.contains(lineStr)) {
            collection_btn.setTextColor(pressColor);
            drawable = Utils.tint(drawable,pressColor);
            setCompoundDrawables(collection_btn, drawable);
        } else {
            collection_btn.setTextColor(normalColor);
            drawable = Utils.tint(drawable,normalColor);
            setCompoundDrawables(collection_btn,drawable);
        }

        drawable = activity.getResources().getDrawable(R.drawable.favtor);
        if (CommonFuction.isEmptyColloctionFolder(getActivity())) {
            favtor_btn.setTextColor(normalColor);
            drawable = Utils.tint(drawable,normalColor);
            setCompoundDrawables(favtor_btn, drawable);
        } else {
            favtor_btn.setTextColor(pressColor);
            drawable = Utils.tint(drawable,pressColor);
            setCompoundDrawables(favtor_btn, drawable);
        }

        drawable = activity.getResources().getDrawable(R.drawable.set_remind);
        if (!isRemind) {
            cancle_remind_btn.setTextColor(normalColor);
            drawable = Utils.tint(drawable,normalColor);
            setCompoundDrawables(cancle_remind_btn, drawable);
        } else {
            cancle_remind_btn.setTextColor(pressColor);
            drawable = Utils.tint(drawable,pressColor);
            setCompoundDrawables(cancle_remind_btn, drawable);
        }
    }

    private void initView(View rootView) {
        linearlayout = (LinearLayout) rootView.findViewById(R.id.linearlayout);
        horizontalScrollView = (HorizontalScrollView) rootView.findViewById(R.id.horizontalScrollView);

        remind_root = (RelativeLayout) rootView.findViewById(R.id.remind_root);
        //hint_text = (TextView)rootView.findViewById(R.id.hint_text);
        start_and_end = (TextView) rootView.findViewById(R.id.start_and_end);
        line_change_introduce = (TextView) rootView.findViewById(R.id.line_change_introduce);
        line_color_view = (RemindLineColorView) rootView.findViewById(R.id.line_color_view);
        current_info_text = (TextView) rootView.findViewById(R.id.current_info_text);
        collection_btn = (TextView) rootView.findViewById(R.id.collection_btn);
        favtor_btn = (TextView) rootView.findViewById(R.id.favtor_btn);
        cancle_remind_btn = (TextView) rootView.findViewById(R.id.cancle_remind_btn);

        mDataManager = ((MainActivity) getActivity()).getDataManager();
        lineNodeWidth = (int) getResources().getDimension(R.dimen.line_node_width);
        lineNodeHeight = (int) getResources().getDimension(R.dimen.line_node_height);
        mNotificationUtil = new NotificationUtil(getActivity());
        activity = (MainActivity) getActivity();

        //hint_text.setOnClickListener(this);
        collection_btn.setOnClickListener(this);
        favtor_btn.setOnClickListener(this);
        cancle_remind_btn.setEnabled(false);
        cancle_remind_btn.setOnClickListener(this);

        mScrollFavoriteManager = new ScrollFavoriteManager(this, rootView);
        mScrollFavoriteManager.setRemindSetViewListener(mRemindSetViewListener);
    }

    private void initDefaultEmptyLine(){
        Log.d(TAG,"initDefaultEmptyLine mDataManager = "+mDataManager+" mDataManager.getAllLineCane() = "+mDataManager.getAllLineCane());
        if(mDataManager != null && mDataManager.getAllLineCane() != null && mDataManager.getLineInfoList() != null) {
            LineInfo lineInfo = mDataManager.getLineInfoList().get(mDataManager.getMinLineid());
            Log.d(TAG,"initDefaultEmptyLine Lineid = "+mDataManager.getMinLineid()+" lineInfo = "+lineInfo);

            if(lineInfo != null) {
                list = lineInfo.getStationInfoList();
                LineObject lineObject = new LineObject();
                lineObject.stationList = list;
                List<Integer> lineidList = new ArrayList<>();
                lineidList.add(mDataManager.getMinLineid());
                lineObject.lineidList = lineidList;
                List transferList = new ArrayList<>();
                int endStation = list.size() > 1 ? list.size()-1:0;
                if(list.size() > endStation) {
                    transferList.add(list.get(endStation));
                    lineObject.transferList = transferList;
                    isDefaultLine = true;
                    initdata(lineObject);
                }
            }
        }
    }

    public ScrollFavoriteManager getScrollFavoriteManager() {
        return mScrollFavoriteManager;
    }

    public void setCompoundDrawables(TextView view, Drawable drawable) {
        drawable.setBounds(0,0,drawableSize,drawableSize);
        //drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), (int) (drawable.getMinimumHeight()));
        view.setCompoundDrawables(null, drawable, null, null);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.collection_btn:
                if (list == null || list.size() <= 0) {
                    Toast.makeText(activity, getResources().getString(R.string.please_select_colloction_line), Toast.LENGTH_SHORT).show();
                } else {
                    saveFavourite();
                }
                break;
            case R.id.favtor_btn:
                List<LineObject> lineObjects = CommonFuction.getAllFavourite(getActivity(), mDataManager);
                if (lineObjects.size() <= 0) {
                    Toast.makeText(getActivity(), getResources().getString(R.string.colloction_catalog_empty), Toast.LENGTH_SHORT).show();
                } else {
                    mScrollFavoriteManager.openScrollView(lineObjects);
                }

                break;
            case R.id.cancle_remind_btn:
                Drawable drawable = activity.getResources().getDrawable(R.drawable.set_remind);
                if (isRemind) {
                    isRemind = false;
                    if (getMainActivity() != null) {
                        getMainActivity().setRemindState(false);
                    }
                    if (cancle_remind_btn != null) {
                        drawable = Utils.tint(drawable,normalColor);
                        cancle_remind_btn.setTextColor(normalColor);
                        cancle_remind_btn.setText(getResources().getString(R.string.startlocation));
                        setCompoundDrawables(cancle_remind_btn, drawable);
                    }
                    if(this.lineObject != null) {
                        MobclickAgent.onEvent(activity, getResources().getString(R.string.event_cancleLocationLine), "取消定位");
                    }
                } else {
                    if(!NetWorkUtils.isGPSEnabled(activity) || !activity.getPersimmions()){
                        ToastUitl.showText(activity,activity.getString(R.string.please_open_location));
                        return;
                    }
                    activity.getPersimmions();
                    if(!NetWorkUtils.isGPSEnabled(activity) || !NetWorkUtils.isNetworkConnected(activity)){
                        ToastUitl.showText(activity,activity.getString(R.string.please_open_location));
                    }
                    tempChangeStationList = new ArrayList<>(needChangeStationList);
                    isRemind = true;
                    if (getMainActivity() != null) {
                        getMainActivity().setRemindState(true);
                    }
                    drawable = Utils.tint(drawable,pressColor);
                    cancle_remind_btn.setTextColor(pressColor);
                    cancle_remind_btn.setText(getResources().getString(R.string.cancle_remind));
                    setCompoundDrawables(cancle_remind_btn, drawable);
                    if(this.lineObject != null) {
                        MobclickAgent.onEvent(activity, getResources().getString(R.string.event_startLocationLine), "开始定位");
                    }
                }
                break;
        }
    }

    public void saveFavourite() {
        String lineStr = CommonFuction.convertStationToString(lineObject);
        String allFavoriteLine = CommonFuction.getSharedPreferencesValue(activity, CommonFuction.FAVOURITE);
        Log.d(TAG, "lineStr = " + lineStr + " allFavoriteLine = " + allFavoriteLine);
        Drawable drawable = activity.getResources().getDrawable(R.drawable.hwpush_no_collection);
        if (allFavoriteLine.contains(lineStr)) {
            collection_btn.setTextColor(normalColor);
            String string[] = allFavoriteLine.split(CommonFuction.TRANSFER_SPLIT);
            StringBuffer newLine = new StringBuffer();
            int size = string.length;
            for (int i = 0; i < size; i++) {
                if (!lineStr.equals(string[i])) {
                    newLine.append(string[i] + CommonFuction.TRANSFER_SPLIT);
                }
            }
            drawable = Utils.tint(drawable,normalColor);
            setCompoundDrawables(collection_btn,drawable );
            CommonFuction.writeSharedPreferences(activity, CommonFuction.FAVOURITE, newLine.toString());
            Log.d(TAG, "remove newLine = " + newLine);
        } else {
            collection_btn.setTextColor(pressColor);
            String allFavoriteLines = CommonFuction.getSharedPreferencesValue(activity, CommonFuction.FAVOURITE);
            StringBuffer newLine = new StringBuffer();
            newLine.append(allFavoriteLines + CommonFuction.TRANSFER_SPLIT + lineStr);
            CommonFuction.writeSharedPreferences(activity, CommonFuction.FAVOURITE, newLine.toString());
            drawable = Utils.tint(drawable,pressColor);
            setCompoundDrawables(collection_btn, drawable);
            Log.d(TAG, "add newLine = " + newLine);
        }
        drawable = activity.getResources().getDrawable(R.drawable.favtor);
        if (CommonFuction.isEmptyColloctionFolder(getActivity())) {
            favtor_btn.setTextColor(normalColor);
            drawable = Utils.tint(drawable,normalColor);
            setCompoundDrawables(favtor_btn, drawable);
        } else {
            favtor_btn.setTextColor(pressColor);
            drawable = Utils.tint(drawable,pressColor);
            setCompoundDrawables(favtor_btn, drawable);
        }
    }

    public void setRemindSetViewListener(RemindSetViewListener mRemindSetViewListener) {
        this.mRemindSetViewListener = mRemindSetViewListener;

    }

    private BDLocation mBDLocation;

    public BDLocation getBDLocation() {
        return mBDLocation;
    }

    @Override
    public void stopRemind(){
        isRemind = false;
        if (getMainActivity() != null) {
            getMainActivity().setRemindState(false);
        }
        Drawable drawable = activity.getResources().getDrawable(R.drawable.set_remind);
        if (cancle_remind_btn != null) {
            cancle_remind_btn.setTextColor(normalColor);
            cancle_remind_btn.setText(getResources().getString(R.string.startlocation));
            drawable = Utils.tint(drawable,normalColor);
            setCompoundDrawables(cancle_remind_btn, drawable);
        }
        getActivity().finish();
    }

    @Override
    public void loactionStation(BDLocation location) {
        if (getMainActivity() != null) {
            MainActivity activity = getMainActivity();
        }
        mBDLocation = location;

        StationInfo nerstStationInfo = PathSerachUtil.getNerastNextStation(location, list);
        if (mScrollFavoriteManager != null)
            mScrollFavoriteManager.setLocation(location);
        if (activity == null || !activity.getPersimmions()) {
            return;
        } else if (nerstStationInfo != null) {
            if (true) {
                LineNodeView lineNodeView = textViewList.get(nerstStationInfo.getCname());
                // LineNodeView lineNodeView = textViewList.get(list.get(num++).getCname());
                if (lineNodeView != null) {
                    if (currentStationView != null) {
                        currentStationView.setBitMap(null);
                    }
                    int size = (int) getResources().getDimension(R.dimen.current_bitmap_siez);
                    Bitmap bitmap = CommonFuction.getbitmap(BitmapFactory.decodeResource(this.getResources(), R.drawable.cm_main_map_pin_location), size, size);
                    lineNodeView.setBitMap(bitmap);
                    lineNodeView.pass = true;
                    currentStationView = lineNodeView;
                    int n = 0;
                    for (StationInfo stationInfo : list) {
                        if (stationInfo.getCname().equals(lineNodeView.getStationInfo().getCname())) {
                            break;
                        }
                        n++;
                    }

                    String surplusNum = String.format(getResources().getString(R.string.surples_station), (list.size() - n) - 1 + "");
                    String currenStr = getResources().getString(R.string.current_station) + lineNodeView.getStationInfo().getCname();
                    LineInfo lineInfo = DataManager.getInstance(getActivity()).getLineInfoList().get(lineNodeView.getStationInfo().lineid);
                    String line = lineInfo == null ? "":CommonFuction.getLineNo(lineInfo.linename)[0];
                    String direction = lineDirection.get(lineNodeView.getStationInfo().lineid) + getResources().getString(R.string.direction);
                    current_info_text.setText(surplusNum + "   " + currenStr + "   " + line + "   " + direction);
                    currentStation = lineNodeView.getStationInfo();
                    if (currentStation != null && nextStation != null && currentStation.getCname().equals(nextStation.getCname())) {
                        horizontalScrollView.scrollTo(lineNodeWidth * n, horizontalScrollView.getScrollY());
                    }
                    if (n + 1 < list.size() - 1) {
                        nextStation = list.get(n + 1);
                    }

                    if (isRemind) {
                        for (StationInfo stationInfo : tempChangeStationList) {
                            if (list.get(list.size() - 1).getCname().equals(stationInfo.getCname()) &&
                                    stationInfo.getCname().equals(currentStation.getCname())) {
                                Log.d(TAG, "arrive stationInfo.getCname()" + stationInfo.getCname());
                                tempChangeStationList.remove(stationInfo);
                                isRemind = false;
                                //sendHint(true, getResources().getString(R.string.arrive), getResources().getString(R.string.hint_arrive_end_station), "");
                                break;
                            } else if (stationInfo.getCname().equals(currentStation.getCname())) {//换乘点

                                tempChangeStationList.remove(stationInfo);
                                String str = String.format(getResources().getString(R.string.change_station_hint), stationInfo.getCname()) +
                                        DataManager.getInstance(getActivity()).getLineInfoList().get(lineNodeView.getStationInfo().lineid).linename;
                                //sendHint(false, getResources().getString(R.string.change), str,
                                //       lineDirection.get(stationInfo.lineid) + getResources().getString(R.string.direction));
                                Log.d(TAG, "change stationInfo.getCname()" + stationInfo.getCname());
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onResume() {
        //cancleNotification();
        super.onResume();
        this.isPause = false;
        MobclickAgent.onPageStart("RemindFragment"); //统计页面("MainScreen"为页面名称，可自定义)
        if(isRemind){
            InAppMessageManager.getInstance(activity).showCardMessage(activity, "main",
                    new IUmengInAppMsgCloseCallback() {
                        //插屏消息关闭时，会回调该方法
                        @Override
                        public void onColse() {
                            Log.i(TAG, "card message close");
                        }
                    });
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        this.isPause = true;
        MobclickAgent.onPageEnd("RemindFragment");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unRegisterLoginBroadcast();
    }

    public void cancleRemind() {
        isRemind = false;
        //cancleNotification();
    }

    public boolean getRemindState() {
        return isRemind;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mScrollFavoriteManager != null && mScrollFavoriteManager.isOpen()) {
                mScrollFavoriteManager.closeScrollView();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean moveTaskToBack() {
        Log.d(TAG, "moveTaskToBack isRemind = " + isRemind);
       /* if (isRemind) {
            NotificationObject mNotificationObject = NotificationUtils.createNotificationObject(getActivity(),lineDirection,currentStation, nextStation);
            setNotification(mNotificationObject);
        }*/
        return true;
    }

    //广播接收器
    private StopLocaionBroadcast mReceiver = new StopLocaionBroadcast();

    //注册广播方法
    private void registerLoginBroadcast(){
        IntentFilter intentFilter = new IntentFilter(STOP_LOCATION_ACTION);
        LocalBroadcastManager.getInstance(this.getActivity()).registerReceiver(mReceiver,intentFilter);
    }

    //取消注册
    private void unRegisterLoginBroadcast(){
        LocalBroadcastManager.getInstance(this.getActivity()).unregisterReceiver(mReceiver);
    }

    //1. 自定义广播接收者
    public class StopLocaionBroadcast extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            stopRemind();
        }
    }

}
