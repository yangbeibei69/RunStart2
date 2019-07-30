package com.example.runstart;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by 杨子浩 on 2018/5/7.
 */

public class MyDatabases extends SQLiteOpenHelper {

    private static final  String CREATE_LBS="create table RunInfo(" +
            "id integer primary key autoincrement," +
            "time text," +
            "step integer);";

    private Context mContext;

    public MyDatabases(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
        mContext=context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_LBS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
