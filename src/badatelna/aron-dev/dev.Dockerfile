FROM httpd:alpine

# Apache conf
COPY ./httpd.conf /usr/local/apache2/conf/httpd.conf
