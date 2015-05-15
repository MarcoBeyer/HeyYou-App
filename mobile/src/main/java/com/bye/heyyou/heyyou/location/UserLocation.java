package com.bye.heyyou.heyyou.location;

/**
 * UserLocation is a Location of a user specified by its longitude, latitude and the accuracy of this position
 */
public class UserLocation {
    private double accuracy;
    private double longitude;
    private double latitude;

    /**
     * creates a UserLocation with the given longitude latitude and accuracy
     * @param longitude longitude of the users position
     * @param latitude latitude of the users position
     * @param accuracy accuracy of the users position
     */
    public UserLocation(double longitude, double latitude, double accuracy){
        this.latitude=latitude;
        this.longitude=longitude;
        this.accuracy= accuracy;

    }

    /**
     * returns the accuracy of the users position
     * @return returns the accuracy of the users position
     */
    public double getAccuracy() {
        return accuracy;
    }

    /**
     * returns the longitude of the users position
     * @return longitude of the users position
     */
    public double getLongitude() {
        return longitude;
    }

    /**
     * returns the latitude of the users position
     * @return the latitude of the users position
     */
    public double getLatitude() {
        return latitude;
    }
}
