import React, { ReactElement } from 'react';
import { isEmpty, some } from 'lodash';
import Divider from '@material-ui/core/Divider';
import IconButton from '@material-ui/core/IconButton';
import CloseIcon from '@material-ui/icons/Close';
import { FormattedMessage } from 'react-intl';

import { RelationshipFilterCreator } from './relationship-filter-creator';
import { ApuPartItemType, Relationship } from '../../../types';
import { useStyles } from './styles';
import { useSpacingStyles } from '../../../styles';
import { getApuPartItemName } from '../../../common-utils';
import { Message } from '../../../enums';

interface Props {
  relationships: Relationship[];
  inDialog?: boolean;
  onChange: (newRelationships: Relationship[]) => void;
  apuPartItemTypes: ApuPartItemType[];
}

export default function RelationshipFilter({
  relationships,
  inDialog,
  onChange,
  apuPartItemTypes,
}: Props): ReactElement {
  const classes = useStyles();
  const spacingClasses = useSpacingStyles();

  const handleRemove = (field: string, value: string) =>
    onChange(
      relationships.filter(
        (r: Relationship) => !(r.value === value && r.field === field)
      )
    );

  const handleChange = (newRelationships: Relationship[]) => {
    onChange([
      ...relationships,
      ...newRelationships.filter(
        ({ field, value }: Relationship) =>
          !some(relationships, { field, value })
      ),
    ]);
  };

  return (
    <>
      {inDialog || !isEmpty(relationships) ? (
        <div className={classes.filterTitle}>
          <FormattedMessage id={Message.RELATIONSHIPS} />
        </div>
      ) : (
        <></>
      )}
      {!isEmpty(relationships) && (
        <div className={spacingClasses.marginBottom}>
          {inDialog && (
            <div className={classes.relationshipFilterLabel}>
              <FormattedMessage id={Message.ACTIVE_FILTERS} />
            </div>
          )}
          {relationships.map(({ field, value }, i) => (
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
              {getApuPartItemName(apuPartItemTypes, field || '')} - {value}
            </div>
          ))}
          {inDialog && <Divider className={spacingClasses.marginTopSmall} />}
        </div>
      )}
      {inDialog && (
        <RelationshipFilterCreator
          {...{ onChange: handleChange, apuPartItemTypes }}
        />
      )}
    </>
  );
}
