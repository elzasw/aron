import React from 'react';

import { WrapperProps } from './types';
import { ImageViewerWrapper } from './image-viewer-wrapper';
// import { PdfViewerWrapper } from './pdf-viewer-wrapper';
// import { NoViewWrapper } from './no-view-wrapper';

export function FileViewerWrapper({ id, children: Children }: WrapperProps) {
  const Wrapper = ImageViewerWrapper;
  // const Wrapper = PdfViewerWrapper;
  // const Wrapper = NoViewWrapper;

  return (
    <Wrapper {...{ id }}>
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
