package com.tango;

import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatDelegate;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.tango.models.QuestionModel;
import com.tango.models.User;

import java.util.HashMap;
import java.util.Map;

public class QuestionPageActivity extends BaseActivity {

    private static final String TAG = "QuestionPageActivity";
    private static final String REQUIRED = "Required";

    // Initialize DataBase
    private DatabaseReference rootDB;


    private EditText questionTitle;
    private EditText questionBody;
    private FloatingActionButton submitButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if(AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES){
            setTheme(R.style.NightTheme);
        } else {
            setTheme(R.style.AppTheme);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question);
        if(Build.VERSION.SDK_INT>=21){
            Window window = this.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            if(AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES){
                window.setStatusBarColor(this.getResources().getColor(R.color.black));
            } else {
                window.setStatusBarColor(this.getResources().getColor(R.color.blue));
            }

        }
        // Set rootDB to root of FireBase DataBase
        rootDB = FirebaseDatabase.getInstance().getReference();

        // Initialize Fields
        questionTitle = findViewById(R.id.field_title);
        questionBody = findViewById(R.id.field_body);
        submitButton = findViewById(R.id.fab_submit_post);

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitPost();
            }
        });
    }

    /**
     * SubmitPost()
     * Checks if all fields have a valid input / not Empty
     * Add new question to the DataBase
     */
    public void submitPost() {
        final String title = questionTitle.getText().toString();
        final String body = questionBody.getText().toString();

        // Title is required
        if (TextUtils.isEmpty(title)) {
            questionTitle.setError(REQUIRED);
            return;
        }

        // Body is required
        if (TextUtils.isEmpty(body)) {
            questionBody.setError(REQUIRED);
            return;
        }

        // Disable button so there are no multi-posts
        setEditingEnabled(false);
        Toast.makeText(this, "Posting...", Toast.LENGTH_SHORT).show();

        // Create an EventListener that will write the new question to DB
        final String userId = getUid();
        rootDB.child("users").child(userId).addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        // Get user value
                        User user = dataSnapshot.getValue(User.class);

                        // [START_EXCLUDE]
                        if (user == null) {
                            // User is null, error out
                            Log.e(TAG, "User " + userId + " is unexpectedly null");
                            Toast.makeText(QuestionPageActivity.this,
                                    "Error: could not fetch user.",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            // Write new question
                            writeNewPost(userId, user.username, title, body);
                        }

                        // Finish this Activity, back to the stream
                        setEditingEnabled(true);
                        finish();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.w(TAG, "getUser:onCancelled", databaseError.toException());

                        setEditingEnabled(true);
                    }
                });
    }

    public void setEditingEnabled(boolean enabled) {
        questionTitle.setEnabled(enabled);
        questionBody.setEnabled(enabled);
        if (enabled) {
            submitButton.setVisibility(View.VISIBLE);
        } else {
            submitButton.setVisibility(View.GONE);
        }
    }

    // Writes the actual post to the DB
    public void writeNewPost(String userId, String username, String title, String body) {
        // Create new questionModel at /user-posts/$userid/$postid and at
        // /posts/$postid simultaneously
        String key = rootDB.child("posts").push().getKey();
        QuestionModel questionModel = new QuestionModel(userId, username, title, body);
        Map<String, Object> postValues = questionModel.toMap();

        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put("/posts/" + key, postValues);
        childUpdates.put("/user-posts/" + userId + "/" + key, postValues);

        rootDB.updateChildren(childUpdates);
    }
    // [END write_fan_out]
}
