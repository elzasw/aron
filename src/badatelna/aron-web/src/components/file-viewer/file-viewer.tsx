import React from 'react';
import classNames from 'classnames';

import { ViewerProps } from './types';
import { useStyles } from './styles';
import { useLayoutStyles } from '../../styles';
import { ImageViewer } from './image-viewer';
// import { PdfViewer } from './pdf-viewer';
// import { NoView } from './no-view';

export function FileViewer({ ...props }: ViewerProps) {
  const classes = useStyles();
  const layoutClasses = useLayoutStyles();

  // if (!props.file) {
  //   return <></>;
  // }

  const Component = ImageViewer;
  // const Component = PdfViewer;
  // const Component = NoView;

  return (
    <div className={classNames(classes.fileViewer, layoutClasses.flexCentered)}>
      <Component {...props} />
    </div>
  );
}
