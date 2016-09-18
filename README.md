# Augmented Reality barcode & QR code detector for >= Android 4.0 #

### What is this repository for? ###

* barcodes and qr codes can be detected in real time camera pictures. Depending on a self defined barcode database - barcodes are connected to commands which are responsible for e.g. showing images via augmented reality (on top of camera image) or playing a sound. Features include automatically taking of photos when a specific barcode is recognized.
* Version 1.0

### How do I get set up? ###

* IDE: Android Studio 1.5.1
* Android SDK
* Dependencies: ZXing (Zebra Crossing) library for barcode detection (automatically included by Gradle)
* Database configuration: Database is located in be.pxl.troger.ar.tools.BarcodeDatabase.java
* Database usage: the database exists out of a **key (barcode value)** and a **connected command**. The command is handled in be.pxl.troger.ar.views.OverlayView.java
* Images location: res/drawable | Sounds location: res/raw
* Automatically taking photos: Flag in CameraPreviewView

### Who do I talk to? ###

* Repo owner and developer: michael.troger@student.pxl.be
