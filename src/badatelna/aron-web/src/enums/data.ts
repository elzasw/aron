import { FilterType } from './filters';
import { ModulePath } from './module';

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
  { from: 1201, until: 1400 },
  { from: 1401, until: 1600 },
  { from: 1601, until: 1800 },
  { from: 1800, until: 2001 },
  { from: 2000, until: null },
  { from: null, until: 1500 },
];

const languages = [
  { name: 'Česky' },
  { name: 'Anglicky' },
  { name: 'Německy' },
];

const accessPointCreation = [
  { from: 1900, until: 2000 },
  { from: 2000, until: null },
];

const toOptions = (
  items: any[],
  createLabel: (item: any) => any,
  createValue: (item: any) => any
) =>
  items.map((item: any, index: number) => ({
    id: index.toString(),
    label: createLabel(item),
    value: createValue(item),
  }));

const getPlace = (q: string,) => archives
  .filter((a) => a.name.includes(q))
  .map((a, i) => ({name: a.name, id: i}))

export const filtersData: { [key in ModulePath]?: any } = {
  [ModulePath.SEARCH]: [],
  [ModulePath.FUND]: [
    {
      type: FilterType.RADIOBUTTON,
      name: 'LANGUAGE',
      title: 'Jazyk',
      options: toOptions(
        languages,
        (l) => l.name,
        (l) => l.name
      ),
    },
    {
      type: FilterType.CHECKBOX,
      name: 'ARCHIVE',
      title: 'Archiv',
      options: toOptions(
        archives,
        (a) => a.name,
        (a) => a.name
      ),
    },
  ],
  [ModulePath.FINDING_AID]: [
    {
      type: FilterType.CHECKBOX,
      name: 'ARCHIVE',
      title: 'Archiv',
      options: toOptions(
        archives,
        (a) => a.name,
        (a) => a.name
      ),
    },
    {
      type: FilterType.SELECT,
      title: 'Druh',
      name: 'FINDING_AID_TYPE',
      options: toOptions(
        [{ type: '1. druh' }, { type: '2.druh' }],
        (t) => t.type,
        (t) => t
      ),
    },
    {
      type: FilterType.CHECKBOX_WITH_RANGE,
      name: 'TIME_RANGE',
      title: 'Časový rozsah',
      options: toOptions(
        timeRanges,
        (tr) => `${tr.from ? tr.from : ''} - ${tr.until ? tr.until : ''}`,
        (tr) => tr
      ),
    },
    {
      type: FilterType.SELECT,
      title: 'Archivní soubor',
      options: toOptions(
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
      type: FilterType.RADIOBUTTON,
      name: 'ARCH_DESC_DIGITAL_ONLY',
      title: 'Digitalizáty',
      options: toOptions(
        [{ type: 'Jen archiválie s digitalizáty' }],
        (t) => t.type,
        (t) => t
      ),
    },
    {
      type: FilterType.CHECKBOX_WITH_RANGE,
      name: 'TIME_RANGE',
      title: 'Časový rozsah',
      options: toOptions(
        timeRanges,
        (tr) => `${tr.from ? tr.from : ''} - ${tr.until ? tr.until : ''}`,
        (tr) => tr
      ),
    },
    {
      type: FilterType.CHECKBOX,
      name: 'ARCH_DESC_TYPE',
      title: 'Typ archiválie',
      options: toOptions(
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
      type: FilterType.INPUTBOX,
      name: 'PLACE_OF_CREATION',
      title: 'Místo vzniku',
      getOptions: getPlace
    },
    {
      type: FilterType.CHECKBOX_WITH_RANGE,
      name: 'ENTITY_CREATION',
      title: 'Rok vzniku',
      options: toOptions(
        accessPointCreation,
        (tr) => `${tr.from ? tr.from : ''} - ${tr.until ? tr.until : ''}`,
        (tr) => tr
      ),
    },
    {
      type: FilterType.SELECT,
      title: 'Typ',
      options: toOptions(
        [{ type: '1. typ PB' }, { type: 'Jiný typ PB' }],
        (t) => t.type,
        (t) => t
      ),
    },
  ],
};
