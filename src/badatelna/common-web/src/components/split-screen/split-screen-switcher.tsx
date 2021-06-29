import React, { useMemo } from 'react';
import { useStyles } from './split-screen-styles';
import ChevronLeftIcon from '@material-ui/icons/ChevronLeft';
import ChevronRightIcon from '@material-ui/icons/ChevronRight';
import { SplitScreenSwitcherProps } from './split-screen-types';
import Button from '@material-ui/core/Button';
import useMeasure from 'react-use-measure';

export function SplitScreenSwitcher({
  handleDragStart,
  handleLeftClicked,
  handleRightClicked,
  leftLabel,
  leftDisabled = false,
  rightLabel,
  rightDisabled = false,
}: SplitScreenSwitcherProps) {
  const classes = useStyles();

  /**
   * In order to make the label visible when on the edge of screen, the width of both buttons needs to be the same.
   */
  const [leftRef, { width: leftBtnWidth }] = useMeasure();
  const [rightRef, { width: rightBtnWidth }] = useMeasure();

  const btnWidth = useMemo(
    () =>
      !leftLabel && !rightLabel ? 40 : Math.max(leftBtnWidth, rightBtnWidth),
    [leftBtnWidth, leftLabel, rightBtnWidth, rightLabel]
  );

  return (
    <div className={classes.switcherWrapper}>
      <Button
        size="small"
        ref={leftRef}
        className={classes.switcherButton}
        disabled={leftDisabled}
        style={{ width: btnWidth, borderRadius: '16px 0 0 16px' }}
        onClick={handleLeftClicked}
      >
        <ChevronLeftIcon />
        {rightLabel}
      </Button>
      <div className={classes.switcherHandle} onMouseDown={handleDragStart}>
        ⋮
      </div>
      <Button
        size="small"
        ref={rightRef}
        className={classes.switcherButton}
        disabled={rightDisabled}
        style={{ width: btnWidth, borderRadius: '0 16px 16px 0' }}
        onClick={handleRightClicked}
      >
        {leftLabel}
        <ChevronRightIcon />
      </Button>
    </div>
  );
}
