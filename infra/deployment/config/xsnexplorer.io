server {
  listen 80 default_server;

  root /var/www/html;

  index index.html;

  server_name xsnexplorer.io;

  # the backend api
  location /api/ltc {
    rewrite ^/api/ltc/(.*) /$1 break;
    proxy_pass http://10.136.151.203:9000;
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
