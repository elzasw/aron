import React, { useEffect } from 'react';

import { useAppState } from '../../common-utils';
import { ModulePath } from '../../enums';
import { Props } from './types';
import { useStyles } from './styles';
import { BreadcrumbItems } from './breadcrumb-items';

export function Module({ children, path, items, ...props }: Props) {
  const classes = useStyles();

  const { appState, updateAppState } = useAppState();

  useEffect(() => {
    if (path) {
      let newPath: ModulePath | null = null;

      switch (path) {
        case ModulePath.ARCH_DESC:
        case ModulePath.ENTITY:
        case ModulePath.FINDING_AID:
        case ModulePath.FUND:
          newPath = path;
          break;
        default:
          break;
      }

      if (appState.evidencePath !== newPath) {
        updateAppState({ evidencePath: newPath });
      }
    }
  }, [path, appState, updateAppState]);

  const key = JSON.stringify(
    items.filter(({ label }) => typeof label === 'string')
  );

  return (
    <div className={classes.module}>
      <BreadcrumbItems {...{ key, items, ...props }} />
      <div className={classes.moduleWrapper}>
        {children}
      </div>
    </div>
  );
}
