ARTS Web server overview
=========================

This is a sub repository for the Antibiotic Resistant Target Seeker.
This can be used to view results generated from the public server at https://arts.ziemertlab.com
or using output from the main analysis pipeline at https://bitbucket.org/ziemertlab/arts
For usage of the web server see https://arts.ziemertlab.com/help

Quickstart with Docker local installation
------------------------------------------
For details on setting up docker and docker-compose: https://docs.docker.com/compose/install/

1) Make an isolated directory and download the docker-compose file to install the pre-built ARTS containers::

    mkdir ARTSwebapp_docker && cd ARTSwebapp_docker
    wget -O docker-compose.yml https://bitbucket.org/ziemertlab/artswebapp/raw/HEAD/docker-compose-artswebapp.yml

2) Set environment variables for port number to run webserver and shared folders of host system (replace /tmp with desired path)::

    echo "ARTS_RESULTS=/tmp" > .env
    echo "ARTS_UPLOAD=/tmp" >> .env
    echo "ARTS_RUN=/tmp" >> .env
    echo "ARTS_WEBPORT=80" >> .env
    
    To create a ".env" file on windows use a text editor such as Notepad++ and save as ".env":
	
	ARTS_RESULTS=/folder/with/results/
	...

3) Build and start the services (from the ARTSdocker directory)::

    docker-compose up

4) Shutting down services and clear containers from disk storage::

    docker-compose down

Extra) Start services in the background, check for running services,
and shutdown without removing containers from disk::

    docker-compose up -d
    docker ps -a
    docker-compose stop

The full ARTS pipeline with analysis engine can be built using similar docker files at https://bitbucket.org/ziemertlab/arts

Installation on linux (Debian 8 tested):
-----------------------------------------

1) Downlaod and install requirements::

    apt-get update
    apt-get install -y python-pip python-dev libjpeg-dev libfreetype6-dev zlib1g-dev
    git clone https://bitbucket.org/ziemertlab/artswebapp
    cd artswebapp
    pip install -r requirements.txt

2) Edit desired folders in configs (config/artsapp_default.conf and config/uwsgi.conf)
3) Run server (from arswebapp folder)::

        uwsgi --ini config/uwsgi.conf

Source install on RHEL6:
------------------------
This guide will compile python2.7 without disturbing the system's python2.6. Skip to step 4 if
python2.7 and pip are pre-installed.

1) Install prerequisites for building python2.7. If no package manager is present, ensure the host has these requirements (root / sudo required)::

    yum groupinstall development
    yum install openssl-devel sqlite-devel zlib-devel git
    export PATH="/usr/bin/local:$PATH"

2) Download sources for python, setuptools, artswebapp, pip install script::

    curl -O https://www.python.org/ftp/python/2.7.6/Python-2.7.6.tgz
    curl -O https://pypi.python.org/packages/61/3c/8d680267eda244ad6391fb8b211bd39d8b527f3b66207976ef9f2f106230/setuptools-1.4.2.tar.gz
    curl -O https://bootstrap.pypa.io/get-pip.py
    git clone https://bitbucket.org/ziemertlab/artswebapp

3) Setup new python2.7 alongside python2.6. Install setuptools and pip (root / sudo required)::

    tar -xzf Python-2.7.6.tgz
    tar -xzf setuptools-1.4.2.tar.gz
    cd Python-2.7.6
    ./configure --prefix=/usr/local
    make && make altinstall
    cd ../setuptools-1.4.2
    python2.7 setup.py install
    python2.7 ../get-pip.py
    cd ../artswebapp

4) Install arts requirements using pip::

    pip2.7 install -r requirements.txt

5) Edit config/artsapp_default.conf to direct to desired results and upload folders
Default uwsgi configs can now be used to run the webserver at port :5000 using uwsgi from the artswebapp directory::

    nano config/uwsgi.config

6) Run artswebapp As a non-root user::

    uwsgi --ini config/uwsgi.config

The following may need to be set before users can connect::

    setsebool -P httpd_can_network_connect 1

Extra) Use nginx to proxy port 80 to 5000. As root::

    yum install nginx
    mv /etc/conf.d/default.conf /etc/conf.d/default.bkup
    cp config/nginx_siteavailible.conf /etc/conf.d/default.conf
    sudo service nginx restart

Support
--------

If you have any issues please feel free to contact us at arts-support@ziemertlab.com

Licence
--------
This software is licenced under the GPLv3. See LICENCE.txt for details.
