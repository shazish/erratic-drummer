package my.proj;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Simple MIDI file exporter for drum patterns
 * Creates basic MIDI files without external dependencies
 */
public class MidiExporter {
    private static final String TAG = "MidiExporter";
    
    // MIDI constants
    private static final int MIDI_CHANNEL = 9; // Channel 10 (0-indexed) - Standard drums
    private static final int TICKS_PER_QUARTER = 480; // Standard MIDI resolution
    
    // Standard MIDI drum notes (General MIDI)
    private static final int[][] DRUM_NOTES = {
        // Jazz sound bank mapping
        {38, 37, 41, 43, 45, 47, 48, 50}, // Snare/rim/toms
        {36, 35}, // Kick drums
        {42, 44, 46}, // Hi-hats
        {39, 54, 56, 58} // Percussion
    };
    
    /**
     * Export current drum pattern to MIDI file
     * @param context Application context
     * @param filename Name of the MIDI file to create
     * @return true if export successful, false otherwise
     */
    public static boolean exportCurrentPattern(Context context, String filename) {
        try {
            // Create output file
            File musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
            if (!musicDir.exists()) {
                musicDir.mkdirs();
            }
            
            File midiFile = new File(musicDir, filename + ".mid");
            FileOutputStream fos = new FileOutputStream(midiFile);
            
            // Write MIDI file
            writeMidiFile(fos);
            fos.close();
            
            Log.i(TAG, "MIDI file exported successfully: " + midiFile.getAbsolutePath());
            return true;
            
        } catch (IOException e) {
            Log.e(TAG, "Error exporting MIDI file: " + e.getMessage());
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error in MIDI export: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Write MIDI file format with drum pattern
     */
    private static void writeMidiFile(FileOutputStream fos) throws IOException {
        // MIDI Header Chunk
        writeMidiHeader(fos);
        
        // MIDI Track Chunk
        writeMidiTrack(fos);
    }
    
    /**
     * Write MIDI header chunk (MThd)
     */
    private static void writeMidiHeader(FileOutputStream fos) throws IOException {
        // MThd chunk
        fos.write("MThd".getBytes());
        writeInt32BE(fos, 6); // Header length
        writeInt16BE(fos, 0); // Format type 0 (single track)
        writeInt16BE(fos, 1); // Number of tracks
        writeInt16BE(fos, TICKS_PER_QUARTER); // Ticks per quarter note
    }
    
    /**
     * Write MIDI track chunk with drum pattern
     */
    private static void writeMidiTrack(FileOutputStream fos) throws IOException {
        // Build track data first
        byte[] trackData = buildTrackData();
        
        // MTrk chunk
        fos.write("MTrk".getBytes());
        writeInt32BE(fos, trackData.length); // Track length
        fos.write(trackData); // Track data
    }
    
    /**
     * Build track data with drum pattern
     */
    private static byte[] buildTrackData() throws IOException {
        java.io.ByteArrayOutputStream track = new java.io.ByteArrayOutputStream();
        
        // Set tempo (120 BPM = 500000 microseconds per quarter note)
        int tempo = DrumPatternCore.tempo;
        int microsecondsPerQuarter = 60000000 / tempo;
        
        // Tempo meta event
        writeVariableLength(track, 0); // Delta time
        track.write(0xFF); // Meta event
        track.write(0x51); // Set tempo
        track.write(0x03); // Length
        writeInt24BE(track, microsecondsPerQuarter);
        
        // Time signature 4/4
        writeVariableLength(track, 0); // Delta time
        track.write(0xFF); // Meta event
        track.write(0x58); // Time signature
        track.write(0x04); // Length
        track.write(0x04); // Numerator
        track.write(0x02); // Denominator (2^2 = 4)
        track.write(0x18); // Clocks per metronome tick
        track.write(0x08); // 32nd notes per quarter note
        
        // Generate drum pattern
        int ticksPerBeat = TICKS_PER_QUARTER / 4; // 16th note resolution
        int noteDuration = ticksPerBeat / 4; // Short drum hits
        
        for (int beat = 0; beat < DrumPatternCore.length; beat++) {
            for (int inst = 0; inst < DrumPatternCore.MAX_SIMULTANEOUS_INST; inst++) {
                int group = DrumPatternCore.group[inst][beat];
                int instrument = DrumPatternCore.instrument[inst][beat];
                
                if (group != -1 && instrument != -1 && group < DRUM_NOTES.length) {
                    // Get MIDI note for this drum
                    int[] groupNotes = DRUM_NOTES[group];
                    int noteIndex = instrument % groupNotes.length;
                    int midiNote = groupNotes[noteIndex];
                    
                    // Calculate timing
                    int noteTime = beat * ticksPerBeat;
                    
                    // Note On event
                    writeVariableLength(track, noteTime);
                    track.write(0x90 | MIDI_CHANNEL); // Note On, channel 10
                    track.write(midiNote);
                    track.write(100); // Velocity
                    
                    // Note Off event
                    writeVariableLength(track, noteDuration);
                    track.write(0x80 | MIDI_CHANNEL); // Note Off, channel 10
                    track.write(midiNote);
                    track.write(0); // Velocity
                }
            }
        }
        
        // End of track
        writeVariableLength(track, 0);
        track.write(0xFF);
        track.write(0x2F);
        track.write(0x00);
        
        return track.toByteArray();
    }
    
    /**
     * Write 32-bit big-endian integer
     */
    private static void writeInt32BE(FileOutputStream fos, int value) throws IOException {
        fos.write((value >> 24) & 0xFF);
        fos.write((value >> 16) & 0xFF);
        fos.write((value >> 8) & 0xFF);
        fos.write(value & 0xFF);
    }
    
    /**
     * Write 16-bit big-endian integer
     */
    private static void writeInt16BE(FileOutputStream fos, int value) throws IOException {
        fos.write((value >> 8) & 0xFF);
        fos.write(value & 0xFF);
    }
    
    /**
     * Write 24-bit big-endian integer
     */
    private static void writeInt24BE(java.io.ByteArrayOutputStream fos, int value) throws IOException {
        fos.write((value >> 16) & 0xFF);
        fos.write((value >> 8) & 0xFF);
        fos.write(value & 0xFF);
    }
    
    /**
     * Write variable length quantity (MIDI format)
     */
    private static void writeVariableLength(java.io.ByteArrayOutputStream fos, int value) throws IOException {
        if (value == 0) {
            fos.write(0);
            return;
        }
        
        int buffer = value & 0x7F;
        while ((value >>= 7) > 0) {
            buffer <<= 8;
            buffer |= 0x80;
            buffer += (value & 0x7F);
        }
        
        while (true) {
            fos.write(buffer & 0xFF);
            if ((buffer & 0x80) != 0) {
                buffer >>= 8;
            } else {
                break;
            }
        }
    }
}