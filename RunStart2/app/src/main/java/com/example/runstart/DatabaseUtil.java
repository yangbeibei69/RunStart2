package com.example.runstart;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

/**
 * Created by 杨子浩 on 2018/5/7.
 */

public class DatabaseUtil {
    //数据库
    private MyDatabases dbHelper;
    //表名
    private String table_RunInfo = "RunInfo";

    //单例模式
    private DatabaseUtil(Context context){
        dbHelper = new MyDatabases(context,"LBSInfo.db",null,1);
    }

    private static DatabaseUtil instance;

    public static synchronized DatabaseUtil getInstance(Context context){
        if (instance==null){
            instance = new DatabaseUtil(context);
        }
        return instance;
    }

    /**
     * 添加数据
     */

    public void InsertData(String time,int SumStep){
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("time",time);
        values.put("step",SumStep);
        db.insert(table_RunInfo,null,values);
    };
}
