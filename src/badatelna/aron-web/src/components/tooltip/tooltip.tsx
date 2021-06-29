import React from 'react';
import MUITooltip from '@material-ui/core/Tooltip';

import { Props } from './types';

export function Tooltip({ title, children }: Props) {
  return title ? <MUITooltip {...{ title }}>{children}</MUITooltip> : children;
}
