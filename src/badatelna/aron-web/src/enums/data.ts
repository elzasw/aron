import { FilterType } from './filters';
import { ModulePath } from './module';
import { toFilterOptions, getISOStringFromYear } from '../common-utils';

export const data = {
  name: 'Parní mlékárna Rudolf Geiger Ronov nad Doubravou',
  archive: 'SOA Zámrsk',
  evidenceNumber: '609',
  time: '1937-1948',
  year: '1999',
  note:
    'Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Fusce suscipit libero eget elit. Fusce tellus odio, dapibus id fermentum quis, suscipit id erat. Vivamus porttitor turpis ac leo. Maecenas libero. Duis pulvinar. Suspendisse sagittis ultrices augue. Duis pulvinar. Nullam justo enim, consectetuer nec, ullamcorper ac, vestibulum in, elit. Etiam dictum tincidunt diam. Nulla quis diam. Duis pulvinar. Proin mattis lacinia justo. Fusce tellus. Aliquam ornare wisi eu metus.',
};

export const dataItems = [
  {
    id: '1',
    sub: '01 AP1',
    ...data,
  },
  {
    id: '2',
    sub: '02 AP2',
    ...data,
  },
  {
    id: '3',
    sub: '03 AP3',
    ...data,
  },
];

const archives = [
  { name: 'SOA v Zámrsku' },
  { name: 'SOkA Hradec Králove' },
  { name: 'SOkA Chrudim' },
  { name: 'SOkA Náchod' },
  { name: 'SOkA Jičín' },
  { name: 'SOkA Svitavy' },
  { name: 'SOkA Rychnov nad Kn.' },
  { name: 'SOkA Ústí nad Orlicí' },
  { name: 'SOkA Pardubice' },
  { name: 'SOkA Trutnov' },
].map((item, id) => ({ ...item, id }));

const timeRanges = [
  { from: 1201, to: 1400 },
  { from: 1401, to: 1600 },
  { from: 1601, to: 1800 },
  { from: 1800, to: 2001 },
  { from: 2000, to: null },
  { from: null, to: 1500 },
];

const languages = [
  { name: 'Česky' },
  { name: 'Anglicky' },
  { name: 'Německy' },
];

const accessPointCreation = [
  { from: 1900, to: 2000 },
  { from: 2000, to: null },
];

const getPlace = (q: string) =>
  archives
    .filter((a) => a.name.includes(q))
    .map((a, i) => ({ name: a.name, id: i }));

export const filtersData: { [key in ModulePath]?: any } = {
  [ModulePath.APU]: [],
  [ModulePath.FUND]: [
    {
      type: FilterType.RADIO,
      field: 'LANGUAGE',
      label: 'Jazyk',
      options: toFilterOptions(
        languages,
        (l) => l.name,
        (l) => l.name
      ),
    },
    {
      type: FilterType.CHECKBOX,
      field: 'ARCHIVE',
      label: 'Archiv',
      options: toFilterOptions(
        archives,
        (a) => a.name,
        (a) => a.name
      ),
    },
  ],
  [ModulePath.FINDING_AID]: [
    {
      type: FilterType.CHECKBOX,
      field: 'ARCHIVE',
      label: 'Archiv',
      options: toFilterOptions(
        archives,
        (a) => a.name,
        (a) => a.name
      ),
    },
    {
      type: FilterType.SELECT,
      label: 'Druh',
      field: 'FINDING_AID_TYPE',
      options: toFilterOptions(
        [{ type: '1. druh' }, { type: '2.druh' }],
        (t) => t.type,
        (t) => t
      ),
    },
    {
      type: FilterType.CHECKBOX_WITH_RANGE,
      field: 'TIME_RANGE',
      label: 'Časový rozsah',
      options: toFilterOptions(
        timeRanges,
        (tr) => `${tr.from ? tr.from : ''} - ${tr.to ? tr.to : ''}`,
        (tr) => ({
          from: getISOStringFromYear(tr.from),
          to: getISOStringFromYear(tr.to),
        })
      ),
    },
    {
      type: FilterType.SELECT,
      label: 'Archivní soubor',
      options: toFilterOptions(
        [
          { filename: 'Parní mlékárna Rudolf Geiger' },
          { filename: 'Další soubor' },
        ],
        (t) => t.filename,
        (t) => t
      ),
    },
  ],
  [ModulePath.ARCH_DESC]: [
    {
      type: FilterType.INPUT,
      field: 'ABSTRACT',
      label: 'Abstrakt',
    },
    {
      type: FilterType.INPUT,
      field: 'UNIT~ID',
      label: 'Id jednotky',
    },

    {
      type: FilterType.RADIO,
      field: 'containsDigitalObjects',
      label: 'Digitalizáty',
      options: [{ label: 'Jen archiválie s digitalizáty', value: true }],
    },
    {
      type: FilterType.CHECKBOX_WITH_RANGE,
      field: 'UNIT~DATE',
      label: 'Časový rozsah',
      options: toFilterOptions(
        timeRanges,
        (tr) => `${tr.from ? tr.from : ''} - ${tr.to ? tr.to : ''}`,
        (tr) => ({
          from: tr.from && new Date(tr.from, 1).toISOString(),
          to: tr.to && new Date(tr.to, 1).toISOString(),
        })
      ),
    },
    {
      type: FilterType.CHECKBOX,
      field: 'ARCH_DESC_TYPE',
      label: 'Typ archiválie',
      options: toFilterOptions(
        [
          { type: 'listina po roce 1850' },
          { type: 'úřední kniha' },
          { type: 'mapa' },
        ],
        (t) => t.type,
        (t) => t
      ),
    },
  ],
  [ModulePath.ENTITY]: [
    {
      type: FilterType.INPUT,
      field: 'PLACE_OF_CREATION',
      label: 'Místo vzniku',
      getOptions: getPlace,
    },
    {
      type: FilterType.CHECKBOX_WITH_RANGE,
      field: 'ENTITY_CREATION',
      label: 'Rok vzniku',
      options: toFilterOptions(
        accessPointCreation,
        (tr) => `${tr.from ? tr.from : ''} - ${tr.to ? tr.to : ''}`,
        (tr) => tr
      ),
    },
    {
      type: FilterType.SELECT,
      label: 'Typ',
      options: toFilterOptions(
        [{ type: '1. typ PB' }, { type: 'Jiný typ PB' }],
        (t) => t.type,
        (t) => t
      ),
    },
  ],
};
