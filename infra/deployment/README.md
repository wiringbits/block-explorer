# Deployment

This folder contains all the required scripts to deploy the projects.

These scripts were tested with ansible 2.6.4, higher versions should work too, it might not work with smaller versions.

## xsnd
Execute the following command to deploy the application to a test server:
- `ansible-playbook -i test-hosts.ini --ask-become-pass --vault-password-file .vault xsnd.yml`
