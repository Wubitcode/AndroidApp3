# AI Reflection – AndroidApp3 Treasure Hunt

## 1. How Did I Use AI in This Assignment?

- I used AI mainly as a guide while building the Treasure Hunt app step by step.
- AI helped me understand how to connect a map, location permission, GPS coordinates,
  treasure markers, clues, and distance checking.
- One example was when I had an error with LocationManagerCompat.getCurrentLocation().
  AI helped me understand that Kotlin could not determine which overloaded method to use,
  so I changed the CancellationSignal type explicitly.
- I also researched alternatives to Google Maps because Google Maps required billing setup.
  I learned how to use MapLibre with OpenFreeMap instead.

## 2. How Did I Understand, Verify, and Adapt the Code?

- I verified changes by running ./gradlew build after major updates.
- I tested the app using the Android emulator and simulated Toronto GPS locations with adb.
- I did not just paste all the code at once. I tested City Hall first, then individual
  treasure locations, the permission dialog, the Check My Location button, and the
  Treasure Reached message.
- I changed the app so only the current treasure marker is visible instead of showing
  every future destination.

## 3. What Did I Learn or Get Better At?

- I became more comfortable working with Android location permissions and GPS coordinates.
- I learned how latitude and longitude can be compared using Location.distanceBetween().
- I learned how Android activity lifecycle methods work with MapView.
- I also learned how repositories help separate location data from MainActivity logic.
- One challenge was resolving dependency and location API errors, but testing each step
  helped me understand where the problems came from.