FROM httpd:alpine

# Apache conf
COPY ./aron-web/docker/httpd.conf /usr/local/apache2/conf/httpd.conf
COPY ./aron-web/docker/.htaccess /usr/local/apache2/htdocs/
