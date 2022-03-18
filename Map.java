package com.easy.it.find.easytofind02;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.MatrixCursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.Toast;

import com.easy.it.find.easytofind02.helper_classes.BooleanFlagContainer;
import com.easy.it.find.easytofind02.helper_classes.Category;
import com.easy.it.find.easytofind02.helper_classes.MyCursorAdapter;
import com.easy.it.find.easytofind02.helper_classes.Place;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;


public class Map extends AppCompatActivity {

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private HashMap<String, Category> categories;
    private HashMap<Marker, Place> placeHashMap;
    private BooleanFlagContainer booleanFlagContainer;

    private PopupMenu categoryDropDownFilter;
    private MyCursorAdapter mAdapter;

    final private int REQUEST_CODE_ASK_PERMISSIONS = 123;

    /**
     * Sets up Google maps. If network is available, imports everything from server
     * and shows an "Enable location" popup dialog. Otherwise it imports everything
     * that was previously stored in memory.
     * @param savedInstanceState -
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        setUpMapIfNeeded();

        placeHashMap = new HashMap<>();
        categories = new HashMap<>();
        booleanFlagContainer = new BooleanFlagContainer();

        if(isNetworkAvailable()) {

            importCategoriesOnline();

            if(Build.VERSION.SDK_INT >= 23) {
                showGpsEnableDialogIfNeededHigherSDK();
            }
            else {
                showGpsEnableDialogIfNeeded();
            }

        } else {
            toast(getResources().getString(R.string.network_unavailable));
            importCategoriesOffline();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onPause() {
        storeData();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    /**
     * Sets up the map if it is possible to do so.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
                // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera.
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     * The method enables app to use location if it is enabled by user. A location change
     * listener is implemented which takes the camera to the users location (this is done
     * only once). Also, on info window click listener is implemented which opens a new
     * activity which displays a card of clicked places details.
     */
    private void setUpMap() {
        mMap.setMyLocationEnabled(true);
        if (mMap != null) {
            mMap.setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener() {
                @Override
                public void onMyLocationChange(Location arg0) {
                    if (!booleanFlagContainer.isLocationChangedOnStartUp()) {
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(arg0.getLatitude(),
                                arg0.getLongitude()), 15));
                        booleanFlagContainer.setLocationChangedOnStartUpFlag(true);
                    }
                }
            });

            mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
                @Override
                public void onInfoWindowClick(Marker marker) {
                    Intent placeCardIntent = new Intent(getApplicationContext(), PlaceCardActivity.class);
                    Gson gson = new Gson();
                    placeCardIntent.putExtra("place", gson.toJson(placeHashMap.get(marker)));
                    startActivity(placeCardIntent);
                }
            });
        }
    }

    /**
     * Method handles the creation of menu in action bar. It sets the filter dropdown menu,
     * and SearchView action listeners as well as view behaviour.
     * @param menu Menu which is being created
     * @return True if success, false otherwise
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_map, menu);

        final SearchView mSearchView = (SearchView)menu.findItem(R.id.menu_search).getActionView();
        setSearchViewIcon(mSearchView);
        setSearchViewExpandListener(menu, mSearchView);
        handleSearchViewActions(mSearchView);

        categoryDropDownFilter = new PopupMenu(this, menu.findItem(R.id.menu_search).getActionView());
        categoryDropDownFilter.getMenuInflater().inflate(R.menu.category_dropdown_menu, categoryDropDownFilter.getMenu());

        setForceShowIcon(categoryDropDownFilter);
        handleCategoryDropDownFilterActions();
        return true;
    }

    /**
     * Method is a click listener on menu Items
     * @param item Item which is clicked
     * @return true if request is successfully processed
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if(item.isCheckable()) {
            item.setChecked(true);
        }

        switch(id) {
            case(R.id.filter):
                categoryDropDownFilter.show();
                break;
            case(R.id.refresh):
                if(mMap.getCameraPosition().zoom > 12) {
                    importPlaces(mMap.getCameraPosition().target);
                } else {
                    toast(getResources().getString(R.string.zoom_level_insufficient));
                }
                break;
            case(R.id.map_type_normal):
                mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                break;
            case(R.id.map_type_terrain):
                mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                break;
            case(R.id.map_type_satellite):
                mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                break;
            case(R.id.map_type_hybrid):
                mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                break;
            case(R.id.contact_us):
                Intent contactUsIntent = new Intent(this, ContactUsActivity.class);
                startActivity(contactUsIntent);
                break;
            case(R.id.share):
                Intent iShare = new Intent(Intent.ACTION_SEND);
                iShare.setType("text/plain");
                iShare.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
                iShare.putExtra(Intent.EXTRA_TEXT,getResources().getString(R.string.download)+ getResources().getString(R.string.google_play_link));
                startActivity(Intent.createChooser(iShare, getResources().getString(R.string.share_service)));
                break;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }




/**********************************************************************************************
 *************************************PRIVATE METHODS******************************************
 **********************************************************************************************/

    /**
     * Method handles actions performed on the searchView item
     * on the action bar
     * @param mSearchView searchView item
     */
    private void handleSearchViewActions(final SearchView mSearchView) {

        setAutocomplete(mSearchView);

        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                MyGeocoder geocoder = new MyGeocoder();
                geocoder.execute(query);
                mSearchView.setQuery("", false);    //Reset text
                mSearchView.setIconified(true);     //Close keyboard and searchView
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (isNetworkAvailable()) {
                    Autocomplete autocomplete = new Autocomplete();
                    autocomplete.execute(newText, mMap.getCameraPosition().target);
                } else {
                    searchResultsFromStorage(newText);
                }
                return true;
            }
        });
    }

    /**
     * Method handles SearchView widgets behaviour when it is expanded and being closed.
     * @param menu menu in action bar
     * @param searchView android SearchView widget
     */
    private void setSearchViewExpandListener(final Menu menu, final SearchView searchView) {
        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                menu.findItem(R.id.refresh).setVisible(false);
                searchView.requestFocus();
            }
        });

        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                menu.findItem(R.id.refresh).setVisible(true);
                return false;
            }
        });
    }

    /**
     * Method sets the icon for SearchView widget, since it is not possible to
     * do so in the menu xml file. It also sets the hint icon and the close
     * search icon
     * @param mSearchView SearchView widget
     */
    private void setSearchViewIcon(SearchView mSearchView) {
        int searchImgId = getResources().getIdentifier("android:id/search_button", null, null);
        ImageView v = (ImageView) mSearchView.findViewById(searchImgId);
        v.setImageResource(R.drawable.ic_search_24dp);

        int searchTextViewId = mSearchView.getContext().getResources().getIdentifier("android:id/search_src_text",
                null, null);
        AutoCompleteTextView searchTextView = (AutoCompleteTextView) mSearchView.findViewById(searchTextViewId);

        SpannableStringBuilder ssb = new SpannableStringBuilder("   "); // for the icon
        ssb.append(getResources().getString(R.string.search_hint));
        Drawable searchIcon = ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_search_24dp);
        int textSize = (int) (searchTextView.getTextSize() * 1.25);
        searchIcon.setBounds(0, 0, textSize, textSize);
        ssb.setSpan(new ImageSpan(searchIcon), 1, 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        searchTextView.setHint(ssb);

        int closeButtonId = mSearchView.getContext().getResources().getIdentifier("android:id/search_close_btn",
                null, null);
        ImageView closeButton = (ImageView) mSearchView.findViewById(closeButtonId);
        closeButton.setImageResource(R.drawable.ic_close_24dp);
    }

    /**
     * Method handles autocomplete scenarios. It implements a CursorAdapter which displays
     * autocomplete items.
     * @param mSearchView SearchView item
     */
    private void setAutocomplete(final SearchView mSearchView)
    {
        mAdapter = new MyCursorAdapter(getApplicationContext(), null, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
        mSearchView.setSuggestionsAdapter(mAdapter);

        mSearchView.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
            @Override
            public boolean onSuggestionClick(int position) {
                mAdapter.getCursor().moveToPosition(position);
                findPlaceOnMap(mAdapter.getCursor().getString(mAdapter.getCursor().getColumnIndex("locationName")),
                        mAdapter.getCursor().getString(mAdapter.getCursor().getColumnIndex("address")));

                mSearchView.setQuery("", false);    //Reset text
                mSearchView.setIconified(true);
                return true;
            }

            @Override
            public boolean onSuggestionSelect(int position) {
                return true;
            }
        });
    }

    /**
     * Hides unchecked items and shows checked ones.
     */
    private void handleCategoryDropDownFilterActions() {
        categoryDropDownFilter.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.isCheckable()) {
                    if (item.isChecked()) {
                        hideUncheckedCategoryPlaces(item.getItemId());
                    } else {
                        showCheckedCategoryPlaces(item.getItemId());
                    }
                    item.setChecked(!item.isChecked());
                }
                categoryDropDownFilter.show();
                return true;
            }
        });
    }

    /**
     * Imports categories from server. Always called if server is available.
     */
    private void importCategoriesOnline() {
        ImportCategories importCategories = new ImportCategories();
        importCategories.execute();
    }

    /**
     * Imports categories from storage. Called if server is unreachable.
     */
    private void importCategoriesOffline() {
        ImportCategoriesFromStorage importCategories = new ImportCategoriesFromStorage();
        importCategories.execute();
    }

    private void populateCategoriesHashMap(Category[] categoriesArray) {
        for (Category aCategoriesArray : categoriesArray) {
            categories.put(aCategoriesArray.getCategory_id(), aCategoriesArray);
        }
    }

    /**
     * Imports places from server. Called if server is available, and markers are
     * imported and ready to use.
     */
    private void importPlaces(LatLng target) {
        if(booleanFlagContainer.areMarkersImportedAndReady()) {
            ImportPlaces places = new ImportPlaces();
            places.execute(target);
        } else {
            toast(getResources().getString(R.string.http_request_failure));
        }
    }

    /**
     * Imports places from storage. Called if server is unreachable.
     */
    private void importPlacesFromStorage() {
        if(booleanFlagContainer.areMarkersImportedAndReady()) {
            ImportPlacesFromStorage places = new ImportPlacesFromStorage();
            places.execute();
        } else {
            toast(getResources().getString(R.string.http_request_failure));
        }
    }

    /**
     * Imports a specific place clicked as a search suggestion.
     */
    private void importSpecificPlace(String locationName, String address) {
        if(booleanFlagContainer.areMarkersImportedAndReady()) {
            ImportPlace place = new ImportPlace();
            place.execute(locationName, address);
        } else {
            toast(getResources().getString(R.string.http_request_failure));
        }
    }

    /**
     * Imports markers from server and assigns them to categories objects.
     * Always called if server is available.
     */
    private void importMarkers() {
        ImportMarkers importMarkers = new ImportMarkers();
        importMarkers.execute();
    }

    /**
     * Imports markers from storage and assigns them to categories objects.
     * Called if server is unreachable.
     */
    private void importMarkersFromStorage() {
        ImportMarkersFromStorage importMarkers = new ImportMarkersFromStorage();
        importMarkers.execute();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

        return cm.getActiveNetworkInfo() != null &&
                cm.getActiveNetworkInfo().isConnectedOrConnecting();
    }

    /**
     * Method handles a HTTP request and returns a response in the form of a (JSON) string.
     * On any exception an empty string is returned.
     *
     * @param uri URI with which a server request is made
     * @return JSON string if a request is successful, empty string otherwise
     */
    private String handleHttpRequest(String uri) {
        HttpURLConnection conn = null;
        StringBuilder jsonResults = new StringBuilder();
        try {
            URL url = new URL(uri);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            InputStreamReader in = new InputStreamReader(conn.getInputStream());

            // Load the results into a StringBuilder
            int read;
            char[] buff = new char[1024];
            while ((read = in.read(buff)) != -1) {
                jsonResults.append(buff, 0, read);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return "";
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        return jsonResults.toString();
    }

    private void toast (String message) {
        Toast.makeText(getApplicationContext(),
                message,
                Toast.LENGTH_LONG).show();
    }

    private void hideUncheckedCategoryPlaces(int itemId) {
        for(java.util.Map.Entry<Marker, Place> entry : placeHashMap.entrySet()) {
            try {
                if (categories.get(entry.getValue().getCategory_id()).getSuper_category().toLowerCase()
                        .equals(getResources().getResourceEntryName(itemId).toLowerCase())) {
                    entry.getKey().setVisible(false);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void showCheckedCategoryPlaces(int itemId) {
        for (java.util.Map.Entry<Marker, Place> entry : placeHashMap.entrySet()) {
            try {
                if (categories.get(entry.getValue().getCategory_id()).getSuper_category().toLowerCase()
                        .equals(getResources().getResourceEntryName(itemId).toLowerCase())) {
                    entry.getKey().setVisible(true);
                }
            } catch (Exception e) {
                e.printStackTrace();
                entry.getKey().setVisible(true);
            }
        }
    }

    private void hideAllMarkers() {
        for (java.util.Map.Entry<Marker, Place> entry : placeHashMap.entrySet()) {
            entry.getKey().setVisible(false);
        }
    }

    /**
     * Show all markers for which their superCatergory is checked in the dropDown filter.
     * Otherwise hide them.
     */
    private void showAllMarkers() {
        for(int i = 0; i < categoryDropDownFilter.getMenu().size(); i++) {
            if(categoryDropDownFilter.getMenu().getItem(i).isChecked()) {
                showCheckedCategoryPlaces(categoryDropDownFilter.getMenu().getItem(i).getItemId());
            } else {
                hideUncheckedCategoryPlaces(categoryDropDownFilter.getMenu().getItem(i).getItemId());
            }
        }
    }

    /**
     * Finds and displays a place using the locations name and address.
     * @param locationName name of the location
     * @param address locations address
     */
    private void findPlaceOnMap(String locationName, String address) {
        for (java.util.Map.Entry<Marker, Place> entry : placeHashMap.entrySet()) {
            if(entry.getValue().getName().equals(locationName) &&
                    (entry.getValue().getAddress()).equals(address))
            {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(entry.getKey().getPosition(), 15));
                entry.getKey().showInfoWindow();
                return;
            }
        }

        importSpecificPlace(locationName, address);
    }

    private void setCameraListenerAndImportPlaces() {
        if(mMap != null) {
            mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
                @Override
                public void onCameraChange(CameraPosition cameraPosition) {
                    if (cameraPosition.zoom > 12) {
                        if (!booleanFlagContainer.areMarkersShownOnZoom()) {
                            showAllMarkers();
                            if(isNetworkAvailable()) {
                                importPlaces(cameraPosition.target);
                            }
                            booleanFlagContainer.setMarkersShownOnZoomFlag(true);
                        }
                    } else if (booleanFlagContainer.areMarkersShownOnZoom()) {
                        hideAllMarkers();
                        booleanFlagContainer.setMarkersShownOnZoomFlag(false);
                    }
                }
            });
        }
    }

    private void storeData() {
        if(categories.isEmpty() || placeHashMap.isEmpty()) {
            return;
        }

        Gson gson = new Gson();
        storeImages();

        SharedPreferences prefs = getSharedPreferences(
                getResources().getString(R.string.shared_prefs), Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.putString(getResources().getString(R.string.categories_shared_prefs),
                gson.toJson(categories.values().toArray()));
        editor.putString(getResources().getString(R.string.places_shared_prefs),
                gson.toJson(placeHashMap.values().toArray()));

        editor.apply();
    }

    private void storeImages() {
        for(Category category : categories.values()) {
            FileOutputStream out;
            try {
                out = getApplicationContext().openFileOutput(category.getCategory_id(), Context.MODE_PRIVATE);
                category.getMarkerIcon().compress(Bitmap.CompressFormat.PNG, 100, out);
                out.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Displays a popup dialog asking for users permission to use location if location is
     * not already enabled by user, or it is not possible to use location.
     */
    private void showGpsEnableDialogIfNeeded() {
        if(booleanFlagContainer.isGpsDialogAlreadyShown()){
            return;
        }

        final GoogleApiClient googleApiClient = new GoogleApiClient.Builder(getApplicationContext())
                .addApi(LocationServices.API).build();
        googleApiClient.connect();

        LocationRequest locationRequest = LocationRequest.create();
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        //**************************
        builder.setAlwaysShow(true); //this is the key ingredient
        //**************************

        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        // All location settings are satisfied.
                        googleApiClient.disconnect();
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied. But could be fixed by showing the user
                        // a dialog.
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            status.startResolutionForResult(
                                    Map.this, 1000);
                        } catch (IntentSender.SendIntentException e) {
                            // Ignore the error.
                        }
                        googleApiClient.disconnect();
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way to fix the
                        // settings so we won't show the dialog.
                        googleApiClient.disconnect();
                        break;
                }
            }
        });

        booleanFlagContainer.setGpsDialogAlreadyShown(true);
    }

    /**
     * Displays a popup dialog used od SDK >=23 for asking for users permission to use location if location is
     * not already enabled by user, or it is not possible to use location.
     */
    private void showGpsEnableDialogIfNeededHigherSDK(){
        int hasLocationPermission = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
        if(hasLocationPermission != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_ASK_PERMISSIONS);
            return;
        }
    }

    /**
     * Method calculates if a place is in the database more than a year by parsing date strings.
     *
     * @param placeDate date when a place is added to the database, or the licence is prolonged
     * @param today today's date
     * @return false if places licence is expired, true otherwise
     */
    private boolean compareDates(String[] placeDate, String[] today) {
        int yearDifference = Integer.valueOf(today[0]) - Integer.valueOf(placeDate[0]);
        return yearDifference == 0 ||
                (yearDifference == 1 &&
                        Integer.valueOf(placeDate[1] + placeDate[2]) > Integer.valueOf(today[1] + today[2]));
    }

    /**
     * Method which enables for menu items which are set as "show_as_action:never" to have
     * Icons displayed as well as text.
     * @param popupMenu categories filter dropdown menu
     */
    private void setForceShowIcon(PopupMenu popupMenu) {
        try {
            Field[] fields = popupMenu.getClass().getDeclaredFields();
            for (Field field : fields) {
                if ("mPopup".equals(field.getName())) {
                    field.setAccessible(true);
                    Object menuPopupHelper = field.get(popupMenu);
                    Class<?> classPopupHelper = Class.forName(menuPopupHelper
                            .getClass().getName());
                    Method setForceIcons = classPopupHelper.getMethod(
                            "setForceShowIcon", boolean.class);
                    setForceIcons.invoke(menuPopupHelper, true);
                    break;
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private void addMarker(Place place, Bitmap markerIcon, Boolean visible) {
        if(placeHashMap.containsValue(place)) {
            placeHashMap.put(getMarkerByPlace(place), place);
        } else {
            placeHashMap.put(mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(place.getLatitude(), place.getLongitude()))
                    .title(place.getName())
                    .icon(BitmapDescriptorFactory.fromBitmap(markerIcon))
                    .visible(visible)), place);
        }
    }

    private Marker getMarkerByPlace(Place place) {
        for(java.util.Map.Entry<Marker, Place> entry : placeHashMap.entrySet()) {
            if(entry.getValue().equals(place)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private void searchResultsFromStorage(String query) {
        final MatrixCursor c = new MatrixCursor(new String[]{ BaseColumns._ID, "locationName", "address" });
        int i = 0;

        for(Place place : placeHashMap.values()) {
            if(place.getName().toLowerCase().contains(query.toLowerCase())) {
                c.addRow(new Object[]{i++, place.getName(),
                        place.getAddress()});
            }
        }
        mAdapter.changeCursor(c);
    }

/*********************************************************************************************
*************************************ASYNC TASKS**********************************************
**********************************************************************************************/

    /**
     * Class which enables autocompletion on search widget.
     */
    private class Autocomplete extends AsyncTask<Object, Void, String> {

        @Override
        protected String doInBackground(Object...params) {

            final String autocompleteUrl = getResources().getString(R.string.app_server_address)
                    + getResources().getString(R.string.app_folder)
                    + getResources().getString(R.string.mobile_api_folder)
                    + getResources().getString(R.string.autocomplete_service)
                    + "?user_lat=" + ((LatLng) params[1]).latitude
                    + "&user_lng=" + ((LatLng) params[1]).longitude
                    + "&lang=" + Locale.getDefault().getLanguage();

            StringBuilder sb = new StringBuilder(autocompleteUrl);
            try {
                sb.append("&filter_word=").append(URLEncoder.encode((String) params[0], "utf8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            return handleHttpRequest(sb.toString());
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if(result.isEmpty()) {
                return;
            }

            final MatrixCursor c = new MatrixCursor(new String[]{ BaseColumns._ID, "locationName", "address" });

            try {
                JSONArray array = new JSONArray(result);
                for (int i = 0; i < array.length(); i++) {
                    c.addRow(new Object[]{i, array.getJSONObject(i).getString("location_name"),
                            array.getJSONObject(i).getString("address")});
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            mAdapter.changeCursor(c);
        }
    }

    /**
     * Class which handles a place search request from the searchView item.
     * It moves the camera onto the newly searched location.
     */
    private class MyGeocoder extends AsyncTask<String, Void, String > {
        ProgressDialog loading = null;

        @Override
        protected void onPreExecute(){
            loading = ProgressDialog.show(Map.this, "", getResources().getString(R.string.loading), true, true);
        }

        /**
         * Method sends a request to the google maps API online and receives
         * a response containing data of a place searched.
         * @param params String from the searchView item
         * @return Data from the first place of google-s suggested places list
         */
        @Override
        protected String doInBackground(String... params) {

            final String geocodingServiceUrl = getResources().getString(R.string.geocoding_service);
            StringBuilder sb = new StringBuilder(geocodingServiceUrl);
            try {
                sb.append("?address=").append(URLEncoder.encode(params[0], "utf8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            return handleHttpRequest(sb.toString());
        }

        @Override
        protected void onPostExecute(String result) {
            LatLng latLng = null;

            try {
                JSONObject jsonObject = new JSONObject(result);

                double lng = ((JSONArray) jsonObject.get("results")).getJSONObject(0)
                        .getJSONObject("geometry").getJSONObject("location")
                        .getDouble("lng");

                double lat = ((JSONArray) jsonObject.get("results")).getJSONObject(0)
                        .getJSONObject("geometry").getJSONObject("location")
                        .getDouble("lat");
                latLng = new LatLng(lat, lng);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            if (latLng != null) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12));
            } else {
                toast(getResources().getString(R.string.location_not_found));
            }
            loading.dismiss();
        }
    }

    /**
     * Imports places from storage, using shared preferences. Puts places as objects in a hashMap
     * which contains a corresponding marker as key. It does this if a specific place isn't already
     * in the map, and if the locations licence has not expired.
     */
    private class ImportPlacesFromStorage extends AsyncTask<Void, Void, String > {

        @Override
        protected String doInBackground(Void... params) {

            SharedPreferences prefs = getSharedPreferences(
                    getResources().getString(R.string.shared_prefs), Context.MODE_PRIVATE);
            return prefs.getString(getResources().getString(R.string.places_shared_prefs), "");
        }

        @Override
        protected void onPostExecute(String result) {
            if(result.isEmpty()) {
                toast(getResources().getString(R.string.http_request_failure));
                return;
            }
            DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
            String[] today = dateFormatter.format(Calendar.getInstance().getTime()).split("-");
            Gson gson = new Gson();
            Place[] places = gson.fromJson(result, Place[].class);

            for (Place place : places) {
                try {
                    //TODO: uncomment compare dates method when the time comes
                    if (!placeHashMap.containsValue(place)/* && compareDates(place.getLocation_date().split("-"), today)*/) {
                        addMarker(place, categories.get(place.getCategory_id()).getMarkerIcon(), false);
                    }
                } catch (Exception e) {
                    if (!placeHashMap.containsValue(place) /*&& compareDates(place.getLocation_date().split("-"), today)*/) {
                        addMarker(place, BitmapFactory.decodeResource(getResources(), R.drawable.default_marker), false);
                    }
                }
            }
            if(mMap.getCameraPosition().zoom > 12) {
                showAllMarkers();
            }
        }
    }

    /**
     * Imports places from server. Puts places as objects in a hashMap which contains a
     * corresponding marker as key. It does this if a specific place isn't already
     * in the map, and if the locations licence has not expired.
     */
    private class ImportPlaces extends AsyncTask<LatLng, Void, String > {

        @Override
        protected String doInBackground(LatLng... params) {

            final String placesUrl = getResources().getString(R.string.app_server_address)
                    + getResources().getString(R.string.app_folder)
                    + getResources().getString(R.string.mobile_api_folder)
                    + getResources().getString(R.string.fetch_places)
                    + "?user_lat=" + params[0].latitude
                    + "&user_lng=" + params[0].longitude
                    + "&lang=" + Locale.getDefault().getLanguage();
            
            return handleHttpRequest(placesUrl);
        }

        @Override
        protected void onPostExecute(String result) {
            if(result.isEmpty()) {
                toast(getResources().getString(R.string.http_request_failure));
                return;
            }
            DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
            String[] today = dateFormatter.format(Calendar.getInstance().getTime()).split("-");
            Gson gson = new Gson();
            Place[] places = gson.fromJson(result, Place[].class);
            int numberOfRejectedPlacesCtr = 0;

            for (Place place : places) {
                try {
//                    if (compareDates(place.getLocation_date().split("-"), today)) {
                        addMarker(place, categories.get(place.getCategory_id()).getMarkerIcon(), true);
//                    } else {numberOfRejectedPlacesCtr++;}
                } catch (Exception e) {
                    if (!placeHashMap.containsValue(place) /*&& compareDates(place.getLocation_date().split("-"), today)*/) {
                        addMarker(place, BitmapFactory.decodeResource(getResources(), R.drawable.default_marker), true);
                    }
                }
            }

            //Only on startup places from storage should be appended
            if(places.length == placeHashMap.size() + numberOfRejectedPlacesCtr) {
                importPlacesFromStorage();
            } else {
                showAllMarkers();
            }
        }
    }

    /**
     * Imports a place clicked as a suggestion on SearchView widget. Puts places as objects
     * in a hashMap which contains a corresponding marker as key. It does this if a specific
     * place isn't already in the map, and if the locations licence has not expired.
     */
    private class ImportPlace extends AsyncTask<String, Void, String > {

        @Override
        protected String doInBackground(String... params) {

            final String placesUrl = getResources().getString(R.string.app_server_address)
                    + getResources().getString(R.string.app_folder)
                    + getResources().getString(R.string.mobile_api_folder)
                    + getResources().getString(R.string.fetch_place)
                    + "?lang=" + Locale.getDefault().getLanguage();


            StringBuilder sb = new StringBuilder(placesUrl);
            try {
                sb.append("&location_name=").append(URLEncoder.encode(params[0], "utf8"));
                sb.append("&location_address=").append(URLEncoder.encode(params[1], "utf8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            return handleHttpRequest(sb.toString());
        }

        @Override
        protected void onPostExecute(String result) {
            if(result.isEmpty()) {
                toast(getResources().getString(R.string.http_request_failure));
                return;
            }
            Gson gson = new Gson();
            Place[] places = gson.fromJson(result, Place[].class);
            Marker marker;

            try {
                marker = mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(places[0].getLatitude(), places[0].getLongitude()))
                    .title(places[0].getName())
                    .icon(BitmapDescriptorFactory.fromBitmap(categories
                            .get(places[0].getCategory_id()).getMarkerIcon())));
            } catch (Exception e){
                e.printStackTrace();
                marker = mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(places[0].getLatitude(), places[0].getLongitude()))
                    .title(places[0].getName())
                    .icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(
                            getResources(), R.drawable.default_marker))));
            }

            placeHashMap.put(marker, places[0]);

            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), 17));
            marker.showInfoWindow();
            importPlaces(marker.getPosition());
        }
    }

    /**
     * Imports categories from server. When categories are imported, calls the method
     * which imports markers.
     */
    private class ImportCategories extends AsyncTask<Void, Void, String > {

        @Override
        protected String doInBackground(Void... params) {

            final String categoriesUrl = getResources().getString(R.string.app_server_address)
                    + getResources().getString(R.string.app_folder)
                    + getResources().getString(R.string.mobile_api_folder)
                    + getResources().getString(R.string.fetch_categories);

            return handleHttpRequest(categoriesUrl);
        }

        @Override
        protected void onPostExecute(String result) {
            if(result.isEmpty()) {
                toast(getResources().getString(R.string.http_request_failure));
                importCategoriesOffline();
                return;
            }
            Gson gson= new Gson();
            Category[] categories = gson.fromJson(result, Category[].class);
            populateCategoriesHashMap(categories);
            importMarkers();
        }
    }

    /**
     * Imports categories from storage, using shared preferences. When categories are
     * imported, calls the method which imports markers.
     */
    private class ImportCategoriesFromStorage extends AsyncTask<Void, Void, String > {

        @Override
        protected String doInBackground(Void... params) {

            SharedPreferences prefs = getSharedPreferences(
                    getResources().getString(R.string.shared_prefs), Context.MODE_PRIVATE);

            return prefs.getString(getResources().getString(R.string.categories_shared_prefs), "");
        }

        @Override
        protected void onPostExecute(String result) {
            if(result.isEmpty()) {
                toast(getResources().getString(R.string.http_request_failure));
                return;
            }
            Gson gson= new Gson();
            Category[] categories = gson.fromJson(result, Category[].class);
            populateCategoriesHashMap(categories);
            importMarkersFromStorage();
        }
    }

    /**
     * Imports markers from server and assigns them to corresponding categories.
     * It set's the "markersImportedAndReady" flag to true, and calls a method which
     * sets camera listeners imports places.
     */
    private class ImportMarkers extends AsyncTask<Void, Void, Void > {

        @Override
        protected Void doInBackground(Void... params) {

            final String markersUrl = getResources().getString(R.string.app_server_address)
                    + getResources().getString(R.string.app_folder)
                    + getResources().getString(R.string.mobile_api_folder);
            HttpURLConnection conn = null;

            for (Category category : categories.values()) {

                try {
                    URL url = new URL(markersUrl + category.getCategory_marker());
                    conn = (HttpURLConnection) url.openConnection();
                    category.setMarkerIcon(BitmapFactory.decodeStream(conn.getInputStream()));
                } catch (IOException e) {
                    e.printStackTrace();
                    category.setMarkerIcon(BitmapFactory.decodeResource(getResources(), R.drawable.default_marker));
                } finally {
                    if (conn != null) {
                        conn.disconnect();
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
            booleanFlagContainer.setMarkersImportedAndReadyFlag(true);
            setCameraListenerAndImportPlaces();
        }
    }

    /**
     * Imports markers from storage, using file input stream, and assigns them to
     * corresponding categories. It set's the "markersImportedAndReady" flag to true,
     * and calls a method which sets camera listeners imports places.
     */
    private class ImportMarkersFromStorage extends AsyncTask<Void, Void, Void > {

        @Override
        protected Void doInBackground(Void... params) {
            Bitmap b = null;

            for (Category category : categories.values()) {
                try {
                    FileInputStream fis = getApplicationContext().openFileInput(category.getCategory_id());
                    b = BitmapFactory.decodeStream(fis);
                    fis.close();
                } catch (Exception e) {
                    category.setMarkerIcon(BitmapFactory.decodeResource(getResources(), R.drawable.default_marker));
                }
                category.setMarkerIcon(b == null ?
                        BitmapFactory.decodeResource(getResources(), R.drawable.default_marker) : b);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
            booleanFlagContainer.setMarkersImportedAndReadyFlag(true);
            importPlacesFromStorage();
            setCameraListenerAndImportPlaces();
        }
    }
}
