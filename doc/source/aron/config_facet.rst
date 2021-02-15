.. _ar_config_facet:

====================================
Konfigurace fazet
====================================

Konfigurace fazet a dialogu pro jejich zobrazení
je uložena v souboru :file:`searchConfig.yml`.
Nastavené fazety umožňují omezit výsledky vyhledávání.

Každá fazeta má definován typ. Možné druhy fazet jsou:
 - :token:`FULLTEXT` - jednoduchý filter na textovou hodnotu
 - :token:`ENUM` - výběr z 0..n výčtových hodnot, funguje jako OR
 - :token:`MULTI_REF` - odkaz na více archivních entit, výběr pomocí našeptávače, funguje jako OR
 - :token:`UNITDATE` - datace, výběr z připravených intervalů nebo ruční zadání
 - :token:`MULTI_REF_EXT` - odkaz na více archivních entit, výběr pomocí našeptávače, funguje jako OR, 
   kombinace s podřízenými podmínkami, musí platit ve stejném partu

Příklad konfigurace fazet
=============================

.. include:: ../../../priklady/searchConfig.yaml
   :code: YAML
