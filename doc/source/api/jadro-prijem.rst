.. _api_jadro_prijem:

=====================
Jádro - příjem dat
=====================

Jádro přijímá data pomocí služby FileTransfer. Pomocí služby jsou přenášeny 
dva druhy dat:

 - :ref:`přenos jednotek publikace / APU <api_jadro_prijem_apu>`
 - :ref:`přenos digitalizátů <api_jadro_prijem_dao>`

.. _api_jadro_prijem_apu:

-----------------------------------
Přenos jednotek publikace / APU
-----------------------------------

Jednotky publikace / APU se přenášejí ve formátu dle schématu :ref:`APUX <api_apux>`.

Nastavení přenosu: ``type="APUSRC"``

Zasílá se soubor pojmenovaný :file:`apusrc-<UUID>.xml` s kořenovým elementem :token:`apusrc`. 
Uvedné ``UUID`` se musí shodovat s identifikátorem uvedeným v XML.

Příklad pojmenování přenášeného souboru: ``apusrc-939cc6c9-69ce-46be-a76b-a0ebdac4b325.xml``.

Příklad obsahu souboru:

.. code-block:: xml

   <?xml version="1.0" encoding="utf-8"?>
   <ax:apusrc uuid="939cc6c9-69ce-46be-a76b-a0ebdac4b325" 
              xmlns:ax="http://www.aron.cz/apux/2020">
     <ax:apus>
       ...
     </ax:apus>
   </ax:apusrc>


.. _api_jadro_prijem_dao:

--------------------
Přenos digitalizátů
--------------------

Digitalizáty se přenášejí v adresářové struktuře. 
V rámci jednoho přenosu se přenáší jeden digitalizát.
Digitalizát je tvořen definičními metadaty a datovými
soubory.

Nastavení přenosu: 

  - ``type="DAO"``
  - ``id="DAO_ID"``

Způsob uložení digitalizátu:

 - :file:`dao-<UUID>.xml` - metadata digitalizátu
 - :file:`files` - adresář obsahující soubory
 - :file:`files/file-<UUID>` - jednotlivý soubor


Metadata digitalizátu
------------------------

Metadata digitalizátu jsou uložena v XML souboru dle schématu 
:ref:`APUX <api_apux>`. Kořenem je element :token:`dao`.

Příklad pojmenování souboru s metadaty: ``dao-77a5f885-3cd5-4690-9828-9585c0c3744b.xml``.
