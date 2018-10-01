# Deploy the web-ui project

## Requisites
- nginx working (see [setup-nginx.md](/infra/misc/setup-nginx.md))


## Build the project

These steps should be run in a place where you have cloned the repository, like your local machine:
- move to the [web-ui](/web-ui) folder: `cd web-ui`
- build the project: `ng build --prod`
- zip the result: `zip -r web-ui.zip dist/*`
- copy to the server: `scp web-ui.zip xsnexplorer.io:~/`


## Server
- login. `ssh xsnexplorer.io`
- unzip the project: `unzip ~/web-ui.zip -d ~/`
- move the files: `sudo rsync -a ~/dist/ /var/www/html/ --remove-source-files`
