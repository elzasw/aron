import React from 'react';
import classNames from 'classnames';

import { ViewerProps } from './types';
import { useStyles } from './styles';
import { useLayoutStyles } from '../../styles';
import { ImageBasicViewer } from './image-basic-viewer';
import { PdfViewer } from './pdf-viewer';
import { NoView } from './no-view';
import { FileType } from './enums';

export function FileViewer({ fileType, ...props }: ViewerProps) {
  const classes = useStyles();
  const layoutClasses = useLayoutStyles();

  let Component = NoView;

  switch (fileType) {
    case FileType.ImageBasic:
      Component = ImageBasicViewer;
      break;
    case FileType.Pdf:
      Component = PdfViewer;
      break;
    default:
      break;
  }

  return (
    <div className={classNames(classes.fileViewer, layoutClasses.flexCentered)}>
      <Component {...props} />
    </div>
  );
}
