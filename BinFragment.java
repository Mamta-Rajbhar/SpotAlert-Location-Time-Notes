package com.example.location;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BinFragment extends Fragment {

    private RecyclerView binRecyclerView;
    private NoteAdapter binAdapter;
    private List<Map<String, Object>> binNotes;

    public BinFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bin, container, false);

        binRecyclerView = view.findViewById(R.id.binRecyclerView);
        binRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binNotes = new ArrayList<>();
        // Updated to include the third parameter (false for bin since these are deleted notes)
        binAdapter = new NoteAdapter(getContext(), binNotes, false,true);
        binRecyclerView.setAdapter(binAdapter);

        fetchDeletedNotes();

        return view;
    }

    private void fetchDeletedNotes() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        String userId = auth.getCurrentUser().getUid();

        db.collection("users").document(userId).collection("bin")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        binNotes.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Map<String, Object> note = document.getData();
                            note.put("id", document.getId()); // Add document ID to the note
                            binNotes.add(note);
                        }
                        binAdapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(getContext(), "Failed to fetch deleted notes", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}