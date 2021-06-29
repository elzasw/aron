import React, { ReactElement } from 'react';
import { isEmpty, some, replace } from 'lodash';
import Divider from '@material-ui/core/Divider';
import IconButton from '@material-ui/core/IconButton';
import CloseIcon from '@material-ui/icons/Close';
import { FormattedMessage } from 'react-intl';

import { Relationship } from '../../../types';
import { useStyles } from './styles';
import { useSpacingStyles } from '../../../styles';
import { getApuPartItemName } from '../../../common-utils';
import { Message } from '../../../enums';
import { RelationshipFilterProps, RelationshipFilterCreator } from '.';
import { Tooltip } from '@eas/common-web';

export function RelationshipFilter({
  source,
  value: filterValue,
  onChange,
  apuPartItemTypes,
  tooltip,
  description,
  inDialog,
  group,
  apiFilters = [],
}: RelationshipFilterProps): ReactElement {
  const classes = useStyles();
  const spacingClasses = useSpacingStyles();

  const handleChange = (newRelationships: Relationship[]) => {
    onChange({
      source,
      value: [
        ...(filterValue ? filterValue : []),
        ...newRelationships.filter(
          ({ field, value }: Relationship) =>
            !some(filterValue, { field, value })
        ),
      ],
    });
  };

  const handleRemove = (field: string, value: string) => {
    onChange({
      source,
      value: filterValue.filter(
        (r: Relationship) => r.value !== value || r.field !== field
      ),
    });
  };

  return (
    <>
      {inDialog || !isEmpty(filterValue) ? (
        <div className={classes.filterTitle}>
          <Tooltip title={tooltip}>
            <div>{getApuPartItemName(apuPartItemTypes, source)}</div>
          </Tooltip>
        </div>
      ) : (
        <></>
      )}
      {inDialog && description ? (
        <div className={classes.filterDescription}>{description}</div>
      ) : (
        <></>
      )}
      {!isEmpty(filterValue) && (
        <div className={spacingClasses.marginBottom}>
          {inDialog && (
            <div className={classes.relationshipFilterLabel}>
              <FormattedMessage id={Message.ACTIVE_FILTERS} />
            </div>
          )}
          {filterValue.map(({ field, value, name }, i) => (
            <div className={classes.relationshipFilterListItem} key={i}>
              {inDialog && (
                <IconButton
                  color="secondary"
                  size="small"
                  onClick={() => field && handleRemove(field, value)}
                >
                  <CloseIcon />
                </IconButton>
              )}
              {getApuPartItemName(apuPartItemTypes, field || '')} -{' '}
              {name || value}
            </div>
          ))}
          {inDialog && <Divider className={spacingClasses.marginTopSmall} />}
        </div>
      )}
      {inDialog && (
        <RelationshipFilterCreator
          {...{
            onChange: handleChange,
            apuPartItemTypes,
            apiFilters,
            source,
            group: group && replace(group, '_', '~'),
          }}
        />
      )}
    </>
  );
}
