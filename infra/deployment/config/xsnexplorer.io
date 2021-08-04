
# upstream xsn {
#   server 10.136.160.52:9000;
#   keepalive 120;
# }

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

  client_body_buffer_size 128k;
  proxy_connect_timeout 60;
  proxy_send_timeout 180s;
  proxy_read_timeout 180s;
  proxy_buffer_size 64k;
  proxy_busy_buffers_size 128k;
  proxy_buffers 64 16k;

  # the backend api
  location /api/ltc {
    proxy_cache my_cache;
    add_header X-Cache-Status $upstream_cache_status;

    rewrite ^/api/ltc/(.*) /$1 break;
    proxy_pass http://10.136.151.203:9000;
  }

  location /api/grs {
    proxy_cache my_cache;
    add_header X-Cache-Status $upstream_cache_status;

    rewrite ^/api/grs/(.*) /$1 break;
    proxy_pass http://10.136.164.36:9000;
  }

  location /api/btc {
    proxy_cache my_cache;
    add_header X-Cache-Status $upstream_cache_status;

    rewrite ^/api/btc/(.*) /$1 break;
    proxy_pass http://10.136.96.184:9000;
  }

  location /api/dash {
    proxy_cache my_cache;
    add_header X-Cache-Status $upstream_cache_status;

    rewrite ^/api/dash/(.*) /$1 break;
    proxy_pass http://10.136.168.102:9000;
  }

  location /api/xsn {
    proxy_cache my_cache;
    add_header X-Cache-Status $upstream_cache_status;

    proxy_http_version 1.1;
    proxy_set_header Connection "";
    rewrite ^/api/xsn/(.*) /$1 break;
    proxy_pass http://10.136.160.52:9000;
  }

  location /api/eth/prices {
    proxy_cache my_cache;
    add_header X-Cache-Status $upstream_cache_status;

    rewrite ^/api/eth/prices /prices/eth break;
    proxy_pass http://10.136.12.87:9000;
  }

  location /api/weth/prices {
    proxy_cache my_cache;
    add_header X-Cache-Status $upstream_cache_status;

    rewrite ^/api/weth/prices /prices/weth break;
    proxy_pass http://10.136.12.87:9000;
  }

  location /api/usdt/prices {
    proxy_cache my_cache;
    add_header X-Cache-Status $upstream_cache_status;

    rewrite ^/api/usdt/prices /prices/usdt break;
    proxy_pass http://10.136.12.87:9000;
  }

  location /api/usdc/prices {
    proxy_cache my_cache;
    add_header X-Cache-Status $upstream_cache_status;

    rewrite ^/api/usdc/prices /prices/usdt break;
    proxy_pass http://10.136.12.87:9000;
  }

  # kept temporary for backwards compatibility with clients
  location /api/weth {
    proxy_cache my_cache;
    add_header X-Cache-Status $upstream_cache_status;

    rewrite ^/api/weth/(.*) /$1 break;
    proxy_pass http://10.136.12.87:9000;
  }

  location /api/eth {
    proxy_cache my_cache;
    add_header X-Cache-Status $upstream_cache_status;

    rewrite ^/api/eth/(.*) /$1 break;
    proxy_pass http://10.136.12.87:9000;
  }

  location /api {
    rewrite ^/api/(.*) /$1 break;

    proxy_http_version 1.1;
    proxy_set_header Connection "";
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
