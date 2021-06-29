import { DaoBundleType } from './dao';

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

export const fakeDaos = [
  {
    id: 'feb44d55-a13f-4bd9-a975-fb7b29729f7d',
    name: 'dao',
    permalink: '',
    files: [
      {
        id: 'eeb44d55-a13f-4bd9-a975-fb7b29729f7d',
        permalink: 'eeb44d55-a13f-4bd9-a975-fb7b29729f7d',
        order: 1,
        type: DaoBundleType.PUBLISHED,
        metadata: [],
        file: {
          id: '000c52c5-21e0-4211-b17e-a82457f5c752',
          created: '2020-12-22T18:25:58.685Z',
          updated: '2020-12-22T18:26:01.410Z',
          deletedByTenant: null,
          name: 'test',
          contentType: 'image/jpeg',
          size: 1948960,
          permanent: false,
        },
      },
      {
        id: '017d757a-bd4b-4961-9beb-ff9feadef64c',
        permalink: 'eeb44d55-a13f-4bd9-a975-fb7b29729f7d',
        order: 1,
        type: DaoBundleType.TILE,
        metadata: [],
        file: {
          id: '007d757a-bd4b-4961-9beb-ff9feadef64c',
          created: '2020-12-22T18:25:58.685Z',
          updated: '2020-12-22T18:26:01.410Z',
          deletedByTenant: null,
          name: 'test',
          contentType: 'image/jpeg',
          size: 1948960,
          permanent: false,
        },
      },
      {
        id: 'cd6ceb5f-858f-4240-85fa-fbbbcd29073e',
        permalink: '',
        order: 1,
        type: DaoBundleType.THUMBNAIL,
        metadata: [
          {
            id: 'b1f3210a-2d5c-47ed-b335-6812e368341a',
            value: 'image/jpeg',
            type: 'mimeType',
          },
        ],
        file: {
          id: 'ebdddb36-ca4c-4091-bc86-ec9e15865f6f',
          created: '2021-01-04T13:22:19.232488Z',
          updated: '2021-01-04T13:22:19.232915Z',
          name: 'undefined',
          contentType: 'image/jpeg',
          size: 32623,
          permanent: true,
        },
      },
      {
        id: '80381d6e-cc3c-435f-8c87-6939fc844c37',
        permalink: '',
        order: 1,
        type: DaoBundleType.TILE,
        metadata: [],
        file: {
          id: 'd5331e4c-95b4-4381-8621-284d415a97e6',
          created: '2021-01-04T13:22:21.306424Z',
          updated: '2021-01-04T13:22:21.306434Z',
          name: 'undefined',
          contentType: 'application/octet-stream',
          size: 5202314,
          permanent: true,
        },
      },
      {
        id: 'fa8dc5fd-3653-45b7-be40-0e1a323e0939',
        permalink: '',
        order: 1,
        type: DaoBundleType.PUBLISHED,
        metadata: [
          {
            id: '93d77dbd-3646-4576-a134-0526c340c7a3',
            value: 'image/jpeg',
            type: 'mimeType',
          },
        ],
        file: {
          id: 'ff209469-827b-445a-bdcd-6b4ec0bc4b3d',
          created: '2021-01-04T13:22:21.310842Z',
          updated: '2021-01-04T13:22:21.310851Z',
          name: 'undefined',
          contentType: 'image/jpeg',
          size: 57071,
          permanent: true,
        },
      },
    ],
  },
  {
    id: 'a51ed9e4-34cc-4954-aad6-3435bb4b1003',
    name: 'dao 2',
    permalink: '',
    files: [],
  },
];
