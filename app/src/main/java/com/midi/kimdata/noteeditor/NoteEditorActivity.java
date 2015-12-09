package com.midi.kimdata.noteeditor;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
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
import android.widget.NumberPicker;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.kimdata.values;
import com.leff.midi.MidiFile;
import com.leff.midi.MidiTrack;
import com.leff.midi.event.ProgramChange;
import com.leff.midi.event.meta.Tempo;
import com.leff.midi.event.meta.TimeSignature;
import com.view.kimdata.NoteHorizontalScrollView;
import com.view.kimdata.NoteVerticalScrollView;
import com.view.kimdata.NoteView;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class NoteEditorActivity extends AppCompatActivity implements View.OnTouchListener {
    public static final int KEY_RANGE = 84;

    public static int TOUCH_OFFSET = 0;

    public static final int BAR = 8;

    public static final int NOTE_DP = 36;
    public static final int NOTE_HEIGHT_DP = 24;

    public static final int PITCH_TOP = 119;
    public static final int PITCH_BOTTOM = 36;

    public static final int ON = 1;
    public static final int OFF = 2;
    public static final int PITCH = 3;
    public static final int VELOCITY = 4;

    public static final int SEMIQUAVER = 120;
    public static final int QUAVER = 240;
    public static final int CROTCHET = 480;
    public static final int MINIM = 960;
    public static final int SEMIBREVE = 1920;

    public enum NoteTouch {NONE, LEFT, MID, RIGHT}

    private HorizontalScrollView m_rulerHScrollView;
    private NoteHorizontalScrollView m_noteHScrollView;
    private NoteVerticalScrollView m_noteVScrollView;

    private DisplayMetrics m_metric;

    private LinearLayout m_rulerContainer;

    private FrameLayout m_noteContainer;

    private Switch m_editSwitch;

    private NoteView m_workNote;

    private Menu m_actionbarMenu;

    private boolean m_isCreatedNote;

    private NoteTouch m_noteMode;

    private ArrayList<NoteView> m_originalViewList;
    private ArrayList<ArrayList<NoteView>> m_undoViewList;
    private ArrayList<ArrayList<NoteView>> m_redoViewList;

    // for Key
    private ArrayList<TextView> m_keyViewList;
    private int m_keyViewBeforePitch;
    private Drawable m_keyViewBeforeDrawable;

    private int m_workNoteRight;

    private boolean m_isNoteMoved;
    private boolean m_isNoteCreated;

    private int m_beforeWidth; // To create note.
    private int m_beforeY; // Pitch to play sound.
    private int m_beforeX;

    private boolean m_isHoldNote; // To change note width.
    private boolean m_isEditDuration;

    // Project Information
    private int m_tempo;
    private int m_numerator; // For Time Signature
    private int m_denominators; // For Time Signature

    // Channel Information
    private int m_channel;
    private int m_program;

    private MediaPlayer m_player;
    private String m_tempSoundFile;

    private View m_timelineIndicator;
    private Handler m_indicatorTimer;

    private View m_editNoteIndicator;

    private boolean m_isPlaying;
    private boolean m_beforeSwitchState;
    private boolean m_isRepeat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_editor);

        m_metric = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(m_metric);

        m_isCreatedNote = false;
        m_noteMode = NoteTouch.NONE;
        m_isNoteMoved = false;
        m_isNoteCreated = false;

        m_beforeWidth = 0;
        m_beforeY = 0;
        m_beforeX = 0;

        m_isHoldNote = false;
        m_isEditDuration = false;

        m_channel = 0;

        m_tempo = 120;
        m_numerator = 4;
        m_denominators = 4;

        m_program = 0;

        m_keyViewBeforePitch = 0;

        m_player = new MediaPlayer();
        m_player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                m_isPlaying = false;

                m_indicatorTimer.postDelayed(null, 0);

                MenuItem playMenu = m_actionbarMenu.findItem(R.id.action_play_stop);
                playMenu.setIcon(R.mipmap.ic_play_arrow_white_48dp);

                m_editSwitch.setChecked(m_beforeSwitchState);

                if (m_isRepeat)
                    playStopSound();
            }
        });
        m_tempSoundFile = "playing.mid";

        m_isPlaying = false;
        m_isRepeat = false;

        m_indicatorTimer = new Handler();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        if (toolbar != null) {
            setSupportActionBar(toolbar);
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

        m_noteContainer.setOnTouchListener(this);

        LinearLayout noteContainerLayout = (LinearLayout) findViewById(R.id.noteContainerLayout);
        noteContainerLayout.addView(m_noteContainer);

        m_rulerContainer = (LinearLayout) findViewById(R.id.rulerContainer);

        m_originalViewList = new ArrayList<>();
        m_undoViewList = new ArrayList<>();
        m_redoViewList = new ArrayList<>();

        for (int i = 0; i < BAR; i++) {
            addBar(i);

            for (int j = 0; j < 7; j++) {
                ImageView iv = new ImageView(this);
                iv.setImageResource(R.drawable.stripe_pattern);
                m_noteContainer.addView(iv);

                int octaveHeight = getDPI(NOTE_HEIGHT_DP, m_metric) * 12;
                setFLRect(iv, i * getDPI(NOTE_DP * 4, m_metric), j * octaveHeight, getDPI(NOTE_DP * 4, m_metric), octaveHeight);
            }
        }

        m_timelineIndicator = new View(this);
        m_timelineIndicator.setBackgroundColor(getColor(R.color.colorOpacityWhite));
        m_noteContainer.addView(m_timelineIndicator);

        TypedValue tv = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true);
        int actionBarHeight = getResources().getDimensionPixelSize(tv.resourceId);

        setFLRect(m_timelineIndicator, 0 - getDPI(NOTE_DP, m_metric), actionBarHeight, getDPI(NOTE_DP, m_metric), getDPI(NOTE_HEIGHT_DP * KEY_RANGE, m_metric));

        // set scroll
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                // octave = 12, 4 octave + 1 key
                int moveOctave = 12 * 4 + 1;
                m_noteVScrollView.setScrollY(getDPI(NOTE_HEIGHT_DP, m_metric) * moveOctave);
            }
        }, 500);

        LinearLayout keyContainer = (LinearLayout) findViewById(R.id.keyContainer);

        m_keyViewList = new ArrayList<>();
        for (int i = 0; i < keyContainer.getChildCount(); i++) {
//            final MediaPlayer keyPlayer = new MediaPlayer();
            TextView keyView = (TextView) keyContainer.getChildAt(i);
            keyView.setOnTouchListener(keyTouchListener);
            Drawable baseBackground = keyView.getBackground();
            keyView.setTag(baseBackground);

            m_keyViewList.add(keyView);
        }

        FrameLayout rulerWholeContainer = (FrameLayout) findViewById(R.id.rulerWholeContainer);

        m_editNoteIndicator = new View(this);
        m_editNoteIndicator.setBackgroundColor(getColor(R.color.colorOpacityWhite));
        rulerWholeContainer.addView(m_editNoteIndicator);

        setFLRect(m_editNoteIndicator, 0, 0 - getDPI(NOTE_HEIGHT_DP, m_metric), getDPI(NOTE_DP, m_metric), getDPI(NOTE_HEIGHT_DP, m_metric));

        m_beforeSwitchState = m_editSwitch.isChecked();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.editor_menu, menu);

        m_actionbarMenu = menu;

        MenuItem instrumentMenu = m_actionbarMenu.findItem(R.id.action_instrument);
        instrumentMenu.setTitle(values.instruments[m_program]);

        MenuItem undoMenu = m_actionbarMenu.findItem(R.id.action_undo);
        undoMenu.setEnabled(false);

        MenuItem redoMenu = m_actionbarMenu.findItem(R.id.action_redo);
        redoMenu.setEnabled(false);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_instrument: {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Select instrument");
                builder.setItems(values.instruments, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();

                        m_program = which;

                        Toast toast = Toast.makeText(getApplicationContext(),
                                "Choose a " + values.instruments[m_program], Toast.LENGTH_SHORT);
                        toast.show();

                        MenuItem instMenu = m_actionbarMenu.findItem(R.id.action_instrument);
                        instMenu.setTitle(values.instruments[m_program]);
                    }

                });

                builder.show();
            }

            return true;
            case R.id.action_time_signature: {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Select time signature");
                builder.setItems(values.timeSignature, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();

                        String signature = values.timeSignature[which];
                        m_numerator = Integer.parseInt(signature.split("/")[0]);
                        m_denominators = Integer.parseInt(signature.split("/")[1]);

                        Toast toast = Toast.makeText(getApplicationContext(),
                                "Choose a " + values.timeSignature[which], Toast.LENGTH_SHORT);
                        toast.show();

                        MenuItem instMenu = m_actionbarMenu.findItem(R.id.action_time_signature);
                        instMenu.setTitle(values.timeSignature[which]);
                    }

                });

                builder.show();
            }

            return true;
            case R.id.action_tempo: {
                LayoutInflater inflater = (LayoutInflater)
                        getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                final View npView = inflater.inflate(R.layout.dialog_number_picker, null);
                final NumberPicker picker = (NumberPicker) npView.findViewById(R.id.pickerTempo);
                picker.setMinValue(50);
                picker.setMaxValue(250);
                picker.setValue(m_tempo);
                picker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
                AlertDialog dlg = new AlertDialog.Builder(this)
                        .setTitle("Choose a tempo")
                        .setView(npView)
                        .setPositiveButton("OK",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        m_tempo = picker.getValue();

                                        Toast toast = Toast.makeText(getApplicationContext(),
                                                "Choose a " + m_tempo + " BPM", Toast.LENGTH_SHORT);
                                        toast.show();

                                        MenuItem instMenu = m_actionbarMenu.findItem(R.id.action_tempo);
                                        instMenu.setTitle(m_tempo + " BPM");
                                    }
                                })
                        .setNegativeButton("Cancel",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                    }
                                })
                        .create();

                dlg.show();
            }

            return true;
            case R.id.action_play_stop:
                if (!m_isPlaying)
                    makeMIDIFile("/playing.mid");

                playStopSound();

                return true;
            case R.id.action_repeat:
                m_isRepeat = !m_isRepeat;

                MenuItem repeatMenu = m_actionbarMenu.findItem(R.id.action_repeat);
                if (m_isRepeat)
                    repeatMenu.setIcon(R.mipmap.ic_repeat_white_48dp);
                else
                    repeatMenu.setIcon(R.mipmap.ic_repeat_black_48dp);

                return true;
            case R.id.action_undo:
                if (m_undoViewList.size() > 0) {
                    ArrayList<NoteView> redoList = new ArrayList<>();
                    redoList.addAll(m_originalViewList);
                    m_redoViewList.add(redoList);

                    MenuItem redoMenu = m_actionbarMenu.findItem(R.id.action_redo);
                    redoMenu.setEnabled(true);

                    for (NoteView view : m_originalViewList)
                        m_noteContainer.removeView(view);

                    m_originalViewList.clear();
                    ArrayList<NoteView> viewList = m_undoViewList.get(m_undoViewList.size() - 1);
                    m_originalViewList.addAll(viewList);
                    m_undoViewList.remove(m_undoViewList.size() - 1);

                    for (NoteView view : m_originalViewList)
                        m_noteContainer.addView(view);

                    if (m_undoViewList.size() == 0) {
                        MenuItem undoMenu = m_actionbarMenu.findItem(R.id.action_undo);
                        undoMenu.setEnabled(false);
                    }
                }

                return true;
            case R.id.action_redo:
                if (m_redoViewList.size() > 0) {
                    ArrayList<NoteView> undoList = new ArrayList<>();
                    undoList.addAll(m_originalViewList);
                    m_undoViewList.add(undoList);

                    MenuItem undoMenu = m_actionbarMenu.findItem(R.id.action_undo);
                    undoMenu.setEnabled(true);

                    for (NoteView view : m_originalViewList)
                        m_noteContainer.removeView(view);

                    m_originalViewList.clear();
                    ArrayList<NoteView> viewList = m_redoViewList.get(m_redoViewList.size() - 1);
                    m_originalViewList.addAll(viewList);
                    m_redoViewList.remove(m_redoViewList.size() - 1);

                    for (NoteView view : m_originalViewList)
                        m_noteContainer.addView(view);

                    if (m_redoViewList.size() == 0) {
                        MenuItem redoMenu = m_actionbarMenu.findItem(R.id.action_redo);
                        redoMenu.setEnabled(false);
                    }
                }

                return true;
            case R.id.action_settings:
                makeMIDIFile("/exampleout.mid");

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

    private void makeMIDIFile(String filename) {
        MidiTrack tempoTrack = new MidiTrack();
        MidiTrack noteTrack = new MidiTrack();
        MidiTrack programTrack = new MidiTrack();

        TimeSignature ts = new TimeSignature();
        ts.setTimeSignature(m_numerator, m_denominators, TimeSignature.DEFAULT_METER, TimeSignature.DEFAULT_DIVISION);

        Tempo tempo = new Tempo();
        tempo.setBpm(m_tempo);

        tempoTrack.insertEvent(ts);
        tempoTrack.insertEvent(tempo);

        ProgramChange pc = new ProgramChange(0, m_channel, m_program);
        programTrack.insertEvent(pc);

        for (NoteView view : m_originalViewList) {
            noteTrack.insertNote(m_channel, view.getPitch(), view.getVelocity(), view.getTick(), view.getDuration());
        }

        ArrayList<MidiTrack> tracks = new ArrayList<>();
        tracks.add(tempoTrack);
        tracks.add(programTrack);
        tracks.add(noteTrack);

        if (isExternalStorageWritable()) {
            MidiFile midi = new MidiFile(MidiFile.DEFAULT_RESOLUTION, tracks);

            String dirPath = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS).getPath();
            m_tempSoundFile = dirPath + filename;
            File output = new File(m_tempSoundFile);

            try {
                midi.writeToFile(output);
            } catch (IOException e) {
                System.err.println(e);
            }
        }
    }

    public void playStopSound() {
        if (m_isPlaying) { // set stop
            m_isPlaying = false;

            m_player.stop();

            m_timelineIndicator.setX(0 - getDPI(NOTE_DP, m_metric));

            m_isPlaying = false;

            m_indicatorTimer.postDelayed(null, 0);

            MenuItem playMenu = m_actionbarMenu.findItem(R.id.action_play_stop);
            playMenu.setIcon(R.mipmap.ic_play_arrow_white_48dp);

            m_editSwitch.setChecked(m_beforeSwitchState);
        } else { // play
            try {
                File midiFile = new File(m_tempSoundFile);
                FileInputStream input = new FileInputStream(midiFile);
                m_player.reset();
                m_player.setDataSource(input.getFD());
                input.close();
                m_player.prepare();
                m_player.start();

                m_timelineIndicator.setX(0);

                m_isPlaying = true;

                // TODO : indicator timer
                m_indicatorTimer.postDelayed(new Runnable() {
                    public void run() {
                        if (m_isPlaying) {
                            int x = (int) m_timelineIndicator.getX();
                            m_timelineIndicator.setX(x + getDPI(NOTE_DP, m_metric));

                            m_indicatorTimer.postDelayed(this, (long) (1000 / (m_tempo / 60.0)));
                        }
                    }
                }, (long) (1000 / (m_tempo / 60.0)));

                MenuItem playMenu = m_actionbarMenu.findItem(R.id.action_play_stop);
                playMenu.setIcon(R.mipmap.ic_stop_white_48dp);

                m_beforeSwitchState = m_editSwitch.isChecked();

                m_editSwitch.setChecked(false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    public static void setFLRect(View view, int x, int y, int width, int height) {
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) view.getLayoutParams();
        params.width = width;
        params.height = height;
        view.setX(x);
        view.setY(y);
        view.setLayoutParams(params);
    }

    public static void setLLRect(View view, int x, int y, int width, int height) {
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) view.getLayoutParams();
        params.width = width;
        params.height = height;
        view.setX(x);
        view.setY(y);
        view.setLayoutParams(params);
    }

    public static int getDPI(int size, DisplayMetrics metrics) {
        return (size * metrics.densityDpi) / DisplayMetrics.DENSITY_DEFAULT;
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

    private View.OnTouchListener keyTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int pitch = PITCH_TOP - ((int) (v.getY() + event.getY()) / getDPI(NOTE_HEIGHT_DP, m_metric));

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    m_keyViewBeforeDrawable = v.getBackground();
                    v.setBackgroundResource(R.drawable.key_pressed);

                    m_keyViewBeforePitch = pitch;

                    playPitch(pitch);

                    m_noteVScrollView.setLockScroll(true);

                    return true;

                case MotionEvent.ACTION_MOVE:
                    if (pitch != m_keyViewBeforePitch) {
                        View newView = m_keyViewList.get(PITCH_TOP - pitch);
                        View beforeView = m_keyViewList.get(PITCH_TOP - m_keyViewBeforePitch);

                        beforeView.setBackground(m_keyViewBeforeDrawable);

                        m_keyViewBeforeDrawable = newView.getBackground();
                        newView.setBackgroundResource(R.drawable.key_pressed);

                        m_keyViewBeforePitch = pitch;

                        playPitch(pitch);
                    }

                    return true;

                case MotionEvent.ACTION_UP:
                    for (View keyView : m_keyViewList) {
                        Drawable baseDrawable = (Drawable) keyView.getTag();
                        keyView.setBackground(baseDrawable);
                    }

                    m_noteVScrollView.setLockScroll(false);

                    return true;
            }

            return false;
        }
    };

    private View.OnTouchListener noteTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (!m_editSwitch.isChecked() || m_isPlaying)
                return true;

            int sideArea;

            if (v.getWidth() / 4 >= (int) getResources().getDimension(R.dimen.note_side_area)) {
                sideArea = v.getWidth() / 4;
            } else {
                sideArea = (int) getResources().getDimension(R.dimen.note_side_area);
            }

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (event.getX() <= sideArea) {
                        m_noteMode = NoteTouch.LEFT;
                        m_isNoteMoved = true;
                    } else if (event.getX() >= v.getWidth() - sideArea) {
                        m_noteMode = NoteTouch.RIGHT;
                        m_isNoteMoved = true;
                    } else {
                        m_noteMode = NoteTouch.MID;
                        m_isNoteMoved = false;

                        TOUCH_OFFSET = ((v.getWidth() / 2) / getDPI(NOTE_DP, m_metric)) * getDPI(NOTE_DP, m_metric);
                    }

                    m_workNote = (NoteView) v;

                    m_workNoteRight = (int) (v.getX() + v.getWidth());

                    m_isHoldNote = true;
                    m_isEditDuration = false;

                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        public void run() {
                            if (m_isHoldNote)
                                m_isEditDuration = true;

                            if (m_workNote != null && m_isEditDuration)
                                m_workNote.setBackgroundResource(R.drawable.note_hold);
                        }
                    }, 1000);

                    // Note indicator
                    setFLRect(m_editNoteIndicator,
                            (int) m_workNote.getX(),
                            0,
                            m_workNote.getWidth(),
                            getDPI(NOTE_HEIGHT_DP, m_metric));

                    break;
                case MotionEvent.ACTION_MOVE:

                    break;
                case MotionEvent.ACTION_UP:

                    break;
            }

            return false;
        }
    };

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (!m_editSwitch.isChecked() || m_isPlaying)
            return true;

        int x = (int) (event.getX() - (event.getX() % getDPI(NOTE_DP, m_metric))) - TOUCH_OFFSET;
        int y = (int) (event.getY() - (event.getY() % getDPI(NOTE_HEIGHT_DP, m_metric)));

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (m_noteMode == NoteTouch.NONE) {
                    NoteView noteView = new NoteView(this);
                    m_noteContainer.addView(noteView);
                    noteView.setBackgroundResource(R.drawable.note);
                    noteView.setVelocity(127);

//                tv.setBackgroundColor(getColor(R.color.colorPrimary));

                    noteView.setOnTouchListener(noteTouchListener);

                    int noteWidth = m_beforeWidth;

                    if (noteWidth == 0)
                        noteWidth = getDPI(NOTE_DP, m_metric);

                    setFLRect(noteView, x, y, noteWidth, getDPI(NOTE_HEIGHT_DP, m_metric));

                    m_workNote = noteView;

                    m_beforeY = y;
                    m_beforeX = x;

                    m_isNoteCreated = true;

                    // TODO: play sound
                    int pitch = PITCH_TOP - ((int) m_workNote.getY() / getDPI(NOTE_HEIGHT_DP, m_metric));

                    View newView = m_keyViewList.get(PITCH_TOP - pitch);

                    m_keyViewBeforeDrawable = newView.getBackground();
                    newView.setBackgroundResource(R.drawable.key_pressed);

                    m_keyViewBeforePitch = pitch;

                    playPitch(pitch);

                    // Note indicator
                    setFLRect(m_editNoteIndicator,
                            x,
                            0,
                            noteWidth,
                            getDPI(NOTE_HEIGHT_DP, m_metric));
                }

                m_isCreatedNote = true;

                m_noteHScrollView.setLockScroll(true);
                m_noteVScrollView.setLockScroll(true);

                return true;
            case MotionEvent.ACTION_MOVE:
                if ((m_isCreatedNote && m_noteMode == NoteTouch.NONE) ||
                        (m_noteMode == NoteTouch.MID && !m_isEditDuration)) {
                    m_workNote.setX(x);
                    m_workNote.setY(y);

                    if (m_beforeY != y) {
                        m_beforeY = y;

                        m_isHoldNote = false;

                        // TODO: play sound
                        int pitch = PITCH_TOP - ((int) m_workNote.getY() / getDPI(NOTE_HEIGHT_DP, m_metric));

                        View newView = m_keyViewList.get(PITCH_TOP - pitch);
                        View beforeView = m_keyViewList.get(PITCH_TOP - m_keyViewBeforePitch);

                        beforeView.setBackground(m_keyViewBeforeDrawable);

                        m_keyViewBeforeDrawable = newView.getBackground();
                        newView.setBackgroundResource(R.drawable.key_pressed);

                        m_keyViewBeforePitch = pitch;

                        playPitch(pitch);
                    }
                } else if (m_noteMode == NoteTouch.LEFT) {
                    if (m_workNoteRight - x <= 0)
                        return true;

                    setFLRect(m_workNote,
                            x,
                            (int) m_workNote.getY(),
                            m_workNoteRight - x,
                            getDPI(NOTE_HEIGHT_DP, m_metric));
                } else if (m_noteMode == NoteTouch.RIGHT) {
                    if ((int) (x - m_workNote.getX()) + getDPI(NOTE_DP, m_metric) <= 0)
                        return true;

                    setFLRect(m_workNote,
                            (int) m_workNote.getX(),
                            (int) m_workNote.getY(),
                            (int) (x - m_workNote.getX()) + getDPI(NOTE_DP, m_metric),
                            getDPI(NOTE_HEIGHT_DP, m_metric));
                }

                // Edit note duration
                if (m_isEditDuration && m_workNote.getX() + (m_workNote.getWidth() / 2) > event.getX()) {
                    setFLRect(m_workNote,
                            x,
                            (int) m_workNote.getY(),
                            m_workNoteRight - x + TOUCH_OFFSET,
                            getDPI(NOTE_HEIGHT_DP, m_metric));
                } else if (m_isEditDuration && m_workNote.getX() + (m_workNote.getWidth() / 2) < event.getX()) {
                    setFLRect(m_workNote,
                            (int) m_workNote.getX(),
                            (int) m_workNote.getY(),
                            (int) (x - m_workNote.getX()) + getDPI(NOTE_DP, m_metric) + TOUCH_OFFSET,
                            getDPI(NOTE_HEIGHT_DP, m_metric));
                }

                if (Math.abs(m_beforeX - x) > getDPI(NOTE_DP / 2, m_metric)) {
                    m_beforeX = x;

                    m_isHoldNote = false;
                }

                m_isNoteMoved = true;

                // Note indicator
                setFLRect(m_editNoteIndicator,
                        (int) m_workNote.getX(),
                        0,
                        m_workNote.getWidth(),
                        getDPI(NOTE_HEIGHT_DP, m_metric));

                return true;
            case MotionEvent.ACTION_UP:
                // Undo, Redo List
                for (ArrayList<NoteView> itemList : m_redoViewList)
                    itemList.clear();
                m_redoViewList.clear();

                ArrayList<NoteView> viewList = new ArrayList<>();
                viewList.addAll(m_originalViewList);
                m_undoViewList.add(viewList);

                MenuItem undoMenu = m_actionbarMenu.findItem(R.id.action_undo);
                undoMenu.setEnabled(true);

                if (m_undoViewList.size() > 10)
                    m_undoViewList.remove(0);

                if (m_isNoteCreated)
                    m_originalViewList.add(m_workNote);

                m_beforeWidth = m_workNote.getWidth();

                // Created note and moved.
                if (m_noteMode == NoteTouch.NONE) {
                    ViewManager parent = (ViewManager) m_workNote.getParent();

                    // Note on same position, out of screen, remove.
                    if (isExistSamePosition(m_workNote) ||
                            x < 0 || x >= m_noteContainer.getWidth() ||
                            y < 0 || y >= m_noteContainer.getHeight()) {
                        parent.removeView(m_workNote);

                        m_originalViewList.remove(m_workNote);

                        m_workNote = null;
                    }
                }

                try {
                    // Not moved, just touch middle of note. Remove.
                    if (m_noteMode == NoteTouch.MID && !m_isNoteMoved && !m_isEditDuration) {
                        ViewManager parent = (ViewManager) m_workNote.getParent();

                        parent.removeView(m_workNote);

                        m_originalViewList.remove(m_workNote);

                        m_workNote = null;
                    }
                } catch (NullPointerException e) {
                    Log.i("junu", "null in check touch mid");
                }

                try {
                    if (m_noteMode != NoteTouch.NONE && m_workNote != null) {
                        // Out of screen, replace.
                        if (m_workNote.getX() < 0)
                            m_workNote.setX(0);
                        else if ((int) m_workNote.getX() + m_workNote.getWidth() > m_noteContainer.getWidth())
                            m_workNote.setX(m_noteContainer.getWidth() - m_workNote.getWidth());

                        if (m_workNote.getY() < 0)
                            m_workNote.setY(0);
                        else if ((int) m_workNote.getY() + m_workNote.getHeight() > m_noteContainer.getHeight())
                            m_workNote.setY(m_noteContainer.getHeight() - m_workNote.getHeight());
                    }
                } catch (NullPointerException e) {
                    Log.i("junu", "null in replace");
                }

                if (m_workNote != null)
                    m_workNote.setBackgroundResource(R.drawable.note);

                // Calculate pitch, on, off
                if (m_workNote != null) {
                    int pitch = PITCH_TOP - ((int) m_workNote.getY() / getDPI(NOTE_HEIGHT_DP, m_metric));

                    m_workNote.setPitch(pitch);
                    m_workNote.setTick((int) m_workNote.getX() / getDPI(NOTE_DP, m_metric) * CROTCHET);
                    m_workNote.setDuration(m_workNote.getWidth() / getDPI(NOTE_DP, m_metric) * CROTCHET);
                }

                Collections.sort(m_originalViewList, noteViewComparator);

                // Initialize.
                m_workNote = null;

                m_isCreatedNote = false;
                m_isNoteMoved = false;
                m_isNoteCreated = false;

                m_noteMode = NoteTouch.NONE;

                TOUCH_OFFSET = 0;

                m_isHoldNote = false;
                m_isEditDuration = false;

                for (View keyView : m_keyViewList) {
                    Drawable baseDrawable = (Drawable) keyView.getTag();
                    keyView.setBackground(baseDrawable);
                }

                // Note indicator
                setFLRect(m_editNoteIndicator,
                        x,
                        0,
                        0,
                        getDPI(NOTE_HEIGHT_DP, m_metric));

                m_noteHScrollView.setLockScroll(false);
                m_noteVScrollView.setLockScroll(false);

                return true;
        }

        return false;
    }

    private boolean isExistSamePosition(View view) {
        for (View item : m_originalViewList) {
            if (item != view &&
                    (int) item.getX() == (int) view.getX() &&
                    (int) item.getY() == (int) view.getY())
                return true;
        }

        return false;
    }

    private void playPitch(int pitch) {
        MidiTrack tempoTrack = new MidiTrack();
        MidiTrack noteTrack = new MidiTrack();
        MidiTrack programTrack = new MidiTrack();

        TimeSignature ts = new TimeSignature();
        ts.setTimeSignature(4, 4, TimeSignature.DEFAULT_METER, TimeSignature.DEFAULT_DIVISION);

        Tempo tempo = new Tempo();
        tempo.setBpm(60);

        tempoTrack.insertEvent(ts);
        tempoTrack.insertEvent(tempo);

        ProgramChange pc = new ProgramChange(0, 0, m_program);
        programTrack.insertEvent(pc);

        noteTrack.insertNote(m_channel, pitch, 127, 0, CROTCHET);

        ArrayList<MidiTrack> tracks = new ArrayList<>();
        tracks.add(tempoTrack);
        tracks.add(programTrack);
        tracks.add(noteTrack);

        if (isExternalStorageWritable()) {
            MidiFile midi = new MidiFile(MidiFile.DEFAULT_RESOLUTION, tracks);

            String dirPath = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS).getPath();
            String keyFilename = dirPath + "/key.mid";
            File output = new File(keyFilename);

            try {
                midi.writeToFile(output);

                FileInputStream input = new FileInputStream(output);
                MediaPlayer keyPlayer = new MediaPlayer();
                keyPlayer.reset();
                keyPlayer.setDataSource(input.getFD());
                input.close();
                keyPlayer.prepare();
                keyPlayer.start();
                keyPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        mp.release();
                    }
                });
            } catch (IOException e) {
                System.err.println(e);
            }
        }
    }

    private final static Comparator<NoteView> noteViewComparator = new Comparator<NoteView>() {
        public int compare(NoteView lhs, NoteView rhs) {
            if ((int) lhs.getX() == (int) rhs.getX())
                return (int) lhs.getY() - (int) rhs.getY();
            else
                return (int) lhs.getX() - (int) rhs.getX();
        }
    };

}
