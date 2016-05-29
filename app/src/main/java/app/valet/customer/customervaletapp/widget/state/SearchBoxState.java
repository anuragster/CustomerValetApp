package app.valet.customer.customervaletapp.widget.state;

import java.io.Serializable;

/**
 * Created by ASAX on 22-05-2016.
 */
public class SearchBoxState implements Serializable{
    //private LatLng latLng;
    private double latitude;
    private double longitude;
    private String address;

    public SearchBoxState(double latitude, double longitude, String address){
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
    }

    public double getLatitude(){
        return this.latitude;
    }

    public void setLatitude(double latitude){
        this.latitude = latitude;
    }

    public double getLongitude(){
        return this.longitude;
    }

    public void setLongitude(double longitude){
        this.longitude = longitude;
    }

    public String getAddress(){
        return this.address;
    }

    public void setAddress(String address){
        this.address = address;
    }

    @Override
    public String toString(){
        return "LatLng - (lat -" + this.latitude+ " , lng - " + this.longitude+ "), and address - " + this.address;
    }
}
