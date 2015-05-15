package com.bye.heyyou.heyyou.user;

public class LocalUser extends User{
    private double distanceInMeters;
    private double accuracy;
    public LocalUser(String userId, double distanceInMeters, double accuracy){
        super(userId);
        this.distanceInMeters = distanceInMeters;
        this.accuracy=accuracy;
    }
    public double getDistanceInMeters() {
        return distanceInMeters;
    }

    public double getAccuracy() {
        return accuracy;
    }
}
