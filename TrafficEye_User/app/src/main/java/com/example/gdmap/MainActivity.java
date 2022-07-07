package com.example.gdmap;

import android.app.Activity;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.MapsInitializer;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.maps.model.Polyline;
import com.amap.api.maps.model.PolylineOptions;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends Activity {
    private MapView mMapView = null;
    // 声明AMapLocationClient类对象
    public AMapLocationClient mLocationClient = null;
    // 声明AMapLocationClientOption对象
    public AMapLocationClientOption mLocationOption = null;

    private TextView tv_lat_data;
    private TextView tv_lon_data;
    private TextView tv_GPS_data;
    private TextView tv_accuracy_data;
    private final String[] locate_state = {"定位失败", "GPS定位", "前次定位",
            "缓存定位", "Wifi定位", "基站定位", "7=null", "离线定位", "最后位置", "10=null",
            "模糊定位"};

    private String lat = "0";   // "32.20240829848673";
    private String lon = "0";   // "118.71504300188374";
    private String person = "5";


    private AMap aMap;
    private MyLocationStyle myLocationStyle;


    private Socket client = null;
    private String message;
    private TextView tv_seats_data;
    private Marker marker_temp;
    private String tcp_ip;
    private String tcp_port;
    private String url;

    private boolean isMapInit = false;
    private TextView tv_next_station_data;
    private TextView tv_predict_time_data;
    private TextView tv_close_station_data;
    private TextView tv_bus_id;
    private TextView tv_bus_fee;

    private double lon_self;
    private double lat_self;
    private String next_station = "0";

    public double[][] station = {             //  8x2
            {32.20528735885596, 118.72526331783759},   // 雷丁
            {32.205173963507434, 118.72412708389605},  // 晖园宿舍
            {32.204889834211325, 118.72205019609808},  // 气象楼
            {32.20483492924057, 118.72093775749222},   // 文德楼
            {32.204461132982914, 118.71856994659764},  // 明德楼
            {32.202648344410605, 118.71637313513355},  // 中院食堂
            {32.20214128687547, 118.71322628298861},   // 图书馆
            {32.201530407707985, 118.71142711844395},  // 逸夫楼
            {32.199837190392756, 118.70763047841916}   // 滨江楼
    };

    public String[] station_name = {
            "雷丁",
            "晖园宿舍",
            "气象楼",
            "文德楼",
            "明德楼",
            "中院食堂",
            "图书馆",
            "逸夫楼",
            "滨江楼"
    };

    public double[][] Road_line = {
            {32.199837190392756, 118.70763047841916},
    {32.20084346990925, 118.7099053391268},
    {32.20150193609834, 118.7114532562021},
    {32.20196208974676, 118.71281767187257},
    {32.20220889228114, 118.71379991336083},
    {32.20240829848673, 118.71504300188374},
    {32.20257112035439, 118.71638565969354},
    {32.20272935082035, 118.71773503608371},
    {32.204314898561506, 118.71753024217494},
    {32.20434812040936, 118.71776810669432},
    {32.20440495645598, 118.71808705716117},
    {32.204431202636954, 118.71853153339941},
    {32.204594901382826, 118.71957233960813},
    {32.20471424687661, 118.72039505623789},
    {32.2047673291156, 118.7209645518032},
    {32.20491218316244, 118.72209388268826},
    {32.205022466022555, 118.72324097939664},
    {32.205136849727786, 118.72432553765135},
    {32.20526075984064, 118.72540630258263}
    };
    private Polyline polyline;
    public String bus_fee = "1";
    public String bus_name = "小公交";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化高德地图权限
        MapsInitializer.updatePrivacyShow(getApplicationContext(), true, true);
        MapsInitializer.updatePrivacyAgree(getApplicationContext(), true);

        // 用getXxxExtra()取出对应类型的数据。取出String只需要指定key
        Intent intent = getIntent();
        url = intent.getStringExtra("url");

        // 初始化ui
        initView();

        // 获取地图控件引用
        mMapView = findViewById(R.id.map);
        // 创建地图View
        mMapView.onCreate(savedInstanceState);
        // 获取地图
        aMap = mMapView.getMap();
        // 禁用旋转
        aMap.getUiSettings().setRotateGesturesEnabled(false);

        // 调试 : 画出BUS位置和剩余人数,初始化也需要使用
        drawPosition(32.20540718, 118.71215622, 8);
        marker_temp.setInfoWindowEnable(false);


        // 绘制车站
        for (int i = 0; i < station.length; i++) {
            drawStation(station[i][0], station[i][1], i);
        }

        // 绘制交通路线
        drawLine();


        // TODO DONE  为了展示效果,先隐藏地图文字
        // 为了避嫌,隐藏地图文字,不显示学校名称
        aMap.setOnMapLoadedListener(new AMap.OnMapLoadedListener() {
            @Override
            public void onMapLoaded() {
                aMap.showMapText(false);
            }
        });


        // 定义 Marker 点击事件监听
        AMap.OnMarkerClickListener markerClickListener = new AMap.OnMarkerClickListener() {
            // 返回 true 则表示接口已响应事件，否则返回false
            @Override
            public boolean onMarkerClick(Marker marker) {
                if (marker.isInfoWindowShown() == true) {
                    marker.hideInfoWindow();
                }

                // 设置title为bus的点击事件
                if (marker.getTitle() == marker_temp.getTitle()) {

                    // 计算大约时间
                    double lat_bus = Double.parseDouble(lat);
                    double lon_bus = Double.parseDouble(lon);
                    double distance = 111320 * Math.abs(lat_bus - lat_self) + 100000 * Math.abs(lon_bus - lon_self);
                    int time = (int) (distance / 200);

                    // 防止显示0分钟
                    if (time == 0) {
                        time = 1;
                    }

                    // 设置显示参数
                    tv_predict_time_data.setText(String.valueOf(time) + "分钟");
                    tv_bus_id.setText(bus_name);
                    tv_seats_data.setText(person);
                    tv_bus_fee.setText("费用:"+bus_fee+"元");
                    tv_next_station_data.setText(station_name[Integer.parseInt(next_station)]);
                }

                return false;
            }
        };

        // 绑定 Marker 被点击事件
        aMap.setOnMarkerClickListener(markerClickListener);


        // 设置地图参数
        myLocationStyle = new MyLocationStyle();//初始化定位蓝点样式类myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE);//连续定位、且将视角移动到地图中心点，定位点依照设备方向旋转，并且会跟随设备移动。（1秒1次定位）如果不设置myLocationType，默认也会执行此种模式。
        myLocationStyle.interval(5000); //设置连续定位模式下的定位间隔，只在连续定位模式下生效，单次定位模式下不会生效。单位为毫秒。
        aMap.setMyLocationStyle(myLocationStyle);//设置定位蓝点的Style
        aMap.setMyLocationEnabled(true);// 设置为true表示启动显示定位蓝点，false表示隐藏定位蓝点并不进行定位，默认是false。
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_FOLLOW_NO_CENTER);//连续定位、蓝点不会移动到地图中心点，地图依照设备方向旋转，并且蓝点会跟随设备移动。


        // 初始化定位
        try {
            mLocationClient = new AMapLocationClient(getApplicationContext());

        } catch (Exception e) {
            e.printStackTrace();
        }

        // 声明定位回调监听器,重新changed函数内容
        // 地图启动时会调用一次
        AMapLocationListener mLocationListener = new AMapLocationListener() {
            @Override
            public void onLocationChanged(AMapLocation amapLocation) {

                // 如果是第一次调用,初始化 地图中心位置 和 地图放大比例
                if (isMapInit == false) {
                    LatLng latLng = new LatLng(amapLocation.getLatitude(), amapLocation.getLongitude());//构造一个位置
                    // 移动到当前位置并将地图放大为17级
                    aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17));
                    isMapInit = true;
                }

                // 设置经纬度信息显示到ui界面
                if (amapLocation != null) {

                    lat_self = amapLocation.getLatitude();
                    lon_self = amapLocation.getLongitude();

                    tv_lat_data.setText(String.format("%.6f",amapLocation.getLatitude())); //获取纬度
                    tv_lon_data.setText(String.format("%.6f",amapLocation.getLongitude())); //获取经度
                    tv_accuracy_data.setText(String.format("%.4f",amapLocation.getAccuracy()) + "m");//获取精度信息
                    tv_GPS_data.setText(locate_state[amapLocation.getLocationType()]);//获取定位模式

                    // 计算最近站点并显示
                    int close_index = 0;
                    double close_distance = Math.sqrt((lat_self - station[0][0]) * (lat_self - station[0][0]) + (lon_self - station[0][1]) * (lon_self - station[0][1]));

                    for (int i = 1; i < station.length; i++) {
                        double distance = Math.sqrt((lat_self - station[i][0]) * (lat_self - station[i][0]) + (lon_self - station[i][1]) * (lon_self - station[i][1]));
                        if (distance < close_distance) {
                            close_distance = distance;
                            close_index = i;
                        }
                    }
                    // 显示最近站点
                    tv_close_station_data.setText(station_name[close_index]);

                    // 删除原先点,后续重新绘制
                    marker_temp.remove();

                    drawPosition(Double.parseDouble(lat), Double.parseDouble(lon), Integer.valueOf(person));

                } else {
                    Toast.makeText(getApplicationContext(),
                            "ErrCode:" + amapLocation.getErrorCode() + ", errInfo:" + amapLocation.getErrorInfo(),
                            Toast.LENGTH_LONG
                    ).show();
                }

            }
        };

        //设置定位回调监听
        mLocationClient.setLocationListener(mLocationListener);

        //初始化AMapLocationClientOption对象
        mLocationOption = new AMapLocationClientOption();
        //设置定位模式为AMapLocationMode.High_Accuracy，高精度模式。
        mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        //设置定位间隔,单位毫秒,默认为2000ms，最低1000ms。
        mLocationOption.setInterval(1000);
        //关闭缓存机制
        mLocationOption.setLocationCacheEnable(false);

        //给定位客户端对象设置定位参数
        mLocationClient.setLocationOption(mLocationOption);

        //启动定位
        mLocationClient.startLocation();
        mLocationOption.setLocationPurpose(AMapLocationClientOption.AMapLocationPurpose.SignIn);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
        mLocationClient.onDestroy();//销毁定位客户端，同时销毁本地定位服务。
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMapView.onPause();
        mLocationClient.stopLocation();//停止定位后，本地定位服务并不会被销毁
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mMapView.onSaveInstanceState(outState);
    }


    private void initView() {
        tv_lat_data = findViewById(R.id.tv_lat_data);
        tv_lon_data = findViewById(R.id.tv_lon_data);
        tv_GPS_data = findViewById(R.id.tv_GPS_data);
        tv_accuracy_data = findViewById(R.id.tv_Accuracy_data);
        tv_seats_data = findViewById(R.id.tv_seats_data);
        tv_next_station_data = findViewById(R.id.tv_next_station_data);
        tv_predict_time_data = findViewById(R.id.tv_predict_time_data);
        tv_close_station_data = findViewById(R.id.tv_close_station_data);
        tv_bus_id = findViewById(R.id.tv_BUS_id);
        tv_bus_fee = findViewById(R.id.tv_bus_fee);


        // 确认TCP地址
        String[] data = url.split(":");
        tcp_ip = data[0];
        tcp_port = data[1];

        // 清空内容
        tv_next_station_data.setText("");
        tv_predict_time_data.setText("");
        tv_seats_data.setText("");

        // 发送识别码并接收数据
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (client == null) {
                        client = new Socket(tcp_ip, Integer.valueOf(tcp_port));
                    }
                    // 发送识别码
                    message = "USER";
                    OutputStream os = client.getOutputStream();
                    os.write(message.getBytes(StandardCharsets.UTF_8));

                    // 接收数据
                    while (true) {
                        if (client.getInputStream() != null) {
                            InputStream in = client.getInputStream();
                            byte[] b = new byte[1024];
                            int len = in.read(b);
                            message = new String(b, 0, len);
                            String[] data = message.split(",");
                            lat = data[0];
                            lon = data[1];
                            person = data[2];
                            next_station = data[3];
                            bus_fee = data[4];
                            bus_name = data[5];
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }).start();
    }


    private void drawPosition(double lat, double lon, int person_left) {

        LatLng latLng = new LatLng(lat, lon);
        MarkerOptions markerOption = new MarkerOptions();
        markerOption.position(latLng);

        markerOption.title("BUS");

        markerOption.draggable(true);//设置Marker可拖动
        markerOption.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory
                .decodeResource(getResources(), R.drawable.bus)));
        // 将Marker设置为贴地显示，可以双指下拉地图查看效果
        markerOption.setFlat(true);//设置marker平贴地图效果

        marker_temp = aMap.addMarker(markerOption);
        marker_temp.setInfoWindowEnable(false);
    }

    private void drawLine() {
        List<LatLng> latLngs = new ArrayList<>();
        for (int i = 0; i < Road_line.length; i++) {
            latLngs.add(new LatLng(Road_line[i][0], Road_line[i][1]));
        }
        polyline = aMap.addPolyline(new PolylineOptions().addAll(latLngs).width(10).color(Color.argb(255,1,1,1)));
    }


    private void drawStation(double lat, double lon, int i) {

        LatLng latLng = new LatLng(lat, lon);
        MarkerOptions markerOption = new MarkerOptions();
        markerOption.position(latLng).title(station_name[i]);

        markerOption.draggable(false);//设置Marker可拖动

        // 将Marker设置为贴地显示，可以双指下拉地图查看效果
        markerOption.setFlat(true);//设置marker平贴地图效果

        Marker marker = aMap.addMarker(markerOption);
    }


}