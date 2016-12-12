package com.example.aweia.map3;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;

import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;


/*******************Http Volley******************/
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
/**********************************************/

import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;
import java.util.UUID;

import android.os.Handler;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback {


    /*******************LAYOUT*********************/
    private Button btn_connectBT;              //藍芽連線的按鈕
    private TextView bpmAverage;            //顯示平均心跳數
    private LinearLayout linearLayout;
    private ImageView bmp_image;
    private ImageView peak_iamge;
    /**********************************************/

    /*******************Internet********************/
    RequestQueue mQueue ;
    /**********************************************/

    /*******************DrawECG********************/
    ECGview ecg ;
    /**********************************************/
    private Heart heart;


    /*******************MAP追蹤協尋************/
    private GoogleMap mMap;                  //GoogleMap物件
    private LocationManager locationManager; //位置管理員 負責開啟GPS定位....之類
    private Location Here;                   //儲存現在的位置
    private Geocoder geocoder;               //將地址轉成經緯度
    /******************************************/

    /*******************BT*********************/
    private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
    private BluetoothSocket btSocket = null;
    private OutputStream outStream;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");    //藍芽編號 必寫且固定
    private static String address = "98:D3:31:90:1A:39";     //欲連線之藍芽位置，目前寫死將來會可選取
    /******************************************/

    Handler bmpHandler = new Handler() {
        public void handleMessage(Message m) {      //用Handler處理傳入進來心跳數
            Log.d("MainActivity","handleMessage");
            if(m.what==0) {
                try {

                    bpmAverage.setText(heart.getbpm());
                    } catch (Exception e) {
                    Log.e("FormatError", e.toString());

                }
            }
            else
                bpmAverage.setText("Err");
        }
    };

    /**********************************************/


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
//        requestPermissions(INITIAL_PERMS, 1337);
//        requestPermissions(WRITE_STORAGE, 1338);

        //開始執行ECGdraw的動作，並新增LAYOUT
        ecg = new ECGview(MainActivity.this);
        linearLayout = (LinearLayout)findViewById(R.id.DrawLayout);
        linearLayout.addView(ecg);

        
        geocoder = new Geocoder(MainActivity.this, Locale.TRADITIONAL_CHINESE);
        mQueue = Volley.newRequestQueue(this);

        btn_connectBT = (Button) findViewById(R.id.btn_connectBT);
        bpmAverage = (TextView) findViewById(R.id.bpmAverage);
        peak_iamge = (ImageView) findViewById(R.id.peak_image);
        bmp_image = (ImageView) findViewById (R.id.bmp_image);

        btn_connectBT.setOnClickListener(clickListener);
        peak_iamge.setOnClickListener(clickListener);


        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.btn_connectBT:
                    if (!mBluetoothAdapter.isEnabled()) {
                        mBluetoothAdapter.enable();
                    }   //檢查使用者有沒有開啟藍芽，沒有則自動開啟
                    try {
                        btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
                        if (!btSocket.isConnected()) {
                            if (BTConnect()) {
                                outStream = btSocket.getOutputStream();
                                outStream.write("S".getBytes());

                                btn_connectBT.setEnabled(false);
                                btn_connectBT.setText("已連線");

                                heart = new Heart(btSocket, ecg, bmpHandler);
                            }
                        }
                    } catch (IOException e) {

                        Log.e("Bt_button", e.toString() + "Line:" + e.getStackTrace()[0].getLineNumber());
                    }

                    break;
                case R.id.peak_image:
                    final String[] level = new String[]{"準位上升", "準位下降", "訊號放大", "訊號縮小"};
                    AlertDialog.Builder dialog_list = new AlertDialog.Builder(MainActivity.this);
                    dialog_list.setTitle("心電訊號校正");
                    dialog_list.setItems(level, new DialogInterface.OnClickListener() {
                        @Override
                        //只要你在onClick處理事件內，使用which參數，就可以知道按下陣列裡的哪一個了
                        public void onClick(DialogInterface dialog, int which) {
                            switch(which) {
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


                            Toast.makeText(MainActivity.this, "準位校正" + level[which] , Toast.LENGTH_SHORT).show();

                        }
                    });
                    dialog_list.show();
                    break;
            }

        }
    };


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
    public boolean BTConnect() {
        try {
            btSocket.connect();
        } catch (IOException e) {
            Toast.makeText(MainActivity.this,"藍芽連接失敗",Toast.LENGTH_SHORT).show();
            try {
                btSocket.close();
            } catch (IOException e2) {
                Toast.makeText(MainActivity.this,"藍芽連接失敗",Toast.LENGTH_SHORT).show();
            }
        } catch (Exception ex){
            Log.e("BT connect",ex.toString() +"  Line:"+ex.getStackTrace()[0].getLineNumber());
        }
        return btSocket.isConnected();
    }
}
