.. _ar_install:

==========================
Instalace Aron
==========================

Požadavky
=============

 * Operační systéme: Linux nebo Windows
 * Java 11 a vyšší
 * Databáze PostgreSQL
 * ElasticSearch
 * Apache HTTPD

Pro jádro publikace je vhodné vytvořit
samostatný databázový účet a samostatný systémový 
účet pro jeho spuštění. V dokumentaci se dále 
předpokládá použití databázového účtu **aron**
a systémového účtu **aronapp**.

Podporované možnosti instalace jsou:
 * Linux/Unix - init.d služba (System V)
 * Linux/Unix - systemd služba
 * služba systému Windows

Podrobný seznam možností konfigurace zde: 
`Dokumentace Spring Boot <https://docs.spring.io/spring-boot/docs/current/reference/html/deployment.html#deployment-install>`_.


Příklady skriptů jsou pro distribuci Debian.

Databáze
============

::

    apt install postgresql-11
    su postgres
    createuser aron --interactive
    createdb -E UTF-8  --locale=cs_CZ.UTF-8 -O aron aron


ElasticSearch
====================

::

    apt install gnupg
    wget -qO - https://artifacts.elastic.co/GPG-KEY-elasticsearch | apt-key add -
    echo "deb https://artifacts.elastic.co/packages/7.x/apt stable main" | tee /etc/apt/sources.list.d/elastic-7.x.list
    apt update
    apt install elasticsearch
    systemctl enable elasticsearch
    /usr/share/elasticsearch/bin/elasticsearch-plugin install analysis-icu
    systemctl start elasticsearch

Apache HTTP
=================

::

    apt install apache2
    a2enmod proxy
    a2enmod proxy_http
    a2enmod rewrite


Následně vytvořit konfiguraci Aron v HTTP serveru.

Příklad konfigurace v souboru :file:`/etc/apache2/sites-available/aron.conf`:
    
::

      <VirtualHost *:80>
        DocumentRoot /opt/aron/web
		
        ProxyPass /api http://localhost:8080/api retry=0
        ProxyPassReverse /api http://localhost:8080/api
        ProxyTimeout 300
		
        DocumentRoot /opt/aron/web
        <Directory "/opt/aron/web">
          AllowOverride All
          Require all granted
        </Directory>
      </VirtualHost>


Povolení konfigurace a rekonfigurace Apache HTTPD:

::

    a2dissite 000-default
    a2ensite aron
    systemctl reload apache2


Instalace Java
========================

:: 

    apt install openjdk-11-jdk


Instalace služby Aron
===========================

- nakopírovat api data do :file:`/opt/aron/api`
- přidat execute práva na jar

Vytvoření služby (systemd)
-----------------------------

Definice služby v souboru :file:`/etc/systemd/system/aron.service`:

:: 

		[Unit]
		Description=ARON
		After=syslog.target
	
		[Service]
		User=aronapp
		WorkingDirectory=/opt/aron/api
		Environment=PATH=/usr/local/bin:/usr/bin:/usr/local/sbin:/usr/sbin:/opt/aron/api
		ExecStart=/opt/aron/api/api-0.0.1.jar
		SuccessExitStatus=143
	
		[Install]
		WantedBy=multi-user.target


Instalace webového rozhraní (Frontend)
===========================================

Nakopírovat web data do :file:`/opt/aron/web`
