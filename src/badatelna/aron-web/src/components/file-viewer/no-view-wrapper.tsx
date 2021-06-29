import React from 'react';

import { WrapperProps } from './types';

export function NoViewWrapper({ fileType, children: Children }: WrapperProps) {
  return (
    <Children
      {...{
        noView: true,
        fileViewerProps: { fileType },
      }}
    />
  );
}
