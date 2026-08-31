 AndroidApp3 – Toronto Treasure Hunt

1 Course Information

Course Code: MWD3B  
Course Name: Android Development  
Assignment: Assignment 5  
Project: Toronto Treasure Hunt  
Language: Kotlin  
IDE: Android Studio  

2  Project Overview

AndroidApp3 is a location-based Treasure Hunt application created for Android Development Assignment 5.

The application was inspired by the location concepts explored in the PlaceBook tutorial. 
The Treasure Hunt begins at Toronto City Hall and guides participants through 20 local Toronto businesses.

Each destination provides a clue leading to the next location. 
The participant must physically reach, or simulate reaching, the active destination before the next treasure is unlocked.

After completing all required locations, the participant becomes eligible to enter the anniversary vacation draw.

3 Main Features

- Interactive Toronto map using MapLibre
- OpenFreeMap / OpenStreetMap-based map data
- Toronto City Hall as the starting location
- 20 Toronto business treasure destinations
- Only the current treasure location is displayed
- Sequential clue-based progression
- Runtime location permission handling
- Precise and approximate location support
- GPS-based location checking
- Distance calculation between the user and treasure locations
- 75-metre treasure completion radius
- "Check My Location" button
- Treasure Reached confirmation dialogs
- Automatic unlocking of the next treasure
- Local treasure progress persistence
- Final Treasure Hunt completion message
- Professionally commented Kotlin code

4 Mapping Technology

The application uses MapLibre with OpenFreeMap instead of Google Maps.

This provides an interactive map without requiring a Google Maps API key, billing account, or credit card.

MapLibre is used to display:

- Toronto streets
- Treasure markers
- Current destination
- Participant location
- Map camera movement

5 Location Functionality

The application requests Android location permissions:

- ACCESS_FINE_LOCATION
- ACCESS_COARSE_LOCATION

Android's LocationManager is used to retrieve the participant's position.

The application calculates the distance between the participant and the active treasure using:

`Location.distanceBetween()`

A treasure is considered reached when the participant is within approximately 75 metres of the destination.


6 Treasure Hunt Flow

Toronto City Hall  
↓  
Treasure Reached  
↓  
Next clue is revealed  
↓  
Next business location appears  
↓  
Participant travels to the destination  
↓  
Check My Location  
↓  
Next treasure is unlocked  
↓  
Continue until all locations are completed

7 Project Structure


AndroidApp3
│
├── app
│   └── src
│       └── main
│           ├── java/com/wubitcode/androidapp3
│           │   ├── MainActivity.kt
│           │   ├── data
│           │   │   └── TreasureRepository.kt
│           │   └── model
│           │       └── TreasureLocation.kt
│           │
│           ├── res
│           │   └── layout
│           │       └── activity_main.xml
│           │
│           └── AndroidManifest.xml
│
├── AIReflection.md
├── build.gradle.kts
└── README.md
