import React from 'react';

import { WrapperProps } from './types';
import { ApiUrl } from '../../enums';
import { createUrl } from '../../common-utils';

export function ImageBasicViewerWrapper({
  id,
  fileType,
  children: Children,
}: WrapperProps) {
  // TODO: add react-zoom-pan-pinch or something else if needed
  return (
    <Children
      {...{
        noAction: true,
        fileViewerProps: {
          fileType,
          src: createUrl(`${ApiUrl.FILE}/${id}`),
        },
      }}
    />
  );
}
