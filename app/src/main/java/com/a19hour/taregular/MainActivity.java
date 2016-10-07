package com.a19hour.taregular;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.wilddog.client.AuthData;
import com.wilddog.client.ChildEventListener;
import com.wilddog.client.DataSnapshot;
import com.wilddog.client.ValueEventListener;
import com.wilddog.client.Wilddog;
import com.wilddog.client.WilddogError;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private ProgressDialog progressDialog;
    private Wilddog ref;
    private ViewFlipper viewFlipper;
    private String accessStr = null;
    private String[] classes;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setMessage("Signing in, please wait...");
        progressDialog.show();
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setCancelable(false);
        Wilddog.setAndroidContext(this);
        SharedPreferences preferences = getSharedPreferences("user",0);
        String email = preferences.getString("email","");
        String password = preferences.getString("password","");
        String authenticated = preferences.getString("authenticated","no");
        if (email.equals("") || !authenticated.equals("yes")){
            //the email is empty, goes to sign up page
            progressDialog.dismiss();
            Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
            startActivity(intent);
        }
        else{
            ref = new Wilddog("https://tar.wilddogio.com");
            Wilddog.AuthResultHandler authResultHandler = new Wilddog.AuthResultHandler() {
                @Override
                public void onAuthenticated(AuthData authData) {
                    SharedPreferences userInfo = getSharedPreferences("user",0);
                    SharedPreferences.Editor editor = userInfo.edit();
                    editor.putString("uid",authData.getUid());
                    editor.commit();
                    ref.child("users").child(authData.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            Map<String, Object> checkIfTutor = new HashMap<String, Object>();
                            checkIfTutor = (HashMap<String, Object>)snapshot.getValue();
                            if (((String)(checkIfTutor.get("id"))).equals("tutor")){
                                SharedPreferences userInfo = getSharedPreferences("user",0);
                                SharedPreferences.Editor editor = userInfo.edit();
                                editor.putString("name", (String)checkIfTutor.get("name"));
                                editor.putString("authenticated", "yes");
                                editor.commit();
                                TextView nav_user_name = (TextView) findViewById(R.id.nav_user_name);
                                TextView nav_user_email = (TextView) findViewById(R.id.nav_user_email);
                                nav_user_name.setText((String)checkIfTutor.get("name"));
                                nav_user_email.setText((String)checkIfTutor.get("email"));
                                progressDialog.dismiss();
                            }
                            else{
                                progressDialog.dismiss();
                                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                                startActivity(intent);
                            }
                        }

                        @Override
                        public void onCancelled(WilddogError wilddogError) {}
                    });
                }
                @Override
                public void onAuthenticationError(WilddogError error) {
                    progressDialog.dismiss();
                    Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                    startActivity(intent);
                }
            };
            ref.authWithPassword(email, password, authResultHandler);
        }

        // Custom UI settings
        viewFlipper = (ViewFlipper) findViewById(R.id.content_main);

        Button mainCreateClass = (Button) findViewById(R.id.create_class_button);
        mainCreateClass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptCreateClass();
            }
        });

        Button backButton = (Button) findViewById(R.id.back_button);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                viewFlipper.setDisplayedChild(viewFlipper.indexOfChild(findViewById(R.id.main_classes)));
            }
        });

        Button returnClassButton = (Button) findViewById(R.id.return_class_button);
        returnClassButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (accessStr != null) {
                    attemptGetStudentsList(accessStr);
                    TextView accessTextView = (TextView) findViewById(R.id.current_class_access_text);
                    accessTextView.setText(accessStr);
                    viewFlipper.setDisplayedChild(viewFlipper.indexOfChild(findViewById(R.id.main_current_class)));
                }
                else {
                    findAccessCode();
                }
            }
        });

        // Navigation view settings
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        // add ads
//        AdSettings.setKey(new String[]{"baidu","北京"}); //创建广告view
//        String adPlaceID = "2871165";//重要:请填上你的代码位ID,否则无法请求到广告
//        AdView adView = new AdView(this,adPlaceID);
//        RelativeLayout main_layout = (RelativeLayout) findViewById(R.id.main_classes);
//        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,RelativeLayout.LayoutParams.WRAP_CONTENT);
//        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
//        main_layout.addView(adView,layoutParams);
        //

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ref.unauth();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//
//        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }
//
//        return super.onOptionsItemSelected(item);
//    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        Fragment fragment;
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (id == R.id.records) {

        } else if (id == R.id.classes) {

        } else if (id == R.id.signout) {
            ref.unauth();
            Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
            startActivity(intent);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void attemptCreateClass() {
        ref = new Wilddog("https://tar.wilddogio.com");
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
        final String currentDateandTime = sdf.format(new Date());
        ref.child("classes").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                SharedPreferences preferences = getSharedPreferences("user", 0);
                String name = preferences.getString("name", "unknown");
                String uid = preferences.getString("uid", "15e773764c41d38ea26b054a9451");
                Map<String, Object> seeAllCode = new HashMap<String, Object>();
                Map<String, Object> update = new HashMap<String, Object>();
                Map<String, Object> updateNode = new HashMap<String, Object>();
                int accessCode = 10000;
                seeAllCode = (HashMap<String, Object>)snapshot.getValue();
                // Today's node is not empty -> This is not the first class of the day
                if (seeAllCode.get(currentDateandTime) != null) {
                    seeAllCode = (HashMap<String, Object>)seeAllCode.get(currentDateandTime);
                    Random rmdGnt = new Random();
                    accessCode = rmdGnt.nextInt();
                    while (accessCode < 10000 || accessCode > 99999 || seeAllCode.get(String.valueOf(accessCode)) != null) {
                        accessCode = rmdGnt.nextInt();
                    }
                    accessStr = String.valueOf(accessCode);
                    update.put("instructor", name);
                    update.put("uid", uid);
                    update.put("code", accessStr);
                    ref.child("classes").child(currentDateandTime).child(accessStr).updateChildren(update, new Wilddog.CompletionListener(){
                        public void onComplete(WilddogError error, Wilddog ref){
                            //finish creating the class, redirect...
                            TextView accessTextView = (TextView) findViewById(R.id.current_class_access_text);
                            accessTextView.setText(accessStr);
                            viewFlipper.setDisplayedChild(viewFlipper.indexOfChild(findViewById(R.id.main_current_class)));
                            attemptGetStudentsList(accessStr);
                        }
                    });
                }
                // This is the first class of the day, the date node is still empty (need to be created)
                else {
                    Random rmdGnt = new Random();
                    accessCode = rmdGnt.nextInt();
                    while (accessCode < 10000 || accessCode > 99999) {
                        accessCode = rmdGnt.nextInt();
                    }
                    accessStr = String.valueOf(accessCode);
                    update.put("instructor", name);
                    update.put("uid", uid);
                    update.put("code", accessStr);
                    updateNode.put(accessStr, update);
                    ref.child("classes").child(currentDateandTime).updateChildren(updateNode, new Wilddog.CompletionListener(){
                        public void onComplete(WilddogError error, Wilddog ref){
                            //finish creating the class, redirect...
                            TextView accessTextView = (TextView) findViewById(R.id.current_class_access_text);
                            accessTextView.setText(accessStr);
                            viewFlipper.setDisplayedChild(viewFlipper.indexOfChild(findViewById(R.id.main_current_class)));
                            attemptGetStudentsList(accessStr);
                        }
                    });
                }
            }

            @Override
            public void onCancelled(WilddogError wilddogError) {}
        });
    }

    private void attemptGetStudentsList(String accessCode) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
        String currentDateandTime = sdf.format(new Date());
        ref = new Wilddog("https://tar.wilddogio.com");
        LinearLayout studentListLayout = (LinearLayout) findViewById(R.id.students_list_layout);
        studentListLayout.removeAllViews();
        ref.child("classes").child(currentDateandTime).child(accessCode).child("students").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue()!=null) {
                    LinearLayout studentListLayout = (LinearLayout) findViewById(R.id.students_list_layout);
                    studentListLayout.removeAllViews();
                    Map<String, Object> students = (HashMap<String, Object>)(dataSnapshot.getValue());
                    Iterator it = students.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry pair = (Map.Entry)it.next();
                        System.out.println(pair.getKey() + " = " + pair.getValue());
                        String name = ((HashMap<String, Object>)pair.getValue()).get("name").toString();
                        Button newStudent = new Button(getApplicationContext());
                        newStudent.setText(name);
                        LinearLayout.LayoutParams buttonLayout = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT);
                        buttonLayout.setMargins(15,7,15,8);
                        newStudent.setLayoutParams(buttonLayout);
                        newStudent.setBackgroundColor(0xFFF4D03F);
                        studentListLayout.addView(newStudent);
                    }
                }
            }

            @Override
            public void onCancelled(WilddogError wilddogError) {

            }
        });
    }

    private void findAccessCode() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
        String currentDateandTime = sdf.format(new Date());
        ref = new Wilddog("https://tar.wilddogio.com");
        ref.child("classes").child(currentDateandTime).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue()!=null) {
                    Map<String, Object> students = (HashMap<String, Object>)(dataSnapshot.getValue());
                    classes = new String[students.keySet().size()];
                    int index = 0;
                    Iterator it = students.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry pair = (Map.Entry)it.next();
                        classes[index++] = pair.getKey().toString();
                    }
                    if (index == 1) {
                        accessStr = classes[0];
                        TextView accessTextView = (TextView) findViewById(R.id.current_class_access_text);
                        accessTextView.setText(accessStr);
                        viewFlipper.setDisplayedChild(viewFlipper.indexOfChild(findViewById(R.id.main_current_class)));
                    } else {
                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setCancelable(true);
                        builder.setNegativeButton("Back", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                        builder.setTitle(R.string.action_choose_accesscode)
                                .setItems(classes, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        accessStr = classes[which];
                                        TextView accessTextView = (TextView) findViewById(R.id.current_class_access_text);
                                        accessTextView.setText(accessStr);
                                        attemptGetStudentsList(accessStr);
                                        viewFlipper.setDisplayedChild(viewFlipper.indexOfChild(findViewById(R.id.main_current_class)));
                                    }
                                });
                        AlertDialog alert = builder.create();
                        alert.show();
                    }
                }

            }

            @Override
            public void onCancelled(WilddogError wilddogError) {

            }
        });
    }
}
