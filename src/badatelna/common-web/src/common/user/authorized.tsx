import React, { PropsWithChildren, useContext, useEffect } from 'react';
import { UserContext } from './user-context';
import { AuthorizedProps } from './user-types';
import { useForceRender } from 'utils/force-render';

export function Authorized(props: PropsWithChildren<AuthorizedProps>) {
  const { hasPermission, reload } = useContext(UserContext);
  const { forceRender } = useForceRender();

  useEffect(() => {
    if (props.shouldReload) {
      reload().then(() => forceRender());
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  if (hasPermission(props.permission)) {
    return <>{props.children}</>;
  } else {
    return <></>;
  }
}
