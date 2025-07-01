package my.proj;

/**
 * Created by shaahinshahbazi on 4/16/15.
 */
public class DrumPatternCore {

    public static final int MAX_SIMULTANEOUS_INST = 2;
    public static final int MAX_RIFF_LENGTH = 16;
    public static final int MIN_RIFF_LENGTH = 4;
    public static final int START_RIFF_LENGTH = 8; // used in main.java
    public static final int MAX_TEMPO = 130;
    public static final int MIN_TEMPO = 80;
    public static final int START_TEMPO = 100;

    static int drum_type = 0;
    static int tempo;
    static int length;
    static int timesToRepeat;
    static int [][] group = new int [MAX_SIMULTANEOUS_INST][MAX_RIFF_LENGTH];; // [simultaneous-instrument #][beat #] -> instrument-type
    static int [][] instrument = new int [MAX_SIMULTANEOUS_INST][MAX_RIFF_LENGTH];; // [simultaneous-instrument #][beat #] -> instrument

}
