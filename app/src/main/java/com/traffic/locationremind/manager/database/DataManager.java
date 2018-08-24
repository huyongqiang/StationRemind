package com.traffic.locationremind.manager.database;

import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.traffic.location.remind.R;
import com.traffic.locationremind.baidu.location.activity.MainActivity;
import com.traffic.locationremind.baidu.location.activity.MainViewActivity;
import com.traffic.locationremind.baidu.location.listener.LoadDataListener;
import com.traffic.locationremind.common.util.CommonFuction;
import com.traffic.locationremind.common.util.FileUtil;
import com.traffic.locationremind.common.util.GrfAllEdge;
import com.traffic.locationremind.common.util.PathSerachUtil;
import com.traffic.locationremind.manager.bean.CityInfo;
import com.traffic.locationremind.manager.bean.LineInfo;
import com.traffic.locationremind.manager.bean.StationInfo;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataManager{

	private static final String TAG = "DataManager";

	private List<LoadDataListener> mLoadDataListener = new ArrayList<>();
	private DataHelper mDataHelper;//数据库
	private Map<String,CityInfo> cityInfoList;//所有城市信息
	private Map<Integer,LineInfo> mLineInfoList;//地图线路
	private CityInfo currentCityNo = null;
	private Map<Integer, Map<Integer,Integer>> allLineCane = new HashMap<Integer, Map<Integer,Integer>>();//用于初始化路线矩阵
	private Map<Integer,Integer> lineColor = new HashMap<>();//路线对应颜色
	private Object mLock = new Object();
	private Map<String, StationInfo> allstations = new HashMap<>();
    private int[][] matirx;
    private Integer allLineNodes[];

	private int maxLineid = 0;

	private static DataManager mDataManager;

	public int[][] getMatirx(){
	    return matirx;
    }

    public Integer[] getAllLineNodes(){
		return allLineNodes;
	}

	public void releaseResource(){
		mDataHelper.Close();
		mDataHelper = null;
		if(allLineCane != null)
			allLineCane.clear();
		if(lineColor != null)
			lineColor.clear();
		if(allstations != null)
			allstations.clear();
		if(cityInfoList != null)
			cityInfoList.clear();
		if(mLineInfoList != null)
			mLineInfoList.clear();
		if(mLoadDataListener != null)
			mLoadDataListener.clear();
	}

	public CityInfo getCurrentCityNo(){
		return currentCityNo;
	}
	public DataManager(Context context){
		this.mDataHelper = DataHelper.getInstance(context);
	}

	public void loadData(Context context){
		if(cityInfoList != null){
			cityInfoList.clear();
		}
		if(mLineInfoList != null){
			mLineInfoList.clear();
		}
		if(allLineCane != null){
			allLineCane.clear();
		}
		if(mLineInfoList != null)
			mLineInfoList.clear();
		if(lineColor != null)
			lineColor.clear();
		new MyAsyncTask().execute(context);
	}

	public void addLoadDataListener(LoadDataListener loadDataListener) {
		mLoadDataListener.add(loadDataListener) ;
	}

	public void removeLoadDataListener(LoadDataListener loadDataListener) {
		mLoadDataListener.remove(loadDataListener);
	}

	public void notificationUpdata(){
		Log.d(TAG,"notificationUpdata");
		for(LoadDataListener loadDataListener:mLoadDataListener){
			loadDataListener.loadFinish();
		}
	}
	public static DataManager getInstance(Context context){
		if(mDataManager == null){
			mDataManager = new DataManager(context);
		}
		return mDataManager;
	}

	public Map<Integer, Map<Integer,Integer>> getAllLineCane(){
		return allLineCane;
	}

	public Map<String,CityInfo> getCityInfoList(){
		return cityInfoList;
	}

	public Map<Integer,LineInfo> getLineInfoList(){
		return mLineInfoList;
	}

	public int getMaxLineid(){
		return maxLineid;
	}
	public DataHelper getDataHelper(){
		return mDataHelper;
	}

	class MyAsyncTask extends AsyncTask<Context,Void,Map<Integer,LineInfo>> {

		//onPreExecute用于异步处理前的操作
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}

		//在doInBackground方法中进行异步任务的处理.
		@Override
		protected Map<Integer,LineInfo> doInBackground(Context... params) {
			if (mDataHelper != null){
				cityInfoList = mDataHelper.getAllCityInfo();
			}else{
				return null;
			}
			//获取传进来的参数
			String shpno = CommonFuction.getSharedPreferencesValue((Context) params[0], CityInfo.CITYNAME);
			maxLineid = 0;
			if (!TextUtils.isEmpty(shpno)) {
				currentCityNo = cityInfoList.get(shpno);
			}else{
				currentCityNo = cityInfoList.get("深圳");
			}

			if (currentCityNo == null) {
				currentCityNo = CommonFuction.getFirstOrNull(cityInfoList);
			}
            if (currentCityNo == null || !FileUtil.dbIsExist((Context) params[0],currentCityNo)) {
                return null;
            }

            mDataHelper.setCityHelper((Context) params[0],currentCityNo.getPingying());
			Map<Integer,LineInfo> list= mDataHelper.getLineList(LineInfo.LINEID, "ASC");
			Log.d("zxc","currentCityNo.getPingying() = "+currentCityNo.getPingying()+" list = "+list);
			for (Map.Entry<Integer,LineInfo> entry : list.entrySet()) {
				entry.getValue().setStationInfoList(mDataHelper.QueryByStationLineNo(entry.getKey(), currentCityNo.getCityNo()));
				List<StationInfo> canTransferlist = mDataHelper.QueryByStationLineNoCanTransfer(entry.getValue().lineid, currentCityNo.getCityNo());
				allLineCane.put(entry.getKey(), PathSerachUtil.getLineAllLined(canTransferlist));
				if(entry.getKey() > maxLineid){
					maxLineid = entry.getKey();
				}
			}
			maxLineid+= 1;//找出路线最大编号加一
			mLineInfoList = list;
			setAllstations(mLineInfoList);
			allLineNodes = null;
			allLineNodes = new Integer[maxLineid];
			for (int i = 0; i < maxLineid; i++) {//初始化所有线路
				allLineNodes[i] = i;
			}
            initGrf(allLineCane,maxLineid);
			//GrfAllEdge.createGraph(allLineNodes, allLineCane);
			return list;
		}

		//onPostExecute用于UI的更新.此方法的参数为doInBackground方法返回的值.
		@Override
		protected void onPostExecute(Map<Integer,LineInfo> list) {
			super.onPostExecute(list);
			notificationUpdata();
		}
	}

    // 初始化图数据
    private void initGrf(Map<Integer, Map<Integer, Integer>> allLineCane,int total) {
        matirx = null;
        this.matirx = new int[total][total];
        for (Map.Entry<Integer, Map<Integer, Integer>> entry : allLineCane.entrySet()) {
            for (Map.Entry<Integer, Integer> value : entry.getValue().entrySet()) {
                this.matirx[entry.getKey()][value.getValue()] = 1;
            }
        }
		GrfAllEdge.printMatrix(total,matirx);
    }

	WeakReference<Callbacks> mCallbacks;
	public interface Callbacks {

	}

	public Map<String, StationInfo> getAllstations() {
		return allstations;
	}

	private void setAllstations(Map<Integer, LineInfo> allLines) {
		allstations.clear();
		if (allLines != null) {
			for (Map.Entry<Integer, LineInfo> entry : allLines.entrySet()) {
				for (StationInfo stationInfo : entry.getValue().getStationInfoList()) {
					allstations.put(stationInfo.getCname(), stationInfo);
				}
			}
		}
	}
	/**
	 * Set this as the current Launcher activity object for the loader.
	 */
	public void initialize(Callbacks callbacks) {
		synchronized (mLock) {
			mCallbacks = new WeakReference<>(callbacks);
		}
	}

}
