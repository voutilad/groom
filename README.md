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
Assuming you've unpacked the dist zip/tar somewhere...you can use the helpful `bin/groom[.bat]` script to run the app:

```
$ ./build/distributions/groom-1.0-SNAPSHOT/bin/groom --help
usage: groom [-h] [--uri URI] [--user USER] [--password PASSWORD] [--tls TLS] [--db DB]
             [--udp-host UDP_HOST] [--udp-port UDP_PORT] [--buffer-size BUFFER_SIZE]
             [--flush-interval FLUSH_INTERVAL]

named arguments:
  -h, --help             show this help message and exit

Neo4j Host Settings:
  --uri URI              Bolt URI to target Neo4j database (will override TLS setting)
  --user USER
  --password PASSWORD
  --tls TLS              Ues a TLS Bolt connection?
  --db DB

UDP Event Listener:
  --udp-host UDP_HOST    IPv4 host to bind to
  --udp-port UDP_PORT    udp port to listen on
  --buffer-size BUFFER_SIZE
                         event buffer size
  --flush-interval FLUSH_INTERVAL
                         event buffer flush interval
```
