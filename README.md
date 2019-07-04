# SeMA
 A framework for designing secure Android apps.

# Steps to set up

- Download the source code for androidx.navigation framework
- Any new tags and attributes in navigation graphs not in the original source code should be added to navigation/../res/attr.xml
- Place the directory *navigation-checks* in /path/to/androidx-master-dev/frameworks/support
- Add the following line to androidx-master-dev/frameworks/support/settings.gradle:
    `includeProject(":navigation-checks","navigation-checks")`

# Steps to build the library
- Use Android Studio Build->Make Project

# Steps to run the analysis
- Copy the jar file in path/to/androidx-master-dev/out/androidx/navigation-checks/build/libs/ to ~/.android/lint/
- Navigate to the app folder where the analysis will be run and use the following command :
    `$ ./gradlew lint`
