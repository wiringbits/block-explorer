# Deploy the server project
The following instructions are for setting up the server project (backend).

## Build the project

These steps should be run in a place where you have cloned the repository, like your local machine:
- Move to the [server](/server) folder: `cd server`
- Build the project: `sbt dist`
- Copy the built project to the server: `scp target/universal/xsn-block-explorer-0.1.0-SNAPSHOT.zip xsnexplorer.io:~/`


## Setup the server (first steps)

### Install java 8

- `sudo add-apt-repository ppa:webupd8team/java`
- `sudo apt-get update && sudo apt-get install oracle-java8-installer`
- verify the version: `java -version`

[source](https://www.digitalocean.com/community/tutorials/how-to-install-java-with-apt-get-on-ubuntu-16-04)

### Setup a new user
- add the new user: `sudo adduser --system play`
- switch to the new user: `sudo su -s /bin/bash - play`
- create the project folder: `mkdir /home/play/server`

### Unpack the project
- Unzip the project: `sudo unzip ~/xsn-block-explorer-0.1.0-SNAPSHOT.zip -d /home/play/server`
- Set the config: `sudo vim /home/play/server/xsn-block-explorer-0.1.0-SNAPSHOT/conf/application.conf`
- Restore permissions: `sudo chown -R play:nogroup /home/play/server`
- Restart the service: `sudo service xsn-backend restart`

### Run the service on system startup
- add the systemd service (see [xsn-backend.service](/infra/systemd-services/xsn-backend.service)): `cp xsn-backend.service /etc/systemd/system/xsn-backend.service`
- reload the services: `sudo systemctl daemon-reload`
- verify the service registration: `sudo service xsn-backend status`
- start the service: `sudo service xsn-backend start`
- verify the status: `sudo service xsn-backend status`
- verify the service: `curl localhost:9000/health`
- enable the service to run on system startup: `sudo systemctl enable xsn-backend`


## Troubleshooting
- systemd logs: `sudo journalctl -u xsn-backend`
- syslog: `sudo tail -f /var/log/syslog`
- check `.env` file permissions
- ensure the `PLAY_APPLICATION_SECRET` is set
