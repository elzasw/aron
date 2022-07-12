import React from 'react';
import { get, find } from 'lodash';
import { Link } from 'react-router-dom';

import { ModulePath, ApuPartItemDataType } from '../../enums';
import { useStyles } from './styles';
import { ApuEntitySimplified } from '../../types';
import { formatUnitDate } from '../../common-utils';
import { EvidenceJSONDisplay } from './evidence-json-display';

export function EvidenceDetailItemValue({
  value,
  type,
  href,
  apus,
}: {
  value: string;
  type: ApuPartItemDataType;
  href?: string;
  apus?: ApuEntitySimplified[];
}) {
  const classes = useStyles();

  let result: string | JSX.Element = value;
  const apu =
    type === ApuPartItemDataType.APU_REF
      ? find(apus, ({ id }) => id === value)
      : null;

  switch (type) {
    case ApuPartItemDataType.UNITDATE:
      result = formatUnitDate(value);
      break;
    case ApuPartItemDataType.APU_REF:
      result = (
        <span>
          <Link
            to={{ pathname: `${ModulePath.APU}/${value}` }}
            className={classes.link}
          >
            {get(apu, 'name', 'Zobrazit APU')}
          </Link>
          {apu && get(apu, 'description') ? (
            <span>, {get(apu, 'description')}</span>
          ) : (
            <></>
          )}
        </span>
      );
      break;
    case ApuPartItemDataType.LINK:
      result = (
        <a
          href={href || value}
          target="_blank"
          rel="noreferrer"
          className={classes.link}
        >
          {value}
        </a>
      );
      break;
    case ApuPartItemDataType.JSON:
        result = <EvidenceJSONDisplay jsonString={value}/>;
        break;
  }

  return <span>{result}</span>;
}
