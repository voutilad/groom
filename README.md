```
          _____ ______  _____  _____ ___  ___
         |  __ \| ___ \|  _  ||  _  ||  \/  |
         | |  \/| |_/ /| | | || | | || .  . |
         | | __ |    / | | | || | | || |\/| |
         | |_\ \| |\ \ \ \_/ /\ \_/ /| |  | |
          \____/\_| \_| \___/  \___/ \_|  |_/

```
# groom
Do you like Doom?

Do you like Graph Databases?

Then this is for you!

## Pre-Requisites
You'll want to grab [my Chocolate-Doom fork](https://github.com/voutilad/chocolate-doom), build it, and have it configured to send telemetry events over UDP.

## Building
Use the gradle wrapper...

```bash
$ ./gradlew distTar
```

or...

```cmd
C:> gradlew.bat distZip
```

## Running
Assuming you've unpacked the dist zip/tar somewhere...

```
$./bin/gradle -h
```

Take a look at the args and then figure out what to set. I'll update the docs later :-)
