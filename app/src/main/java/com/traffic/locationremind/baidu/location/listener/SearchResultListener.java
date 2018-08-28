package com.traffic.locationremind.baidu.location.listener;

import com.traffic.locationremind.manager.bean.StationInfo;

import java.util.List;
import java.util.Map;

public interface SearchResultListener{
    void updateSingleResult(List<Integer> list);
    void updateResult(List<Map.Entry<List<Integer>, List<StationInfo>>> lastLinesLast);
    void cancleDialog(List<Map.Entry<List<Integer>, List<StationInfo>>> lastLinesLast);
    void setLineNumber(int number);
}