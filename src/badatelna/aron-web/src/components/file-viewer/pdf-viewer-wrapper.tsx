import React, { useState } from 'react';

import { WrapperProps } from './types';

export function PdfViewerWrapper({
  id,
  fileType,
  children: Children,
}: WrapperProps) {
  const [pageNumber, setPageNumber] = useState(0);
  const [numPages, setNumPages] = useState(0);
  const [zoom, setZoom] = useState(1);

  const previousDisabled = pageNumber <= 0;
  const nextDisabled = pageNumber >= numPages;
  const zoomInDisabled = zoom >= 3;
  const zoomOutDisabled = zoom <= 0.25;

  const previous = () => !previousDisabled && setPageNumber(pageNumber - 1);

  const next = () => !nextDisabled && setPageNumber(pageNumber + 1);

  const zoomIn = () => !zoomInDisabled && setZoom(zoom + 0.25);

  const zoomOut = () => !zoomOutDisabled && setZoom(zoom - 0.25);

  const reset = () => setZoom(1);

  const onLoadSuccess = ({ numPages }: { numPages: number }) =>
    setNumPages(numPages);

  return (
    <Children
      {...{
        previous,
        next,
        zoomIn,
        zoomOut,
        reset,

        previousEnabled: true,
        nextEnabled: true,

        previousDisabled,
        nextDisabled,
        zoomInDisabled,
        zoomOutDisabled,

        fileViewerProps: {
          file: id,
          fileType,
          zoom,
          pageNumber,
          onLoadSuccess,
        }, // TODO:
      }}
    />
  );
}
