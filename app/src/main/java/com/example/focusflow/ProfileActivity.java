package com.example.focusflow;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public class ProfileActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private FirebaseUser username;
    private FirebaseUser email;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private Uri selectedImageUri;
    private ImageView profilePic;
    private FirebaseFirestore fStore; // Add this
    private String Uid;


    private void savePFPtoFirestore(String imageUrl) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        Uid = currentUser.getUid();
        DocumentReference documentReference = fStore.collection("users").document(Uid);

        Map<String, Object> user = new HashMap<>();
        user.put("pfp", imageUrl); // Only updating the profile picture field

        documentReference.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                documentReference.update(user) // Update only the "pfp" field
                        .addOnSuccessListener(unused -> Log.d("TAG", "Profile picture updated for " + Uid))
                        .addOnFailureListener(e -> Log.e("Firestore", "Error updating profile picture", e));
            } else {
                documentReference.set(user) // Create document if it doesn't exist
                        .addOnSuccessListener(unused -> Log.d("TAG", "Profile created with picture for " + Uid))
                        .addOnFailureListener(e -> Log.e("Firestore", "Error creating profile", e));
            }
        }).addOnFailureListener(e -> Log.e("Firestore", "Error fetching user document", e));
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_profile);

        mAuth = FirebaseAuth.getInstance();
        username = mAuth.getCurrentUser();
        fStore = FirebaseFirestore.getInstance(); // Initialize Firestore


        if (username == null) {
            Intent intent = new Intent(getApplicationContext(), Login.class);
            startActivity(intent);
            finish();
            return;
        }

        profilePic = findViewById(R.id.profilePic);
        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null && data.getData() != null) {
                            selectedImageUri = data.getData();
                            Glide.with(this).load(selectedImageUri).into(profilePic);

                            // Save to Firestore
                            savePFPtoFirestore(String.valueOf(selectedImageUri));
                        }
                    }
                });

        profilePic.setOnClickListener(view -> {
            ImagePicker.with(ProfileActivity.this)
                    .cropSquare()
                    .compress(512)
                    .maxResultSize(512, 512)
                    .createIntent(new Function1<Intent, Unit>() {
                        @Override
                        public Unit invoke(Intent intent) {
                            imagePickerLauncher.launch(intent);
                            return null;
                        }
                    });
        });

        // display name and email of user
        TextView profileUsername = findViewById(R.id.profile_Username);
        TextView profileEmail = findViewById(R.id.profile_Email);

        // Load user details from Firestore
        loadUserDetails(profileUsername, profileEmail);


        ImageButton btnAccPage = findViewById(R.id.account);
        ImageButton btnStatsPage = findViewById(R.id.stats);
        ImageButton btnHomePage = findViewById(R.id.home);
        ImageButton btnLog = findViewById(R.id.ActivityLog);


        btnAccPage.setOnClickListener(view -> {
            new AlertDialog.Builder(ProfileActivity.this)
                    .setTitle("Notice")
                    .setMessage("Are you sure you want to log out?")
                    .setPositiveButton("Log out", (dialog, which) -> {
                        dialog.dismiss();
                        FirebaseAuth.getInstance().signOut();
                        Intent intent = new Intent(getApplicationContext(), Login.class);
                        startActivity(intent);
                        finish();
                    })
                    .setNegativeButton("Keep me Logged In", (dialog, which) -> dialog.dismiss())
                    .show();
        });

        NavigationUtility.setNavigation(this, btnStatsPage, StreaksPage.class);
        NavigationUtility.setNavigation(this, btnHomePage, Dashboard.class);
        NavigationUtility.setNavigation(this, btnLog, LogsPopup.class);
    }

    //debugging purposes :P
    private void loadUserDetails(TextView profileUsername, TextView profileEmail) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            DocumentReference userDoc = fStore.collection("users").document(userId);

            userDoc.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String username = documentSnapshot.getString("username");
                    String email = documentSnapshot.getString("email");
                    String profileImageUrl = documentSnapshot.getString("pfp");

                    profileUsername.setText(username != null ? username : "No Name");
                    profileEmail.setText(email != null ? email : "@unknown");

                    if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                        Glide.with(this).load(profileImageUrl).into(profilePic);
                    }
                }
            }).addOnFailureListener(e ->
                    Toast.makeText(ProfileActivity.this, "Failed to load user data", Toast.LENGTH_SHORT).show()
            );
        } else {
            Toast.makeText(ProfileActivity.this, "User not logged in", Toast.LENGTH_SHORT).show();
        }
    }
}