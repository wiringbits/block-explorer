# Deployment

This folder contains all the required scripts to deploy the projects.

These scripts were tested with ansible 2.6.4, higher versions should work too, it might not work with smaller versions.

## Run
Execute the following commands to deploy the applications to a test server:
- xsnd: `ansible-playbook -i test-hosts.ini --ask-become-pass --vault-password-file .vault xsnd.yml`
- postgres: `ansible-playbook -i test-hosts.ini --ask-become-pass --vault-password-file .vault postgres.yml`
- xsn-backend: `ansible-playbook -i test-hosts.ini --ask-become-pass --vault-password-file .vault xsn-backend.yml`
- all `ansible-playbook -i test-hosts.ini --ask-become-pass --vault-password-file .vault xsnd.yml postgres.yml xsn-backend.yml`
