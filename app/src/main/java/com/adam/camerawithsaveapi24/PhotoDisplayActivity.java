package com.adam.camerawithsaveapi24;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import clarifai2.api.ClarifaiResponse;
import clarifai2.dto.input.ClarifaiImage;
import clarifai2.dto.input.ClarifaiInput;
import clarifai2.dto.model.ConceptModel;
import clarifai2.dto.model.output.ClarifaiOutput;
import clarifai2.dto.prediction.Concept;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class PhotoDisplayActivity extends AppCompatActivity {

    //    private DataHolder dataHolder;
    private String fpath;
    private byte[] imageBytes;
    protected List<Concept> concepts = new ArrayList<>();
    private Button b1;
    private Button b2;
    private Button b3;
    private Button noneOfThese;
    private ImageView imageView;



    @NonNull
    private final RecognizeConceptsAdapter adapter = new RecognizeConceptsAdapter();

    protected byte[] getBytesFromFile(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        return stream.toByteArray();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_display);

        //Set up views findViewById
        imageView = findViewById(R.id.photoDisplayView);
        b1 = findViewById(R.id.button1);
        b2 = findViewById(R.id.button2);
        b3 = findViewById(R.id.button3);
        noneOfThese = findViewById(R.id.button4);

        Intent intent = getIntent();
        fpath = intent.getStringExtra("fpath");
        Bitmap bm = BitmapFactory.decodeFile(fpath);
        imageBytes = getBytesFromFile(bm);
        imageView.setImageBitmap(bm);
    }

    public void changeButtonText(){
        b1.setText(concepts.get(0).name());
        b2.setText(concepts.get(1).name());
        b3.setText(concepts.get(2).name());
    }
    

    @Override
    protected void onStart() {
        super.onStart();
        onImagePicked(imageBytes);
    }

    // TODO:
    @SuppressLint("StaticFieldLeak")
    private void onImagePicked(@NonNull final byte[] imageBytes) {
        setBusy(true);

        // Clear Concept List, just in case its still holding data from before
        adapter.setData(Collections.<Concept>emptyList());

        final AsyncTask<Void, Void, ClarifaiResponse<List<ClarifaiOutput<Concept>>>> execute = new AsyncTask<Void, Void, ClarifaiResponse<List<ClarifaiOutput<Concept>>>>() {
            @Override
            protected ClarifaiResponse<List<ClarifaiOutput<Concept>>> doInBackground(Void... params) {
                // The default Clarifai model that identifies concepts in images
                final ConceptModel model = App.get().clarifaiClient().getDefaultModels().foodModel();

                // Use this model to predict, with the image that the user just selected as the input
                return model.predict()
                        .withInputs(ClarifaiInput.forImage(ClarifaiImage.of(imageBytes)))
                        .executeSync();
            }

            @Override
            protected void onPostExecute(ClarifaiResponse<List<ClarifaiOutput<Concept>>> response) {
                setBusy(false);
                if (!response.isSuccessful()) {
                    showErrorSnackbar(R.string.error_while_contacting_api);
                    return;
                }
                final List<ClarifaiOutput<Concept>> predictions = response.get();
                if (predictions.isEmpty()) {
                    showErrorSnackbar(R.string.no_results_from_api);
                    return;
                }
                adapter.setData(predictions.get(0).data());
                concepts = predictions.get(0).data();
                adapter.printToConsole();
                changeButtonText();
            }

            private void showErrorSnackbar(@StringRes int errorString) {
                Snackbar.make(
                        findViewById(R.id.container_bot_nav),
                        errorString,
                        Snackbar.LENGTH_INDEFINITE
                ).show();
            }
        }.execute();
    }


    private void setBusy(final boolean busy) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
//                switcher.setDisplayedChild(busy ? 1 : 0);
//                imageView.setVisibility(busy ? GONE : VISIBLE);
//                fab.setEnabled(!busy);
            }
        });
    }

}
