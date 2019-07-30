# RunStart2
一款的Android原生实现的跑步运动应用，除了界面丑点，步数，记时，地图，轨迹，存储数据库信息等功能都没问题
  ![image](https://github.com/yangbeibei69/RunStart2/blob/master/%E5%9B%BE%E7%89%87/runStart2.png)

## 这个应用程序的界面确实丑了一点，功能经测试没什么问题

## 项目中有apk可以测试查看

## 如何导入AndroidStudio中运行
  ### 项目下载后导入后，将下图中的百度地图秘钥替换成自己的
  ![image](https://github.com/yangbeibei69/RunStart2/blob/master/%E5%9B%BE%E7%89%87/runstart_1.jpg)

## 主要功能有：
  ### 实时地图显示：（百度地图）
  ### 运动轨迹显示：
  #### 具体实现：将运动中实时采集的经纬度，保存到列中，通过一定的过滤，保存有价值的经纬度，然后画出来
  ### 计时：（Chronometer组件）
  ### 步数： 
  #### 网上开源的计步算法，类名：StepDetector.java，其中静态变量SENSITIVITY可以设置传感器的敏感度

### 里程数

### 距离

### 速度

### 支持保存到的SQLite：（保存哪些数据，我也忘了，大二时写的，现在毕业才上传上来）

### 后期作者有时间的话，会加上后台服务器，将每次运动数据记录到后台中，支持在线查阅历史记录
