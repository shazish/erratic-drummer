# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is "Erratic Drummer" - an Android drum machine application that generates randomized drum patterns. The app uses native C++ audio processing with the Oboe library for low-latency audio playback and supports multiple sound banks (jazz, metal, exotic).

## Build & Development Commands

### Building the Project
```bash
./gradlew assembleDebug          # Build debug APK
./gradlew assembleRelease        # Build release APK
./gradlew installDebug           # Install debug APK on connected device
```

### Testing
```bash
./gradlew test                   # Run unit tests
./gradlew connectedAndroidTest   # Run instrumented tests on device
```

### Clean Build
```bash
./gradlew clean                  # Clean build artifacts
./gradlew clean assembleDebug    # Clean and build debug
```

## Architecture Overview

### Core Components

1. **Main.java** (`my.proj.Main`) - Primary activity handling UI interactions, drum pattern playback controls, and sound bank switching
2. **SoundMaker.java** (`my.proj.SoundMaker`) - Core audio engine that manages pattern generation, sound playback, and threading
3. **DrumPatternCore.java** (`my.proj.DrumPatternCore`) - Static constants and data structures for drum patterns
4. **native-lib.cpp** - C++ audio engine using Oboe for low-latency audio processing
5. **DatabaseManager.java** - Handles saving/loading drum patterns
6. **LoadPopups.java** - UI dialogs for loading saved patterns and app information

### Audio Architecture

- **Native Layer**: C++ with Oboe library for low-latency audio rendering
- **Java Layer**: SoundMaker class manages pattern generation and playback threading
- **Sound Banks**: Three distinct sound sets (jazz, metal, exotic) with different instrument groupings
- **Pattern Generation**: Randomized drum patterns based on user-configurable density and instrument priorities

### Key Data Structures

- `DrumPatternCore.group[MAX_SIMULTANEOUS_INST][MAX_RIFF_LENGTH]` - Instrument group assignments
- `DrumPatternCore.instrument[MAX_SIMULTANEOUS_INST][MAX_RIFF_LENGTH]` - Specific instrument selections
- `SoundMaker.loadedSoundIds[][]` - Native sound IDs organized by instrument groups
- `SoundMaker.instGroupPriority[]` - User-configurable instrument group weights

## Development Notes

### Sound Bank Structure
Each sound bank has 4 instrument groups:
- **Jazz**: Snare.Rim.Tom, Kick, Hat, Perc
- **Metal**: Snare, Kick, Hat.Crash, Rim.Tom  
- **Exotic**: Dumbek, Tabla, Gourd, Sticks

### Threading Model
- UI thread handles all UI updates and user interactions
- SoundMaker runs on separate thread (`mSoundMakerThread`)
- Native audio processing runs on Oboe's audio thread
- AsyncTask used for database operations and sound bank loading

### Pattern Generation Algorithm
Patterns are generated using weighted random selection:
1. Random density check (35-80% chance based on density setting)
2. Weighted selection of instrument group based on user priorities
3. Random instrument selection within chosen group
4. Supports variable tempo and length based on preferences

### Build Configuration
- Target SDK: 36, Min SDK: 30
- Uses Gradle Version Catalog (libs.versions.toml)
- NDK integration for C++ audio processing
- Oboe library via Prefab for native audio
- libsndfile for audio file loading (requires manual inclusion)

### Key Files to Modify
- **Main.java**: UI logic, SeekBar handlers, button click listeners
- **SoundMaker.java**: Pattern generation, audio playback, threading
- **native-lib.cpp**: Low-level audio processing, sound loading
- **CMakeLists.txt**: Native build configuration
- **build.gradle.kts**: App configuration, dependencies

### Testing Strategy
- Unit tests in `ExampleUnitTest.java`
- Instrumented tests in `ExampleInstrumentedTest.java`
- Manual testing on physical devices recommended for audio functionality