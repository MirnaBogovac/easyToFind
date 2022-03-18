package com.easy.it.find.easytofind02.helper_classes;

/**
 * Created by ante on 1/10/16.
 */
public class Place {
    private String location_id;
    private String location_date;
    private String location_name;
    private String category_id;
    private String address;
    private String location_image;
    private double latitude;
    private double longitude;
    private String phone;
    private String website;
    private String mail;
    private String description;

    /**
     * Constructor
     */
    public Place() {}


/*********************************************************************************************
 ************************************* GETTERS **********************************************
 **********************************************************************************************/

    public String getId() {
        return this.location_id;
    }

    public String getLocation_date() {
        return this.location_date;
    }

    public String getName() {
        return this.location_name;
    }

    public String getCategory_id() {
        return this.category_id;
    }

    public String getAddress() {
        return this.address;
    }

    public String getImage() {
        return this.location_image;
    }

    public double getLatitude() {
        return this.latitude;
    }

    public double getLongitude() {
        return this.longitude;
    }

    public String getPhone() {
        return this.phone;
    }

    public String getWebsite() {
        return this.website;
    }

    public String getMail() {
        return this.mail;
    }

    public String getDescription() {
        return this.description;
    }

    @Override
    public boolean equals(Object otherPlace) {
        if(otherPlace instanceof Place) {
            return this.location_id.equals(((Place)otherPlace).location_id);
        }
        return false;
    }
}
