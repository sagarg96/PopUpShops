package com.example.sagar.popupshops_buyerside.Shop;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.sagar.popupshops_buyerside.R;
import com.example.sagar.popupshops_buyerside.Registration.GPSTracker;
import com.example.sagar.popupshops_buyerside.Registration.LaunchActivity;
import com.example.sagar.popupshops_buyerside.SelectActionActivity;
import com.example.sagar.popupshops_buyerside.Shop.SellerRecycleView.recycle;
import com.example.sagar.popupshops_buyerside.Utility.FirebaseEndpoint;
import com.example.sagar.popupshops_buyerside.Utility.FirebaseUtils;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class vendor_dashboard extends AppCompatActivity {

    private static final int REQUEST_CODE_PERMISSION = 2;
    private static final String TAG = "vendor_dashboard";
    String id;
    EditText shopDescription;
    EditText shopName;
    LinearLayout setUpLayout;
    LinearLayout dashboardLayout;
    boolean isSetup;
    Button updateLocation;
    Button setLocation;
    String mPermission = Manifest.permission.ACCESS_FINE_LOCATION;

    // GPSTracker class
    GPSTracker gps;

    //location coordinates
    double latitude;
    double longitude;

    @Override
    public void onStart() {
        super.onStart();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.vendor_dashboard);

        //Location Permissions
        try {
            if (ActivityCompat.checkSelfPermission(this, mPermission) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this, new String[]{mPermission},
                        REQUEST_CODE_PERMISSION);

                // If any permission above not allowed by user, this condition will execute every time, else your else part will work
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Bundle extras = getIntent().getExtras();
        isSetup = extras.getBoolean("setup");

        id = FirebaseUtils.getCurrentUser().getUid();

        setUpLayout = (LinearLayout) findViewById(R.id.setupLayout);
        dashboardLayout = (LinearLayout) findViewById(R.id.dashboardLayout);

        setUpUI(isSetup);

        //dashboard layout buttons
        Button viewItemList = (Button) findViewById(R.id.viewItems);
        updateLocation = (Button) findViewById(R.id.updateLocation); //location button
        final Button updateShopStatus = (Button) findViewById(R.id.closeShop);
        Button addItem = (Button) findViewById(R.id.addItem);

        //setup layout buttons
        Button setUpShop = (Button) findViewById(R.id.setUpSubmit);
        setLocation = (Button) findViewById(R.id.setUpLocation);


        shopDescription = (EditText) findViewById(R.id.shopDescr);
        shopDescription.setOnKeyListener(new descriptionTextHandler());
        shopName = (EditText) findViewById(R.id.shopName);
        shopName.setOnKeyListener(new nameTextHandler());


        shopDescription.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    hideKeyboard(v);
                }
            }
        });

        shopName.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    hideKeyboard(v);
                }
            }
        });


        viewItemList.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent(vendor_dashboard.this, recycle.class);
                intent.putExtra("shopID", "self");
                startActivity(intent);
            }
        });

        addItem.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent(vendor_dashboard.this, add.class);
                startActivity(intent);
            }
        });

        setLocation.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                setLocationAttributes();

            }
        });


        updateLocation.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                setLocationAttributes();

                //update location in shopTree
                FirebaseUtils.getCurrentShopID(new FirebaseUtils.Callback() {
                    @Override
                    public void OnComplete(String value) {
                        DatabaseReference databaseReference = FirebaseUtils.getShopsRef().child(value).child(FirebaseEndpoint.SHOPS.LOCATION);
                        Map<String, Double> location = new HashMap<String, Double>();
                        location.put("latitude", latitude);
                        location.put("longitude", longitude);
                        databaseReference.setValue(location);
                    }
                });

                FirebaseUtils.getCurrentShopID(new FirebaseUtils.Callback() {
                    @Override
                    public void OnComplete(String value) {
                        DatabaseReference geoRef = FirebaseUtils.getShopLocationRef();
                        GeoFire geofire = new GeoFire(geoRef);
                        geofire.setLocation(value, new GeoLocation(latitude, longitude));

                    }
                });

                FirebaseUtils.getCurrentShopID(new FirebaseUtils.Callback() {
                    @Override
                    public void OnComplete(String value) {
                        final Query itemQuery = FirebaseUtils.getItemRef().orderByChild(FirebaseEndpoint.ITEMS.SHOPID).equalTo(value);
                        itemQuery.addChildEventListener(new ChildEventListener() {
                            @Override
                            public void onChildAdded(DataSnapshot itemSnapshot, String s) {
                                Log.d(TAG, "onChildAdded:" + itemSnapshot.getKey());
                                DatabaseReference geoRef = FirebaseUtils.getItemLocationRef();
                                GeoFire geofire = new GeoFire(geoRef);
                                geofire.setLocation(itemSnapshot.getKey(), new GeoLocation(latitude, longitude));

                            }

                            @Override
                            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                            }

                            @Override
                            public void onChildRemoved(DataSnapshot dataSnapshot) {
                                Log.d(TAG, "onChildRemoved:" + dataSnapshot.getKey());

                            }

                            @Override
                            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                                Log.w(TAG, "itemsDisplay:onCancelled", databaseError.toException());

                            }

                        });

                    }
                });
            }
        });

        updateShopStatus.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {

                //change shop status to closed
                FirebaseUtils.getCurrentShopID(new FirebaseUtils.Callback() {
                    @Override
                    public void OnComplete(String value) {
                        Log.w(TAG, "updateShopStatus -> " + value);
                        final DatabaseReference shopStatusRef = FirebaseUtils.getShopsRef().child(value).child(FirebaseEndpoint.SHOPS.SHOPSTATUS);
                        shopStatusRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                Log.w(TAG, "updateShopStatus h ->" + dataSnapshot.getValue().toString());
                                if (dataSnapshot.getValue().toString().equals(ShopStatus.CLOSED.name())) {
                                    shopStatusRef.setValue(ShopStatus.OPEN);
                                    updateShopStatus.setText("Close Shop");

                                } else if (dataSnapshot.getValue().toString().equals(ShopStatus.OPEN.name())) {
                                    Log.w(TAG, "updateShopStatus2 ->" + dataSnapshot.getValue().toString());
                                    shopStatusRef.setValue(ShopStatus.CLOSED);
                                    updateShopStatus.setText("Open Shop");
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });


                    }
                });
            }
        });


        setUpShop.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View view) {
                createShop(shopName.getText().toString(), shopDescription.getText().toString());
                Intent intent = new Intent(vendor_dashboard.this, SelectActionActivity.class);
                startActivity(intent);
            }
        });
    }

    private void setLocationAttributes() {
        // create class object
        gps = new GPSTracker(vendor_dashboard.this);

        // check if GPS enabled
        if (gps.canGetLocation()) {

            latitude = gps.getLatitude();
            longitude = gps.getLongitude();

            // \n is for new line
            Toast.makeText(getApplicationContext(), "Your Location is - \nLat: "
                    + latitude + "\nLong: " + longitude, Toast.LENGTH_SHORT).show();
        } else {
            // can't get location
            // GPS or Network is not enabled
            // Ask user to enable GPS/network in settings
            gps.showSettingsAlert();
        }
    }

    private void setUpUI(boolean isSetup) {
        if (isSetup) {
            setUpLayout.setVisibility(View.VISIBLE);
            dashboardLayout.setVisibility(View.GONE);
        } else {
            setUpLayout.setVisibility(View.GONE);
            setUpLayout.setVisibility(View.VISIBLE);
            populateFields();
        }


    }

    private void populateFields() {
        Query shopQuery = FirebaseUtils.getShopsRef();
        shopQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    if (snapshot.hasChild("userID")) {
                        if (snapshot.child("userID").getValue().toString().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
                            shopDescription.setText(snapshot.child(FirebaseEndpoint.SHOPS.DESCRIPTION).getValue().toString(), TextView.BufferType.EDITABLE);
                            shopName.setText(snapshot.child(FirebaseEndpoint.SHOPS.SHOPNAME).getValue().toString(), TextView.BufferType.EDITABLE);
                        }
                    }
                }


            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void createShop(String shopName, String shopDescription) {
        //create shop in shop table
        ShopProfile newShop = new ShopProfile(shopName, shopDescription, latitude, longitude, FirebaseAuth.getInstance().getCurrentUser().getUid());
        DatabaseReference databaseReference = FirebaseUtils.getShopsRef().push();
        databaseReference.setValue(newShop);

        //create geofire entry
        DatabaseReference geoRef = FirebaseUtils.getShopLocationRef();
        GeoFire geofire = new GeoFire(geoRef);
        geofire.setLocation(databaseReference.getKey(), new GeoLocation(latitude, longitude));

        //update shops in users table
        List shopList = new ArrayList<String>();
        shopList.add(databaseReference.getKey());
        FirebaseUtils.getUsersRef().child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child(FirebaseEndpoint.USERS.SHOPS).setValue(shopList);
    }

    public void hideKeyboard(View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(vendor_dashboard.this, SelectActionActivity.class);
            startActivity(intent);
        }
        if (id == R.id.action_settings2) {
            FirebaseUtils.logoutUser();
            Intent intent = new Intent(vendor_dashboard.this, LaunchActivity.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    public class descriptionTextHandler implements View.OnKeyListener {
        @Override
        public boolean onKey(View view, int KeyCode, KeyEvent keyEvent) {
            int viewID = view.getId();
            EditText input_text = (EditText) findViewById(viewID);
            final String the_text = input_text.getText().toString();

            if (KeyCode == KeyEvent.KEYCODE_ENTER) {
                Toast.makeText(vendor_dashboard.this, "Description Updated " + the_text, Toast.LENGTH_SHORT).show();
                //press enter to update shop description

                Query shopExistQuery = FirebaseUtils.getUsersRef().child(FirebaseUtils.getCurrentUser().getUid()).child(FirebaseEndpoint.USERS.SHOPS);
                shopExistQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        boolean notDone = true;
                        if (dataSnapshot.exists()) {
                            Log.w("here", "shop exists " + dataSnapshot.getValue().toString());
//                            DatabaseReference databaseReference = FirebaseUtils.getCurrentUserShop().child(FirebaseEndpoint.SHOPS.DESCRIPTION).push();
//                            databaseReference.setValue(the_text);
                            if (notDone) {
                                String val = dataSnapshot.getValue().toString();
                                DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference(FirebaseEndpoint.ROOTS.SHOPS).child(val.substring(1, val.length() - 1)).child(FirebaseEndpoint.SHOPS.DESCRIPTION);
                                databaseReference.setValue(the_text);
                                notDone = false;
                            }


                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        // Getting Post failed, log a message
                        Log.w("here", "loadPost:onCancelled", databaseError.toException());
                        // ...

                    }
                });

            }
            return false;
        }
    }

    public class nameTextHandler implements View.OnKeyListener {
        @Override
        public boolean onKey(View view, int KeyCode, KeyEvent keyEvent) {
            int viewID = view.getId();
            EditText input_text = (EditText) findViewById(viewID);
            final String the_text = input_text.getText().toString();

            if (KeyCode == KeyEvent.KEYCODE_ENTER) {
                Toast.makeText(vendor_dashboard.this, "Shop Name Updated " + the_text, Toast.LENGTH_SHORT).show();
                Query shopExistQuery = FirebaseUtils.getUsersRef().child(FirebaseUtils.getCurrentUser().getUid()).child(FirebaseEndpoint.USERS.SHOPS);
                shopExistQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        boolean notDone = true;
                        if (dataSnapshot.exists()) {
                            Log.w("here", "shop exists " + dataSnapshot.getValue().toString());
                            if (notDone) {
                                String val = dataSnapshot.getValue().toString();
                                DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference(FirebaseEndpoint.ROOTS.SHOPS).child(val.substring(1, val.length() - 1)).child(FirebaseEndpoint.SHOPS.SHOPNAME);
                                databaseReference.setValue(the_text);
                                notDone = false;
                            }


                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        // Getting Post failed, log a message
                        Log.w("here", "loadPost:onCancelled", databaseError.toException());
                        // ...

                    }
                });


            }
            return false;
        }
    }

}
