package com.example.aweia.map3;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.media.ExifInterface;
import android.os.CountDownTimer;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by aweia on 2016/12/12.
 */
public class Locate {
    private View markView ;
    private GoogleMap mMap ;
    private Geocoder geocoder ;
    private RequestQueue mQueue ;
    private String IP = "192.168.43.227";


    //倒數計時器，2分鐘問一次Server是否有資料能下載
    CountDownTimer cTimer = new CountDownTimer(1000*30, 1000) {
        public void onTick(long millisUntilFinished) {}

        public void onFinish() {
            mQueue.add(new StringRequest(Request.Method.POST, "http://"+IP+"/search_image/Phone_Whether_Data.php",
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


    public Locate(View markView,GoogleMap mMap,RequestQueue mQueue,Geocoder geocoder){
        this.markView = markView ;
        this.mMap = mMap ;
        this.geocoder = geocoder ;
        this.mQueue = mQueue ;
        //將計時器丟到多執行緒執行
        new TimerDown(cTimer).start();
    }



    /*************************************透過網路下載圖片**********************************/

    private void dowload_Image(){
        mQueue.add(new StringRequest(Request.Method.POST, "http://"+IP+"/search_image/Send_Image.php",
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
            final MarkerOptions markerOptions = new MarkerOptions();

            mMap.setInfoWindowAdapter(new CMuseumAdapter(markView));

            ((ImageView) markView.findViewById(R.id.src_img)).setImageBitmap(BitmapFactory.decodeFile(path));

            //設定經緯度
            ((TextView) markView.findViewById(R.id.Marker_longitude)).setText("經:" + alt[0]);
            ((TextView) markView.findViewById(R.id.Marker_latitude)).setText("緯:" + alt[1]);

            //設定經緯度抓到的名稱
            List<Address> lstAddress = geocoder.getFromLocation(alt[0],alt[1],1);
            ((TextView)markView.findViewById(R.id.Marker_name)).setText(lstAddress.get(0).getAddressLine(0));
            markerOptions.position(new LatLng(alt[0], alt[1]));

            //設定日期
            Date date = new Date();
            ((TextView) markView.findViewById(R.id.date_text)).setText(
                    date.getHours() + ":" + date.getMinutes());

            //設定原始小圖片
            mQueue.add(new ImageRequest("http://"+IP+"/search_image/Send_Src_Image.php",
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
