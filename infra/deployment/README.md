# Deployment

This folder contains all the required scripts to deploy the projects.

These scripts were tested with ansible 2.6.4, higher versions should work too, it might not work with smaller versions.

## Run
Execute the following commands to deploy the applications to a test server:
- xsnd: `ansible-playbook -i hosts/production-xsn.ini --ask-become-pass --vault-password-file .vault playbooks/nodes/xsnd.yml`
- postgres: `ansible-playbook -i hosts/production-xsn.ini --ask-become-pass --vault-password-file .vault playbooks/postgres.yml`
- xsn-backend: `ansible-playbook -i hosts/production-xsn.ini --ask-become-pass --vault-password-file .vault playbooks/xsn-backend.yml`
- all `ansible-playbook -i hosts/production-xsn.ini --ask-become-pass --vault-password-file .vault playbooks/xsnd.yml playbooks/postgres.yml playbooks/xsn-backend.yml`
