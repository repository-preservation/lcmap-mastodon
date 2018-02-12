.. image:: https://travis-ci.org/USGS-EROS/lcmap-mastodon.svg?branch=develop
    :target: https://travis-ci.org/USGS-EROS/lcmap-mastodon


Mastodon
========
Tools for facilitating LCMAP data curation.

Features
--------
* Clojurescript for providing a simple UI
* Standard Clojure for CLI interaction and ingest parallelization
* Report on ARD ingest status on a per tile basis
* Ingest available ARD that has not been processed
* Report ARD that has been ingested, and which is now missing 

Running
-------
The Mastodon UI is a simple static HTML file and javascript served
up by NGINX within a Docker container.

To get the latest image:

   docker pull usgseros/lcmap-mastodon

The usgseros/lcmap-mastodon image depends on definition of three 
environment variables to run
.. code-block:: bash
    docker run -e ARD_HOST=$ARD_HOST -e INGEST_HOST=$INGEST_HOST -e IWDS_HOST=$IWDS_HOST -p 8080:80 usgseros/lcmap-mastodon:develop-0.1.13

Where $ARD_HOST is the resource which provides the list of available ARD, $IWDS_HOST is the resource which
receives HTTP POST requests for ARD ingest into the IWDS, and optionally $INGEST_HOST if a different 
service is being used to provide HTTP access to the ARD.

The ARD_HOST  should model the behavior of the https://github.com/USGS-EROS/lcmap-anteater project.
The IWDS_HOST should model the behavior of the https://github.com/USGS-EROS/lcmap-chipmunk project.


Alternatively, should you prefer to ingest large amounts of ARD
from the command line, you have that option as well.

.. code-block:: bash
    lein uberjar
    ...
    $ java -jar target/lcmap-mastodon-0.1.13-standalone.jar 005015
      Tile Status Report for:  005015
      To be ingested:  28
      Already ingested:  3

      Ingest? (y/n) 
      y
      layer: LT05_CU_005015_19840508_20170912_C01_V01_SRB1.tif 200
      layer: LE07_CU_005015_20021221_20170919_C01_V01_SRB4.tif 200
      layer: LT05_CU_005015_19840508_20170912_C01_V01_SRB5.tif 200


You'll need to export the ARD_HOST, IWDS_HOST and optionally INGEST_HOST variables
before running the jar.

Optionally you can add the `-y` flag to the end of the java command, and any not yet ingested
ARD will begin ingesting without user input.


## Development Clojurescript

To get an interactive development environment run:

    lein figwheel

and open your browser at [localhost:3449](http://localhost:3449/).
This will auto compile and send all changes to the browser without the
need to reload. After the compilation process is complete, you will
get a Browser Connected REPL. An easy way to try it is:

    (js/alert "Am I connected?")

and you should see an alert in the browser window.

To clean all compiled files:

    lein clean

To create a production build run:

    lein do clean, cljsbuild once min

And open your browser in `resources/public/index.html`. You will not
get live reloading, nor a REPL. 

## Development Clojure

To get a repl run:
  
  lein repl

## Testing

  make runtests

## License
Unlicense

Mastodon, it'll probably be extinct soon

