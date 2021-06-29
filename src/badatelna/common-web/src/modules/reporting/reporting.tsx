import React, { useState } from 'react';
import { useIntl } from 'react-intl';
import AutoSizer from 'react-virtualized-auto-sizer';
import { SplitScreen } from 'components/split-screen/split-screen';
import { useScreenMode } from './hook/screen-mode-hook';
import { useStyles } from './reporting-styles';
import { ReportDefinitions } from './components/report-definitions';
import { ReportDetail } from './components/report-detail';
import { ReportDefinition } from './reporting-types';

export function reportingFactory({ exportTag }: { exportTag?: string }) {
  return function Reporting() {
    const intl = useIntl();
    const classes = useStyles();

    const { splitScreenRef } = useScreenMode();

    const [selected, setSelected] = useState<ReportDefinition>();

    return (
      <div className={classes.evidence}>
        <AutoSizer disableWidth={true}>
          {({ height }) => (
            <div style={{ height, display: 'flex' }}>
              <SplitScreen
                ref={splitScreenRef}
                rightLabel={intl.formatMessage({
                  id: 'EAS_REPORTING_SPLIT_DATA',
                  defaultMessage: 'Report',
                })}
                leftLabel={intl.formatMessage({
                  id: 'EAS_REPORTING_SPLIT_DEFINITIONS',
                  defaultMessage: 'Výběr',
                })}
              >
                <ReportDefinitions selected={selected} onSelect={setSelected} />

                <ReportDetail exportTag={exportTag} definition={selected} />
              </SplitScreen>
            </div>
          )}
        </AutoSizer>
      </div>
    );
  };
}
