import React from 'react';

import { Module } from '../../components';
import { useSpacingStyles } from '../../styles';
import { Props } from './types';

export function Help({ path, label }: Props) {
  const spacingClasses = useSpacingStyles();

  return (
    <Module
      {...{
        path,
        items: [{ label }],
      }}
    >
      <div className={spacingClasses.paddingBig}>
        Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Fusce suscipit
        libero eget elit. Fusce tellus odio, dapibus id fermentum quis, suscipit
        id erat. Vivamus porttitor turpis ac leo. Maecenas libero. Duis
        pulvinar. Suspendisse sagittis ultrices augue. Duis pulvinar. Nullam
        justo enim, consectetuer nec, ullamcorper ac, vestibulum in, elit. Etiam
        dictum tincidunt diam. Nulla quis diam. Duis pulvinar. Proin mattis
        lacinia justo. Fusce tellus. Aliquam ornare wisi eu metus.
      </div>
    </Module>
  );
}
