import React, {
  PropsWithChildren,
  useContext,
  useEffect,
  useState,
} from 'react';
import { UserContext } from './user-context';
import { LoggedInProps } from './user-types';
import { Redirect } from 'react-router-dom';

export function LoggedIn(props: PropsWithChildren<LoggedInProps>) {
  const { isLogedIn, reload } = useContext(UserContext);

  const [loaded, setLoaded] = useState(props.shouldReload ? false : true);

  useEffect(() => {
    if (props.shouldReload) {
      reload().then(() => {
        setLoaded(true);
      });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  if (loaded === false) {
    console.log('not loaded');
    // if not loaded yet, wait and show nothing
    return <></>;
  } else {
    if (isLogedIn()) {
      console.log('logged in');
      // if logged in, pass through
      return <>{props.children}</>;
    } else {
      console.log('not logged in');
      // if not logged in
      if (props.redirectUrl !== undefined) {
        // if redirectUrl is specified, do the redirect
        return <Redirect to={props.redirectUrl} />;
      } else {
        // otherwise show nothing
        return <></>;
      }
    }
  }
}
