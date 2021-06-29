import React from 'react';

import { Props } from './types';
import BookIcon from '../../assets/icons/book.svg';

export function Icon({ size = 24, color, ...props }: Props) {
  return (
    <img
      {...props}
      src={BookIcon}
      style={{ width: size, height: size, color }}
    />
  );
}
