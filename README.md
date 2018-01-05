# IoTSenseHat
The aim of this project is to demonstrate the use of a Raspberry SenseHat in combination with Android Things and Google IoT Core.

## Get started
Clone both project into the same directory.
```
git clone -b MacroYau-sensehat git@github.com:Ebolon/contrib-drivers.git
git clone git@github.com:Ebolon/iotsensehat.git
```
Open the `iotsense` project with Android Studio. In the following you can follow the documentation of the [original demo](https://github.com/androidthings/sensorhub-cloud-iot) to prepare and register the device. Keep in mind to change the namespace in the commands to `de.justif.iotsensehat`.

## Open issues
Currently support for the LSM9DS1 is missing.

## Related projects
This project is build on top of the [Android Things Cloud IoT Sensor Hub Demo](https://github.com/androidthings/sensorhub-cloud-iot) and uses a [modified version](https://github.com/Ebolon/contrib-drivers/tree/MacroYau-sensehat) of the SenseHat driver implementation from MacroYou. The original [pull request](https://github.com/androidthings/contrib-drivers/pull/24) for contrib-drivers is still pending.