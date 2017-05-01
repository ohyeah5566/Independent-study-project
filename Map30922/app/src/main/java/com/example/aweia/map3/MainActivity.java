package com.example.aweia.map3;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Geocoder;
import android.location.LocationManager;

import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;


/*******************Http Volley******************/
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.neurosky.thinkgear.TGDevice;
/**********************************************/

import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;
import java.util.UUID;

import android.os.Handler;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback {

    private Button btn_connectBT;              //藍芽連線的按鈕
    private TextView bpmAverage;            //顯示平均心跳數
    private LinearLayout linearLayout;
    private LinearLayout connectBrain;
    private HorizontalScrollView scroll_mainBottom;
    private ImageView peak_iamge;
    private LinearLayout templayout;
    private TextView textview_attention;
    private ProgressBar attentionBar;

    RequestQueue mQueue ;
    ECGview ecg ;
    private GetDataThread getDataThread;

    private GoogleMap mMap;                  //GoogleMap物件
    private LocationManager locationManager; //位置管理員 負責開啟GPS定位....之類
    private Geocoder geocoder;               //將地址轉成經緯度

    private BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
    private BluetoothSocket btSocket = null;
    private OutputStream outStream;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");    //藍芽編號 必寫且固定
    private static String address = "98:D3:31:90:1A:39";     //欲連線之藍芽位置，目前寫死將來會可選取
    private TGDevice tgDevice ;

    //------------------------------------------------------抓取腦波數值的副程式
    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case TGDevice.MSG_STATE_CHANGE:

                    switch (msg.arg1) {
                        case TGDevice.STATE_IDLE:
                            break;
                        case TGDevice.STATE_CONNECTING:
                            textview_attention.setText("Connecting...");
                            break;
                        case TGDevice.STATE_CONNECTED:
                            textview_attention.setText("Connected.");
                            tgDevice.start();
                            break;
                        case TGDevice.STATE_NOT_FOUND:
                            textview_attention.setText("Can't find");
                            break;
                        case TGDevice.STATE_NOT_PAIRED:
                            textview_attention.setText("not paired");
                            break;
                        case TGDevice.STATE_DISCONNECTED:
                            textview_attention.setText("Disconnected");
                    }
                    break;
                case TGDevice.MSG_POOR_SIGNAL:
                    if(msg.arg1>0) {
                        textview_attention.setText("Missing data : " + msg.arg1);
                    }
                    break;

                case TGDevice.MSG_ATTENTION:
                    attentionBar.setProgress(msg.arg1);
                    UserStatus(msg.arg1);
                    break;

                default:
                    break;
            }
        }
    };


    Handler bmpHandler = new Handler() {
        public void handleMessage(Message m) {      //用Handler處理傳入進來心跳數
            bpmAverage.setText(m.getData().getString("bpm","Err"));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        templayout = new LinearLayout(MainActivity.this);
        templayout.setOrientation(LinearLayout.HORIZONTAL);
        scroll_mainBottom = (HorizontalScrollView) findViewById(R.id.scroll_mainBottom);
        View v = LayoutInflater.from(MainActivity.this).inflate(R.layout.ecg,null);
        templayout.addView(v);
        v = LayoutInflater.from(MainActivity.this).inflate(R.layout.attention,null);
        templayout.addView(v);
        scroll_mainBottom.addView(templayout);

        //開始執行ECGdraw的動作，並新增LAYOUT
        ecg = new ECGview(MainActivity.this);
        linearLayout = (LinearLayout)findViewById(R.id.DrawLayout);
        linearLayout.addView(ecg);

        tgDevice = new TGDevice(bluetoothAdapter, handler);
        geocoder = new Geocoder(MainActivity.this, Locale.TRADITIONAL_CHINESE);
        mQueue = Volley.newRequestQueue(this);

        btn_connectBT = (Button) findViewById(R.id.btn_connectBT);
        bpmAverage = (TextView) findViewById(R.id.bpmAverage);
        peak_iamge = (ImageView) findViewById(R.id.peak_image);
        textview_attention = (TextView) findViewById(R.id.textview_attention);
        connectBrain = (LinearLayout) findViewById(R.id.connect_brain);
        attentionBar = (ProgressBar) findViewById(R.id.progressBar);
        attentionBar.setProgressDrawable(getDrawable(R.drawable.progress_font));
        connectBrain.setOnClickListener(clickListener);
        btn_connectBT.setOnClickListener(clickListener);
        peak_iamge.setOnClickListener(clickListener);


        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_connectBT:
                    try {
                        BTConnect();
                        startStream();
                        getDataThread = new GetDataThread(btSocket,ecg,bmpHandler);
                    } catch (IOException e) {
                        Toast.makeText(MainActivity.this, "藍芽連接失敗", Toast.LENGTH_SHORT);
                        Log.e("connectBT", e.toString());
                    }

                    break;
                case R.id.peak_image:
                    final String[] level = new String[]{"準位上升", "準位下降", "訊號放大", "訊號縮小"};
                    AlertDialog.Builder dialog_list = new AlertDialog.Builder(MainActivity.this);
                    dialog_list.setTitle("心電訊號校正");
                    dialog_list.setItems(level, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which) {
                                case 0:
                                    ecg.setLevel(-50);
                                    break;
                                case 1:
                                    ecg.setLevel(50);
                                    break;
                                case 2:
                                    ecg.setCompression(-1);
                                    break;
                                case 3:
                                    ecg.setCompression(1);
                                    break;
                            }
                            Toast.makeText(MainActivity.this, "準位校正" + level[which], Toast.LENGTH_SHORT).show();
                        }
                    });

                    dialog_list.show();
                    break;

                case R.id.connect_brain:
                    if (tgDevice.getState() != TGDevice.STATE_CONNECTING
                            && tgDevice.getState() != TGDevice.STATE_CONNECTED)
                        tgDevice.connect(true);

                    break;
            }
        }
    };

    private void UserStatus(int attention) {
            textview_attention.setText("專注力 "+ attention+"%");
    }

    @Override
    public void onStart() {
        super.onStart();

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, 1);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);    //取得系統定位服務
        mMap.setMyLocationEnabled(true);  //是否顯示現在位置

        new Locate(getLayoutInflater().inflate(R.layout.infowindow,null),mMap,mQueue,geocoder);

        locationManager =  (LocationManager)getSystemService(LOCATION_SERVICE);
        locationManager.getProvider(LocationManager.GPS_PROVIDER);

        UiSettings uiSettings = mMap.getUiSettings();
        // Add a marker in Sydney and move the camera
        uiSettings.setMyLocationButtonEnabled(true);
    }



    /*************************藍芽連線*************************/
    private void BTConnect() throws IOException{
        btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
        btSocket.connect();
        if(!btSocket.isConnected())
            throw new IOException();

        btn_connectBT.setEnabled(false);
        btn_connectBT.setText("已連線");

    }

    private void startStream() throws IOException{
        outStream = btSocket.getOutputStream();
        outStream.write("S".getBytes());
    }
}



