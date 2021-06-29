import React from 'react';

import { WrapperProps } from './types';
import { ImageViewerWrapper } from './image-viewer-wrapper';
import { ImageBasicViewerWrapper } from './image-basic-viewer-wrapper';
// import { PdfViewerWrapper } from './pdf-viewer-wrapper';
import { NoViewWrapper } from './no-view-wrapper';
import { FileType } from './enums';

export function FileViewerWrapper({
  id,
  highResImage,
  children: Children,
}: WrapperProps) {
  const fileType = id
    ? highResImage
      ? FileType.Image
      : FileType.ImageBasic
    : FileType.Unknown;

  let Wrapper = NoViewWrapper;

  switch (fileType) {
    case FileType.Image:
      Wrapper = ImageViewerWrapper;
      break;
    case FileType.ImageBasic:
      Wrapper = ImageBasicViewerWrapper;
      break;
    // case FileType.Pdf:
    //   Wrapper = PdfViewerWrapper;
    //   break;
    default:
      break;
  }

  return (
    <Wrapper {...{ id, fileType }}>
      {(props: any) => (
        <Children
          {...{
            previousEnabled: false,
            nextEnabled: false,
            ...props,
          }}
        />
      )}
    </Wrapper>
  );
}
