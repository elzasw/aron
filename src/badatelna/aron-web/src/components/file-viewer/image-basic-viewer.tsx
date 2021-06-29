import React from 'react';

import { ViewerProps } from './types';

export function ImageBasicViewer({ src }: ViewerProps) {
  return <img {...{ src, alt: 'dao-file' }} />;
}
