package com.kimdata;

public class values {

    public static final String instruments[] = {
            "Acoustic Grand Piano",
            "Bright Acoustic Piano",
            "Electric Grand Piano",
            "Honky-tonk Piano",
            "Rhodes Piano",
            "Chorused Piano",
            "Harpsichord",
            "Clavinet",
            "Celesta",
            "Glockenspiel",
            "Music Box",
            "Vibraphone",
            "Marimba",
            "Xylophone",
            "Tubular Bells",
            "Dulcimer",
            "Hammond Organ",
            "Percussive Organ",
            "Rock Organ",
            "Church Organ",
            "Reed Organ",
            "Accordion",
            "Harmonica",
            "Tango Accordion",
            "Acoustic Nylon Guitar",
            "Acoustic Steel Guitar",
            "Electric Jazz Guitar",
            "Electric Clean Guitar",
            "Electric Muted Guitar",
            "Overdriven Guitar",
            "Distortion Guitar",
            "Guitar Harmonics",
            "Acoustic Bass",
            "Fingered Electric Bass",
            "Plucked Electric Bass",
            "Fretless Bass",
            "Slap Bass 1",
            "Slap Bass 2",
            "Synth Bass 1",
            "Synth Bass 2",
            "Violin",
            "Viola",
            "Cello",
            "Contrabass",
            "Tremolo Strings",
            "Pizzicato Strings",
            "Orchestral Harp",
            "Timpani",
            "String Ensemble 1",
            "String Ensemble 2",
            "Synth Strings 1",
            "Synth Strings 2",
            "Choir Aah",
            "Choir Ooh",
            "Synth Voice",
            "Orchestral Hit",
            "Trumpet",
            "Trombone",
            "Tuba",
            "Muted Trumpet",
            "French Horn",
            "Brass Section",
            "Synth Brass 1",
            "Synth Brass 2",
            "Soprano Sax",
            "Alto Sax",
            "Tenor Sax",
            "Baritone Sax",
            "Oboe",
            "English Horn",
            "Bassoon",
            "Clarinet",
            "Piccolo",
            "Flute",
            "Recorder",
            "Pan Flute",
            "Bottle Blow",
            "Shakuhachi",
            "Whistle",
            "Ocarina",
            "Square Wave Lead",
            "Sawtooth Wave Lead",
            "Calliope Lead",
            "Chiff Lead",
            "Charang Lead",
            "Voice Lead",
            "Fifths Lead",
            "Bass Lead",
            "New Age Pad",
            "Warm Pad",
            "Polysynth Pad",
            "Choir Pad",
            "Bowed Pad",
            "Metallic Pad",
            "Halo Pad",
            "Sweep Pad",
            "Rain Effect",
            "Soundtrack Effect",
            "Crystal Effect",
            "Atmosphere Effect",
            "Brightness Effect",
            "Goblins Effect",
            "Echoes Effect",
            "Sci-Fi Effect",
            "Sitar",
            "Banjo",
            "Shamisen",
            "Koto",
            "Kalimba",
            "Bagpipe",
            "Fiddle",
            "Shanai",
            "Tinkle Bell",
            "Agogo",
            "Steel Drums",
            "Woodblock",
            "Taiko Drum",
            "Melodic Tom",
            "Synth Drum",
            "Reverse Cymbal",
            "Guitar Fret Noise",
            "Breath Noise",
            "Seashore",
            "Bird Tweet",
            "Telephone Ring",
            "Helicopter",
            "Applause",
            "Gun Shot"
    };

    public static final String timeSignature[] = {
            "4/4",
            "2/4",
            "3/4",
            "12/8"
    };

    private static values ourInstance = new values();

    public static values getInstance() {
        return ourInstance;
    }

    private values() {
    }
}