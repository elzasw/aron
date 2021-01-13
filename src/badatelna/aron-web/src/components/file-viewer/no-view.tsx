import React from 'react';

import { ViewerProps } from './types';

export function NoView({ file }: ViewerProps) {
  console.log('file :>> ', file);
  // TODO: show file type icon
  return <></>;
}
