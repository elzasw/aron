import React, { useContext, PropsWithChildren } from 'react';
import { LinkProps } from './link-types';
import { NavigationContext } from 'composite/navigation/navigation-context';
import { useEventCallback } from 'utils/event-callback-hook';
import { useStyles } from './link-styles';

export function Link({
  to,
  replace,
  state,
  children,
}: PropsWithChildren<LinkProps>) {
  const { navigate } = useContext(NavigationContext);
  const classes = useStyles();

  const handleClick = useEventCallback(() => {
    navigate(to, replace, state);
  });

  return (
    <a className={classes.root} onClick={handleClick}>
      {children}
    </a>
  );
}
