Grille
======

A Java application for generating classical grille ciphers.
[See grille ciphers on Wikipedia][1]

Usage
-----

The application is run from the command line. To run the application
an `order` must be specified, this determines the size of the grille
that will be generated. For example:

    java -jar grille-app.jar --order 4

Will produce a PNG image file in the current directory for an 8x8
grille. The grille will have been produced using a `seed`. This seed
will be included on the generated grille and may be used to generate
the same grille again, for example:

    java -jar grille-app.jar --order 4 --seed 883426228

The image will be written to an automatically generated file name
which includes the order and the seed. To override this and specify
a different filename:

    java -jar grille-app.jar --order 3 --filename new-grille.png

Options to control the appearance and generation of grilles may be
specified in a separate settings file (see below).

Help is also available:

```
java -jar grille-app.jar
    --filename, -f
       Filename of generated image
    --help, -h
       Display this help
       Default: false
  * --order, -o
       Size of grille
    --seed, -s
       Seed for grille generation
        --settings
       Path to settings file
```

Settings
--------

The application allows for some customizations of a grille's
appearance and generation through a [Java properties file][2].

The available properties are listed below with their default values:

```
dpi (300)           - Pixel density of generated image
mmPerSquare (8)     - Size of individual grille squares
outputDir (".")     - Directory to which images should be written
filename ("grille") - Filename assigned to grille image
design ("circle")   - Design used to mark positions (circle or square)
shaded (true)       - Whether grille positions should be shaded
attempts (100000)   - Number of attempts made to find a 'good' grille
```

Building
--------

Grille uses Maven. To build the application jar simply run:

    git clone https://github.com/tomgibara/grille.git
    cd grille
    mvn package

The executable jar will be found at `target/grille-app-[VERSION].jar'

--------

[1]: http://en.wikipedia.org/wiki/Grille_%28cryptography%29
[2]: http://en.wikipedia.org/wiki/.properties
