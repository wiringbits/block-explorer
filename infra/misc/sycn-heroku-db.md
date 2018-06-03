# Sync Heroku psql
At the moment, we are using heroku postgres, if we let the server sync the whole chain, it will take more than a day to complete the sync which is unreasonable.

The whole process takes a couple of hours if we use postgres locally, these steps allow us to avoid the bottleneck caused by heroku, we'll seed a local database and then import that into heroku.

Heroku is a bottleneck on the initial seeding, the idea is to seed the database locally and export it to heroku.

## Commands
- export from local: `pg_dump -Fc --no-acl --no-owner -U postgres xsn_blockchain > backup.dump`
- upload the file to a public location: `scp backup.dump xsnexplorer.io:~/`, in the server `mv backup.dump /var/www/html/
`
- restore dump: `heroku pg:backups:restore 'http://xsnexplorer.io/backup.dump' postgresql-graceful-31330 -a xsnexplorer`
- delete the file from the public location: `rm /var/www/html/backup.dump`

### Source
- https://devcenter.heroku.com/articles/heroku-postgres-import-export
