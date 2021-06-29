import React, { forwardRef, useImperativeHandle } from 'react';
import { useStyles } from './split-screen-styles';
import { SplitScreenProps, SplitScreenHandle } from './split-screen-types';
import { useSplitScreen } from './split-screen-hook';
import { SplitScreenSwitcher } from './split-screen-switcher';
import { CSSProperties } from '@material-ui/core/styles/withStyles';

export const SplitScreen = forwardRef<SplitScreenHandle, SplitScreenProps>(
  function SplitScreen(
    {
      children,
      midPoint = 600,
      leftMinWidth = 0,
      rightMinWidth = 0,
      leftLabel,
      rightLabel,
    },
    ref
  ) {
    const {
      handleDragStart,
      leftSize,
      transitionOn,
      handleFullSizeLeft,
      handleFullSizeRight,
      handleMoveToMiddle,
      handleMoveLeft,
      handleMoveRight,
      isLeftOnFullscreen,
      isRightOnFullscreen,
    } = useSplitScreen({ leftMinWidth, midPoint, rightMinWidth });

    useImperativeHandle(
      ref,
      () => ({
        handleFullSizeLeft,
        handleFullSizeRight,
        handleMoveToMiddle,
        isLeftOnFullscreen,
        isRightOnFullscreen,
      }),
      [
        handleFullSizeLeft,
        handleFullSizeRight,
        handleMoveToMiddle,
        isLeftOnFullscreen,
        isRightOnFullscreen,
      ]
    );

    /**
     * Styles
     */
    const classes = useStyles();
    const commonStyles: CSSProperties = {
      overflow: 'auto',
      boxSizing: 'border-box',
      transition: transitionOn ? 'width 0.5s linear' : undefined,
    };

    const leftContainerStyles = {
      ...commonStyles,
      width: `${leftSize}px`,
      minWidth: `${leftMinWidth}px`,
    };

    const rightContainerStyles = {
      ...commonStyles,
      width: `calc(100% - ${leftSize}px)`,
      minWidth: `${rightMinWidth}px`,
    };

    return (
      <>
        <div style={leftContainerStyles}>{children[0]}</div>
        <div className={classes.dragger}>
          <SplitScreenSwitcher
            handleDragStart={handleDragStart}
            handleLeftClicked={handleMoveLeft}
            handleRightClicked={handleMoveRight}
            leftLabel={leftSize < 50 ? leftLabel : ''}
            leftDisabled={leftSize === leftMinWidth}
            rightLabel={leftSize > window.innerWidth - 50 ? rightLabel : ''}
            rightDisabled={leftSize === window.innerWidth - rightMinWidth}
          />
        </div>
        <div style={rightContainerStyles}>{children[1]}</div>,
      </>
    );
  }
);
