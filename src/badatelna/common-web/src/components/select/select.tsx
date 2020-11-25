import React, { ChangeEvent, useMemo } from 'react';
import { uniqBy } from 'lodash';
import MuiSelect from '@material-ui/core/Select';
import MuiMenuItem from '@material-ui/core/MenuItem';
import MuiTooltip from '@material-ui/core/Tooltip';
import InputAdornment from '@material-ui/core/InputAdornment';
import IconButton from '@material-ui/core/IconButton';
import ClearIcon from '@material-ui/icons/Clear';
import LibraryAddCheckOutlinedIcon from '@material-ui/icons/LibraryAddCheckOutlined';
import { useEventCallback } from 'utils/event-callback-hook';
import { DomainObject } from 'common/common-types';
import { SelectProps } from './select-types';
import { useStyles } from './select-styles';

export function Select<OPTION extends DomainObject>({
  disabled,
  source,
  value,
  onChange,
  showTooltip = false,
  valueIsId = false,
  multiple = false,
  clearable = true,
  selectableAll = true,
  idMapper = (option: OPTION) => option.id,
  labelMapper = (option: OPTION) => (option as any).name,
  tooltipMapper = (option: OPTION) => (option as any).tooltip,
}: SelectProps<OPTION>) {
  // fix undefined value
  value = value ?? null;

  type VALUE = OPTION | string;
  type VALUES = OPTION[] | string[];

  const valueToId = useEventCallback((value: VALUE) => {
    return valueIsId ? (value as string) : idMapper(value as OPTION);
  });

  const valuesToIds = useEventCallback((values: VALUES) => {
    return (values as Array<OPTION | string>).map((value) => valueToId(value));
  });

  // in some cases the value is not included in the options, adds it at the end
  const options: OPTION[] = useMemo(() => {
    if (value && !valueIsId) {
      if (multiple) {
        return uniqBy([...source.items, ...(value as OPTION[])], valueToId);
      } else {
        return uniqBy([...source.items, value as OPTION], valueToId);
      }
    } else {
      return source.items;
    }
  }, [value, source, multiple, valueIsId, valueToId]);

  const idToValue = useEventCallback((id: string) => {
    return valueIsId ? id : options.find((option) => idMapper(option) === id)!;
  });

  const idsToValues = useEventCallback((ids: string[]) => {
    return ids.map((id) => idToValue(id)) as VALUES;
  });

  const handleChange = useEventCallback(
    (e: ChangeEvent<{ name?: string; value: unknown }>) => {
      const ids = e.target.value;

      let values: VALUE | VALUES | null;
      if (ids === undefined || ids === '') {
        values = null;
      } else if (multiple) {
        values = idsToValues(ids as string[]);
      } else {
        values = idToValue(ids as string);
      }

      onChange(values);
    }
  );

  const handleClear = useEventCallback(() => {
    onChange(null);
  });

  const handleSelectAll = useEventCallback(() => {
    const ids = source.items.map((item) => item.id);
    const values = idsToValues(ids);
    onChange(values);
  });

  function getLocalValue(value: VALUE | VALUES | null) {
    if (value === null) {
      return multiple ? [] : '';
    }

    const optionIds = options.map((option) => idMapper(option));

    if (multiple) {
      const ids = valuesToIds(value as VALUES);
      return ids.filter((id) => optionIds.includes(id));
    } else {
      const id = valueToId(value as VALUE);
      return optionIds.includes(id) ? id : '';
    }
  }

  const { root, input, list, item, adornment } = useStyles();

  return (
    <MuiSelect
      endAdornment={
        <InputAdornment position="end" classes={{ root: adornment }}>
          {selectableAll && multiple && !disabled && (
            <IconButton size="small" onClick={handleSelectAll}>
              <LibraryAddCheckOutlinedIcon fontSize="small" />
            </IconButton>
          )}
          {clearable && !disabled && value !== null && (
            <IconButton size="small" onClick={handleClear}>
              <ClearIcon fontSize="small" />
            </IconButton>
          )}
        </InputAdornment>
      }
      className={input}
      classes={{ root }}
      disabled={disabled}
      multiple={multiple}
      value={getLocalValue(value)}
      onChange={handleChange}
      autoWidth={true}
      MenuProps={{
        classes: { list },
      }}
    >
      {options.map((option) => (
        <MuiMenuItem
          key={idMapper(option)}
          value={idMapper(option)}
          classes={{ root: item }}
        >
          {showTooltip ? (
            <MuiTooltip title={<>{tooltipMapper(option)}</>}>
              <>{labelMapper(option)}</>
            </MuiTooltip>
          ) : (
            labelMapper(option)
          )}
        </MuiMenuItem>
      ))}
    </MuiSelect>
  );
}
