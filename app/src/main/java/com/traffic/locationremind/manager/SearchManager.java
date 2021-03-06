package com.traffic.locationremind.manager;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import com.traffic.location.remind.R;
import com.traffic.locationremind.baidu.location.activity.MainActivity;
import com.traffic.locationremind.baidu.location.dialog.SearchLoadingDialog;
import com.traffic.locationremind.baidu.location.listener.RemindSetViewListener;
import com.traffic.locationremind.baidu.location.listener.SearchResultListener;
import com.traffic.locationremind.baidu.location.object.LineObject;
import com.traffic.locationremind.baidu.location.search.adapter.CardAdapter;
import com.traffic.locationremind.baidu.location.search.adapter.GridViewAdapter;
import com.traffic.locationremind.baidu.location.search.adapter.SearchAdapter;
import com.traffic.locationremind.baidu.location.search.widge.SearchView;
import com.traffic.locationremind.common.util.ToastUitl;
import com.traffic.locationremind.manager.AsyncTaskManager;
import com.traffic.locationremind.common.util.CommonFuction;
import com.traffic.locationremind.common.util.PathSerachUtil;
import com.traffic.locationremind.manager.bean.CityInfo;
import com.traffic.locationremind.manager.bean.StationInfo;
import com.traffic.locationremind.manager.database.DataManager;
import com.umeng.analytics.MobclickAgent;

import java.lang.ref.WeakReference;
import java.util.*;

public class SearchManager implements SearchView.SearchViewListener, SearchResultListener {

    private final static String TAG = "SearchManager";
    /**
     * 搜索结果列表view
     */
    private ListView lvResults;
    private ListView serachResults;
    private GridView recentSerachGrid;
    private SearchView searchView;
    //private View loading;
    private RemindSetViewListener mRemindSetViewListener;

    GridViewAdapter mGridViewAdapter;

    CardAdapter mCardAdapter = null;
    int searchTaskNum = 0;
    int finishTaskNum = 0;
    List<LineObject> lastLinesLast = new ArrayList<>();

    private MyHandler myHandler;
    private List<String> recentList = new ArrayList<>();
    /**
     * 自动补全列表adapter
     */
    private SearchAdapter autoCompleteAdapter;

    private Map<String, StationInfo> allstations = new HashMap<>();
    private DataManager mDataManager;
    private MainActivity activity;
    private SearchLoadingDialog mSearchLoadingDialog;
    List<List<Integer>> allLines = new ArrayList<>();
    Map<String, List<Integer>> allLinesMap = new HashMap<>();

    public class MyHandler extends Handler {
        private WeakReference<MainActivity> mActivity;

        public MyHandler(WeakReference<MainActivity> weakReference) {
            mActivity = weakReference;
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (lastLinesLast.size() <= 0) {
                return;
            }
            mCardAdapter.setData(lastLinesLast);

        }
    }

    public void setSearchText(String start, String end) {
        if (searchView != null) {
            searchView.setStartInput(start);
            searchView.setendInput(end);
        }
        if (autoCompleteData != null) {
            autoCompleteData.clear();
        }
    }

    /**
     * 搜索过程中自动补全数据
     */
    private List<StationInfo> autoCompleteData = new ArrayList<>();

    /**
     * 初始化视图
     */
    public void initViews(final Context context, final SearchView searchView) {
        activity = (MainActivity) context;
        myHandler = new MyHandler(new WeakReference<>(activity));
        this.searchView = searchView;
        ViewGroup serachLayoutManagerRoot = (ViewGroup) ((MainActivity) context).findViewById(R.id.serach_layout_manager_root);
        serachResults = searchView.getResultListview();
        lvResults = searchView.getLvTips();
        recentSerachGrid = searchView.getRecentSerachGrid();
        autoCompleteAdapter = new SearchAdapter(context, autoCompleteData, R.layout.item_bean_list);
        //设置监听
        searchView.setSearchViewListener(this);
        //设置adapter
        searchView.setAutoCompleteAdapter(autoCompleteAdapter);
        lvResults.setAdapter(autoCompleteAdapter);
        lvResults.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                searchView.setSelectStation(position);
            }
        });
        mCardAdapter = new CardAdapter(activity, new ArrayList<LineObject>(), R.layout.serach_result_item_layout);
        serachResults.setAdapter(mCardAdapter);
        getRecendData(context);
        mGridViewAdapter = new GridViewAdapter(context, recentList);
        recentSerachGrid.setAdapter(mGridViewAdapter);
        recentSerachGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                searchView.setRecentSelectStation(recentList.get(position));
            }
        });

        serachResults.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                if (mRemindSetViewListener != null) {
                    mRemindSetViewListener.openSetWindow(mCardAdapter.getItem(position));
                }
            }
        });

        serachLayoutManagerRoot.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
    }

    /**
     * 初始化数据
     */
    public void initData(Context context, DataManager dataManager) {
        this.mDataManager = dataManager;
        //从数据库获取数据
        getDbData();
        getAutoCompleteData(context, null);
    }

    public void reloadData(Context context) {
        getDbData();
        getAutoCompleteData(context, null);
    }

    private void getRecendData(Context context) {
        recentList.clear();
        String string[] = CommonFuction.getRecentSearchHistory(context);
        if (string != null && string.length > 0) {
            for (int i = 0; i < string.length; i++) {
                if (!TextUtils.isEmpty(string[i]))
                    recentList.add(string[i]);
            }
        }
    }

    public void setRemindSetViewListener(RemindSetViewListener mRemindSetViewListener) {
        this.mRemindSetViewListener = mRemindSetViewListener;
    }

    @Override
    public void notificationRecentSerachChange(Context context) {
        getRecendData(context);
        mGridViewAdapter.notifyDataSetChanged();
        Log.d(TAG, "notificationRecentSerachChange");
    }

    /**
     * 获取db 数据
     */
    private void getDbData() {
        allstations = mDataManager.getAllstations();
    }

    /**
     * 获取自动补全data 和adapter
     */
    private void getAutoCompleteData(Context context, String text) {
        //Log.d(TAG, "getAutoCompleteData text = "+text);
        if (TextUtils.isEmpty(text)) {
            return;
        }
        if (autoCompleteData == null) {
            //初始化
            autoCompleteData = new ArrayList<>();
        }
        if (true) {
            // 根据text 获取auto data
            autoCompleteData.clear();
            Log.d(TAG, "getAutoCompleteData text = " + text);
            for (Map.Entry<String, StationInfo> entry : allstations.entrySet()) {
                if (!TextUtils.isEmpty(text) && (entry.getKey().contains(text.trim())) ||
                        entry.getValue().getPname().toLowerCase().contains(text.trim().toLowerCase()) ||
                        entry.getValue().getAname().toLowerCase().contains(text.trim().toLowerCase())) {
                    Log.d(TAG, "getAutoCompleteData serach result = " + entry.getValue().getCname());
                    autoCompleteData.add(entry.getValue());
                }
            }
        }
        if (autoCompleteAdapter == null) {
            autoCompleteAdapter = new SearchAdapter(context, autoCompleteData, R.layout.item_bean_list);
        } else {
            autoCompleteAdapter.notifyDataSetChanged();
        }
    }

    /**
     * 当搜索框 文本改变时 触发的回调 ,更新自动补全数据
     *
     * @param text
     */
    @Override
    public void onRefreshAutoComplete(Context context, String text) {
        //更新数据
        getAutoCompleteData(context, text);
    }

    StationInfo startStation = null, endStation = null, prestartStation, preendStation;

    /**
     * 点击搜索键时edit text触发的回调
     */
    @Override
    public void onSearch(final Context context, String start, String end) {

        AsyncTaskManager.getInstance().stopAllGeekRunable();
        if (allstations == null || allstations.size() <= 0) {
            getDbData();
        }
        synchronized (lock) {

            String currentCity = CommonFuction.getSharedPreferencesValue(activity, CityInfo.CITYNAME);
            String locationCity = CommonFuction.getSharedPreferencesValue(activity, CityInfo.LOCATIONNAME);
            if (mSearchLoadingDialog != null) {
                mSearchLoadingDialog.dismiss();
            }
            //更新result数据
            if (TextUtils.isEmpty(start) || TextUtils.isEmpty(end))
                return;
            String current = context.getResources().getString(R.string.current_location);

            if ((start.equals(current) || end.equals(current)) && !currentCity.equals(locationCity) ||
                    (prestartStation != null && preendStation != null && prestartStation.getCname().equals(start) &&
                            preendStation.getCname().equals(end))) {
                ToastUitl.showText(activity, activity.getString(R.string.hint_open_network_gps));
                return;
            }
            if (start.equals(current)) {
                StationInfo stationInfo = activity.getLocationCurrentStation();
                if (stationInfo != null) {
                    startStation = stationInfo;
                }
            } else {
                startStation = allstations.get(start);
            }

            if (end.equals(current)) {
                StationInfo stationInfo = activity.getLocationCurrentStation();
                if (stationInfo != null) {
                    endStation = stationInfo;
                }
            } else {
                endStation = allstations.get(end);
            }
            if (startStation == null) {
                //Toast.makeText(context, "请输入有效起点站名", Toast.LENGTH_SHORT).show();
                return;
            }
            if (endStation == null) {
                //Toast.makeText(context, "请输入有效终点站名", Toast.LENGTH_SHORT).show();
                return;
            }
            searchView.hideSoftInput();
            MobclickAgent.onEvent(activity, activity.getResources().getString(R.string.event_start_station_and_end_station), startStation.getCname()+"  "+endStation.getCname());
            Log.d(TAG, "onSearch start = " + start + " end = " + end + " startStation.getCname() = " + startStation.getCname() + " endStation.getCname() = " + endStation.getCname());
            if (mSearchLoadingDialog == null) {
                mSearchLoadingDialog = new SearchLoadingDialog(activity, R.style.Dialog);
                mSearchLoadingDialog.setContentView(R.layout.search_loading);
            }
            searchView.saveRecentSearch(end);
            mSearchLoadingDialog.show();
            lastLinesLast.clear();
            allLines.clear();
            allLinesMap.clear();
            mCardAdapter.setData(null);
            searchTaskNum = 0;
            finishTaskNum = 0;
            autoCompleteAdapter.clearData();
            PathSerachUtil.getReminderLines(this, myHandler, startStation, endStation,
                    activity.getBDLocation(), mDataManager);
        }
    }

    private Object lock = new Object();

    @Override
    public void setLineNumber(int number) {
        finishTaskNum = number;
    }

    @Override
    public void updateSingleResult(List<Integer> list) {
        if (allLinesMap.get(list.toString()) != null) {
            return;
        }
        allLinesMap.put(list.toString(), list);
        allLines.clear();
        allLines.add(list);
        lastLinesLast.addAll(PathSerachUtil.getReuslt(allLines, mDataManager, startStation, endStation));
        PathSerachUtil.getRecomendLines(lastLinesLast);
        if (mSearchLoadingDialog != null && (lastLinesLast.size() > 0)) {
            mSearchLoadingDialog.cancel();
        }
        Message message = myHandler.obtainMessage();
        message.what = 0;
        myHandler.sendMessageDelayed(message, 100);
    }

    @Override
    public void cancleDialog(List<LineObject> lines) {
        if (mSearchLoadingDialog != null) {
            mSearchLoadingDialog.cancel();
        }
        if (!AsyncTaskManager.getInstance().isSearch() && lastLinesLast == null || lastLinesLast.size() <= 0) {
            Toast.makeText(activity, activity.getResources().getString(R.string.search_result_empty), Toast.LENGTH_LONG).show();
        }
    }

    public void clearResult() {
        lastLinesLast.clear();
        mCardAdapter.setData(lastLinesLast);
    }

    @Override
    public void updateResultList(List<List<Integer>> lists) {
        allLines.clear();
        for (List<Integer> list : lists) {
            allLinesMap.put(list.toString(), list);
            allLines.add(list);
        }
        lastLinesLast.addAll(PathSerachUtil.getReuslt(allLines, mDataManager, startStation, endStation));
        PathSerachUtil.getRecomendLines(lastLinesLast);
        if (mSearchLoadingDialog != null && (lastLinesLast.size() > 0)) {
            mSearchLoadingDialog.cancel();
        }
        Message message = myHandler.obtainMessage();
        message.what = 0;
        myHandler.sendMessageDelayed(message, 100);
        if (lastLinesLast.size() == 1) {
            if (mRemindSetViewListener != null) {
                mRemindSetViewListener.openSetWindow(lastLinesLast.get(0));
            }
        }
    }

    @Override
    public void updateResult(List<LineObject> lines) {
        if (mSearchLoadingDialog != null) {
            mSearchLoadingDialog.cancel();
        }
        lastLinesLast.addAll(lines);
        PathSerachUtil.getRecomendLines(lastLinesLast);
        Message message = myHandler.obtainMessage();
        message.what = 0;
        myHandler.sendMessageDelayed(message, 100);
        if (!AsyncTaskManager.getInstance().isSearch() && (lastLinesLast == null || lastLinesLast.size() <= 0)) {
            Toast.makeText(activity, activity.getResources().getString(R.string.search_result_empty), Toast.LENGTH_LONG).show();
        }
        if (lastLinesLast.size() == 1) {
            if (mRemindSetViewListener != null) {
                mRemindSetViewListener.openSetWindow(lastLinesLast.get(0));
            }
        }
    }
}
