package com.example.location;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private EditText signupEmail, signupPassword, signupUsername;
    private Button signupButton;
    private TextView loginRedirectText;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        signupEmail = findViewById(R.id.signup_email);
        signupPassword = findViewById(R.id.signup_password);
        signupUsername = findViewById(R.id.signup_username);
        signupButton = findViewById(R.id.signup_button);
        loginRedirectText = findViewById(R.id.loginRedirectText);
        progressBar = findViewById(R.id.progressBar);

        signupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String user = signupEmail.getText().toString().trim();
                String pass = signupPassword.getText().toString().trim();
                String username = signupUsername.getText().toString().trim();

                // Reset errors
                signupUsername.setError(null);
                signupEmail.setError(null);
                signupPassword.setError(null);

                boolean hasError = false;

                // Validate username
                if (TextUtils.isEmpty(username)) {
                    signupUsername.setError("Username cannot be empty");
                    Toast.makeText(SignUpActivity.this, "Username cannot be empty", Toast.LENGTH_SHORT).show();
                    hasError = true;
                }

                // Validate email
                if (TextUtils.isEmpty(user)) {
                    signupEmail.setError("Email cannot be empty");
                    Toast.makeText(SignUpActivity.this, "Email cannot be empty", Toast.LENGTH_SHORT).show();
                    hasError = true;
                } else if (!Patterns.EMAIL_ADDRESS.matcher(user).matches()) {
                    signupEmail.setError("Please enter a valid email");
                    Toast.makeText(SignUpActivity.this, "Please enter a valid email", Toast.LENGTH_SHORT).show();
                    hasError = true;
                }

                // Validate password
                if (TextUtils.isEmpty(pass)) {
                    signupPassword.setError("Password cannot be empty");
                    Toast.makeText(SignUpActivity.this, "Password cannot be empty", Toast.LENGTH_SHORT).show();
                    hasError = true;
                } else if (!isPasswordValid(pass)) {
                    signupPassword.setError("Password must be 8+ characters with letters and numbers");
                    Toast.makeText(SignUpActivity.this, "Password must be 8+ characters with letters and numbers", Toast.LENGTH_LONG).show();
                    hasError = true;
                }

                if (hasError) {
                    return;
                }

                // Show progress bar
                progressBar.setVisibility(View.VISIBLE);
                signupButton.setEnabled(false);

                auth.createUserWithEmailAndPassword(user, pass).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        progressBar.setVisibility(View.GONE);
                        signupButton.setEnabled(true);

                        if (task.isSuccessful()) {
                            String userId = auth.getCurrentUser().getUid();
                            Map<String, Object> userMap = new HashMap<>();
                            userMap.put("username", username);
                            userMap.put("email", user);

                            db.collection("users").document(userId).set(userMap)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                Toast.makeText(SignUpActivity.this, "Registration successful! Welcome " + username, Toast.LENGTH_SHORT).show();
                                                startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
                                                finish();
                                            } else {
                                                Toast.makeText(SignUpActivity.this, "Failed to save user data. Please try again.", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                        } else {
                            try {
                                throw task.getException();
                            } catch (FirebaseAuthWeakPasswordException e) {
                                signupPassword.setError("Password is too weak");
                                signupPassword.requestFocus();
                                Toast.makeText(SignUpActivity.this, "Password should be at least 8 characters with letters and numbers", Toast.LENGTH_LONG).show();
                            } catch (FirebaseAuthUserCollisionException e) {
                                signupEmail.setError("Email already registered");
                                signupEmail.requestFocus();
                                Toast.makeText(SignUpActivity.this, "This email is already in use. Please login instead.", Toast.LENGTH_LONG).show();
                            } catch (Exception e) {
                                Toast.makeText(SignUpActivity.this, "Registration failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                });
            }
        });

        loginRedirectText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
            }
        });
    }

    private boolean isPasswordValid(String password) {
        return password.length() >= 8 && password.matches(".*[a-zA-Z].*") && password.matches(".*\\d.*");
    }

    @Override
    protected void onDestroy() {
        // Clean up to prevent memory leaks
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
        super.onDestroy();
    }
}