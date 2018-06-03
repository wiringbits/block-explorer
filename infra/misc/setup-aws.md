# Setup AWS

## set up the locale
- `export LC_ALL="en_US.UTF-8"`
- `export LC_CTYPE="en_US.UTF-8"`
- `sudo dpkg-reconfigure locales`

## install aws cli
- `sudo apt update && sudo apt install -y python-pip python-setuptools`
- `sudo pip install --upgrade pip`
- `sudo pip install awscli`
