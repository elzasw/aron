import React, { ReactNode } from 'react';
import { useStyles } from './styles';

interface Props {
  text: ReactNode;
  count: number;
}

export const TextWithCount = ({ text, count }: Props) => {
  const classes = useStyles();
  return (
    <div>
      <span className={classes.text}>{text}</span>
      <span className={classes.count}>
        <span>(</span>
        {count}
        <span>)</span>
      </span>
    </div>
  );
};
