package com.example.aweia.map3;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;

import android.media.ExifInterface;
import android.os.CountDownTimer;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.toolbox.ImageRequest;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;


/*******************Http Volley******************/
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
/**********************************************/

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.Map;

import android.os.Handler;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback {

    /*******************MAP追蹤協尋*********************/
    private GoogleMap mMap;                  //GoogleMap物件
    private LocationManager locationManager; //位置管理員 負責開啟GPS定位....之類
    MarkerOptions markerOptions ;
    private Location Here;                   //儲存現在的位置
    private Geocoder geocoder;               //將地址轉成經緯度
    List < Address > destaddresses;
    /******************************************/

    /*******************BT*********************/
    private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
    private BluetoothSocket btSocket = null;
    private OutputStream outStream;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");    //藍芽編號 必寫且固定
    private static String address = "98:D3:31:90:1A:39";     //欲連線之藍芽位置，目前寫死將來會可選取


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


    /*******************LAYOUT*********************/
    private Button Connect_Bt;              //藍芽連線的按鈕
    private TextView bpmAverage;            //顯示平均心跳數
    private LinearLayout linearLayout;
    private ImageView bmp_image;
    private ImageView peak_iamge;
    View markView;                          //標記點用的View
    /**********************************************/

    /*******************Internet********************/
    RequestQueue mQueue ;
    /**********************************************/

    /*******************DrawECG********************/
    ECGview ecg ;
    private CountDownTimer cTimer;
    /**********************************************/
    private Heart heart;


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



        //一些Layout的初始化設定
        geocoder = new Geocoder(MainActivity.this, Locale.TRADITIONAL_CHINESE);
        bpmAverage = (TextView) findViewById(R.id.bpmAverage);
        mQueue = Volley.newRequestQueue(this);

        //藍芽按鈕的事件
        Connect_Bt = (Button) findViewById(R.id.Connect_Bt);
        Connect_Bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mBluetoothAdapter.isEnabled()) {
                    mBluetoothAdapter.enable();
                }   //檢查使用者有沒有開啟藍芽，沒有則自動開啟
                try {
                    btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
                    if (!btSocket.isConnected()) {
                        if (BTConnect()) {
                            outStream = btSocket.getOutputStream();
                            outStream.write("S".getBytes());

                            Connect_Bt.setEnabled(false);
                            Connect_Bt.setText("已連線");

                            heart = new Heart(btSocket,ecg,bmpHandler);
                        }
                    }
                } catch (IOException e) {

                    Log.e("Bt_button", e.toString() + "Line:" + e.getStackTrace()[0].getLineNumber());
                }

            }
        });

        peak_iamge = (ImageView) findViewById(R.id.peak_image);
        peak_iamge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
            }
        });

        bmp_image = (ImageView) findViewById (R.id.bmp_image);


        //倒數計時器，2分鐘問一次Server是否有資料能下載
        cTimer = new CountDownTimer(1000*30, 1000) {
            public void onTick(long millisUntilFinished) {
            }
            public void onFinish() {
                mQueue.add(new StringRequest(Request.Method.POST, "http://192.168.43.227/search_image/Phone_Whether_Data.php",
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                Log.d("can get data?", "=" + response + response.length());
                                if (response.substring(0, 4).equals("true"))
                                    dowload_Image();

                            }
                        }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("dowload_Image_Response", error.toString());
                    }
                }) {
                    @Override
                    protected Map<String, String> getParams() {
                        //在這裡設置需要post的参数
                        Map<String, String> params = new HashMap<String, String>();
                        params.put("data", "false");
                        return params;
                    }
                });

                cTimer.start();
                }
            };
        //將計時器丟到多執行緒執行
        new TimerDOwn(cTimer).start();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
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


        Here = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);  //取得現在位置

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



    /*************************************透過網路下載圖片**********************************/

    private void dowload_Image(){
        mQueue.add(new StringRequest(Request.Method.POST, "http://192.168.43.227/search_image/Send_Image.php",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Base64_To_Image(response);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("dowload_Image_Response", error.toString());
            }
        }));
    }

    private void Base64_To_Image(String image64) {
        //讀取圖片詳細資料所需的物件
        ExifInterface exif ;

        //設定檔案名稱(filename) 和 存放位置(path)
        String filename = new Date()+".JPEG";
        //String path = Environment.getExternalStorageDirectory() + "/Pictures/test";
        String path = "/sdcard/PICTURES/test";
        File dest = new File(path, filename);

        //將圖片檔案儲存下來
        try {
            //將base64編碼 轉成byte直接output成jpeg檔案；如此一來圖片便不會被壓縮可保留EXIF
            FileOutputStream out = new FileOutputStream(dest);
            byte[] decodeStr = Base64.decode(image64, Base64.DEFAULT);
            out.write(decodeStr);

            out.flush(); //反正這行很重要
            out.close();
        } catch(Exception ex){
            Log.e("Open Image",ex.toString() + "Line:" + ex.getStackTrace()[0].getLineNumber());
        }

        //開啟圖片檔案並讀取exif
        try {
            float altitude[] = new float[2];
            exif = new ExifInterface(path +"/"+filename);
            exif.getLatLong(altitude);
            Log.d("EXIF", "lat =" + altitude[0] + ";alt =" + altitude[1]);

            CreateDialog(altitude, path + "/" + filename);
        } catch (Exception ex){
            Log.e("Get Exif",ex.toString());
        }
    }
    public void CreateDialog(float alt[],String path){
        // new tracking().start();
        try {
            markerOptions = new MarkerOptions();

            markView = getLayoutInflater().inflate(R.layout.infowindow, null);
            mMap.setInfoWindowAdapter(new CMuseumAdapter(markView));

            ((ImageView) markView.findViewById(R.id.src_img)).setImageBitmap(BitmapFactory.decodeFile(path));

            //設定經緯度
            ((TextView) markView.findViewById(R.id.Marker_longitude)).setText("經:" + alt[0]);
            ((TextView) markView.findViewById(R.id.Marker_latitude)).setText("緯:" + alt[1]);

            //設定經緯度抓到的名稱
            Geocoder gc = new Geocoder(MainActivity.this, Locale.TRADITIONAL_CHINESE);
            List<Address> lstAddress = gc.getFromLocation(alt[0],alt[1],1);
            ((TextView)markView.findViewById(R.id.Marker_name)).setText(lstAddress.get(0).getAddressLine(0));
            markerOptions.position(new LatLng(alt[0], alt[1]));

            //設定日期
            Date date = new Date();
            ((TextView) markView.findViewById(R.id.date_text)).setText(
                    date.getHours() + ":" + date.getMinutes());

            //設定原始小圖片
            mQueue.add(new ImageRequest("http://192.168.43.227/search_image/Send_Src_Image.php",
                    new Response.Listener<Bitmap>() {
                        @Override
                        public void onResponse(Bitmap response) {
                            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(response));
                            mMap.addMarker(markerOptions);

                            Log.d("CreateDialog", "Create Successful");
                            //  ((ImageView) markView.findViewById(R.id.src_img)).setImageBitmap(response);
                        }
                    }, 0, 0, null,
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.e("dowload_Image_Response", error.toString());
                        }
                    }));
/*
                String url = getURL();
                DownloadTask downloadTask = new DownloadTask();
                downloadTask.execute(url);
*/
        } catch (Exception e) {
            Log.e("CreateDialog",e.toString()+"Line:"+e.getStackTrace()[0].getLineNumber());
        }
    }





}
