# Block-Explorer [![Build Status](https://travis-ci.org/X9Developers/block-explorer.svg?branch=master)](https://travis-ci.org/X9Developers/block-explorer) [![Codacy Badge](https://api.codacy.com/project/badge/Grade/96a6179dc4c645bd96f9473fa069250d)](https://www.codacy.com/app/AlexITC/block-explorer?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=X9Developers/block-explorer&amp;utm_campaign=Badge_Grade)

The XSN Block Explorer (https://xsnexplorer.io/) is composed by several sub projects:
- [server](server)
- [web-ui](web-ui)
- [infra](infra)

## server
The server is a backend service that interacts with the `xsnd` (RPC server) to provide a RESTful API for retrieving information from the blockchain, it also maps the blockchain to a relational database to support several operations that otherwise would kill the application performance.

## web-ui
The web-ui is the frontend application that interacts with the `server` project to display the data in a browser.

## infra
The infra project contains all stuff related to the required infrastructure, like deployment steps and hopefully deployment scripts (TODO).

# Contributing
We are happy to accept contributions, please read the [CONTRIBUTING.md](CONTRIBUTING.md] file to see our rules.
