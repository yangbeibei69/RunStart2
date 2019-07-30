package com.example.runstart;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;

public class StepCounterService extends Service {

    public static Boolean START_SERVICE = false;// 服务运行标志
    private SensorManager mSensorManager;// 传感器服务
    private StepDetector detector;// 传感器监听对象

    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return null;
    }

    //创建服务，创建传感器实例，绑定监听事件
    @Override
    public void onCreate() {
        super.onCreate();
        // 标记为服务正在运行
        START_SERVICE = true;
        // 创建监听器类，实例化监听对象
        detector = new StepDetector(this);
        // 获取传感器的服务，初始化传感器
        mSensorManager = (SensorManager) this.getSystemService(SENSOR_SERVICE);
        // 注册传感器，注册监听器
        mSensorManager.registerListener(detector, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_FASTEST);

    }

    //停止服务，解绑传感器监听事件
    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        START_SERVICE = false;// 服务停止
        if (detector != null) {
            mSensorManager.unregisterListener(detector);
        }
    }
}

