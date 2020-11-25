import React, { useContext } from 'react';
import classNames from 'classnames';

import { NavigationContext } from '@eas/common-web';

import { ListProps } from './types';
import { useStyles } from './styles';
import { useSpacingStyles } from '../../styles';
import { getPathByType } from './utils';

export function EvidenceList({ items }: ListProps) {
  const classes = useStyles();
  const spacingClasses = useSpacingStyles();

  const { navigate } = useContext(NavigationContext);

  return (
    <div className={classes.list}>
      <div className={spacingClasses.padding}>
        {items.length ? (
          items.map(({ id, type, name, description }) => (
            <div
              key={id}
              onClick={() => navigate(`${getPathByType(type)}/${id}`)}
              className={classNames(
                classes.listItem,
                spacingClasses.marginBottom
              )}
            >
              <h4>{name}</h4>
              <p>{description}</p>
            </div>
          ))
        ) : (
          <div
            className={classNames(classes.listEmpty, spacingClasses.padding)}
          >
            Žádné položky nenalezeny. Zkuste změnit zadaná kritéria.
          </div>
        )}
      </div>
    </div>
  );
}
