import React, {useEffect, useState} from 'react';
import { Switch, Route } from 'react-router-dom';
import { useTheme } from '@material-ui/core/styles';

import { EvidenceDetail } from './evidence-detail';
import { EvidenceDetail2 } from './evidence-detail-2';
import { EvidenceDetail3 } from './evidence-detail-3';
import { EvidenceMain } from './evidence-main';
import { Props } from './types';
// import { useConfiguration } from '../../components';

export function Evidence(props: Props) {
  const [layout, setLayout] = useState<"2_COLUMN" | "3_COLUMN" | undefined>();
  const theme = useTheme() 
  const { path } = props;
  // const {evidenceLayout} = useConfiguration();

  useEffect(() => {
    const mdQuery = window.matchMedia(theme.breakpoints.up('md').replace('@media', ''));
    const lgQuery = window.matchMedia(theme.breakpoints.up('xl').replace('@media', ''));
    setLayout(mdQuery.matches ? lgQuery.matches ? "3_COLUMN" : "2_COLUMN" : undefined);

    function handleQueryChange() {
      if(mdQuery.matches && lgQuery.matches){
        setLayout("3_COLUMN");
      }
      else if(mdQuery.matches && !lgQuery.matches){
        setLayout("2_COLUMN");
      } else {
        setLayout(undefined);
      }
    }

    lgQuery.addListener(handleQueryChange);
    mdQuery.addListener(handleQueryChange);

    return function cleanup() {
      lgQuery.removeListener(handleQueryChange);
      mdQuery.removeListener(handleQueryChange);
    }
  }, [])

  const getComponent = (_layout: typeof layout) => {
    if(_layout === "2_COLUMN") { return EvidenceDetail2 }
    if(_layout === "3_COLUMN") { return EvidenceDetail3 }
    return EvidenceDetail;
  }

  return (
    <Switch>
      {[
        { path, Component: EvidenceMain, exact: true },
        { path: `${path}/:id`, Component: getComponent(layout), exact: false, showLayoutSwitch: true },
      ].map(({ Component, showLayoutSwitch, ...route }) => (
        <Route {...{ key: route.path, ...route }}>
          <Component {...{ ...props, modulePath: path }} />
        </Route>
      ))}
    </Switch>
  );
}
