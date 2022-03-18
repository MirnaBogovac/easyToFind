package com.easy.it.find.easytofind02.helper_classes;

/**
 * Created by ante on 2/19/16.
 */
public class BooleanFlagContainer {
    private boolean locationChangedOnStartUp;
    private boolean markersImportedAndReady;
    private boolean markersShownOnZoom;
    private static boolean gpsDialogAlreadyShown;

    public BooleanFlagContainer() {
        locationChangedOnStartUp = false;
        markersShownOnZoom = false;
        markersImportedAndReady = false;
    }

    public boolean isLocationChangedOnStartUp() {
        return locationChangedOnStartUp;
    }

    public boolean areMarkersImportedAndReady() {
        return markersImportedAndReady;
    }

    public boolean areMarkersShownOnZoom() {
        return markersShownOnZoom;
    }

    public boolean isGpsDialogAlreadyShown() {
        return gpsDialogAlreadyShown;
    }

    public void setLocationChangedOnStartUpFlag(boolean value) {
        locationChangedOnStartUp = value;
    }

    public void setMarkersImportedAndReadyFlag(boolean value) {
        markersImportedAndReady = value;
    }

    public void setMarkersShownOnZoomFlag(boolean value) {
        markersShownOnZoom = value;
    }

    public void setGpsDialogAlreadyShown(boolean value) {
        gpsDialogAlreadyShown = value;
    }

}
