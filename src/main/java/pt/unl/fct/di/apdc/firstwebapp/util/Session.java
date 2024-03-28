package pt.unl.fct.di.apdc.firstwebapp.util;

import java.util.UUID;

public class Session {

    public String city,country,host,ip,latLong,region;
    public long creationDate,expirationDate;

    public Session(){}
    public Session(String city,String country,String host, String ip, String latLong, String region, long creationDate, long expirationDate){
        this.city = city;
        this.country = country;
        this.host = host;
        this.ip = ip;
        this.latLong = latLong;
        this.region = region;
        this.creationDate = creationDate;
        this.expirationDate = expirationDate;
    }
}
