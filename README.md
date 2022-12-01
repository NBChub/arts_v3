Antibiotic Resistant Target Seeker (ARTS) Overview
====================================================
ARTS is a webserver and analysis pipeline for screening for known and
putative antibiotic resistance markers in order to identify and prioritize
their corresponding biosynthetic gene clusters. ARTS can be installed locally
or you can use the free public webserver located at https://arts.ziemertlab.com

See https://bitbucket.org/ziemertlab/artswebapp for a guide on installing the webserver independantly.

Running ARTS
-------------
ARTS uses a webserver to queue jobs to the analysis pipeline. Details on webserver usage can be
found at: https://arts.ziemertlab.com/help

Alternatively jobs can be run directly using the artspipeline1.py script (see -h for options). Example::

    artspipeline1.py antismashOutput.final.gbk reference/actinobacteria/ -rd myresults/ -cpu 8 -opt kres,phyl


ARTS Analysis server local install
===================================

The analysis server uses a daemon, runjobs.py, which consumes jobs submitted though the web interface.

Quick start with docker and docker compose:
-------------------------------------------
For details on setting up docker and docker-compose see https://docs.docker.com/compose/install/
This will install local instances of the analysis and web server on a unix/linux system.
For a windows installation see https://docs.docker.com/docker-for-windows/
The end result should be an directory with the docker-compose and ".env" file
(On windows renaming a file ".file." will produce ".file", or choose save as "*.*" in notepad)

1) Make an isolated directory and download the docker-compose file to install the pre-built ARTS containers

```bash
    mkdir ARTSdocker && cd ARTSdocker
    wget -O docker-compose.yml https://bitbucket.org/ziemertlab/arts/raw/HEAD/docker-compose-arts.yml
```

2) Set environment variables for multiprocessing, port number to run webserver, and shared folders of host system (replace /tmp with desired path or use these as the default)

```bash
    echo "ARTS_RESULTS=/tmp" > .env
    echo "ARTS_UPLOAD=/tmp" >> .env
    echo "ARTS_RUN=/tmp" >> .env
    echo "ARTS_CPU=1" >> .env
    echo "ARTS_WEBPORT=80" >> .env
```

3) Build and start the services (from the ARTSdocker directory)
```bash
    docker-compose up
```

4) Shutting down services and clear containers from disk storage
```bash
    docker-compose down
```

Extra) Start services in the background, check for running services,
and shutdown without removing containers from disk::
```bash
    docker-compose up -d
    docker ps -a
    docker-compose stop
```

Source install on Debian 8
---------------------------
ARTS funcitons work under Anaconda3 (with python 3.8). So, first of all Anaconda3 should be installed.

1) Clone/Download the repository (root / sudo required)
```bash
    git clone https://bitbucket.org/ziemertlab/arts
```
or
```bash
    wget https://bitbucket.org/ziemertlab/arts/get/b4789c6b3a88.zip
```
2) Install required libraries and applications (root / sudo required)
```bash
    cd arts
    conda env create -f environment.yml
    conda activate arts
```

3) Install required binaries from dependencies.txt file. Or use pre-compiled linux64bit bins (root / sudo required)
```bash
    tar -xzf linux64_bins.tar.gz -C /usr/local/bin/ && hash -r
```

4) Edit configuration file to define server to listen for job submissions, antismash location, and custom folder paths

5) Start the analysis daemon (see -h for options)::
```bash
    python runjobs.py -pid /tmp/runjobs.pid
```
Support
--------

If you have any issues please feel free to contact us at arts-support@ziemertlab.com

Licence
--------
This software is licenced under the GPLv3. See LICENCE.txt for details.
