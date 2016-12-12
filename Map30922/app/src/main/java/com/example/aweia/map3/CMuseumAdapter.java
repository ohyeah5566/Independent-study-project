package com.example.aweia.map3;


import android.view.View;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;

class CMuseumAdapter implements GoogleMap.InfoWindowAdapter
{
    private View view;
    public CMuseumAdapter(View view){
        this.view = view;
    }

    @Override
    public View getInfoContents(Marker m)
    {
        View infoWindow = view;

        return infoWindow;
    }

    @Override
    public View getInfoWindow(Marker m)
    {
        return null;
    }
}