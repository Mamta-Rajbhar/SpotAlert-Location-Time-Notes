package com.example.location;

public class NoteCallbackManager {

    private static NoteCallbackManager instance;
    private AddNoteActivity.OnNoteSavedListener listener;

    private NoteCallbackManager() {
        // Private constructor to enforce singleton pattern
    }

    public static NoteCallbackManager getInstance() {
        if (instance == null) {
            instance = new NoteCallbackManager();
        }
        return instance;
    }

    public void setListener(AddNoteActivity.OnNoteSavedListener listener) {
        this.listener = listener;
    }

    public AddNoteActivity.OnNoteSavedListener getListener() {
        return listener;
    }


}