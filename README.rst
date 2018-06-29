.. image:: https://travis-ci.org/USGS-EROS/lcmap-mastodon.svg?branch=develop
    :target: https://travis-ci.org/USGS-EROS/lcmap-mastodon


Mastodon
========
Tools for facilitating LCMAP data curation.

Features
--------
* Report on IWDS inventory status for Analysis Ready and Auxiliary data on a per tile basis
* Handle parallelization of data ingest requests
* Identify data that has been ingested, which is now missing 

Running
-------
The LCMAP Mastodon application is deployed as a Docker container.  All interactions
are handled over HTTP, via NGINX.

At a minimum, you'll need to set the following variables
.. code-block:: bash

   docker run \
   -v /workspace/data:/data \
   -e "ARD_PATH=${ARD_PATH}" \
   -e "ARD_HOST=${ARD_HOST}"\
   -e "NEMO_HOST=${NEMO_HOST}" \
   -e "CHIPMUNK_HOST=${CHIPMUNK_HOST}" \
   -e "DATA_TYPE=${DATA_TYPE}" \ 
   -e "PARTITION_LEVEL=${PARTITION_LEVEL}" \
   usgseros/lcmap-mastodon


Configuration
-------------
You need to mount a volume to your container at `/data`. This should be the base dir
to where the ARD tarballs can be found

.. code-block:: bash

   -v /localardpath/data:/data

And the following environment variables:

${ARD_PATH} is used by a glob function to determine what ARD tarballs are available for a 
given Tile ID.  The value is determined by the directory structure where the ARD is kept

Analysis Ready Data (ARD) are expected to be organized by Landsat Mission. From the 
mounted dir, the directory structure should mirror this: 
<mission>/ARD_Tile/<year acquired>/CU/<HHH>/<VVV>/

HHH and VVV constituting the 3 digit tile-id.  The H and V values DO NOT need to be included
in your ${ARD_PATH} definition.

.. code-block:: bash

   export ARD_PATH=/data/\{tm,etm,oli_tirs\}/ARD_Tile/*/CU/


${ARD_HOST} is your hostname or IP address for the deployed lcmap-mastodon instance

${NEMO_HOST} is the url to the deployed `lcmap-nemo <https://github.com/USGS-EROS/lcmap-nemo>`_ instance 

${CHIPMUNK_HOST} is the url to the deployed `lcmap-chipmunk <https://github.com/USGS-EROS/lcmap-chipmunk>`_ instance

${DATA_TYPE} tells the lcmap-mastodon instance what kind of data it is working with. 
Valid values include "ard" and "aux".

${PARTITION_LEVEL} determines the level of parallelization applied to the ingest process

${AUX_HOST} needs to be defined if ${DATA_TYPE} is defined as "aux". It is the hostname or ip
address where auxiliary data is provided.

Optionally, you can define the following:

${INVENTORY_TIMEOUT} defines, in milliseconds, the HTTP request timeout for inventory queries against lcmap-nemo 
Defaults to 120000 (2 minutes).

${INGEST_TIMEOUT} defines, in milliseconds, the HTTP request timeout for ingest requests against lcmap-chipmunk.
Defaults to 120000 (2 minutes).

User Interface
--------------
The Mastodon UI is simple HTML and javascript. If you exposed port 8080 as in the previous example, 
the UI will be available at http://127.0.0.1:8080


CLI Interaction
---------------
You have the option to manage ingest from the command line as well.  Just build an uberjar with
leiningen, and export environment variables for the ${CHIPMUNK_HOST}, ${PARTITION_LEVEL}, ${DATA_TYPE}
and ${ARD_HOST}.  The ${ARD_HOST} is an instance of Mastodon running in server mode. It is this Mastodon instance
which exposes the ARD over HTTP for ingest.

If you want to ingest Auxiliary data instead of ARD, you'll need to set the DATA_TYPE accordingly, and also
define ${AUX_HOST}.

Optionally, you can export FROM_DATE and TO_DATE environment variables, to filter by year the ARD you're 
concerned with.  Format is: YYYY.

To build the standalone jar file:

.. code-block:: bash
  
    lein uberjar

With your jar built, and your environment setup

.. code-block:: bash
  
    java -jar target/lcmap-mastodon-0.1.13-standalone.jar 005015

And follow the prompts. If you want to automatically ingest any non-ingested data, 
add `-y` after the tile id.


Development Clojurescript
-------------------------

To get an interactive development environment run:

.. code-block:: bash

    lein figwheel

and open your browser at http://localhost:3449/.
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


Docker
------
Before building a new docker image, you'll need to create a new uberjar and transpile the 
clojurescript

.. code-block:: bash

   lein uberjar

.. code-block:: bash

   docker build -t < your tag here > .



License
-------
Unlicense

Mastodon, it'll probably be extinct soon

