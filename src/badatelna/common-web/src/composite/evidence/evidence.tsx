import React from 'react';
import AutoSizer from 'react-virtualized-auto-sizer';
import { DomainObject } from 'common/common-types';
import { Table } from 'composite/table/table';
import { EvidenceProps } from './evidence-types';
import { useEvidence } from './evidence-hook';
import { useStyles } from './evidence-styles';
import { EvidenceSwitcher } from './evidence-switcher';
import { useScreenMode } from './hook/screen-mode-hook';
import { Detail } from 'composite/detail/detail';
import { useEvidenceTableToolbar } from './hook/toolbar-hook';
import { EvidenceContext } from './evidence-context';

export function Evidence<OBJECT extends DomainObject>(
  props: EvidenceProps<OBJECT>
) {
  const {
    tableProps,
    detailProps,
    SwitcherComponent = EvidenceSwitcher,
  } = props;

  const {
    columns,
    FieldsComponent,
    tableSource,
    crudSource,
    tableRef,
    detailRef,
    handleActiveRowChange,
    handleDetailPersisted,
  } = useEvidence(props);

  const {
    screenMode,
    changeScreenMode,
    tableStyle,
    detailStyle,
  } = useScreenMode();

  const { BeforeToolbar } = useEvidenceTableToolbar({ tableRef });

  const classes = useStyles();

  return (
    <EvidenceContext.Provider
      value={{
        apiUrl: props.apiProps.url,
        tableSource,
        crudSource,
        tableRef,
        detailRef,
      }}
    >
      <div className={classes.evidence}>
        <AutoSizer disableWidth={true}>
          {({ height }) => (
            <div style={{ height, display: 'flex' }}>
              <div style={tableStyle}>
                <Table
                  tableId={props.identifier + '_TABLE'}
                  version={props.version}
                  ref={tableRef}
                  {...tableProps}
                  columns={columns}
                  source={tableSource}
                  height={height}
                  onActiveChange={handleActiveRowChange}
                />
              </div>
              <div style={{ ...detailStyle, height: '100%' }}>
                <Detail
                  toolbarProps={{ before: BeforeToolbar }}
                  ref={detailRef}
                  {...detailProps}
                  FieldsComponent={FieldsComponent}
                  source={crudSource}
                  onPersisted={handleDetailPersisted}
                />
              </div>
              <SwitcherComponent
                screenMode={screenMode}
                onChange={changeScreenMode}
              />
            </div>
          )}
        </AutoSizer>
      </div>
    </EvidenceContext.Provider>
  );
}
