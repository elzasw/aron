import React, { useContext } from 'react';
import { useEventCallback } from 'utils/event-callback-hook';
import { DomainObject } from 'common/common-types';
import { Checkbox } from 'components/checkbox/checkbox';
import { TableContext, TableSelectedContext } from './table-context';

export interface TableRowSelectionProps<OBJECT extends DomainObject> {
  value: OBJECT;
}

export function TableRowSelection<OBJECT extends DomainObject>({
  value,
}: TableRowSelectionProps<OBJECT>) {
  const { toggleRowSelection } = useContext<TableContext<OBJECT>>(TableContext);
  const { selected } = useContext(TableSelectedContext);

  const handleSelectClick = useEventCallback(() => {
    toggleRowSelection(value.id);
  });

  return (
    <Checkbox
      value={selected.includes(value.id)}
      onChange={handleSelectClick}
      highlighted={false}
    />
  );
}
