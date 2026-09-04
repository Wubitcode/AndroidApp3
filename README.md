AndroidApp3 – Toronto Treasure Hunt

1. Course Information

**Course Code:** MWD3B  
**Course Name:** Android Development  
**Assignment:** Assignment 6  
**Project:** Toronto Treasure Hunt  
**Language:** Kotlin  
**IDE:** Android Studio



2. Project Overview

AndroidApp3 is a location-based Toronto Treasure Hunt application developed for MWD3B Android Development.

The project began in Assignment 5 and was expanded and polished in Assignment 6.

The application was inspired by the location-based concepts explored in the PlaceBook tutorial. The Treasure Hunt begins at Toronto City Hall and guides participants through 20 local Toronto business destinations.

There are 21 total Treasure Hunt locations:

•	Toronto City Hall as the official starting point
•	20 Toronto business destinations

Participants must reach each destination in sequence. Only the currently unlocked treasure is shown on the map. After successfully reaching a destination, the application records the progress, reveals the next clue, and unlocks the next treasure.

Assignment 6 adds Room database persistence, treasure detail and progress screens, camera/photo support, and improved user-interface resources.

After completing all required locations, the participant becomes eligible to enter the anniversary vacation draw.







3. Main Features

Mapping and Location

•	Interactive Toronto map using MapLibre
•	OpenFreeMap map style
•	No Google Maps API key or billing account required
•	Toronto City Hall as the starting location
•	20 additional Toronto treasure destinations
•	Only the currently active treasure marker is displayed
•	Participant location displayed on the map
•	Runtime Android location permission handling
•	Precise and approximate location support
•	Android `LocationManager` integration
•	GPS location checking with network fallback
•	Distance calculation using `Location.distanceBetween()`
•	75-metre treasure completion radius
•	**Check My Location** button

Treasure Hunt Progression

•	Sequential Treasure Hunt workflow
•	Current treasure remains active until completed
•	Future destinations remain hidden
•	Treasure Reached confirmation dialog
•	Next clue revealed after completion
•	Automatic unlocking of the next treasure
•	Final Treasure Hunt completion message
•	Vacation draw eligibility message

Persistent Room Database

Room is used to store:

•	Treasure destination information
•	Completion status
•	Treasure photo file paths

Participant progress remains available after the application is closed, reopened, or restarted.

The database was upgraded from version 1 to version 2 using a Room migration so existing Treasure Hunt progress could be preserved.


4. Treasure Progress Screen

Assignment 6 includes a dedicated Treasure Hunt Progress screen.

The progress screen displays:

	Number of completed treasures
	Completed destinations
	Currently active destination
	Current treasure clue
	Locked future destinations

Future destination names and clues remain hidden until they are unlocked.

Example:


1 of 21 treasures completed

Stop 1 — Toronto City Hall
✓ Completed

Stop 2 — Ben McNally Books
● Current Treasure

Stop 3 — Locked Treasure
🔒 Not Yet Unlocked


Participants can return to the main map using the **Back to Map** button.


5. Treasure Detail Screen

Participants can tap the currently active treasure marker to open a dedicated detail screen.

The Treasure Detail screen displays:

•	Treasure name
•	Street address
•	Current clue
•	Completion status
•	Treasure photo
•	Take Treasure Photo button
•	Back to Map button

The Treasure Detail Activity receives only the treasure ID from the main map Activity and retrieves the matching treasure information directly from Room.

This avoids unnecessary duplication of database information between Activities.



6. Treasure Photo Feature

Assignment 6 adds camera and photo support.

Participants can capture a photo for the currently selected treasure destination.

The photo workflow is:

Treasure Detail Screen
↓
Take Treasure Photo
↓
Android Camera Opens
↓
Photo Captured
↓
Photo Stored in App-Specific Storage
↓
Photo Path Saved in Room
↓
Photo Displayed on Detail Screen


Photos are stored inside the application's private external Pictures directory.

Example storage location:

Android/data/com.wubitcode.androidapp3/files/Pictures/treasure_photos/


When the treasure is opened again, the saved photo is loaded from the stored photo path and displayed on the detail screen.


7. FileProvider Security

Android `FileProvider` is used to securely share treasure photo files with the camera application.

The application does not expose direct filesystem paths. Instead, the camera receives temporary access through a secure content URI.

FileProvider configuration is stored in:

app/src/main/res/xml/file_paths.xml


The provider is registered in:

AndroidManifest.xml


Because treasure photos are stored inside the application's app-specific directory, general storage permission is not required.

8. Mapping Technology

The application uses MapLibre with OpenFreeMap instead of Google Maps.

This provides an interactive map without requiring:

	Google Maps API keys
	Google Cloud billing
	Credit card information

MapLibre is used to display:

	Toronto streets
	Active treasure marker
	Participant location marker
	Treasure clues
	Map camera positioning

Map style:


https://tiles.openfreemap.org/styles/liberty


9. Location Functionality

The application requests the following Android permissions:


ACCESS_FINE_LOCATION
ACCESS_COARSE_LOCATION


Android's `LocationManager` retrieves the participant's current position.

GPS is preferred when precise location permission is available. Network location is used as a fallback when necessary.

The distance between the participant and the active treasure is calculated using:

kotlin
Location.distanceBetween()


A treasure is considered reached when the participant is within approximately **75 metres** of the destination.


10. Treasure Hunt Flow

Toronto City Hall
↓
Check My Location
↓
Treasure Reached
↓
Completion Saved in Room
↓
Next Clue Revealed
↓
Next Treasure Unlocked
↓
Participant Travels to Destination
↓
Check My Location
↓
Continue Through All 21 Stops
↓
Treasure Hunt Complete
↓
Eligible for Vacation Draw


11. Assignment 6 Improvements

Assignment 6 expands the original Assignment 5 application with:

	Room database integration
	Persistent treasure completion status
	Room database migration from version 1 to version 2
	Treasure Detail Activity
	Treasure marker navigation
	Treasure Progress Activity
	Completed, current, and locked treasure states
	Camera integration
	Full-size treasure photo capture
	Secure FileProvider implementation
	Photo path persistence in Room
	Photo restoration after reopening a treasure
	Improved layout structure
	String resources instead of hardcoded UI text
	Professional Kotlin, XML, and Room documentation
	Android Lint and Gradle build verification

Assignment 5 remains preserved in Git history through the `assignment5-final` tag.


12. Project Structure


AndroidApp3
│
├── app
│   └── src
│       └── main
│           │
│           ├── java/com/wubitcode/androidapp3
│           │   ├── MainActivity.kt
│           │   ├── TreasureDetailActivity.kt
│           │   ├── TreasureProgressActivity.kt
│           │   │
│           │   ├── data
│           │   │   ├── TreasureDao.kt
│           │   │   ├── TreasureDatabase.kt
│           │   │   └── TreasureRepository.kt
│           │   │
│           │   └── model
│           │       └── TreasureLocation.kt
│           │
│           ├── res
│           │   ├── layout
│           │   │   ├── activity_main.xml
│           │   │   ├── activity_treasure_detail.xml
│           │   │   └── activity_treasure_progress.xml
│           │   │
│           │   ├── values
│           │   │   └── strings.xml
│           │   │
│           │   └── xml
│           │       └── file_paths.xml
│           │
│           └── AndroidManifest.xml
│
├── AIReflection.md
├── README.md
├── build.gradle.kts
└── settings.gradle.kts


13. Technologies Used

•	Kotlin
•	Android Studio
•	Android SDK
•	MapLibre
•	OpenFreeMap
•	Room Database
•	Room DAO
•	KSP
•	Kotlin Coroutines
•	Android Lifecycle
•	LocationManager
•	LocationManagerCompat
•	Activity Result APIs
•	FileProvider
•	Android Camera
•	XML Layouts
•	Git
•	GitHub


14. Testing

The application was tested using a Pixel 7 Android emulator.

Location testing can be simulated using Android Debug Bridge.

Example:

~/Library/Android/sdk/platform-tools/adb emu geo fix -79.3841 43.6534


Important:

adb emu geo fix LONGITUDE LATITUDE


Toronto City Hall

Longitude: -79.3841
Latitude: 43.6534


Ben McNally Books

Longitude: -79.36950
Latitude: 43.65199


Room persistence was verified by completing Toronto City Hall, restarting the application, and confirming that Ben McNally Books remained the current destination.

Photo persistence was verified by capturing a treasure photo, returning to the map, reopening the Treasure Detail screen, and confirming that the saved photo remained available.

15. Build and Quality Verification

The project was verified using:

./gradlew build

Result:

BUILD SUCCESSFUL


Android Lint was also executed using:

./gradlew lintDebug

BUILD SUCCESSFUL


These checks confirm that the project successfully compiles and completes the Android Lint task.


16. Code Documentation

The project includes professional documentation throughout the Kotlin and XML source files.

Comments explain:

o	Class responsibilities
o	Room database architecture
o	DAO operations
o	Repository responsibilities
o	Database migration
o	Location permission handling
o	Location calculations
o	Treasure progression
o	Activity navigation
o	Camera integration
o	FileProvider security
o	Photo persistence
o	Activity lifecycle management
o	MapLibre lifecycle management
o	Non-obvious implementation decisions

The documentation improves readability and maintainability without unnecessarily commenting basic Kotlin syntax.


•	17. Git and Version Control

The project is maintained using Git and GitHub.

**Repository:**  
https://github.com/Wubitcode/AndroidApp3

The `main` branch contains the current Assignment 6 implementation.

Assignment 5 was preserved before Assignment 6 development using:

assignment5-final


This provides a stable historical checkpoint while Assignment 6 continues in the same repository.


18. Assignment Status

Assignment 6 is complete and includes the required Treasure Hunt functionality together with persistent Room storage, progress tracking, treasure details, camera/photo support, professional comments, and successful build and lint verification.


