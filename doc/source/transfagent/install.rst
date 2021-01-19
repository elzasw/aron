.. _ta_install:

=======================
Instalace
=======================

Požadavky
=============

 * Operační systéme: Linux nebo Windows
 * Java 11 a vyšší
 * Databáze PostgreSQL

Pro transformačního agenta je vhodné vytvořit
samostatný databázový účet a samostatný systémový 
účet pro jeho spuštění. V dokumentaci se dále 
předpokládá použití databázového účtu **transfagent**
a systémového účtu **transfagentapp**.


Umístění
================

Komponenta se obvykle instaluje do adresáře :file:`/opt/transfagent`.
Instalace se provádí z distribuce z podobě ZIP souboru. Tento soubor
obsahuje spustitelný JAR soubor a vzorovou konfiguraci.

Do složky :file:`/opt/transfagent` nahrajte soubory:
 - :file:`transfagent.yml.template` - výchozí konfigurace
 - :file:`transfagent-<VERSION>.jar` - vlastní Komponenta

Následně je vhodné vytvořit symbolický odkaz na .jar umožňující 
nastavení služby nezávisle na aktuální verzi aplikace.

Příklad:

::

  chmod 'u+x,g+x,o+x' transfagent-<VERSION>.jar
  ln -s transfagent-<VERSION>.jar transfagent.jar


.. _ta_install_db:

Vytvoření databáze
=====================

Databáze se vytváří pro data v kódování utf-8 
s českým řazením.

Příklad (vlastníkem bude uživatel **transfagent**):

::
 
  createdb  -E UTF-8  --locale=cs_CZ.UTF-8 -O transfagent transfagent

.. _ta_install_config:

Vytvoření a úprava konfigurace
=================================

::

  cd /opt/transfagent
  cp transfagent.yml.template transfagent.yml


Následně provedena její úprava.

.. _ta_install_svc:

Příprava služby
---------------

Přidání souboru služby :file:`/etc/systemd/system/transfagent.service`.

Definice služby:

::

  [Unit]
  Description=ARON Transformační agent
  After=syslog.target
  
  [Service]
  User=transfagentapp
  ExecStart=/opt/transfagent/transfagent.jar
  WorkingDirectory=/opt/transfagent
  SuccessExitStatus=143
  
  [Install]
  WantedBy=multi-user.target


Povolení služby:

::

  systemctl enable transfagent.service
