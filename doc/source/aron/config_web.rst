.. _ar_config_web:

==================================
Webové rozhraní
==================================

Konfigurace pro webové rozhraní je uložena v hlavním 
:ref:`konfiguračním souboru <ar_config_app>` v části
:token:`webResources`. Konfigurace umožňuje definovat:

 - :ref:`základní rozložení stránky (záhlaví a zápatí) <ar_config_web_template>`
 - :ref:`konfigurace novinek <ar_config_web_news>`
 - :ref:`oblíbené dotazy <ar_config_web_fq>`
 - :ref:`fazety pro vyhledávání <ar_config_facet>`
 - logo systému a úvodní obrázek

.. _ar_config_web_template:

Základní rozložení stránky
===========================

Základní rozložení stránky je definováno v souboru :file:`pageTemplate.yaml`. Soubor
umožňuje určit, které jazyky jsou dostupné, název systému, 
texty uváděné na domovské stránce a v zápatí.

.. include:: ../../../priklady/aron/config/pageTemplate.yaml
   :code: YAML

.. _ar_config_web_news:

Konfigurace novinek
===========================

Zobrazované novinky jsou definovány v souboru :file:`news.yaml`.
Každá novinka má svůj samostatný záznam. Soubor se načítá
při každém přístupu na stránku s novinkami.

.. include:: ../../../priklady/aron/config/news.yaml
   :code: YAML


.. _ar_config_web_fq:

Oblíbené dotazy
===================

Oblíbené dotazy jsou definovány v souboru :file:`favoriteQueries.yaml`.
Soubor obsahuje definici oblíbených dotazů. Každý dotaz je pojmenován,
má určen typ a sadu fazet, které ho definují.

.. include:: ../../../priklady/favoriteQueries.yaml
   :code: YAML
