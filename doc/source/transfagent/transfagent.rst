.. _ta_ta:

=======================
Transformační agent
=======================

Komponenta zajišťující transformaci vstupních formátů 
do vnitřní reprezentace publikace. Komponenta bezpečným
způsobem odděluje publikaci a vnitřní informační systémy.
Komponenta má možnost napojení na systémy DSpace a Elza 
odkud může čerpat data pro publikaci. V průběhu času je 
možné přidat konektory na další systémy včetně PEvA II.

Komponenta poskytuje WSDL rozhraní pro příjem požadavků
od jádra na zaslání daných APU.

.. toctree::
   
   install.rst
   config.rst
   input.rst