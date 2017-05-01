package com.example.aweia.map3;

import android.util.Log;

/**
 * Created by aweia on 2016/12/12.
 */
public class Heart {

    public String getbpm(int[] data, int uselessData) {
        if (uselessData < 950) {
            Log.d("Heart", "uselessData = " + uselessData);
            return "Err";
        } else {
            return peak_detect(data);
        }
    }


    String peak_detect(int in[]) {
        final int N = 2000;                //需要計算的心跳資料筆數 2048約15s 800約6s
        final int sample_freq = 200;    //每秒的點數
        final int P = 100;             //可接受的Peak的最大數

        int cnt_peak = 0;
        int n;
        int temp_index;
        double temp1, temp2;
        int i, j, k, m;
        int FirstPeak = 0;
        int LastPeak = 0;

        double X1[];
        double X2[];
        double X4[];
        int X5[] = new int[N];          //PEAK的分散狀態，0→1代表有 peak
        int R[];                       //儲存有可能是PEAK的資料位置
        int R_t[];                     //儲存有可能是PEAK的資料位置
        int R_site[] = new int[P];
        double M[];
        double com_max[] = new double[2];

        for (i = 0; i < 25; i++)
            R_site[i] = 0;


        /***********************X1處理**************************/
        X1 = new double[N];
        X1[0] = in[0];
        for (i = 1; i < N - 1; i++)
            X1[i] = (in[i - 1] + 2 * in[i] + in[i + 1]) / 4.0;

        X1[N - 1] = in[N - 1];
        /*******************************************************/

        /***********************X2處理**************************/
        n = 2;
        X2 = new double[N];
        M = new double[2 * n + 1];

        for (i = 0; i < (2 * n + 1); i++)
            M[i] = X1[i];

        for (k = 0; k < N; k++) {
            if (k < n || k + n >= N)
                X2[k] = X1[k];
            else {
                M[(k + n) % (2 * n + 1)] = X1[k + n];
                X2[k] = median(M, n);
            }
        }

        n = 15;
        M = new double[2 * n + 1];

        for (i = 0; i < (2 * n + 1); i++)
            M[i] = X2[i];
        for (k = n; k < N - n; k++) {
            M[(k + n) % (2 * n + 1)] = X2[k + n];
            X2[k] = median(M, n);
        }
        /*******************************************************/

        /***********************X4處理**************************/
        X4 = new double[N];

        m = 3;
        for (i = 0; i < N; i++) {
            temp1 = 0;
            if (i < m) {
                for (j = 0; j <= i + m; j++) {
                    temp2 = (X2[j] - X1[j]) / 4;
                    temp1 = temp1 + temp2 * temp2;
                }
            } else {
                if ((i + m) >= N) {
                    for (j = i - m; j < N; j++) {
                        temp2 = (X2[j] - X1[j]) / 4;
                        temp1 = temp1 + temp2 * temp2;
                    }
                } else {
                    for (j = i - m; j <= i + m; j++) {
                        temp2 = (X2[j] - X1[j]) / 4;
                        temp1 = temp1 + temp2 * temp2;
                    }
                }


            }
            X4[i] = temp1;
        }
        /*******************************************************/

        /***********************X5處理**************************/
        temp2 = 0;
        for (j = 0; j < sample_freq / 2; j++)
            temp2 = temp2 + X4[j];

        temp1 = sample_freq / 2;
        com_max[1] = 100.0;
        temp_index = 0;
        for (i = 0; i < N; i++) {

            if (i < sample_freq / 2) {
                temp2 = temp2 + X4[i + sample_freq / 2];
                temp1 = temp1 + 1;
            } else {
                if (i + sample_freq / 2 >= N) {
                    temp2 = temp2 - X4[i - sample_freq / 2];
                    temp1 = temp1 - 1;
                } else {
                    temp2 = temp2 + X4[i + sample_freq / 2] - X4[i - sample_freq / 2];
                }
            }
            //threshold_value = temp2/temp1;
            com_max[0] = temp2 / temp1;//threshold_value;

            if (X4[i] >= Max(com_max, 2))
                X5[i] = 1;
            else
                X5[i] = 0;

            if (i > 0 && (X5[i] < X5[i - 1]))
                temp_index = i;

            if (i > 0 && (X5[i] > X5[i - 1]))
                if ((i - temp_index) < 10)
                    for (j = temp_index; j <= i; j++)
                        X5[j] = 1;
        }

        //尋找peak的位置。當X5的資料由0→1或1→0，有可能是PEAK，因此將資料的位置儲存在R[]裡面
        j = 0;
        R = new int[P];
        for (i = 0; i < N; i++) {
            if (i > 0 && (X5[i] + X5[i - 1]) == 1) {
                R[j] = i;                             //639
                j = j + 1;
            }
        }
        /*******************************************************/

        k = 0;
        R_t = new int[P];
        for (i = 0; i < j / 2; i++) {
            temp_index = R[2 * i + 1] - R[2 * i] + 1;
            M = new double[temp_index];

            for (m = 0; m < temp_index; m++)
                M[m] = in[R[2 * i] + m];

            double MAX = Max(M, temp_index);

            m = 0;
            while (true) {
                if (in[R[2 * i] + m] == MAX)
                    break;
                else
                    m = m + 1;
            }
            R_t[k] = R[2 * i] + m;
            k = k + 1;
        }

        if (j > 0) {
            R_site[0] = R_t[0];
            cnt_peak = 1;

            for (m = 1; m < k; m++) {
                if (R_t[m] - R_t[m - 1] > 70) {     //判斷peak與peak之間需有多大的空間，避免將雜訊與雜訊之間也當成是peak
                    if (cnt_peak == 1)
                        FirstPeak = R_t[m];
                    R_site[cnt_peak] = R_t[m];
                    cnt_peak = cnt_peak + 1;
                    LastPeak = R_t[m];
                } else {
                    if (in[R_t[m]] > in[R_site[temp_index - 1]])
                        R_site[cnt_peak - 1] = R_t[m];
                }
            }
        }


        double bpm = Math.round((double) 60 * 200 * (cnt_peak - 1) / (LastPeak - FirstPeak) * 10) / 10.0;
        if (bpm < 60 || bpm > 160)
            return "Err";

        return bpm + "";

    }

    double median(double M[], int n) {
        double T[] = new double[n * 2 + 1];
        double temp;

        for (int i = 0; i < 2 * n + 1; i++)
            T[i] = M[i];

        for (int i = 0; i < 2 * n + 1; i++) {
            for (int j = i; j < 2 * n + 1; j++) {
                if (T[j] < T[i]) {
                    temp = T[j];
                    T[j] = T[i];
                    T[i] = temp;
                }
            }
        }
        temp = T[n];
        return temp;
    }

    double Max(double temp[], int n) {
        double A;
        double MAX = 0;
        int k;

        for (k = 0; k < n; k++) {
            A = temp[k];
            if (MAX < A) {
                MAX = A;
            }
        }
        return MAX;
    }
}