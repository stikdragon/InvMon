# InvMon
A piece of software to monitor (and eventually control) an off-grid inverter system.  It's built specifically for my house which has a 48v lead acid battery and a pair of MPP Solar PIP-MAX 8048 inverters running in phase, however it is build relatively cleanly so should be straightforward to apply to other systems, too.

It's intended to run on a raspberry pi zero that lives in my garage, and talks to the inverter via RS232.  It's modular:

- **Inverter**: Talks to inverter via RS232.  Takes an inverter model class, which could be customised for a different system
- **ConsoleOutput**: Draws realtime info to a local terminal on the Pi
- **HTMLOutput**: Draws charts to a static HTML page which is written to a local directory (then you can point a webserver at it, for instance)
- **HTTPServer**: Runs NanoHTTPD to serve a very basic website that has some lightly interactive charts
- **JSONSend**: Connects to a `JSONRecv` module on another host
- **JSONRecv**: Receives inverter data packets from another host and presents them as if the inverter was connected here instead

It's not perfectly implemented, i've been doing it in a big rush.  

Runs against Java8, which has been a bit of a nuisance at times.  This is required because there doesn't seem to be a 9+ JVM that works on a Pi Zero's ARM chip
