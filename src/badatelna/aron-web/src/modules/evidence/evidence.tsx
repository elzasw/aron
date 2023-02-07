import React from 'react';
import { Route, Switch } from 'react-router-dom';
import { EvidenceDetail } from './evidence-detail';
import { EvidenceMain } from './evidence-main';
import { Props } from './types';
import { ModulePath } from '../../enums';

export function createApuDaoFileUrl(id: string, daoId?: string, fileId?: string) {
  if(!daoId){
    return `${ModulePath.APU}/${id}`
  }
  if(!fileId){
    return `${ModulePath.APU}/${id}/dao/${daoId}`
  }
  return `${ModulePath.APU}/${id}/dao/${daoId}/file/${fileId}`
}

export interface ApuPathParams {
  id: string;
  daoId?: string;
  fileId?: string;
}

export function Evidence(props: Props) {
  const { path } = props;

  return (
    <Switch>
      <Route path={[
        `${ModulePath.APU}/:id/dao/:daoId/file/:fileId`,
        `${ModulePath.APU}/:id/dao/:daoId`, 
        `${ModulePath.APU}/:id`
      ]}>
        <EvidenceDetail {...{...props, modulePath: path}}/>
      </Route>
      {[
        { path, Component: EvidenceMain, exact: true },
        { path: `${path}/:id`, Component: EvidenceDetail, exact: false, showLayoutSwitch: true },
      ].map(({ Component, showLayoutSwitch, ...route }) => (
        <Route {...{ key: route.path, ...route }}>
          <Component {...{ ...props, modulePath: path }} />
        </Route>
      ))}
    </Switch>
  );
}
