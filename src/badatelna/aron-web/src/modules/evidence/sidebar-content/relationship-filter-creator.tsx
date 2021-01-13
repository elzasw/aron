import React, { ReactElement, useState, useCallback } from 'react';
import LinearProgress from '@material-ui/core/LinearProgress';
import { get, isArray, isEmpty } from 'lodash';
import { FormattedMessage } from 'react-intl';

import { Autocomplete, Button } from '../../../components';
import { Option, ApuPartItemType, Relationship } from '../../../types';
import {
  useGetEntityRelationships,
  useGetMatchingName,
} from '../../../common-utils';
import { ClickableSelection } from '../../../components/clickable-selection/clickable-selection';
import { useStyles } from './styles';
import { useSpacingStyles } from '../../../styles';
import { getApuPartItemName } from '../../../common-utils';
import { Message } from '../../../enums';

interface Props {
  onChange: (newRelationships: Relationship[]) => void;
  apuPartItemTypes: ApuPartItemType[];
}

export function RelationshipFilterCreator({
  onChange,
  apuPartItemTypes,
}: Props): ReactElement {
  const classes = useStyles();
  const spacingClasses = useSpacingStyles();

  const [searchValue, setSearchValue] = useState<Option | null>(null);
  const [query, setQuery] = useState('');

  const [resultAutocomplete, loadingAutocomplete] = useGetMatchingName(query);
  const [resultRelationships, loadingRelationships] = useGetEntityRelationships(
    searchValue?.id
  );

  const [selectedRelationships, setSelectedRelationships] = useState<
    {
      name: string;
      field: string;
    }[]
  >();

  const updateFilterValue = useCallback(
    (fields: string[], value: string) => {
      onChange(fields.map((field: string) => ({ field, value })));
    },
    [onChange]
  );

  return (
    <div className={classes.relationshipFilterCreatorWrapper}>
      <div className={classes.relationshipFilterLabel}>
        <FormattedMessage id={Message.NEW_FILTER} />
      </div>
      <Autocomplete
        {...{
          value: searchValue,
          onChange: (o: Option | Option[] | null) =>
            setSearchValue(o && !isArray(o) ? o : null),
          loading: loadingAutocomplete,
          onQueryChange: setQuery,
          options: get(resultAutocomplete, 'items', []),
        }}
      />
      {searchValue &&
        (loadingRelationships ? (
          <LinearProgress color="primary" />
        ) : !isEmpty(resultRelationships) ? (
          <>
            <div className={classes.relationshipFilterLabel}>
              <FormattedMessage id={Message.RELATIONSHIP_TYPE_CONSTRAINT} />
            </div>
            <ClickableSelection
              options={resultRelationships.map(
                (relationship: { key: string; value: string }) => ({
                  field: relationship.key,
                  name: `${getApuPartItemName(
                    apuPartItemTypes,
                    relationship.key
                  )} (${relationship.value})`,
                })
              )}
              onChange={setSelectedRelationships}
            />
            <Button
              contained
              color="primary"
              size="small"
              label={<FormattedMessage id={Message.ADD_TO_FILTER} />}
              className={spacingClasses.marginTopSmall}
              onClick={() =>
                updateFilterValue(
                  (selectedRelationships || []).map((r) => r.field),
                  searchValue?.name
                )
              }
            />
          </>
        ) : (
          <FormattedMessage id={Message.RELATIONSHIP_NOT_FOUND} />
        ))}
    </div>
  );
}
