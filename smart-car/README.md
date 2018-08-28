# Smart car - Road Safety Project

The Android Things Car sample demonstrates how to create a “smart” car.
The sample captures photos , location , and other user health data and upload it to fire base
and application will receive real time notification.
Solution will gather data from all sensors(installed) and manipulate to detect driver driving behavior , accident collosion are , predict accident.
Machine learning to predict accident (on basis of pre-uploaded data).
Personalize notification in faimly members voice  

## Pre-requisites

- Android Things compatible board
- Android Things compatible camera (for example, the Raspberry Pi 3 camera module)
- Android Studio 2.2+
- Google Cloud project with Cloud Vision API enabled
- Firebase project with Database and Storage
- The following individual components:
    - 1 push button
    - 1 resistor
    - jumper wires
    - 1 breadboard
    - PIR Sensor
    - Accelerometer
    - RFID Tag Receiver
    - Toy car
    -Temprature Sensor
    -Rain drop sensor
    -Battery/Power bank
    -switch
    -Pulse Sensor
    -Ethonal/Alcohol Sensor

## Setup and Build

To setup, follow these steps below.

1.  Add a valid Google Cloud Vision API key in the constant `CloudVisionUtils.CLOUD_VISION_API_KEY`
  - Create a Google Cloud Platform (GCP) project on [GCP Console](https://console.cloud.google.com/)
  - Enable Cloud Vision API under Library
  - Add an API key under Credentials
  - Copy and paste the Cloud Vision API key to the constant in `CloudVisionUtils.java`

2.  Add a valid `google-services.json` from Firebase to `app/` and
    `companionApp/`
  - Create a Firebase project on [Firebase Console](https://console.firebase.google.com)
  - Add an Android app with your specific package name in the project
  - Download the auto-generated `google-services.json` and save to `app/` and `companionApp/` folders

3.  Ensure the security rules for your Firebase project allow public read/write
    access. **Note:** The rules in this section are set to public read/write for
    demonstration purposes only.
  - Firebase -> Database -> Rules:

          {
            "rules": {
              ".read": true,
              ".write": true
            }
          }

  - Firebase -> Storage -> Rules:

          service firebase.storage {
            match /b/{bucket}/o {
              match /{allPaths=**} {
                allow read, write;
              }
            }
          }


There are three modules: `app` ,`companionAppForEmergecyHelp` and `companionAppForDashboard`, the former is on device while the latter on
companion device e.g. Android phone.

## Running

