import React, { useState } from 'react';
import classNames from 'classnames';

import { useStyles } from './styles';
import {
  // useLayoutStyles,
  useSpacingStyles,
} from '../../styles';
import { DetailDaoProps } from './types';
import { EvidenceDetailDaoDialog } from './evidence-detail-dao-dialog';

export function EvidenceDetailDao({ item }: DetailDaoProps) {
  const classes = useStyles();
  // const layoutClasses = useLayoutStyles();
  const spacingClasses = useSpacingStyles();

  const [open, setOpen] = useState(false);

  return (
    <div
      className={classNames(
        classes.dao,
        spacingClasses.marginRight,
        spacingClasses.marginBottom
      )}
    >
      <div
        className={classNames(
          classes.daoPreview,
          spacingClasses.marginBottomSmall
        )}
      ></div>
      <div className={classes.link} onClick={() => setOpen(true)}>
        Zobrazit
      </div>
      <EvidenceDetailDaoDialog
        {...{ open, item, onClose: () => setOpen(false) }}
      />
    </div>
  );
}
