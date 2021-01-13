import React from 'react';
import { Document, Page } from 'react-pdf';

import { ViewerProps } from './types';
import { useStyles } from './styles';

export function PdfViewer({ file, scale, pageNumber }: ViewerProps) {
  const classes = useStyles();

  return (
    <Document
      file={file}
      error={
        <div className={classes.pdfViewerError}>
          Nepodařilo se načíst soubor!
        </div>
      }
      loading={<div />}
    >
      <Page
        pageNumber={pageNumber}
        height={window.innerHeight * 0.9}
        scale={scale}
      />
    </Document>
  );
}
