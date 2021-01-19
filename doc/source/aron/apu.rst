.. _ar_apu:

=================================
APU
=================================

APU je zkratka z **Archival Publication Unit** a představuje
základní jednotku publikace. Každá jednotka publikace má pevně 
určen svůj typ, své ID a permaLink pro odkazování. Příznak 
**published** určuje stav publikování jednotky popisu. 
Správce má možnost publikování zablokovat přepnutím tohoto příznaku. 
Apu nemusí být definována, tj. jedná se o prázdnou entitu sloužící 
pro odkazování z jiných entit. Tento stav je identifikován chybějícím
odkazem na zdroj (Source).

Každá APU musí mít své pojmenování. Toto je preferované označení 
dané APU a používá se při referencování APU z jiné APU. Toto označení
je předáno jako součást APU. V případě změny preferovaného označení 
dojde k aktualizaci APU odkazujících na tuto APU.
Permalink má obvykle také označení, které slouží pro reprezentaci
Apu v případě, že je na něj odkazováno.

Apu má definován svůj zdroj odkazem na Source.
Apu může mít nadřízenou jinou jednotku publikace. Případem užití 
je archivní pomůcka a její vnitřní hierarchie.

ApuType
==========

Druh APU, možné hodnoty:
 * ARCH_DESC - jednotka popisu (z EAD)
 * ARCH_ENT - archivní entita / přístupový bod
 * COLLECTION - archivní sbírka / soupis archiválií
 * FINDING_AID - archivní pomůcka
 * FUND - archivní soubor
 * INSTITUTION - archiv jako instituce


Source
==========
Zdroj archivní entity. Každý zdroj má své id/UUID a je předáván takto 
z transformačního agenta. V atributu data je uložena předaná podoba Apu. 
Součástí předaných dat mohou také být přílohy - návazné datové soubory, 
ty jsou uloženy formou příloh v úložišti.
U zdroje je uveden čas publikace.

ApuPart
===============
Část jednotky publikace. Slouží pro logické rozdělení atributů 
v jednotce publikace do menších celků. Část má definován svůj typ. 
Typ umožňuje určit název části a případně definovat její další vizuální 
vlastnosti. Každá část má hodnotu (atribut value) určující její 
textovou reprezentaci.

Část obsahuje jednotlivé prvky popisu (DescItem).

Část je přímo vázána na Apu nebo může být vázána na nadřízenou část. 
Zde je možná jen jedna úroveň vnoření, tj. část se může skládat z 0..n podčástí. 
Hlubší zanoření není přípustné.

DescItem
=============

Prvek popisu je nositelem hodnoty atributu. Prvek popisu 
je součástí právě jedné části a v rámci ní je uveden v určitém pořadí.
Prvek popisu je definovaného typu a má určitou pozici.
Prvek popisu může být určen jen pro indexování. Takový prvek popisu 
se u jednotky popisu přímo nezobrazuje a slouží jen pro její vyhledání.

ItemType
===========

Typ prvku popisu.

Typ prvku je definován svým kódem, názvem a datovým typem. 
Na úrovni prvku popisu se definuje způsob indexace (IndexType).

Kód prvku popisu se použije v překladech pro identifikaci překladu.

DataType
============
Datový typ pro prvek popisu.

Slouží pro určení druhu dat daného prvku popisu.

Přípustné datové formáty:
 * ENUM - výčtový typ
 * APU_REF - odkaz na jinou jednotku publikace
 * INTEGER - číselná hodnota
 * STRING - textová hodnota
 * UNITDATE - datace
 * LINK - odkaz na web s popisem


IndexType
=============

Způsob indexování prvku popisu.
 * not indexed - prvek není předmětem indexace, umožňuje jen zobrazení hodnoty
 * indexed - prvek se indexuje standardním způsobem

PartType
============

Druh části jednotky publikace.
Druh má své pojmenování, kód a pořadí pro zobrazení.
Každá část APU by měla mít určen svůj druh.

Kód typu části lze použít pro překlad názvu části.
