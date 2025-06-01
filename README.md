# QuizAppfromCSV

A simple Android quiz application that loads questions from CSV files. This app allows users to import their own quiz questions through CSV files and take quizzes on their Android device.

## Features

- Load quiz questions from CSV files
- User-friendly quiz interface with real-time feedback
- Support for custom question sets through CSV imports
- Question grouping functionality
- Timer tracking for each question group
- Progress tracking with correct/incorrect answer counts

## Requirements

### For Users
- Android device running Android 8.0 (API level 26) or higher
- Storage permission to read CSV files
- CSV files with quiz questions in the correct format

### For Developers
- Android Studio Arctic Fox (2020.3.1) or newer
- JDK 11 or higher
- Android SDK with minimum API level 26
- Kotlin 1.8 or higher

## Building the App

1. Clone this repository:
```bash
git clone https://github.com/igalvit/QuizAppfromCSV.git
```

2. Open the project in Android Studio

3. Build the app using one of these methods:
   - Click the "Build" button in Android Studio
   - Run `./gradlew assembleDebug` from the command line
   - For release version: `./gradlew assembleRelease`

The APK file will be generated in:
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Release APK: `app/build/outputs/apk/release/app-release.apk`

## CSV File Format

Your CSV file must use semicolons (;) as separators and follow this format:
```
questionText;option1;option2;option3;option4;correctAnswer;group
What is 2+2?;3;4;5;6;B;Math
What is the capital of France?;London;Paris;Berlin;Madrid;B;Geography
```

### CSV Fields:
- questionText: The question to be asked
- option1 to option4: Multiple choice options
- correctAnswer: Must be A, B, C, or D (corresponding to options 1-4)
- group: Category or group name for the question (e.g., "Math", "1-50", etc.)

## Installation

1. Enable "Install from Unknown Sources" in your Android device settings
2. Transfer the APK to your Android device
3. Tap the APK file to install
4. Grant necessary permissions when prompted

## Usage

1. Launch the app
2. Select a CSV file containing your quiz questions
3. Start answering the questions
   - Timer starts automatically when questions are loaded
   - Timer resets when changing question groups
   - Timer stops when all questions in a group are answered
4. Use the group filter to switch between question sets
5. Track your progress with the score counter and timer
6. View your results at the end of the quiz

## Contributing

Feel free to submit issues and enhancement requests!

## License

This work is dedicated to the public domain under CC0 1.0 Universal.

To the extent possible under law, all copyright and related or neighboring rights to this work have been waived worldwide. This work is distributed without any warranty.

You can copy, modify, distribute and perform the work, even for commercial purposes, all without asking permission.

For more information about CC0 1.0 Universal, see:
https://creativecommons.org/publicdomain/zero/1.0/

A copy of the CC0 1.0 Universal license text can be found at:
https://creativecommons.org/publicdomain/zero/1.0/legalcode
