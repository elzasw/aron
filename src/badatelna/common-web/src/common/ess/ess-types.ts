import { DatedObject, DictionaryObject, FileRef } from 'common/common-types';

/**
 * Spis
 *
 * Spis je entita, v níž jsou organizovány dokumenty vztahující se ke stejnému předmětu
 * (věci). Spisy se vyskytují pouze ve věcných skupinách, které neobsahují jiné věcné
 * skupiny nebo typové spisy.
 */
export interface Record extends DatedObject {
  /**
   * Identifikátor
   */
  essId: EssIdentifier;

  /**
   * Spisová značka.
   */
  recordSymbol: string;

  /**
   * Název.
   */
  name: string;

  /**
   * Čárový kód.
   */
  barCode: string;

  /**
   * Popis.
   */
  description: string;

  /**
   * Skartační režim.
   */
  shreddingMode: ShreddingMode;

  /**
   * Spisový znak.
   */
  classificationCode: string;

  /**
   * Iniciační dokument.
   */
  document: Document;
}

/**
 * Typ dokumentu.
 *
 * Typem dokumentu se rozumí věcná charakteristika popisující dokument. Konkrétní
 * charakteristika umožňuje eSSL, aby spravoval dokumenty stejného typu (konkrétní
 * charakteristiky) shodně a stanoveným, určitým způsobem bez ohledu na jejich
 * zařazení do seskupení. Typem dokumentu jsou například "faktury", "smlouvy" nebo
 * "webové stránky"
 */
export type DocumentType = DictionaryObject;

/**
 * Dokument
 *
 * Dokumentem je každá písemná, obrazová, zvuková nebo jiná zaznamenaná
 * informace, ať již v podobě analogové nebo digitální, která byla vytvořena původcem
 * nebo byla původci doručena [§ 2 písm. e) zákona č. 499/2004 Sb., o archivnictví
 * a spisové službě a o změně některých zákonů, ve znění pozdějších předpisů, (dále jen
 * „zákon“)].
 * Dokument tvoří jedna nebo více komponent (například průvodní dopis má připojeny
 * přílohy). Dokument vytvořený původcem vzniká jako koncept a do okamžiku přidělení
 * evidenčního čísla existuje v eSSL jako rozpracovaný dokument.
 * Dokument lze zaznamenat na jakémkoliv médiu a v jakémkoli datovém formátu.
 */
export interface Document extends DatedObject {
  /**
   * Identifikátor
   */
  essId: EssIdentifier;

  /**
   * Datum evidence ve spisové službě.
   */
  registered: string;

  /**
   * Čárový kód.
   */
  barCode: string;

  /**
   * Název.
   */
  name: string;

  /**
   * Popis.
   */
  description: string;

  /**
   * Číslo jednací.
   */
  referenceNumber: string;

  /**
   * Skartační režim.
   */
  shreddingMode: ShreddingMode;

  /**
   * Typ dokumentu.
   */
  type: DocumentType;

  /**
   * Spisový znak.
   */
  classificationCode: string;

  /**
   * Přílohy
   */
  components: Component[];

  /**
   * Spis dokumentu
   */
  record: Record;
}

/**
 * Komponenta
 *
 * Komponentou v digitální podobě se rozumí jednoznačně vymezený proud bitů tvořící
 * počítačový soubor. V analogové podobě je komponentou dále nedělitelná část
 * dokumentu (průvodní dopis, příloha). Komponenta, popřípadě skupina komponent,
 * vytváří rozpracovaný dokument nebo dokument.
 */
export interface Component extends DatedObject {
  /**
   * Identifikátor
   */
  essId: EssIdentifier;

  /**
   * Čas synchronizace.
   */
  syncTime: string;

  /**
   * Typ komponenty.
   */
  type: ComponentType;

  /**
   * Soubor.
   */
  file: FileRef;

  /**
   * Dokument, ke kterému se komponenta váže.
   */
  document: Document;
}

export enum DispatchState {
  CREATED = 'CREATED',
  SENT = 'SENT',
  DELIVERED = 'DELIVERED',
}

/**
 * Zásilka (Vypravení)
 *
 * Zásilka je prostředek pro doručování dokumentů v analogové nebo digitální podobě.
 * Zásilkou je nejčastěji listinná obálka, datová zpráva z informačního systému datových
 * schránek, e-mail, optický disk nebo flash disk.
 */
export interface Dispatch extends DatedObject {
  /**
   * Identifikátor
   */
  essId: EssIdentifier;

  /**
   * Dokument.
   */

  document: Document;

  /**
   * Subjekt.
   */
  subject: Subject;

  /**
   * Způsob odeslání.
   */
  method: DispatchMethod;

  /**
   * Stav.
   */
  state: DispatchState;
}

export interface EssIdentifier {
  id: string;
  source: string;
}

/**
 * Skartační režim.
 *
 * Skartační režim je původcem stanovený systém vyřazování entit, který vymezuje dobu
 * jejich ukládání (skartační lhůta) a určuje typ skartační operace (skartační znak:
 * A – návrh na trvalé uložení, V – předložení k přezkumu, S – zničení), popřípadě z roku
 * zařazení dokumentu do skartačního řízení a jiné skutečnosti, kterou veřejnoprávní
 * původce stanoví jako spouštěcí událost. Při posouzení se v rámci odborné prohlídky
 * vyhodnocují
 * a) metadata,
 * b) obsah dokumentu, nebo
 * c) metadata a obsah dokumentu.
 */
export interface ShreddingMode extends DictionaryObject {
  /**
   * Identifikátor
   */
  essId: EssIdentifier;

  /**
   * Skartační znak.
   */
  symbol: string;

  /**
   * Skartační lhůta
   */
  period: number;
}

/**
 * Typ komponenty
 */
export enum ComponentType {
  MAIN = 'MAIN',
  DELIVERY_NOTE = 'DELIVERY_NOTE',
}

/**
 * Spůsob doručení
 */
export enum DispatchMethod {
  EMAIL = 'EMAIL',
  DATABOX = 'DATABOX',
  POSTAL = 'POSTAL',
}

/**
 * Příjemce zásilky
 */
export interface Subject {
  /**
   * Jméno.
   */
  firstName: string;

  /**
   * Příjmení.
   */
  lastName: string;

  /**
   * Titul před.
   */
  titleBefore: string;

  /**
   * Titul za.
   */
  titleAfter: string;

  /**
   * Název organizace.
   */
  companyName: string;

  /**
   * IČ organizace.
   */
  companyId: string;

  /**
   * Datová schránka.
   */
  databox: string;

  /**
   * Email.
   */
  email: string;

  /**
   * Poštovní adresa.
   */
  postalAddress: PostalAddress;
}

/**
 * Poštovní adresa.
 */
export interface PostalAddress {
  /**
   * Obec.
   */
  town: string;

  /**
   * Ulice.
   */
  street: string;

  /**
   * Číslo orientační.
   */
  orientationNumber: string;

  /**
   * Číslo popisné.
   */
  descriptiveNumber: string;

  /**
   * PSČ.
   */
  zip: string;
}
