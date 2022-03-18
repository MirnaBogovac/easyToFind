package com.easy.it.find.easytofind02;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by Bogi on 21.3.2016..
 */
public class ContactUsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.Theme_IndigoNoActionBar);
        setContentView(R.layout.contact_us_layout);

        ImageView phone = (ImageView)findViewById(R.id.ic_phone);
        ImageView email = (ImageView)findViewById(R.id.ic_email);
        ImageView webAddress = (ImageView)findViewById(R.id.ic_webSite);
        ImageView imageView = (ImageView)findViewById(R.id.imageView);
        TextView icons8link = (TextView) findViewById(R.id.description);


        phone.setBackgroundResource(R.drawable.ic_call_black_24dp);
        email.setBackgroundResource(R.drawable.ic_email_black_24dp);
        webAddress.setBackgroundResource(R.drawable.ic_language_black_24dp);
        imageView.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.default_picture));


        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        FloatingActionButton contact_us = (FloatingActionButton) findViewById(R.id.directions_button);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Set hyperlink link
        icons8link.setText(Html.fromHtml(getString(R.string.app_description)));
        icons8link.setMovementMethod(LinkMovementMethod.getInstance());

        CollapsingToolbarLayout collapsingToolbar = (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);
        collapsingToolbar.setTitle("Easy to find");

        contact_us.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                        "mailto", getResources().getString(R.string.mail), null));
                intent.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.ask_rodinia));
                startActivity(Intent.createChooser(intent, getResources().getString(R.string.mail_service)));
            }
        });
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
                iShare.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.title_activity_map));
                iShare.putExtra(Intent.EXTRA_TEXT, getResources().getString(R.string.details) +
                        getResources().getString(R.string.website));
                startActivity(Intent.createChooser(iShare, getResources().getString(R.string.share_service)));
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_place, menu);
        return true;
    }
}