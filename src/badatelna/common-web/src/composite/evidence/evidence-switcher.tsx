import React, { useCallback } from 'react';
import clsx from 'clsx';
import ChevronLeftIcon from '@material-ui/icons/ChevronLeft';
import ChevronRightIcon from '@material-ui/icons/ChevronRight';
import { useStyles } from './evidence-styles';
import { EvidenceSwitcherProps, EvidenceScreenMode } from './evidence-types';

/**
 * Screen mode switcher.
 */
export function EvidenceSwitcher({
  screenMode,
  onChange,
}: EvidenceSwitcherProps) {
  const classes = useStyles();

  const disabledCollapse = screenMode === EvidenceScreenMode.DETAIL;
  const disabledExpand = screenMode === EvidenceScreenMode.TABLE;

  const collapse = useCallback(() => {
    if (screenMode === EvidenceScreenMode.SPLIT) {
      onChange(EvidenceScreenMode.DETAIL);
    } else {
      onChange(EvidenceScreenMode.SPLIT);
    }
  }, [onChange, screenMode]);

  const expand = useCallback(() => {
    if (screenMode === EvidenceScreenMode.SPLIT) {
      onChange(EvidenceScreenMode.TABLE);
    } else {
      onChange(EvidenceScreenMode.SPLIT);
    }
  }, [onChange, screenMode]);

  return (
    <div
      className={clsx(classes.switcherWrapper, {
        [classes.switcherDisabledCollapse]: disabledCollapse,
        [classes.switcherDisabledExpand]: disabledExpand,
      })}
    >
      <ChevronLeftIcon
        onClick={collapse}
        className={clsx({ [classes.disabledIcon]: disabledCollapse })}
      />
      <ChevronRightIcon
        onClick={expand}
        className={clsx({ [classes.disabledIcon]: disabledExpand })}
      />
    </div>
  );
}
