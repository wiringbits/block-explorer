server {
  listen 80 default_server;

  return 301 https://$host$request_uri;
}

server {
  listen 443 ssl;

    ssl_certificate /etc/letsencrypt/live/xsnexplorer.io/fullchain.pem; # managed by Certbot
    ssl_certificate_key /etc/letsencrypt/live/xsnexplorer.io/privkey.pem; # managed by Certbot
    include /etc/letsencrypt/options-ssl-nginx.conf; # managed by Certbot
    ssl_dhparam /etc/letsencrypt/ssl-dhparams.pem; # managed by Certbot

  root /var/www/html;

  index index.html;

  server_name xsnexplorer.io;

  # the backend api
  location /api/ltc {
    rewrite ^/api/ltc/(.*) /$1 break;
    proxy_pass http://10.136.151.203:9000;
  }

  location /api/grs {
    rewrite ^/api/grs/(.*) /$1 break;
    proxy_pass http://10.136.164.36:9000;
  }

  location /api/xsn {
    rewrite ^/api/xsn/(.*) /$1 break;
    proxy_pass http://10.136.160.52:9000;
  }

  location /api {
    rewrite ^/api/(.*) /$1 break;
    proxy_pass http://10.136.160.52:9000;
  }

  # caching static assets
  location ~* \.(jpg|jpeg|png|gif|ico|css|js)$ {
    expires 7d;
  }

  location / {
    try_files $uri $uri/ /index.html;
  }
}
