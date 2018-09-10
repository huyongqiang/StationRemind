package com.traffic.locationremind.baidu.location.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.geocode.*;
import com.traffic.location.remind.R;
import com.traffic.locationremind.baidu.location.activity.MainActivity;
import com.traffic.locationremind.baidu.location.adapter.AllLineAdapter;
import com.traffic.locationremind.baidu.location.adapter.ColorLineAdapter;
import com.traffic.locationremind.baidu.location.dialog.SearchLoadingDialog;
import com.traffic.locationremind.baidu.location.dialog.SettingReminderDialog;
import com.traffic.locationremind.common.util.CharSort;
import com.traffic.locationremind.common.util.MapComparator;
import com.traffic.locationremind.common.util.ReadExcelDataUtil;
import com.traffic.locationremind.manager.bean.ExitInfo;
import com.traffic.locationremind.manager.bean.LineInfo;
import com.traffic.locationremind.manager.bean.StationInfo;
import com.traffic.locationremind.manager.database.DataManager;

import java.util.*;

public class LineMapFragment extends Fragment implements ReadExcelDataUtil.DbWriteFinishListener, View.OnClickListener {

    private final static String TAG = "LineMapFragment";

    private GridView sceneMap;
    private GridView lineMap;
    private TextView currentLineInfoText;

    private AllLineAdapter sceneMapAdapter;
    private ColorLineAdapter colorLineAdapter;
    private View rootView;
    private DataManager mDataManager;
    private List<LineInfo> list = new ArrayList<>();
    MainActivity activity;
    StationInfo start,end;
    private SettingReminderDialog mSettingReminderDialog;
    private int currentIndex = 0;

    private Handler handler = new Handler() {

        public void handleMessage(Message msg) {
            setCurrentLine(msg.what);
        }

    };

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (null != rootView) {
            ViewGroup parent = (ViewGroup) rootView.getParent();
            if (null != parent) {
                parent.removeView(rootView);
            }
        } else {
            rootView = inflater.inflate(R.layout.line_map_layout, container, false);
            initView(rootView);// 控件初始化
        }
        if(savedInstanceState != null){
            upadaData();
            setCurrentLine(currentIndex);
        }
        Log.d(TAG, "onCreateView rootView = "+rootView+" savedInstanceState = "+savedInstanceState);
        return rootView;
    }

    private void initView(View rootView) {
        Log.d(TAG, "initView rootView = "+rootView);
        sceneMap = (GridView) rootView.findViewById(R.id.sceneMap);
        currentLineInfoText = (TextView) rootView.findViewById(R.id.text);
        Log.d(TAG, "initView currentLineInfoText = "+currentLineInfoText);
        lineMap = (GridView) rootView.findViewById(R.id.lineMap);
        //linenail = getResources().getString(R.string.line_tail);
        mDataManager = ((MainActivity) getActivity()).getDataManager();

        colorLineAdapter = new ColorLineAdapter(this.getActivity());
        lineMap.setAdapter(colorLineAdapter);
        sceneMapAdapter = new AllLineAdapter(this.getActivity(), mDataManager);
        sceneMap.setAdapter(sceneMapAdapter);

        sceneMap.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                showDialog(sceneMapAdapter.getItem(position));
            }
        });
        activity = (MainActivity) getActivity();
        lineMap.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                setCurrentLine(position);
            }
        });

    }

    private void showDialog(final StationInfo stationInfo) {
        Log.d(TAG, "showDialog stationInfo.getCname() = " + stationInfo.getCname());
        List<ExitInfo> existInfoList = mDataManager.getDataHelper().QueryByExitInfoCname(stationInfo.getCname());
        Collections.sort(existInfoList, new MapComparator());
        String existInfostr = "";
        if (existInfoList != null) {
            final int existInfoList_size = existInfoList.size();// Moved  existInfoList.size() call out of the loop to local variable existInfoList_size
            for (int n = 0; n < existInfoList_size; n++) {
                existInfostr += existInfoList.get(n).getExitname() + " " + existInfoList.get(n).getAddr() + "\n";
            }
        }
        if (mSettingReminderDialog != null) {
            mSettingReminderDialog.dismiss();
            mSettingReminderDialog = null;
        } else {
        }
        mSettingReminderDialog = new SettingReminderDialog(getActivity(),
                R.style.Dialog, new SettingReminderDialog.NoticeDialogListener() {
            @Override
            public void onClick(View view) {
                try {
                    switch (view.getId()) {
                        case R.id.start:
                            start = stationInfo;
                            mSettingReminderDialog.dismiss();
                            break;
                        case R.id.end:
                            mSettingReminderDialog.dismiss();
                            end = stationInfo;
                            String startText = start==null ? LineMapFragment.this.getActivity().getResources().getString(R.string.current_location):
                                    start.getCname();
                            String endText = end==null?LineMapFragment.this.getActivity().getResources().getString(R.string.current_location):
                                    end.getCname();
                            ((MainActivity)LineMapFragment.this.getActivity()).searchStation(startText,endText);
                            Log.d(TAG,"startText = "+startText+" endText = "+endText);
                            start = null;
                            end = null;
                            break;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void selectLine(int lineid){
                setCurrentLineByLineid(lineid);
                mSettingReminderDialog.dismiss();
            }
        }, stationInfo.getTransfer(), existInfostr,
                "" + stationInfo.getLineid(), mDataManager.getCurrentCityNo().getCityName(), stationInfo.getCname());
        mSettingReminderDialog.setContentView(R.layout.setting_reminder_dialog);
        mSettingReminderDialog.show();
    }

    public void upadaData() {
        if (mDataManager != null && mDataManager.getLineInfoList() != null) {
            list.clear();
            for (Map.Entry<Integer, LineInfo> entry : mDataManager.getLineInfoList().entrySet()) {
                list.add(entry.getValue());
            }
        }

        Collections.sort(list, new Comparator<LineInfo>() {
            @Override
            public int compare(LineInfo o1, LineInfo o2) {
                if (o1.getLineid() < o1.getLineid()) {
                    return -1;
                } else if (o1.getLineid() == o1.getLineid()) {
                    return 0;
                } else {
                    return 1;
                }
            }
        });
        setCurrentLine(0);
        if (colorLineAdapter != null) {
            colorLineAdapter.setData(list);
        }
        if (mDataManager != null && mDataManager.getLineInfoList() != null) {
            if (mDataManager.getLineInfoList().size() < 6) {
                ViewGroup.LayoutParams linearParams = lineMap.getLayoutParams();
                linearParams.height = (int) getResources().getDimension(R.dimen.single_color_height);
                lineMap.setLayoutParams(linearParams);
            }else{
                ViewGroup.LayoutParams linearParams = lineMap.getLayoutParams();
                linearParams.height = (int) getResources().getDimension(R.dimen.line_color_height);
                lineMap.setLayoutParams(linearParams);
            }
        }

    }

    private void setCurrentLineByLineid(int lineid) {
        LineInfo lineInfo = mDataManager.getLineInfoList().get(lineid);
        if(lineInfo == null){
            return;
        }
        if (currentLineInfoText != null) {
            /*if (lineid >= list.size()) {
                currentLineInfoText.setBackgroundColor(Color.WHITE);
                currentLineInfoText.setTextColor(Color.WHITE);
                currentLineInfoText.setText("");
                sceneMapAdapter.setData(null);
                return;
            }*/
            String string = lineInfo.linename + " (" +
                    lineInfo.getForwad() + "," +
                    lineInfo.getReverse() + ")\n" + lineInfo.getLineinfo();

            currentLineInfoText.setText(string);
            currentLineInfoText.setBackgroundColor(lineInfo.colorid);
        }
        if (sceneMap != null) {
            sceneMapAdapter.setData(lineInfo);
            int height = (int) activity.getResources().getDimension(R.dimen.count_line_node_rect_height) * (lineInfo.getStationInfoList().size() / 5 + 1);
            ViewGroup.LayoutParams linearParams = sceneMap.getLayoutParams();
            linearParams.height = height;
            sceneMap.setLayoutParams(linearParams); //使设置好的布局参数应用到控件
        }

    }

    private void setCurrentLine(int index) {

        currentIndex = index;
        if (currentLineInfoText != null) {
            if (index >= list.size()) {
                currentLineInfoText.setBackgroundColor(Color.WHITE);
                currentLineInfoText.setTextColor(Color.WHITE);
                currentLineInfoText.setText("");
                sceneMapAdapter.setData(null);
                return;
            }
            String string = list.get(index).linename + " (" + list.get(index).getForwad() + "," + list.get(index).getReverse() + ")\n" + list.get(index).getLineinfo();
            currentLineInfoText.setText(string);
            currentLineInfoText.setBackgroundColor(list.get(index).colorid);
        }
        if (sceneMap != null) {
            sceneMapAdapter.setData(mDataManager.getLineInfoList().get(list.get(index).lineid));
            int height = (int) activity.getResources().getDimension(R.dimen.count_line_node_rect_height) * (list.get(index).getStationInfoList().size() / 5 + 1);
            ViewGroup.LayoutParams linearParams = sceneMap.getLayoutParams();
            linearParams.height = height;
            sceneMap.setLayoutParams(linearParams); //使设置好的布局参数应用到控件
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
    @Override
    public void onClick(View v) {

    }

    public void dbWriteFinishNotif() {

    }
}
