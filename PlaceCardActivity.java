package com.easy.it.find.easytofind02;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.easy.it.find.easytofind02.helper_classes.Category;
import com.easy.it.find.easytofind02.helper_classes.Place;
import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by ante on 2/21/16.
 */
public class PlaceCardActivity extends AppCompatActivity {

    private Place place;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.Theme_IndigoNoActionBar);
        setContentView(R.layout.place_card_layout);

        place = null;
        Gson gson = new Gson();
        place = gson.fromJson(getIntent().getExtras().getString("place"), Place.class);
        DownloadImage downloadImage = new DownloadImage();
        downloadImage.execute();


        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        final LinearLayout adBanner = (LinearLayout) findViewById(R.id.ad_container);
        FloatingActionButton directionsButton = (FloatingActionButton) findViewById(R.id.directions_button);

        TextView placeDescription = (TextView)findViewById(R.id.description);
        TextView placePhone = (TextView)findViewById(R.id.phone);
        TextView placeAddress = (TextView)findViewById(R.id.address);
        TextView placeEmail = (TextView)findViewById(R.id.email);
        TextView placeWebAddress = (TextView)findViewById(R.id.web_address);

        ImageView phone = (ImageView)findViewById(R.id.ic_phone);
        ImageView address = (ImageView)findViewById(R.id.ic_addrese);
        ImageView email = (ImageView)findViewById(R.id.ic_email);
        ImageView webAddress = (ImageView)findViewById(R.id.ic_webSite);
        ImageView adImage = (ImageView)findViewById(R.id.ic_ad_image);
        ImageButton adClose = (ImageButton)findViewById(R.id.ic_ad_close);



        phone.setBackgroundResource(R.drawable.ic_call_black_24dp);
        address.setBackgroundResource(R.drawable.ic_place_black_24dp);
        email.setBackgroundResource(R.drawable.ic_email_black_24dp);
        webAddress.setBackgroundResource(R.drawable.ic_language_black_24dp);
        adImage.setBackgroundResource(R.drawable.addidas_icon);
        adClose.setBackgroundResource(R.drawable.ic_close_black_24dp);

        adClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adBanner.setVisibility(LinearLayout.GONE);
            }
        });

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        CollapsingToolbarLayout collapsingToolbar = (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);
        collapsingToolbar.setTitle(place.getName());

        placeDescription.setText(Html.fromHtml(place.getDescription()));
        placePhone.setText(place.getPhone());
        placeAddress.setText(place.getAddress());
        placeWebAddress.setText(place.getWebsite());
        placeEmail.setText(place.getMail());

        if (place.getMail().equals("-") || place.getMail().isEmpty()) {
            placeEmail.setVisibility(View.GONE);
            email.setVisibility(View.GONE);
        }
        if(place.getWebsite().equals("-") || place.getWebsite().isEmpty()) {
            placeWebAddress.setVisibility(View.GONE);
            webAddress.setVisibility(View.GONE);
        }

        directionsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getDirectionsToLocation(new LatLng(place.getLatitude(), place.getLongitude()));
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void toast (String message) {
        Toast.makeText(getApplicationContext(),
                message,
                Toast.LENGTH_LONG).show();
    }

    // Listener for option menu
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;

            case (R.id.menu_item_share):
                Intent iShare = new Intent(Intent.ACTION_SEND);
                iShare.setType("text/plain");
                String url;
                url = getResources().getString(R.string.app_server_address) +
                      getResources().getString(R.string.app_folder) +
                      getResources().getString(R.string.share_place_url) +
                      place.getId();
                iShare.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
                iShare.putExtra(Intent.EXTRA_SUBJECT, place.getName());
                iShare.putExtra(Intent.EXTRA_TEXT, getResources().getString(R.string.details) + url);
                startActivity(Intent.createChooser(iShare, getResources().getString(R.string.share_service)));
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate menu resource file.
        getMenuInflater().inflate(R.menu.menu_place, menu);
        return true;
    }

    /*******************************************************************************************
     *************************************PRIVATE METHODS******************************************
     **********************************************************************************************/


    private void getDirectionsToLocation (LatLng position) {
        Uri gmmIntentUri = Uri.parse(getResources().getString(R.string.directions_service)
                + position.latitude + "," + position.longitude);

        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(mapIntent, 0);
        } else {
            mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            startActivityForResult(mapIntent, 0);
        }
    }

    private void sendIncrementHitCounterReq() {
        IncrementHitCounterReq req = new IncrementHitCounterReq();
        req.execute();
    }

    /*********************************************************************************************
     *************************************ASYNC TASKS**********************************************
     **********************************************************************************************/

    private class DownloadImage extends AsyncTask<Void, Void, Bitmap > {

        ProgressDialog loading = null;

        @Override
        protected void onPreExecute(){
            loading = ProgressDialog.show(PlaceCardActivity.this, "", getResources().getString(R.string.loading), true, true);
        }

        @Override
        protected Bitmap doInBackground(Void...empty) {

            final String imageUrl = getResources().getString(R.string.app_server_address)
                    + getResources().getString(R.string.app_folder)
                    + getResources().getString(R.string.mobile_api_folder);
            HttpURLConnection conn = null;

            try {
                URL url = new URL(imageUrl + place.getImage());
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(4000);
                conn.setReadTimeout(5000);

                return BitmapFactory.decodeStream(conn.getInputStream());
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap image) {
            ImageView imageView =(ImageView) findViewById(R.id.imageView);
            loading.dismiss();

            if(image == null) {
                imageView.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.default_picture));
                return;
            }

            imageView.setImageBitmap(image);
            sendIncrementHitCounterReq();
        }
    }

    /**
     * Sends a request to server in order to increment the hit counter for the given location
     */
    private class IncrementHitCounterReq extends AsyncTask<Void, Void, Void > {
        @Override
        protected Void doInBackground(Void...empty) {

            final String hitCounterUrl = getResources().getString(R.string.app_server_address)
                    + getResources().getString(R.string.app_folder)
                    + getResources().getString(R.string.mobile_api_folder)
                    + getResources().getString(R.string.increment_hit_counter)
                    + "?id=" + place.getId();

            HttpURLConnection conn = null;

            try {
                URL url = new URL(hitCounterUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(4000);
                conn.getResponseCode();     // Without waiting for response, connection gets closed too early
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
            return null;
        }
    }
}