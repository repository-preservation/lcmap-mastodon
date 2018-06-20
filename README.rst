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

.. code-block:: bash

   docker run -p 8080:80 -v /workspace/data:/data -e "SERVER_TYPE=${SERVER_TYPE}" -e "ARD_PATH=${ARD_PATH}" -e "ARD_HOST=${ARD_HOST}"\
   -e "IWDS_HOST=${IWDS_HOST}" -e "PARTITION_LEVEL=${PARTITION_LEVEL}" --ip="192.168.43.4" usgseros/lcmap-mastodon


Configuration
-------------
There are up to 10 environment variables, and two configurations that need to be defined.

You need to mount a volume to your container at `/data`. This should be the base dir
to where the ARD tarballs can be found

.. code-block:: bash

   -v /localardpath/data:/data


The ${ARD_PATH} environment variable is used by a glob function to determine what ARD 
tarballs are available for a given Tile ID.  The value is determined by the directory 
structure where the ARD is kept

Analysis Ready Data (ARD) are expected to be organized by Landsat Mission. From the 
mounted dir, the directory structure should mirror this: 
<mission>/ARD_Tile/<year acquired>/CU/<HHH>/<VVV>/

HHH and VVV constituting the 3 digit tile-id.  The H and V values DO NOT need to be included
in your ${ARD_PATH} definition.

.. code-block:: bash

   export ARD_PATH=/data/\{tm,etm,oli_tirs\}/ARD_Tile/*/CU/


The ${ARD_HOST} environment variable is your hostname or IP address for the deployed lcmap-mastodon
instance

The ${IWDS_HOST} environment variable is the hostname or IP address for the deployed `lcmap-chipmunk <https://github.com/USGS-EROS/lcmap-chipmunk>`_
instance

The ${SERVER_TYPE} environment variable tells the lcmap-mastodon instance what kind of data it is 
working with. Valid values include "ard" and "aux".

${AUX_HOST} needs to be defined if ${SERVER_TYPE} is defined as "aux". It is the hostname or ip
address where auxiliary data is provided.

The ${PARTITION_LEVEL} environment variable determines the level of parallelization applied to
the ingest process 

INVENTORY_TIMEOUT (ms) Defaults to 120000 (2 mins). It sets the HTTP request timeout for requests
against the IWDS_HOST for data ingest status.

INGEST_TIMEOUT (ms) Defaults to 120000 (2 mins). Sets the HTTP request timeout for data ingest requests.

Unless requests to the application are being routed through a DNS server, you'll need to declare what
IP address the container should use with `--ip`. This value should correspond to the ${ARD_HOST} 
environment variable mentioned previously

.. code-block:: bash

   --ip="192.168.43.4"


User Interface
--------------
The Mastodon UI is simple HTML and javascript. If you exposed port 8080 as in the previous example, 
the UI will be available at http://127.0.0.1:8080


CLI Interaction
---------------
You have the option to manage ingest from the command line as well.  Just build an uberjar with
leiningen, and export environment variables for the ${IWDS_HOST}, ${PARTITION_LEVEL}, and ${ARD_HOST}.
The ${ARD_HOST} is an instance of Mastodon running in server mode. It is this Mastodon instance which 
exposes the ARD over HTTP for ingest.

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

