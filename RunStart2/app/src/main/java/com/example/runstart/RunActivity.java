package com.example.runstart;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.CoordType;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.Polyline;
import com.baidu.mapapi.map.PolylineOptions;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.utils.DistanceUtil;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import static android.content.Context.NOTIFICATION_SERVICE;

public class RunActivity extends AppCompatActivity implements View.OnClickListener {

    private BottomNavigationView navigation;




    //测试按钮(没什么用)
    private Button yang;

   //UI相关
    private Button start, stop,card;//开始结束打卡按钮
    private TextView step_Text,time_text,show; //步数与时间，距离
    private TextView randomTime,daka,speed; //随机分钟,打卡所在地,里程速度
    private TextView positionText;  //显示所有信息
    private Thread thread;      //线程对象

    //百度地图相关
    private double GDistance = 0.0;     //轨迹距离
    private double ZDistance = 0.0;     //直线距离
    private LocationClient mLocationClient;     //定位服务
    private Boolean isFirstLocate = true;       //是否第一次定位
    private MyLocationData locationData;        //定位数据
    private double mCurrentLat = 0.0;       //经度
    private double mCurrentLon = 0.0;       //纬度
    private MapView mapView;        //地图
    private BaiduMap baiduMap;      //地图
    LatLng last = new LatLng(0, 0);//上一个位置点
    List<LatLng> points = new ArrayList<LatLng>();//位置点集合
    Polyline mPolyline;                         //运动轨迹图层
    MapStatus.Builder builder;
    private SensorManager sensormanager;        //方向传感器
    private double lastX = 0.0;                 //传感器方向数据
    private int mCurrentDirection = 0;          //转换到地图上的方向

    private ActionBar actionBar;

    //计步器相关
    private int sumStep = 0;    //总步数
    //时间相关
    private Chronometer timer;//计时器

    //语音播报相关
    private IntentFilter intentFilter;
    private VoiceBoradcastReceiver boradcastReceiver;
    public MediaPlayer mediaPlayer;
    //随机数
    private String tt;


    //数据库相关
    private DatabaseUtil databaseUtil;


    //UI更新
    Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            //调用步数方法
            countStep();
            // 显示当前步数
            step_Text.setText("步数："+sumStep);

            switch (msg.what){
                case 1:
                    card.setEnabled(false);
                    break;
            }
        }
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //全屏显示
        hideActionBarWithUI();

        MapView();
        addView();
        //地图控制器
        baiduMap = mapView.getMap();
        //开启显示自己位置
        baiduMap.setMyLocationEnabled(true);
        //运行时权限
        StartPermission();
        yang = findViewById(R.id.yang);
        start.setOnClickListener(this);
        stop.setOnClickListener(this);
        card.setOnClickListener(this);
        yang.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(RunActivity.this, points.size()+"个", Toast.LENGTH_SHORT).show();
            }
        });
        MyThread();
    }



    /**
     * 隐藏ActionBar与导航栏，全屏显示
     */
    private void hideActionBarWithUI(){
        //隐藏ActionBar
        actionBar=getSupportActionBar();
        actionBar.hide();


        //隐藏虚拟按键，并且全屏
        if (Build.VERSION.SDK_INT > 11 && Build.VERSION.SDK_INT < 19) { // lower api
            View v = this.getWindow().getDecorView();
            v.setSystemUiVisibility(View.GONE);
        } else if (Build.VERSION.SDK_INT >= 19) {
            //for new api versions.
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
        }
    }

    //百度地图初始化
    private void MapView(){
        mLocationClient = new LocationClient(getApplicationContext());
        //绑定定位监听
        mLocationClient.registerLocationListener(new MyLocationListener());
        //百度地图初始化
        SDKInitializer.initialize(getApplicationContext());
        //设置坐标类型
        SDKInitializer.setCoordType(CoordType.GCJ02);
    }

    //初始化组件
    private void addView(){
        setContentView(R.layout.activity_run);

        //初始化导航栏
        navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);


        //实例化组件
        timer = findViewById(R.id.timer);
        start = findViewById(R.id.start);
        card = findViewById(R.id.card);
        card.setEnabled(false);
        stop = findViewById(R.id.stop);
        stop.setEnabled(false);
        step_Text = findViewById(R.id.stepText);
        time_text = findViewById(R.id.TimeText);
        daka  = findViewById(R.id.daka);
        speed = findViewById(R.id.speed);
        positionText = findViewById(R.id.positionText);
        show = findViewById(R.id.show);
        randomTime = findViewById(R.id.random);
        mapView = findViewById(R.id.mapview);
        //方向传感器
        sensormanager= (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        //灵敏度
        StepDetector.CURRENT_STEP = 0;
        //实际总步数
        countStep();
        step_Text.setText("运动步数："+sumStep);
        //绑定语音播报广播
        registerBroadcast();
        handler.removeCallbacks(thread);
    }

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()){
                case R.id.navigation_home:
                    Toast.makeText(RunActivity.this, "item1", Toast.LENGTH_SHORT).show();
                    return true;

                case R.id.navigation_dashboard:
                    Toast.makeText(RunActivity.this, "item2", Toast.LENGTH_SHORT).show();
                    return true;

                case R.id.navigation_notifications:
                    Toast.makeText(RunActivity.this, "item3", Toast.LENGTH_SHORT).show();
                    return true;
            }
            return false;
        }
    };


    private void MyThread(){
        if (thread == null) {
            thread = new Thread() {
                @Override
                public void run() {
                    super.run();
                    int temp = 0;
                    while (true) {
                        try {
                            //
                            Thread.sleep(200);

                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        if (StepCounterService.START_SERVICE) {
                            Message msg = new Message();
                            if (temp != StepDetector.CURRENT_STEP) {
                                temp = StepDetector.CURRENT_STEP;
                            }
                            handler.sendMessage(msg);
                        }
                    }
                }
            };
            thread.start();
        }
    }

    //实际运动总步数
    private void countStep() {
        if (StepDetector.CURRENT_STEP % 2 == 0) {
            sumStep = StepDetector.CURRENT_STEP;
        } else {
            sumStep = StepDetector.CURRENT_STEP + 1;
        }

        sumStep = StepDetector.CURRENT_STEP;
    }

    //开始计时
    private void StartTime() {
        //设置计时器开始时间(清零)
        timer.setBase(SystemClock.elapsedRealtime());
        //设置计时器的格式(SystemClock.elapsedRealtime获取设备boot后的时间)
        int hour = (int) ((SystemClock.elapsedRealtime() - timer.getBase()) / 1000 / 60);
        timer.setFormat("0" + String.valueOf(hour) + ":%s");
        //开始
        timer.start();
    }

    //结束计时
    private void StopTime() {
        timer.stop();
    }

    //获取运动时间
    private void RunTime(){
        time_text.setText(timer.getText().toString());
    }

    //按钮监听
    @Override
    public void onClick(View v) {
        Intent service = new Intent(this, StepCounterService.class);

        switch (v.getId()) {
            case R.id.start: {
                //开启记录步数服务
                startService(service);
                StepDetector.CURRENT_STEP = 0;
                //开启计时器
                StartTime();
                //开启定位
                if (mLocationClient != null && !mLocationClient.isStarted()) {
                    requestLocation();
                }
                mapView.setVisibility(View.VISIBLE);
                //开启计时器监听
                ChronometerListener();
                //随机时间
                tt = get()+"";
                //不能使用开始
                start.setEnabled(false);
                stop.setEnabled(true);

                break;
            }

            case R.id.stop: {
                //停止计步数服务
                stopService(service);
                //停止计时
                StopTime();
                //停止定位
                mLocationClient.stop();
                //显示当前运动时间
                RunTime();

                getSpeed();

                card.setEnabled(false);
                //向数据库添加数据(时间，步数)
                databaseUtil.InsertData(timer.getText().toString(),sumStep);
                //结束图标
                try {
                    MarkerOptions oFinish = new MarkerOptions();// 地图标记覆盖物参数配置类
                    oFinish.position(points.get(points.size() - 1));
                    oFinish.icon( BitmapDescriptorFactory.fromResource(R.drawable.icon_en));// 设置覆盖物图片
                    baiduMap.addOverlay(oFinish); // 在地图上添加此图层
                    //解除广播
                    unregisterBroadcast();
                    //显示一共记录了几个点
                    StringBuilder s = new StringBuilder();
                    s.append("记录点集合个数:").append(points.size()).append(",");
                    //距离保留几位小数
                    DecimalFormat    df   = new DecimalFormat("######0.000");
                    s.append("轨迹距离：").append(df.format(getGDistanse())).append("公里").append(",");
                    //直线距离
                    s.append("直线距离：").append(df.format(getZDistance())).append("公里");
                    show.setText(s);
                    }catch (Exception e) {
                    e.printStackTrace();
                }
                    //复位
                    StepDetector.CURRENT_STEP = 0;
                    points.clear();
                    last = new LatLng(0, 0);
                    isFirstLocate = true;
    //                baiduMap.clear();
                    handler.removeCallbacks(thread);
                    //可以使用开始按钮
                    stop.setEnabled(false);
                    start.setEnabled(true);
    //                Toast.makeText(this, "结束", Toast.LENGTH_SHORT).show();
                    break;
                }

            case R.id.card:{

//                Toast.makeText(this, "diddddddddddddddddddddddddddd", Toast.LENGTH_SHORT).show();
                try {
                    if (!points.isEmpty()){
                        daka.setText(points.get(points.size() - 1).toString());
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
                break;
            }
        }
    }


    //计时器监听事件（语音播报）
    private void ChronometerListener(){
        timer.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
            @Override
            public void onChronometerTick(Chronometer chronometer) {
                String a[] = timer.getText().toString().split(":");
                if (a[1].equals(tt) || timer.getText().toString().equals("00:00:05") ){
                    card.setEnabled(true);
                    //发出广播
                    Intent intent = new Intent("com.example.runstart.PLAY_VOICE");
                    sendBroadcast(intent);
                }
            }
        });
    }


    //获取随机数(10-59之间)
    private int  get(){
                int a=new Random().nextInt(50) + 10;
                randomTime.setText("随机打卡分钟:"+a);
                return a;
    }

    //广播接收器
    public class VoiceBoradcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mediaPlayer==null) {
                mediaPlayer = MediaPlayer.create(context, R.raw.yangzihao);
            }
//           mediaPlayer.start();
            Toast.makeText(context, "语音", Toast.LENGTH_SHORT).show();

            //延迟五分钟更新打卡按钮
            Message message = new Message();
            message.what = 1;
            handler.sendMessageDelayed(message,1000*60*5);
        }
    }

    //绑定广播
    private void registerBroadcast(){
        intentFilter = new IntentFilter("com.example.runstart.PLAY_VOICE");
        boradcastReceiver = new VoiceBoradcastReceiver();
        registerReceiver(boradcastReceiver,intentFilter);

    }

    //解除广播
    private void unregisterBroadcast(){
        unregisterReceiver(boradcastReceiver);
    }

    //里程配速
    private void getSpeed(){
        double sudu = 0;
        double distance = 0;
        int SumTime =0;
        String a[] = timer.getText().toString().split(":");
        int h = Integer.parseInt(a[0]);
        int m = Integer.parseInt(a[1]);
        int s = Integer.parseInt(a[2]);
         SumTime = h*60 + m*60 +s;
        Toast.makeText(this, h+"小时"+m+"分"+s+"秒"+"共计："+SumTime+"秒", Toast.LENGTH_SHORT).show();
        DecimalFormat    df   = new DecimalFormat("######0.000");
        distance = Double.parseDouble(df.format(getGDistanse()));
        sudu = SumTime / distance;

        int fen  = (int) (sudu / 60);
        int miao = (int) (sudu % 60);
        speed.setText(fen+"分"+miao+"秒"+"/公里");
    }

    //开启定位
    private void requestLocation(){
        InitLocation();
        mLocationClient.start();
    }

    //设置定位的一些属性
    private void InitLocation() {
        LocationClientOption option = new LocationClientOption();
        //设置定位模式，只使用GPS
//        option.setLocationMode(LocationClientOption.LocationMode.Device_Sensors);
        //设置只使用GPS
        option.setOpenGps(true);
        //设置更新时间
        option.setScanSpan(1000);
        //设置方向
        option.setNeedDeviceDirect(true);
        //设置定位参数
        mLocationClient.setLocOption(option);
    }

    //接收经纬度
    public class MyLocationListener implements BDLocationListener {
        @Override
        public void onReceiveLocation(BDLocation bdLocation) {

            if (bdLocation == null || mapView == null) {
                return;
            }

            //精度圈
//            mCurrentLat = bdLocation.getLatitude();
//            mCurrentLon = bdLocation.getLongitude();
//            mCurrentAccracy = bdLocation.getRadius();
//            locationData = new MyLocationData.Builder().accuracy(bdLocation.getRadius())
//                    // 此处设置开发者获取到的方向信息，顺时针0-360
//                    .direction(mCurrentDirection).latitude(bdLocation.getLatitude()).longitude(bdLocation.getLongitude()).build();
//            baiduMap.setMyLocationData(locationData);


//            //方向   显示“我”的位置
//            mCurrentMode = MyLocationConfiguration.LocationMode.FOLLOWING;
//            baiduMap.setMyLocationConfiguration(
//                    new MyLocationConfiguration(mCurrentMode, true, mCurrentMarker));
//            MapStatus.Builder MapStatusbuilder = new MapStatus.Builder();
//            MapStatusbuilder.overlook(0);
//            baiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(MapStatusbuilder.build()));



            StringBuilder currentPosition = new StringBuilder();
            currentPosition.append("纬度：").append(bdLocation.getLatitude()).append(",");
            currentPosition.append("经度：").append(bdLocation.getLongitude()).append(",");
            currentPosition.append("定位方式：");

            if (bdLocation.getLocType() == BDLocation.TypeGpsLocation) {
                currentPosition.append("GPS").append("\n");

                if (isFirstLocate) {
                    isFirstLocate = false;
//                LatLng ll = new LatLng(bdLocation.getLatitude(), bdLocation.getLongitude());
//                MapStatusUpdate update = MapStatusUpdateFactory.newLatLng(ll);
//                baiduMap.animateMapStatus(update);
//                update = MapStatusUpdateFactory.zoomTo(18f);
//                baiduMap.animateMapStatus(update);


                    LatLng ll = null;
                    ll = getJingzhunLocation(bdLocation);
                    if (ll==null){
                        return;
                    }else {
                        points.add(ll);
                        last = ll;
                        locateAndZoom(bdLocation, ll);

                        MarkerOptions oStart = new MarkerOptions();// 地图标记覆盖物参数配置类
                        oStart.position(points.get(0));// 覆盖物位置点，第一个点为起点
                        oStart.icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_st));// 设置覆盖物图片
                        baiduMap.addOverlay(oStart); // 在地图上添加此图层

                    }
                    return;
                }

                LatLng ll = new LatLng(bdLocation.getLatitude(),bdLocation.getLongitude());
                if (DistanceUtil.getDistance(last,ll) <5 ){
                    return;
                }else {

                    points.add(ll);
                    last = ll;

                    locateAndZoom(bdLocation, ll);

                    mapView.getMap().clear();

                    MarkerOptions oStart = new MarkerOptions();
                    oStart.position(points.get(0));
                    oStart.icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_st));
                    baiduMap.addOverlay(oStart);

                    OverlayOptions ooPolyline = new PolylineOptions().width(10).color(0xff00ff00).points(points);
                    mPolyline = (Polyline) baiduMap.addOverlay(ooPolyline);


                }

            }
            else if (bdLocation.getLocType() == BDLocation.TypeNetWorkLocation) {
                currentPosition.append("net work").append("\n");
                LatLng ll = new LatLng(bdLocation.getLatitude(),bdLocation.getLongitude());
                MapStatusUpdate update = MapStatusUpdateFactory.newLatLng(ll);
                baiduMap.animateMapStatus(update);
                update = MapStatusUpdateFactory.zoomTo(16);
                baiduMap.animateMapStatus(update);
            }
            currentPosition.append("类型：").append(bdLocation.getLocType());
            currentPosition.append("误差半径：").append(bdLocation.getRadius()).append(",");
            currentPosition.append("方向:").append(bdLocation.getDirection());//.append("\n");
            positionText.setText(currentPosition);
        }
    }

    //第一次GPS定位获取最精准的定位
    private LatLng getJingzhunLocation(BDLocation bdLocation) {
        if (bdLocation.getRadius() >25){
            return null;
        }
        LatLng ll = new LatLng(bdLocation.getLatitude(),bdLocation.getLongitude());

        if (DistanceUtil.getDistance(last,ll) >20){
            last = ll;
            points.clear();
            return null;
        }
        points.add(ll);
        if (points.size() >=8 ){
            points.clear();
            return ll;
        }
        return null;
    }

    //方向传感器
    SensorEventListener listener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
//每次方向改变，重新给地图设置定位数据，用上一次onReceiveLocation得到的经纬度、精度
            double x = event.values[SensorManager.DATA_X];
            if (Math.abs(x - lastX) > 1.0) {// 方向改变大于1度才设置，以免地图上的箭头转动过于频繁
                mCurrentDirection = (int) x;
                locationData = new MyLocationData.Builder()
                        // 此处设置开发者获取到的方向信息，顺时针0-360
                        .direction(mCurrentDirection).latitude(mCurrentLat).longitude(mCurrentLon).build();
                baiduMap.setMyLocationData(locationData);
            }
            lastX = x;

        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    //显示‘我’和方向
    private void locateAndZoom(final BDLocation location, LatLng ll) {
        mCurrentLat = location.getLatitude();
        mCurrentLon = location.getLongitude();
        locationData = new MyLocationData.Builder().accuracy(0)
                // 此处设置开发者获取到的方向信息，顺时针0-360
                .direction(mCurrentDirection).latitude(location.getLatitude())
                .longitude(location.getLongitude()).build();
        baiduMap.setMyLocationData(locationData);

        builder = new MapStatus.Builder();
//        builder.target(ll).zoom(20);
        builder.zoom(15);
        baiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
        baiduMap.setMyLocationConfiguration(new MyLocationConfiguration(
                com.baidu.mapapi.map.MyLocationConfiguration.LocationMode.FOLLOWING, true, null));
    }

    //运动完后获取运动轨迹的总距离
    private double getGDistanse(){
        double distance = 0.0;
        for (int i=0; i < points.size()-1; i++){
            distance =  (distance+DistanceUtil.getDistance(points.get(i),points.get(i+1)));
        }
        GDistance = distance/1000;
        return GDistance;
    }

    //获取路线的直线距离
    private double getZDistance(){
        double distance = 0.0;
        distance = DistanceUtil.getDistance(points.get(0),points.get(points.size()-1));
        ZDistance = distance/1000;
        return ZDistance;
    }

    //运行时权限
    private void StartPermission(){
        List<String> permissionList = new ArrayList<>();
        //运行时权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(this,Manifest.permission.READ_PHONE_STATE)!=PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.READ_PHONE_STATE);
        }
        if (ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (!permissionList.isEmpty()){
            String [] permissions = permissionList.toArray(new String[permissionList.size()]);
            ActivityCompat.requestPermissions(this,permissions,1);
        }
        else {
//            requestLocation();
            databaseUtil = DatabaseUtil.getInstance(this);
        }
    }

    //运行时权限回调函数
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case 1:
                if (grantResults.length>0){
                    for (int result : grantResults){
                        if (result != PackageManager.PERMISSION_GRANTED){
                            Toast.makeText(this, "必须同意申请", Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                    }
//                    requestLocation();
                    databaseUtil= DatabaseUtil.getInstance(this);

                }else {
                    Toast.makeText(this, "未知错误", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
        }
    }

    //释放资源
    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
        sensormanager.registerListener(listener,sensormanager.getDefaultSensor(Sensor.TYPE_ORIENTATION),SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        sensormanager.unregisterListener(listener);
    }

    //活动销毁时停止获取经纬度
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLocationClient.stop();
        mapView.onDestroy();
        baiduMap.setMyLocationEnabled(false);
        mediaPlayer.stop();
    }

}
