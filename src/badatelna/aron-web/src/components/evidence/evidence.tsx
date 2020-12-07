import React from 'react';
import { Switch, Route } from 'react-router-dom';

import { EvidenceDetail } from './evidence-detail';
import { EvidenceMain } from './evidence-main';
import { Props } from './types';

export function Evidence(props: Props) {
  const { path } = props;

  return (
    <Switch>
      {[
        { path, Component: EvidenceMain, exact: true },
        { path: `${path}/:id`, Component: EvidenceDetail, exact: false },
      ].map(({ Component, ...route }) => (
        <Route {...{ key: route.path, ...route }}>
          <Component {...{ ...props, modulePath: path }} />
        </Route>
      ))}
    </Switch>
  );
}