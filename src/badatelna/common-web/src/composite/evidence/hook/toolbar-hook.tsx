import React, { useMemo, RefObject } from 'react';
import ButtonGroup from '@material-ui/core/ButtonGroup/ButtonGroup';
import ChevronLeftIcon from '@material-ui/icons/ChevronLeft';
import ChevronRightIcon from '@material-ui/icons/ChevronRight';
import { DetailToolbarButton } from 'composite/detail/detail-toolbar-button';
import { FormattedMessage } from 'react-intl';
import { TableHandle } from 'composite/table/table-types';

export function useEvidenceTableToolbar<OBJECT>({
  tableRef,
}: {
  tableRef: RefObject<TableHandle<OBJECT>>;
}) {
  const BeforeToolbar = useMemo(
    () => (
      <ButtonGroup
        size="small"
        variant="outlined"
        aria-label="Outlined button group"
      >
        <DetailToolbarButton
          label={<ChevronLeftIcon />}
          tooltip={
            <FormattedMessage
              id="EAS_EVIDENCE_TOOLBAR_TOOLTIP_PREVIOUS"
              defaultMessage="Zobrazit předchozí záznam"
            />
          }
          onClick={() => {
            tableRef.current?.setPrevRowActive();
          }}
        />
        <DetailToolbarButton
          label={<ChevronRightIcon />}
          tooltip={
            <FormattedMessage
              id="EAS_EVIDENCE_TOOLBAR_TOOLTIP_NEXT"
              defaultMessage="Zobrazit nadchazející záznam"
            />
          }
          onClick={() => {
            tableRef.current?.setNextRowActive();
          }}
        />
      </ButtonGroup>
    ),
    [tableRef]
  );

  return { BeforeToolbar };
}
