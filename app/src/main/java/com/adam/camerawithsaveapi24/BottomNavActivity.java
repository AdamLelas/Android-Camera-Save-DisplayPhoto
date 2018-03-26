package com.adam.camerawithsaveapi24;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static android.view.View.*;

public class BottomNavActivity extends AppCompatActivity implements OnClickListener {

    private String mCurrentPhotoPath;
    static final int REQUEST_IMAGE_CAPTURE = 1001;
    static final int REQUEST_TAKE_PHOTO = 1;
    private String todaysLogName;
    private String timeNow;
    private UserDetails userDetails;
    private FirebaseAuth mAuth;
    private FirebaseDatabase database;
    private DatabaseReference dbRef;
    private FirebaseUser user;
    private List<FoodItem> foodItemsList;
    private Button printButton;


    private TextView tvDate, tvCalRem, tvBudgetVal, tvConsVal;
    private ProgressBar calPB;


    private double dailyCarb, dailyCal, dailyChol, dailyFib, dailyPro, dailySatFat, dailyTotFat, calRec;

    private LinearLayout progAdd;
    private LayoutInflater inflater;
    private View row;

    private View includeLayout;


    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:

                    return true;

                case R.id.navigation_account_settings:
                    findViewById(R.id.navigation_account_settings).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (mAuth.getCurrentUser() == null) {
                                dispatchSignInIntent();
                            } else {
                                dispatchSignOutIntent();
                            }
                        }
                    });
                    return true;

                case R.id.navigation_camera:
                    findViewById(R.id.navigation_camera).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (ContextCompat.checkSelfPermission(BottomNavActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                dispatchTakePictureIntent();
                            } else {
                                if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                                    Toast.makeText(getApplicationContext(), "Permission Needed.", Toast.LENGTH_LONG).show();
                                }
                                requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_IMAGE_CAPTURE);
                            }
                        }
                    });
                    return true;
            }
            return false;
        }
    };

    protected void dispatchSignInIntent() {
        Intent signInIntent = new Intent(this, SignInActivity.class);
        startActivity(signInIntent);
    }

    protected void dispatchSignOutIntent() {
        Intent signOutIntent = new Intent(this, AccountManagementActivity.class);
        startActivity(signOutIntent);
    }

    private void dispatchSignUpIntent() {
        Intent signUpIntent = new Intent(this, SignUpActivity.class);
        startActivity(signUpIntent);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_bottom_nav);

        //views
        includeLayout = findViewById(R.id.include_botnav);
        tvBudgetVal = includeLayout.findViewById(R.id.tv_budget_val);
        tvCalRem = includeLayout.findViewById(R.id.tv_cal_rem);
        tvConsVal = includeLayout.findViewById(R.id.tv_cons_val);
        tvDate = includeLayout.findViewById(R.id.tv_date);
        calPB = includeLayout.findViewById(R.id.calories_percent_pb);
        progAdd = includeLayout.findViewById(R.id.progAdd);
        inflater = LayoutInflater.from(this);

        BottomNavigationView navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        foodItemsList = new ArrayList<>();
        int testtt;

        //      Firebase
        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        dbRef = database.getReference();
        user = mAuth.getCurrentUser();

        //calculates and sets the recommended calories
        calRec = getRecCal();

//        Misc
        timeNow = new SimpleDateFormat("dd-MM-yyyy").format(new Date());
        tvDate.setText(timeNow);

        gatherTodaysFood();
        gatherUserDetails();
        try {
            createFoodLogFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        updateUI(user);

        if (user != null) {

        } else {
            System.out.println("USER WAS NULL");
        }
    }

    public double getRecCal(){
//        TODO: Calculate recommended calories height/weight or w/e
        return 2000;
    }

    private void gatherTodaysFood() {
        DatabaseReference logRef = dbRef.child("users").child(user.getUid()).child("log").child(timeNow).child("food");
        logRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                extractFoodLog(dataSnapshot);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        printArrayList();
    }

    private void gatherUserDetails(){
        DatabaseReference userDetailsRef = dbRef.child("users").child(user.getUid()).child("user-details");
        userDetailsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                extractUserDetails(dataSnapshot);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void extractUserDetails(DataSnapshot ds){
        userDetails = ds.getValue(UserDetails.class);
    }


    public void printArrayList() {
        System.out.println("PRINT ARRAY");
        for (int i = 0; i < foodItemsList.size(); i++) {
            System.out.println(foodItemsList.get(i));
        }
    }

    public void createTVs() {

    }

    public void extractFoodLog(DataSnapshot dataSnapshot) {
        for (DataSnapshot ds : dataSnapshot.getChildren()) {
            FoodItem tFood = (ds.getValue(FoodItem.class));
            if (tFood != null) {
                tFood.setFood_name(ds.getKey());
            }
            foodItemsList.add(tFood);
        }
        getDailyTotals();
    }

    public void populateScreen() {
        tvDate.setText(timeNow);
        tvConsVal.setText(String.valueOf(dailyCal));
        tvCalRem.setText(String.valueOf(calRec - dailyCal));
        tvBudgetVal.setText(String.valueOf(calRec));

//        TODO: More views to set here
    }


    public void getDailyTotals() {
//        TODO: code the totaling of each nutritional info

//        Reset Daily totals
        dailyCarb = 0;
        dailyCal = 0;
        dailyChol = 0;
        dailyFib = 0;
        dailyPro = 0;
        dailySatFat = 0;
        dailyTotFat = 0;

//        Populate daily totals
        for (FoodItem i : foodItemsList) {
            double servings = i.getServings();
            dailyCarb = dailyCarb + (i.getCarbs() * servings);
            dailyCal = dailyCal + (i.getCalories() * servings);
            dailyChol = dailyChol + (i.getCholesterol() * servings);
            dailyFib = dailyFib + (i.getFiber() * servings);
            dailyPro = dailyPro + (i.getProtein() * servings);
            dailySatFat = dailySatFat + (i.getSaturated_fat() * servings);
            dailyTotFat = dailyTotFat + (i.getTotal_fat() * servings);
        }
        populateScreen();
    }

//TODO: REMOVE
    private void appendFoodLogFile(String[] strArr) throws IOException {
        String timeNow = new SimpleDateFormat("HH:mm").format(new Date());
        if (todaysLogName != null) {
            try {
                FileOutputStream outputStream = openFileOutput(todaysLogName, MODE_APPEND);
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
                for (String aStrArr : strArr) {
                    outputStreamWriter.write(timeNow + ": " + aStrArr);
                    outputStreamWriter.flush();
                }
                outputStreamWriter.close();
            } catch (FileNotFoundException e) {
                Log.e("FileNotFound: ", "File not found exception thrown");
                e.printStackTrace();
            }
        }
    }

    private void createFoodLogFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyy_MM_dd").format(new Date());
        String logFileName = "foodLog_" + timeStamp + ".txt";
        FileOutputStream outputStream;
        todaysLogName = logFileName;

        if (isFilePresent(logFileName)) {
            Log.i("File_Exists_Already", "File: " + logFileName + " exists already");
        } else {
            Log.i("Creating_File:", logFileName);
            try {
                outputStream = openFileOutput(logFileName, Context.MODE_APPEND);
                outputStream.write(logFileName.getBytes());
                outputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isFilePresent(String fileName) {
        String path = getBaseContext().getFilesDir().getAbsolutePath() + "/" + fileName;
//        Log.i("AbsolutePath: ",  path);
        File file = new File(path);
        return file.exists();
    }

    private void updateUI(FirebaseUser firebaseUser) {
        if (firebaseUser != null) {
//            mTextMessage.setText(firebaseUser.getEmail().toString());
        }
    }


    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {

                ex.printStackTrace();
            }
            // Continues only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.adam.camerawithsaveapi24.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }


    @Nullable
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "IMG_4YP_" + timeStamp + "_";
        File storageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "Cam_Save");
        if (!storageDir.exists()) {
            if (!storageDir.mkdirs()) {
                Log.d("Y4P", "failed to create directory");
                return null;
            }
        }
        File image = File.createTempFile(imageFileName,
                ".jpg",
                storageDir
        );
        // Save file path for use to pass to other activities / methods
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {


            Intent displayPhotoIntent = new Intent(this, PhotoDisplayActivity.class);
            displayPhotoIntent.putExtra("fpath", mCurrentPhotoPath);
            startActivity(displayPhotoIntent);

            // ScanFile so it will be appeared on Gallery
//            MediaScannerConnection.scanFile(MainActivity.this,
//                    new String[]{imageUri.getPath()}, null,
//                    new MediaScannerConnection.OnScanCompletedListener() {
//                        public void onScanCompleted(String path, Uri uri) {
//                        }
//                    });
        }
    }


    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == printButton.getId()) {
            printArrayList();
        }
    }
}
