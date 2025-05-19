package com.example.location;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class FeedbackActivity extends AppCompatActivity {

    private EditText feedbackInput;
    private Button submitButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);

        feedbackInput = findViewById(R.id.feedbackInput);
        submitButton = findViewById(R.id.submitButton);

        submitButton.setOnClickListener(v -> {
            String feedback = feedbackInput.getText().toString();
            if (!feedback.isEmpty()) {
                new SendMailTask("spotalertteam@gmail.com", "Feedback from SpotAlert User", feedback) {
                    @Override
                    protected void onPostExecute(Boolean success) {
                        if (success) {
                            Toast.makeText(FeedbackActivity.this, "Feedback Sent Successfully!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(FeedbackActivity.this, "Failed to Send Feedback. Please try again.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }.execute();
            } else {
                Toast.makeText(this, "Please enter feedback", Toast.LENGTH_SHORT).show();
            }
        });
    }
}