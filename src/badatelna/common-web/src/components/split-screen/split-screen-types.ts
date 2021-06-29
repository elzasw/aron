import { ReactChild } from 'react';

export interface SplitScreenProps {
  /**
   * Defines the default size of left container
   */
  midPoint?: number;
  /**
   * Minimal size of right container in px, the dragger will not be able to
   * scroll further
   */
  rightMinWidth?: number;
  /**
   * Minimal size of left container in px, the dragger will not be able to
   * scroll further
   */
  leftMinWidth?: number;

  /**
   * Label for the switcher component for the left screen visible when left width is small
   * (Note: the label for left screen is shown on right of the switcher)
   */
  leftLabel?: string;

  /**
   * Label for the switcher component for the right screen visible when right width is small
   * (Note: the label for left screen is shown on left of the switcher)
   */
  rightLabel?: string;

  /**
   * Children, containing exactly two elements
   */
  children: [ReactChild, ReactChild];
}

export interface SplitScreenSwitcherProps {
  handleDragStart: (e: React.MouseEvent<Element, MouseEvent>) => void;
  handleRightClicked: () => void;
  handleLeftClicked: () => void;
  leftLabel?: string;
  leftDisabled?: boolean;
  rightLabel?: string;
  rightDisabled?: boolean;
}

export interface SplitScreenHandle {
  handleFullSizeLeft: VoidFunction;
  handleFullSizeRight: VoidFunction;
  handleMoveToMiddle: VoidFunction;
  isLeftOnFullscreen: () => boolean;
  isRightOnFullscreen: () => boolean;
}
