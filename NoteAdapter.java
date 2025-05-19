 package com.example.location;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import android.widget.Filter;
import android.widget.Filterable;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> implements Filterable {
    private WeakReference<Context> contextRef;
    private List<Map<String, Object>> notes;
    private List<Map<String, Object>> notesFull;
    private boolean isArchive;
    private boolean isBin;

    public NoteAdapter(Context context, List<Map<String, Object>> notes, boolean isArchive, boolean isBin) {
        this.contextRef = new WeakReference<>(context);
        this.notes = notes;
        this.notesFull = new ArrayList<>(notes);
        this.isArchive = isArchive;
        this.isBin = isBin;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.note_item, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        Context context = contextRef.get();
        if (context == null) return;

        Map<String, Object> note = notes.get(position);

        // Hide the options button for archived and bin notes
        holder.optionsButton.setVisibility((isArchive || isBin) ? View.GONE : View.VISIBLE);

        // Show restore button for archived notes
        holder.restoreButton.setVisibility(isArchive ? View.VISIBLE : View.GONE);

        // Show delete button for bin notes
        holder.deleteButton.setVisibility(isBin ? View.VISIBLE : View.GONE);

        // Set up restore button click listener
        holder.restoreButton.setOnClickListener(v -> {
            String noteId = (String) note.get("id");
            restoreNoteFromArchive(noteId, note);
        });

        // Set up delete button click listener
        holder.deleteButton.setOnClickListener(v -> {
            String noteId = (String) note.get("id");
            deleteNotePermanently(noteId, note);
        });

        // Handle null values for title, content, place, and dateTime
        String title = note.containsKey("title") && note.get("title") != null
                ? note.get("title").toString()
                : "No Title";
        String content = note.containsKey("content") && note.get("content") != null
                ? note.get("content").toString()
                : "No Content";
        String place = note.containsKey("place") && note.get("place") != null
                ? note.get("place").toString()
                : "No Place";
        String dateTime = note.containsKey("dateTime") && note.get("dateTime") != null
                ? note.get("dateTime").toString()
                : "No Date";

        holder.noteTitle.setText(title);
        holder.noteContent.setText(content);
        holder.notePlace.setText(place);
        holder.noteDateTime.setText(dateTime);

        // Handle image display
        if (note.containsKey("imagePath") && note.get("imagePath") != null) {
            String imagePath = note.get("imagePath").toString();
            holder.noteImage.setVisibility(View.VISIBLE);
            Glide.with(context)
                    .load(imagePath)
                    .into(holder.noteImage);

            holder.noteImage.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse(imagePath), "image/*");
                context.startActivity(intent);
            });
        } else {
            holder.noteImage.setVisibility(View.GONE);
        }

        // Set click listener for the three-dot menu (only for regular notes)
        if (!isArchive && !isBin) {
            holder.optionsButton.setOnClickListener(v -> showPopupMenu(v, position));
        }
    }


    private void restoreNoteFromArchive(String noteId, Map<String, Object> note) {
        Context context = contextRef.get();
        if (context == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        String userId = auth.getCurrentUser().getUid();

        // Remove from archive
        db.collection("users").document(userId).collection("archive")
                .document(noteId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Add to main notes collection
                    db.collection("users").document(userId).collection("notes")
                            .document(noteId)
                            .set(note)
                            .addOnSuccessListener(aVoid1 -> {
                                Toast.makeText(context, "Note restored", Toast.LENGTH_SHORT).show();
                                // Remove from local list and update UI
                                int position = notes.indexOf(note);
                                if (position != -1) {
                                    notes.remove(position);
                                    notifyItemRemoved(position);
                                }
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(context, "Failed to restore note", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to restore note", Toast.LENGTH_SHORT).show();
                });
    }

    private void deleteNotePermanently(String noteId, Map<String, Object> note) {
        Context context = contextRef.get();
        if (context == null) return;

        new AlertDialog.Builder(context)
                .setTitle("Delete Permanently")
                .setMessage("Are you sure you want to delete this note permanently?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    FirebaseAuth auth = FirebaseAuth.getInstance();
                    String userId = auth.getCurrentUser().getUid();

                    db.collection("users").document(userId).collection("bin")
                            .document(noteId)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(context, "Note deleted permanently", Toast.LENGTH_SHORT).show();
                                // Remove from local list and update UI
                                int position = notes.indexOf(note);
                                if (position != -1) {
                                    notes.remove(position);
                                    notifyItemRemoved(position);
                                }
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(context, "Failed to delete note", Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    public static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView noteTitle, noteContent, notePlace, noteDateTime;
        ImageView noteImage;
        ImageButton optionsButton, restoreButton, deleteButton;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            noteTitle = itemView.findViewById(R.id.noteTitle);
            noteContent = itemView.findViewById(R.id.noteContent);
            notePlace = itemView.findViewById(R.id.notePlace);
            noteDateTime = itemView.findViewById(R.id.noteDateTime);
            noteImage = itemView.findViewById(R.id.noteImage);
            optionsButton = itemView.findViewById(R.id.noteMenu);
            restoreButton = itemView.findViewById(R.id.restoreButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }

    private void showPopupMenu(View view, int position) {
        Context context = contextRef.get();
        if (context == null) return;

        PopupMenu popupMenu = new PopupMenu(context, view);
        popupMenu.inflate(R.menu.note_menu);
        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.menu_delete) {
                showDeleteConfirmationDialog(position);
                return true;
            } else if (itemId == R.id.menu_archive) {
                moveNoteToArchive(position);
                return true;
            } else if (itemId == R.id.menu_share) {
                shareNote(position);
                return true;
            } else {
                Toast.makeText(context, "Invalid menu option", Toast.LENGTH_SHORT).show();
                return false;
            }
        });
        popupMenu.show();
    }

    private void shareNote(int position) {
        Context context = contextRef.get();
        if (context == null) return;

        if (position < 0 || position >= notes.size()) {
            Toast.makeText(context, "Invalid note position", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> note = notes.get(position);

        String title = note.containsKey("title") && note.get("title") != null
                ? note.get("title").toString()
                : "No Title";
        String content = note.containsKey("content") && note.get("content") != null
                ? note.get("content").toString()
                : "No Content";
        String dateTime = note.containsKey("dateTime") && note.get("dateTime") != null
                ? note.get("dateTime").toString()
                : "No Date";

        String placeName = "No Place";
        if (note.containsKey("place") && note.get("place") != null) {
            Map<String, Object> place = (Map<String, Object>) note.get("place");
            if (place != null && place.containsKey("name") && place.get("name") != null) {
                placeName = place.get("name").toString();
            }
        }

        String shareMessage = "Title: " + title + "\n"
                + "Content: " + content + "\n"
                + "Place: " + placeName + "\n"
                + "Date & Time: " + dateTime;

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, title);
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);

        context.startActivity(Intent.createChooser(shareIntent, "Share Note"));
    }

    private void showDeleteConfirmationDialog(int position) {
        Context context = contextRef.get();
        if (context == null) return;

        new AlertDialog.Builder(context)
                .setTitle("Delete Note")
                .setMessage("Are you sure you want to delete this note?")
                .setPositiveButton("Yes", (dialog, which) -> deleteNote(position))
                .setNegativeButton("No", null)
                .show();
    }

    private void deleteNote(int position) {
        Context context = contextRef.get();
        if (context == null) return;

        if (notes == null || notes.isEmpty() || position < 0 || position >= notes.size()) {
            Toast.makeText(context, "Invalid note position", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> note = notes.get(position);
        String noteId = note != null && note.containsKey("id") ? note.get("id").toString() : null;
        if (noteId == null) {
            Toast.makeText(context, "Invalid note ID", Toast.LENGTH_SHORT).show();
            return;
        }

        NoteManager.deleteNote(context, noteId, note, new NoteManager.OnNoteOperationListener() {
            @Override
            public void onSuccess() {
                notes.removeIf(n -> noteId.equals(n.get("id")));
                notesFull.removeIf(n -> noteId.equals(n.get("id")));
                notifyDataSetChanged();
                Toast.makeText(context, "Note moved to bin", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public Filter getFilter() {
        return noteFilter;
    }

    private Filter noteFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<Map<String, Object>> filteredList = new ArrayList<>();

            if (constraint == null || constraint.length() == 0) {
                filteredList.addAll(notesFull);
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();

                for (Map<String, Object> note : notesFull) {
                    String title = note.containsKey("title") && note.get("title") != null
                            ? note.get("title").toString().toLowerCase()
                            : "";
                    String content = note.containsKey("content") && note.get("content") != null
                            ? note.get("content").toString().toLowerCase()
                            : "";

                    if (title.contains(filterPattern) || content.contains(filterPattern)) {
                        filteredList.add(note);
                    }
                }
            }

            FilterResults results = new FilterResults();
            results.values = filteredList;
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            notes.clear();
            notes.addAll((List) results.values);
            notifyDataSetChanged();
        }
    };

    public void updateFullList(List<Map<String, Object>> newList) {
        notesFull.clear();
        notesFull.addAll(newList);
        notifyDataSetChanged();
    }

    private void moveNoteToArchive(int position) {
        Context context = contextRef.get();
        if (context == null) return;

        if (notes == null || notes.isEmpty() || position < 0 || position >= notes.size()) {
            Toast.makeText(context, "Invalid note position", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> note = notes.get(position);
        String noteId = note != null && note.containsKey("id") ? note.get("id").toString() : null;
        if (noteId == null) {
            Toast.makeText(context, "Invalid note ID", Toast.LENGTH_SHORT).show();
            return;
        }

        NoteManager.archiveNote(context, noteId, note, new NoteManager.OnNoteOperationListener() {
            @Override
            public void onSuccess() {
                notes.removeIf(n -> noteId.equals(n.get("id")));
                notesFull.removeIf(n -> noteId.equals(n.get("id")));
                notifyDataSetChanged();
                Toast.makeText(context, "Note archived", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
}