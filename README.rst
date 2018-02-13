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
The Mastodon UI is simple HTML and javascript served up by NGINX within a Docker container.

To get the latest image:

.. code-block:: bash

   docker pull usgseros/lcmap-mastodon


The usgseros/lcmap-mastodon image depends on definition of three 
environment variables to run

.. code-block:: bash

    docker run -e ARD_HOST=$ARD_HOST -e INGEST_HOST=$INGEST_HOST -e IWDS_HOST=$IWDS_HOST -p 8080:80 usgseros/lcmap-mastodon:develop-0.1.13


- $ARD_HOST is the resource which provides the list of available ARD.
- $IWDS_HOST is the resource which receives HTTP POST requests for ARD ingest into the IWDS. 
- $INGEST_HOST, optional, if a different service is being used to provide HTTP access to the ARD.


The ARD_HOST instance should model the behavior of the https://github.com/USGS-EROS/lcmap-anteater project.

The IWDS_HOST instance should model the behavior of the https://github.com/USGS-EROS/lcmap-chipmunk project.


Alternatively, should you prefer to ingest large amounts of ARD
from the command line, you have that option as well.  Just build an uberjar
first.

.. code-block:: bash

    $ lein uberjar

    $ java -jar target/lcmap-mastodon-0.1.13-standalone.jar 005015
    Tile Status Report for:  005015
    To be ingested:  28
    Already ingested:  3
    Ingest? (y/n) 
    y
    layer: LT05_CU_005015_19840508_20170912_C01_V01_SRB1.tif 200
    layer: LE07_CU_005015_20021221_20170919_C01_V01_SRB4.tif 200
    layer: LT05_CU_005015_19840508_20170912_C01_V01_SRB5.tif 200


You'll need to export the ARD_HOST, IWDS_HOST, PARTITION_LEVEL, and optionally INGEST_HOST variables
before running.

PARTITION_LEVEL determines how many ingest requests will be handled at a time.  If you define
``export PARTITION_LEVEL=10`` , the list of ARD to be ingested will be divided into 10 groups
which will be passed to Clojure's `pmap <https://clojuredocs.org/clojure.core/pmap>`_ function for parallelization.

If you add the `-y` flag to the end of the java command, any not yet ingested ARD will begin 
ingesting automatically.


Development Clojurescript
-------------------------

To get an interactive development environment run:

.. code-block:: bash

    lein figwheel

and open your browser at [localhost:3449](http://localhost:3449/).
This will auto compile and send all changes to the browser without the
need to reload. After the compilation process is complete, you will
get a Browser Connected REPL. An easy way to try it is:

.. code-block:: javascript

    (js/alert "Am I connected?")

and you should see an alert in the browser window.

To clean all compiled files:

.. code-block:: bash

    lein clean

To create a production build run:

.. code-block:: bash

    lein do clean, cljsbuild once min


Development Clojure
-------------------

To get a repl:

.. code-block:: bash  

    lein repl


To run the main function:

.. code-block:: bash

    lein run


Testing
-------

.. code-block:: bash

  make runtests

License
-------
Unlicense

Mastodon, it'll probably be extinct soon

