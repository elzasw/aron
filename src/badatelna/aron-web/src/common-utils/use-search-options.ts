import { useIntl } from 'react-intl';

import {
  ModulePath,
  Message,
  // FacetType
} from '../enums';
// import { ApiFilterOperation } from '../types';

export function useSearchOptions() {
  const { formatMessage } = useIntl();

  return [
    {
      name: formatMessage({ id: Message.ARCH_DESC_DAO_ONLY }),
      path: ModulePath.ARCH_DESC,
      filters: [
        // {
        //   source: 'containsDigitalObjects',
        //   type: FacetType.DAO_ONLY,
        //   operation: ApiFilterOperation.EQ,
        //   value: [true],
        // },
        {
          source: 'DIGITAL',
          value: ['Ano'],
        },
      ],
    },
    {
      name: formatMessage({ id: Message.ARCH_DESC }),
      path: ModulePath.ARCH_DESC,
    },
    { name: formatMessage({ id: Message.FUND }), path: ModulePath.FUND },
    {
      name: formatMessage({ id: Message.FINDING_AID }),
      path: ModulePath.FINDING_AID,
    },
    { name: formatMessage({ id: Message.ENTITY }), path: ModulePath.ENTITY },
  ];
}
