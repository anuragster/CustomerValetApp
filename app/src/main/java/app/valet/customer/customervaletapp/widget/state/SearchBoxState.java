package app.valet.customer.customervaletapp.widget.state;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by ASAX on 22-05-2016.
 */
public class SearchBoxState {
    private LatLng latLng;
    private String address;

    public SearchBoxState(LatLng latLng, String address){
        this.latLng = latLng;
        this.address = address;
    }

    public LatLng getLatLng(){
        return this.latLng;
    }

    public void setLatLng(LatLng latlng){
        this.latLng = latlng;
    }

    public String getAddress(){
        return this.address;
    }

    public void setAddress(String address){
        this.address = address;
    }

    @Override
    public String toString(){
        return "LatLng - " + this.latLng + ", and address - " + this.address;
    }
}
