package com.midi.kimdata.noteeditor;

import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.HorizontalScrollView;

import java.io.IOException;

public class NoteEditorActivity extends AppCompatActivity {

    private Toolbar m_toolbar;

    private HorizontalScrollView m_rulerHScrollView;
    private HorizontalScrollView m_noteHScrollView;

    private View.OnScrollChangeListener noteScrollListener = new View.OnScrollChangeListener() {
        @Override
        public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
            m_rulerHScrollView.setOnScrollChangeListener(null);
            m_rulerHScrollView.setScrollX(scrollX);
            m_rulerHScrollView.setOnScrollChangeListener(rulerScrollListener);
        }
    };

    private View.OnScrollChangeListener rulerScrollListener = new View.OnScrollChangeListener() {
        @Override
        public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
            m_noteHScrollView.setOnScrollChangeListener(null);
            m_noteHScrollView.setScrollX(scrollX);
            m_noteHScrollView.setOnScrollChangeListener(noteScrollListener);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_editor);

        m_toolbar = (Toolbar)findViewById(R.id.toolbar);

        if (m_toolbar != null)
        {
            setSupportActionBar(m_toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        m_rulerHScrollView = (HorizontalScrollView) findViewById(R.id.rulerHScrollView);
        m_noteHScrollView = (HorizontalScrollView) findViewById(R.id.noteHScrollView);

        m_noteHScrollView.setOnScrollChangeListener(noteScrollListener);
        m_rulerHScrollView.setOnScrollChangeListener(rulerScrollListener);

        MidiFile mf = new MidiFile();

        // Test 1 — play a C major chord

        // Turn on all three notes at start-of-track (delta=0)
        mf.noteOn (0, 60, 127);
        mf.noteOn (0, 64, 127);
        mf.noteOn (0, 67, 127);

        // Turn off all three notes after one minim.
        // NOTE delta value is cumulative — only _one_ of
        //  these note-offs has a non-zero delta. The second and
        //  third events are relative to the first
        mf.noteOff (MidiFile.MINIM, 60);
        mf.noteOff (0, 64);
        mf.noteOff (0, 67);

        // Test 2 — play a scale using noteOnOffNow
        //  We don't need any delta values here, so long as one
        //  note comes straight after the previous one

//        mf.noteOnOffNow (MidiFile.QUAVER, 60, 127);
//        mf.noteOnOffNow (MidiFile.QUAVER, 62, 127);
//        mf.noteOnOffNow (MidiFile.QUAVER, 64, 127);
//        mf.noteOnOffNow (MidiFile.QUAVER, 65, 127);
//        mf.noteOnOffNow (MidiFile.QUAVER, 67, 127);
//        mf.noteOnOffNow (MidiFile.QUAVER, 69, 127);
//        mf.noteOnOffNow (MidiFile.QUAVER, 71, 127);
//        mf.noteOnOffNow (MidiFile.QUAVER, 72, 127);
//
//        // Test 3 — play a short tune using noteSequenceFixedVelocity
//        //  Note the rest inserted with a note value of -1
//
//        int[] sequence = new int[]
//                {
//                        60, MidiFile.QUAVER + MidiFile.SEMIQUAVER,
//                        65, MidiFile.SEMIQUAVER,
//                        70, MidiFile.CROTCHET + MidiFile.QUAVER,
//                        69, MidiFile.QUAVER,
//                        65, MidiFile.QUAVER / 3,
//                        62, MidiFile.QUAVER / 3,
//                        67, MidiFile.QUAVER / 3,
//                        72, MidiFile.MINIM + MidiFile.QUAVER,
//                        -1, MidiFile.SEMIQUAVER,
//                        72, MidiFile.SEMIQUAVER,
//                        76, MidiFile.MINIM,
//                };
//
//        // What the heck — use a different instrument for a change
//        mf.progChange(10);
//
//        mf.noteSequenceFixedVelocity(sequence, 127);

        String dirPath = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS).getPath();

        try {
            mf.writeToFile(dirPath + "/test1.mid");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
