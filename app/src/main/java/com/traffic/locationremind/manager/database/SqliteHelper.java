package com.traffic.locationremind.manager.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.traffic.locationremind.manager.bean.CityInfo;
import com.traffic.locationremind.manager.bean.ExitInfo;
import com.traffic.locationremind.manager.bean.LineInfo;
import com.traffic.locationremind.manager.bean.StationInfo;


public class SqliteHelper extends SQLiteOpenHelper {

    public static final String TB_LINE = "line";
    public static final String TB_STATION = "station";
    public static final String TB_EXIT_INFO = "exitinfo";
    public static final String TB_CITY_INFO = "city";

    public SqliteHelper(Context context, String name, CursorFactory factory,
                        int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TB_LINE + "("
                + LineInfo.LINEID + " int,"
                + LineInfo.LINENAME + " varchar,"
                + LineInfo.LINEINFO + " varchar,"
                + LineInfo.RGBCOOLOR + " varchar,"
                + CityInfo.CITYNO + " varchar"
                + ")");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TB_STATION + "("
                + StationInfo.LINEID + " int,"
                + StationInfo.PM + " int,"
                + StationInfo.CNAME + " varchar,"
                + StationInfo.PNAME + " varchar,"
                + StationInfo.ANAME + " varchar,"
                + StationInfo.LOT + " varchar,"
                + StationInfo.LAT + " varchar,"
                + StationInfo.PRESTATION + " varchar,"
                + StationInfo.NEXTSTATION + " varchar,"
                + StationInfo.STATIONINFO + " varchar,"
                + StationInfo.TRANSFER + " varchar,"
                + CityInfo.CITYNO + " varchar"
                + ")");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TB_EXIT_INFO + "("
                + ExitInfo.CNAME + " varchar,"
                + ExitInfo.EXITNAME + " varchar,"
                + ExitInfo.ADDR + " varchar,"
                + CityInfo.CITYNO + " varchar"
                + ")");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TB_CITY_INFO + "("
                + CityInfo.CITYNO + " varchar,"
                + CityInfo.CITYNAME + " varchar"
                + ")");

        Log.e("Database", "onCreate create table " + TB_LINE + " " + TB_STATION + " " + TB_STATION);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TB_LINE);
        db.execSQL("DROP TABLE IF EXISTS " + TB_STATION);
        db.execSQL("DROP TABLE IF EXISTS " + TB_EXIT_INFO);
        onCreate(db);
    }

    public void updateColumn(SQLiteDatabase db, String tbName, String oldColumn,
                             String newColumn, String typeColumn) {
        try {
            db.execSQL("ALTER TABLE " + tbName + " CHANGE " + oldColumn + " "
                    + newColumn + " " + typeColumn);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
