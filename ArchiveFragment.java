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

public class ArchiveFragment extends Fragment {

    private RecyclerView archiveRecyclerView;
    private NoteAdapter archiveAdapter;
    private List<Map<String, Object>> archiveNotes;

    public ArchiveFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_archive, container, false);

        archiveRecyclerView = view.findViewById(R.id.archiveRecyclerView);
        archiveRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        archiveNotes = new ArrayList<>();

        // Modified to pass true for isArchive to show restore button
        archiveAdapter = new NoteAdapter(getContext(), archiveNotes, true,false);
        archiveRecyclerView.setAdapter(archiveAdapter);

        fetchArchivedNotes();

        return view;
    }

    private void fetchArchivedNotes() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        String userId = auth.getCurrentUser().getUid();

        db.collection("users").document(userId).collection("archive")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        archiveNotes.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Map<String, Object> note = document.getData();
                            note.put("id", document.getId()); // Add document ID to the note
                            archiveNotes.add(note);
                        }
                        archiveAdapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(getContext(), "Failed to fetch archived notes", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}