package com.midi.kimdata.noteeditor;

import android.content.Context;
import android.media.midi.MidiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewManager;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.view.kimdata.NoteHorizontalScrollView;
import com.view.kimdata.NoteVerticalScrollView;

import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class NoteEditorActivity extends AppCompatActivity implements View.OnTouchListener {

    public static final int KEY_RANGE = 84;

    public static int TOUCH_OFFSET = 0;

    public static final int BAR = 8;

    public static final int NOTE_DP = 36;
    public static final int NOTE_HEIGHT_DP = 24;

    public enum NoteTouch {NONE, LEFT, MID, RIGHT}

    private Toolbar m_toolbar;

    private HorizontalScrollView m_rulerHScrollView;
    private NoteHorizontalScrollView m_noteHScrollView;
    private NoteVerticalScrollView m_noteVScrollView;

    private DisplayMetrics m_metric;

    private LinearLayout m_rulerContainer;

    private FrameLayout m_noteContainer;

    private Switch m_editSwitch;

    private TextView m_workNote;

    private boolean m_isOnWork;

    private NoteTouch m_noteMode;

    //    private ArrayList<ArrayList<TextView>> m_noteViewList;
    private ArrayList<TextView> m_originalViewList;

    private int m_workNoteRight;

    private boolean m_isNoteMoved;

    private int m_beforeY;

    public static void setFLRect(View view, int x, int y, int width, int height) {
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) view.getLayoutParams();
        params.width = width;
        params.height = height;
        view.setX(x);
        view.setY(y);
        view.setLayoutParams(params);
    }

    public static int getDPI(int size, DisplayMetrics metrics) {
        return (size * metrics.densityDpi) / DisplayMetrics.DENSITY_DEFAULT;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_editor);

        m_metric = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(m_metric);

        m_isOnWork = false;
        m_noteMode = NoteTouch.NONE;
        m_isNoteMoved = false;

//        TOUCH_OFFSET = getDPI(NOTE_DP, m_metric);

        m_toolbar = (Toolbar) findViewById(R.id.toolbar);

        if (m_toolbar != null) {
            setSupportActionBar(m_toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        m_editSwitch = (Switch) findViewById(R.id.editSwitch);

        m_rulerHScrollView = (HorizontalScrollView) findViewById(R.id.rulerHScrollView);
        m_noteHScrollView = (NoteHorizontalScrollView) findViewById(R.id.noteHScrollView);
        m_noteVScrollView = (NoteVerticalScrollView) findViewById(R.id.noteVScrollView);

        m_noteHScrollView.setOnScrollChangeListener(noteScrollListener);
        m_rulerHScrollView.setOnScrollChangeListener(rulerScrollListener);

        m_noteHScrollView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return m_editSwitch.isChecked();
            }
        });

        m_noteVScrollView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return m_editSwitch.isChecked();
            }
        });

        m_noteContainer = new FrameLayout(this);

        m_noteContainer.setLayoutParams(new FrameLayout.LayoutParams((int) getResources().getDimension(R.dimen.note_width) * BAR * 4, FrameLayout.LayoutParams.MATCH_PARENT));
//        m_noteContainer.setBackgroundColor(getColor(R.color.colorBackBlack));
//        m_noteContainer.setBackground(getDrawable(R.drawable.stripe_pattern));
//        m_noteContainer.setBackgroundResource(R.drawable.stripe_pattern);

        m_noteContainer.setOnTouchListener(this);

        LinearLayout noteContainerLayout = (LinearLayout) findViewById(R.id.noteContainerLayout);
        noteContainerLayout.addView(m_noteContainer);

        m_rulerContainer = (LinearLayout) findViewById(R.id.rulerContainer);

        // TODO : noteviewlist
//        m_noteViewList = new ArrayList<>();
        m_originalViewList = new ArrayList<>();

        for (int i = 0; i < BAR; i++) {
            addBar(i);

            for (int j = 0; j < 7; j++) {
                ImageView iv = new ImageView(this);
                iv.setImageResource(R.drawable.stripe_pattern);
                m_noteContainer.addView(iv);

                int octaveHeight = getDPI(NOTE_HEIGHT_DP, m_metric) * 12;
//            setFLRect(iv, 0, i * octaveHeight, FrameLayout.LayoutParams.MATCH_PARENT, octaveHeight);
                setFLRect(iv, i * getDPI(NOTE_DP * 4, m_metric), j * octaveHeight, getDPI(NOTE_DP * 4, m_metric), octaveHeight);
            }

            ArrayList<TextView> noteListInBar = new ArrayList<>();
            // TODO : noteviewlist
//            m_noteViewList.add(noteListInBar);
        }

        // set scroll
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                // octave = 12, 3 octave + 1 key
                m_noteVScrollView.setScrollY(getDPI(NOTE_HEIGHT_DP, m_metric) * 37);
            }
        }, 500);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.editor_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i("junu", "" + (item.getItemId() == R.id.action_settings));

        switch (item.getItemId()) {
            case R.id.action_skip_previous:

                return true;
            case R.id.action_play_pause:

                return true;
            case R.id.action_repeat:

                return true;
            case R.id.action_undo:

                return true;
            case R.id.action_redo:

                return true;
            case R.id.action_settings:
                makeMIDIFile();

                return true;
            default:

                return super.onOptionsItemSelected(item);
        }
    }

    private void addBar(int i) {
        int layout_width = getDPI(NOTE_DP * 4, m_metric);

        LinearLayout container = new LinearLayout(this);
        container.setLayoutParams(new LinearLayout.LayoutParams(layout_width, (int) getResources().getDimension(R.dimen.note_height)));
        container.setBackground(getDrawable(R.drawable.left_border));
        container.setOrientation(LinearLayout.HORIZONTAL);

        TextView tv = new TextView(this);
        tv.setText("  " + (i + 1));
        tv.setLayoutParams(new LinearLayout.LayoutParams((int) getResources().getDimension(R.dimen.note_height), (int) getResources().getDimension(R.dimen.note_height)));
        tv.setGravity(Gravity.CENTER_VERTICAL);

        container.addView(tv);

        m_rulerContainer.addView(container);
    }

    private void makeMIDIFile() {
        MidiManager manager = (MidiManager) getSystemService(Context.MIDI_SERVICE);

        MidiFile mf = new MidiFile();

        // Test 1 — play a C major chord

        // Turn on all three notes at start-of-track (delta=0)
        mf.noteOn(0, 60, 127);
        mf.noteOn(0, 64, 127);
        mf.noteOn(0, 67, 127);

        // Turn off all three notes after one minim.
        // NOTE delta value is cumulative — only _one_ of
        //  these note-offs has a non-zero delta. The second and
        //  third events are relative to the first
        mf.noteOff(MidiFile.MINIM, 60);
        mf.noteOff(0, 64);
        mf.noteOff(0, 67);

        // Test 2 — play a scale using noteOnOffNow
        //  We don't need any delta values here, so long as one
        //  note comes straight after the previous one

        mf.noteOnOffNow(MidiFile.CROTCHET, 60, 127);
        mf.noteOnOffNow(MidiFile.QUAVER, 62, 127);
        mf.noteOnOffNow(MidiFile.CROTCHET, 64, 127);
        mf.noteOnOffNow(MidiFile.QUAVER, 65, 127);
        mf.noteOnOffNow(MidiFile.CROTCHET, 67, 127);
        mf.noteOnOffNow(MidiFile.QUAVER, 69, 127);
        mf.noteOnOffNow(MidiFile.CROTCHET, 71, 127);
        mf.noteOnOffNow(MidiFile.QUAVER, 72, 127);

        // Test 3 — play a short tune using noteSequenceFixedVelocity
        //  Note the rest inserted with a note value of -1

        int[] sequence = new int[]
                {
                        60, MidiFile.QUAVER,
                        -1, MidiFile.CROTCHET,
                        62, MidiFile.QUAVER,
                        -1, MidiFile.CROTCHET,
                        64, MidiFile.QUAVER,
                };

        // What the heck — use a different instrument for a change
//        mf.progChange(10);

        mf.noteSequenceFixedVelocity(sequence, 127);

        String dirPath = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS).getPath();

        try {
            mf.writeToFile(dirPath + "/test1.mid");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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

    private View.OnTouchListener noteTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int sideArea;

            if (v.getWidth() / 4 >= (int) getResources().getDimension(R.dimen.note_side_area)) {
                sideArea = v.getWidth() / 4;
            } else {
                sideArea = (int) getResources().getDimension(R.dimen.note_side_area);
            }

            int midArea = v.getWidth() - (sideArea * 2);

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (event.getX() <= sideArea) {
                        m_noteMode = NoteTouch.LEFT;
                    } else if (event.getX() >= v.getWidth() - sideArea) {
                        m_noteMode = NoteTouch.RIGHT;
                    } else {
                        m_noteMode = NoteTouch.MID;

                        TOUCH_OFFSET = ((v.getWidth() / 2) / getDPI(NOTE_DP, m_metric)) * getDPI(NOTE_DP, m_metric);
                    }

                    m_workNote = (TextView) v;

                    m_workNoteRight = (int) (v.getX() + v.getWidth());

                    m_isNoteMoved = false;

                    break;
                case MotionEvent.ACTION_MOVE:

                    break;
                case MotionEvent.ACTION_UP:
                    Log.i("junu", "delete");

                    break;
            }

            return false;
        }
    };

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (!m_editSwitch.isChecked())
            return true;

        int x = (int) (event.getX() - (event.getX() % getDPI(NOTE_DP, m_metric))) - TOUCH_OFFSET;
        int y = (int) (event.getY() - (event.getY() % getDPI(NOTE_HEIGHT_DP, m_metric)));

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (m_noteMode == NoteTouch.NONE) {
                    TextView tv = new TextView(this);
                    m_noteContainer.addView(tv);
                    tv.setBackgroundResource(R.drawable.note);
//                tv.setBackgroundColor(getColor(R.color.colorPrimary));

                    tv.setOnTouchListener(noteTouchListener);

                    setFLRect(tv, x, y, getDPI(NOTE_DP, m_metric), getDPI(NOTE_HEIGHT_DP, m_metric));

                    m_originalViewList.add(tv);

                    m_workNote = tv;

                    m_isNoteMoved = true;

                    m_beforeY = y;

                    Log.i("junu", "note created");

                    // TODO: play sound
                }

                m_isOnWork = true;

                m_noteHScrollView.setLockScroll(true);
                m_noteVScrollView.setLockScroll(true);

                return true;
            case MotionEvent.ACTION_MOVE:
                // TODO: On move or edit size, calculate a collision.
                if ((m_isOnWork && m_noteMode == NoteTouch.NONE) || m_noteMode == NoteTouch.MID) {

//                    if (m_noteMode == NoteTouch.MID) {
//                        if (isCollisionLeftNote(m_workNote)) {
//                            m_isNoteMoved = true;
//
//                            return true;
//                        }
//
//                        if (isCollisionRightNote(m_workNote)) {
//                            m_isNoteMoved = true;
//
//                            return true;
//                        }
//                    }

                    m_workNote.setX(x);
                    m_workNote.setY(y);
                } else if (m_noteMode == NoteTouch.LEFT) {
                    if (m_workNoteRight - x <= 0)
                        return true;

//                    if (isCollisionLeftNote(m_workNote))
//                        return true;

                    setFLRect(m_workNote, x, (int) m_workNote.getY(), m_workNoteRight - x, getDPI(NOTE_HEIGHT_DP, m_metric));
                } else if (m_noteMode == NoteTouch.RIGHT) {
                    if ((int) (x - m_workNote.getX()) + getDPI(NOTE_DP, m_metric) <= 0)
                        return true;

//                    if (isCollisionRightNote(m_workNote))
//                        return true;

                    setFLRect(m_workNote, (int) m_workNote.getX(), (int) m_workNote.getY(), (int) (x - m_workNote.getX()) + getDPI(NOTE_DP, m_metric), getDPI(NOTE_HEIGHT_DP, m_metric));
                }

                if (m_beforeY != y) {
                    m_beforeY = y;

                    // TODO: play sound
                }

                m_isNoteMoved = true;

                return true;
            case MotionEvent.ACTION_UP:
                int barSize = getDPI(NOTE_DP, m_metric) * 4;
                int targetBar = x <= 0 ? 0 : x / barSize;

                // TODO : noteviewlist
//                ArrayList<TextView> targetViewList = m_noteViewList.get(targetBar);

                // Created note and moved.
                if (m_noteMode == NoteTouch.NONE) {
                    ViewManager parent = (ViewManager) m_workNote.getParent();

                    // Out of screen or note on same position, remove.
                    if (x < 0 || x >= m_noteContainer.getWidth()) {
                        parent.removeView(m_workNote);

                        m_originalViewList.remove(m_workNote);

                        Log.i("junu", "note delete out");
                    } else if (y < 0 || y >= m_noteContainer.getHeight()) {
                        parent.removeView(m_workNote);

                        m_originalViewList.remove(m_workNote);

                        Log.i("junu", "note delete out2");
                    } else if (isExistSamePosition(m_workNote, targetBar)) {
                        parent.removeView(m_workNote);

                        m_originalViewList.remove(m_workNote);

                        Log.i("junu", "note delete same");
                    } else {
                        // TODO : noteviewlist
//                        targetViewList.add(m_workNote);
                    }
                }

                // Not moved, just touch middle of note. Remove.
                if (m_noteMode == NoteTouch.MID && !m_isNoteMoved) {
                    ViewManager parent = (ViewManager) m_workNote.getParent();

                    parent.removeView(m_workNote);

                    m_originalViewList.remove(m_workNote);

                    Log.i("junu", "note delete touch");
                }

                // TODO : noteviewlist
//                Collections.sort(targetViewList, noteViewComparator);
                Collections.sort(m_originalViewList, noteViewComparator);

                // Initialize.
                m_workNote = null;

                m_isOnWork = false;
                m_isNoteMoved = false;

                m_noteMode = NoteTouch.NONE;

                TOUCH_OFFSET = 0;

                m_noteHScrollView.setLockScroll(false);
                m_noteVScrollView.setLockScroll(false);

                Log.i("junu", "view list : " + m_originalViewList.size());
                for (View view : m_originalViewList)
                    Log.i("junu", view.getX() + ", " + view.getY());

                return true;
        }

        return false;
    }

    private boolean isExistSamePosition(View view, int targetBar) {
        // TODO : noteviewlist
//        for (View item : m_noteViewList.get(targetBar)) {
//            if ((int) item.getX() == (int) view.getX() && (int) item.getY() == (int) view.getY())
//                return true;
//        }

        for (View item : m_originalViewList) {
            if (item != view &&
                    (int) item.getX() == (int) view.getX() &&
                    (int) item.getY() == (int) view.getY())
                return true;
        }

        return false;
    }

//    private boolean isCollisionLeftNote(View v) {
//        // TODO : noteviewlist
////        for (ArrayList<TextView> itemList : m_noteViewList) {
////            for (View item : itemList) {
////                if ((int) item.getY() != (int) v.getY() || (int) item.getX() >= (int) v.getX() || item == v)
////                    continue;
////
////                if ((int) item.getX() + item.getWidth() >= v.getX())
////                    return true;
////            }
////        }
//        for (View item : m_originalViewList) {
//            if (item != v &&
//                    (int) item.getY() == (int) v.getY() &&
//                    (int) item.getX() < (int) v.getX() &&
//                    (int) item.getX() + item.getWidth() >= v.getX())
//                return true;
//        }
//
//        return false;
//    }
//
//    private boolean isCollisionRightNote(View v) {
//        // TODO : noteviewlist
////        for (ArrayList<TextView> itemList : m_noteViewList) {
////            for (View item : itemList) {
////                if ((int) item.getY() != (int) v.getY() || (int) item.getX() + item.getWidth() <= (int) v.getX() || item == v)
////                    continue;
////
////                if ((int) item.getX() <= v.getX() + v.getWidth())
////                    return true;
////            }
////        }
//        for (View item : m_originalViewList) {
//            if (item != v &&
//                    (int) item.getY() == (int) v.getY() &&
//                    (int) item.getX() >= (int) v.getX() + v.getWidth() &&
//                    (int) item.getX() <= (int) v.getX() + v.getWidth())
//                return true;
//        }
//
//        return false;
//    }

    private final static Comparator<TextView> noteViewComparator = new Comparator<TextView>() {
        public int compare(TextView lhs, TextView rhs) {
            if ((int) lhs.getX() == (int) rhs.getX())
                return (int) lhs.getY() - (int) rhs.getY();
            else
                return (int) lhs.getX() - (int) rhs.getX();
        }
    };

}
