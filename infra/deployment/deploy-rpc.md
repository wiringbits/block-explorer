# Deploy the rpc server (xsnd)
The following steps are for setting up the rpc server that runs on system startup.

## Requisites
- set up aws, see [aws-setup.md](/infra/misc/setup-aws.md)

## Set up a new user
- create the user: `sudo adduser --system rpc`
- switch to the new user: `sudo su -s /bin/bash - rpc`
- set aws credentials: `aws configure`
- create the folder for xsn data: `mkdir /home/rpc/.xsncore`
- set the xsn config (see [xsn.conf](/infra/misc/xsn.conf)): `vim /home/rpc/.xsncore/xsn.conf`

## Install the rpc server (use the new user)
- switch to the new user: `sudo su -s /bin/bash - rpc`
- download the client (ensure it is the latest version): `wget https://github.com/X9Developers/XSN/releases/download/v1.0.9/xsncore-1.0.9-linux64.tar.gz -O xsn.tar.gz`
- unpack the file: `tar -zxvf xsn.tar.gz`
- create a folder for the executables: `mkdir /home/rpc/xsn`
- move the executables: `mv xsncore-1.0.9/bin/xsn* xsn/`
- add the script for new blocks: `vim xsn/script.sh`
- set the script as executable (see [script.sh](/infra/misc/script.sh)): `chmod +x xsn/script.sh`
- test the script: `./xsn/script.sh working`

## Add the systemd service (as super user)
- create the service file (see [xsn-rpc.service](/infra/systemd-services/xsn-rpc.service)): `sudo cp xsn-rpc.service /etc/systemd/system/xsn-rpc.service`
- reload services: `sudo systemctl daemon-reload`
- check that the service is recognized: `sudo service xsn-rpc status`
- start the service: `sudo service xsn-rpc start`
- verify it is working: `sudo service xsn-rpc status`
- run the service on system startup: `sudo systemctl enable xsn-rpc`

## Test the rpc service
- switch to the new user: `sudo su -s /bin/bash - rpc`
- test the service: `./xsn/xsn-cli getinfo`
