import React from 'react';

import { WrapperProps } from './types';

export function NoViewWrapper({ children: Children }: WrapperProps) {
  return (
    <Children
      {...{
        noView: true,
        fileViewerProps: {},
      }}
    />
  );
}
