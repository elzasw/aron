import React from 'react';

import { ViewerProps } from './types';
import { useStyles } from './styles';

export function ImageViewer({ viewerRef }: ViewerProps) {
  const classes = useStyles();

  return <div {...{ ref: viewerRef, className: classes.imageViewer }} />;
}
