# InvMon
A piece of software to monitor (and eventually control) an off-grid inverter system.  It's built mainly for my house which has a couple of 48v lead acid batteries and a pair of MPP Solar PIP-MAX 8048 inverters running in phase, however it is built relatively cleanly so should be straightforward to apply to other systems, too, if you fancy writing interfacing classes for them.

It's modular and intended to run on pretty minimal hardware (it originally ran on a raspberry pi zero in my garage, although it stressed that out a bit too much so now it's on an old i3)

It's not perfectly implemented, i've been doing it in a big rush so there's some questionable design choices, i expect. 

Runs against Java8, which has been a bit of a nuisance at times.  This is required because there doesn't seem to be a 9+ JVM that works on a Pi Zero's ARM chip.  Since i no longer run it on a Pi i might relax this requirement eventually. 

## Stuff to do
- I want to integrate it with a BMS, but first i need to build a BMS.  The data model is already set up to support individual cell voltages, but by default the fields are all turned off. 
- Run as a windows service (in progress)

## Stack
- It's built with Ant, because i like it.  
- It uses the `jSerialComm` library to talk serial, which has native JNI stuff bundled in with the Jar, so it should Just Work.  
- The web front end is written in HTML5 using Java, and cross-compiled into javascript by a project called `TeaVM`.  
- The database is a simplistic custom thing i called `MiniDB`
    - It's write-once, records can't be edited
    - Each record is fixed-width, the layout is configured by a "Model" file which defines the set of fields+types
    - Each record has a timestamp, and some number of fields. 
    - It queries data based on timestamps, so you ask it for "N records between these two timestamps" and it interpolates the data it's got
    - It favours speed over complexity or storage efficiency
    - Should scale up to 2 billion records
    - It supports calculated fields, so eg. if you have a voltage and a current you can also have a power field derived from them

## Installation
1. Make sure Java is installed somewhere.  It's enough to just have it unpacked and the `\bin` folder on your system path
2. Unpack the .zip file somewhere.  
3. Rename (or make copies of) the `example-model.xml` and `example-config.xml` files as `model.xml` and `config.xml`
4. Edit the `config.xml` file:
    1. define the serial port your inverter is on
    2. define the directory you want the database file to go in.  Over time this will get big, it'll generate about 20Mb/day if you leave it with the default 1s sample rate
5. Start the process, either with the `.bat` file or the `.sh` script

## Configuration
The layout of the pages is configured by a fairly verbose XML file, so you can customise graphs, layouts etc.  Best to have a look at the example XML file.  

The data model is defined by `model.xml` and is moderately flexible.  At its core it's a list of `<Field>` objects, but there's some additional complexity with a form of templating you can use to duplicate sets of fields.  For example, in my case, i have two inverters, so a lot of fields have duplicates (eg. charge current per inverter)

## Pictures
![image](https://github.com/stikdragon/InvMon/assets/6278403/67e983e6-ca5e-4e8e-a778-1cf710948aee)
