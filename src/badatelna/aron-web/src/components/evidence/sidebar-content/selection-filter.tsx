import React, { useState, useCallback } from 'react';
import { isEmpty } from 'lodash';
import CloseIcon from '@material-ui/icons/Close';
import IconButton from '@material-ui/core/IconButton';
import Radio from '@material-ui/core/Radio';

import { TextField } from '@eas/common-web';
import { Checkbox } from '@eas/common-web';

import { useStyles } from './styles';
import { useLayoutStyles } from '../../../styles/layout';
import { FilterType } from '../../../enums';
import { Select } from '../../select/select';
import { FilterObject, FilterChangeCallBack } from '../types';

interface Range {
  from: string | null;
  until: string | null;
}

export interface SelectionFilterOption {
  label: string;
  value: any;
  id: string;
}

interface SelectionFilterProps {
  type: FilterType;
  name: string;
  title: string;
  options: SelectionFilterOption[];
  displayCount?: number;
  onChange: FilterChangeCallBack;
  value: FilterObject;
  inDialog?: boolean;
}

export const SelectionFilter: React.FC<SelectionFilterProps> = ({
  type,
  name,
  title,
  options,
  displayCount = 5,
  onChange,
  value: filterValue = {},
  inDialog,
}) => {
  const updateFilterValue = useCallback(
    (newFilterValue: FilterObject) => {
      onChange(name, !isEmpty(newFilterValue) ? newFilterValue : null);
    },
    [name, onChange]
  );

  const handleClick = (isSelected: boolean, id: string, value: any) => {
    if (isSelected) {
      updateFilterValue(
        type !== FilterType.RADIOBUTTON
          ? { ...filterValue, [id]: value }
          : { [id]: value }
      );
    } else {
      const newFilterValue = filterValue;
      delete newFilterValue[id];
      updateFilterValue({ ...newFilterValue });
    }
  };

  const [showAll, setShowAll] = useState(false);
  const data =
    inDialog || showAll || type === FilterType.SELECT
      ? options
      : options.slice(0, displayCount);

  const classes = useStyles();
  const classesLayout = useLayoutStyles();

  const [showRangeInput, setShowRangeInput] = useState(false);
  const [range, setRange] = useState<Range>({
    from: null,
    until: null,
  });

  const updateRange = (newRange: Range) => {
    setRange(newRange);
    if (newRange.from === null && newRange.until === null) {
      const newFilterValue = filterValue;
      delete newFilterValue['_RANGE'];
      updateFilterValue(newFilterValue);
    } else {
      updateFilterValue({ ...filterValue, _RANGE: newRange });
    }
  };

  return (
    <div>
      <div className={classes.filterTitle}>{title}</div>
      {type === FilterType.SELECT ? (
        <Select
          value={filterValue['_SELECTION']}
          options={options.map((o: SelectionFilterOption) => ({
            id: o.id,
            name: o.label,
          }))}
          onChange={(v: any) => updateFilterValue({ _SELECTION: v })}
        />
      ) : (
        <>
          <div>
            {data.map((item) => (
              <div
                key={item.id}
                onClick={() =>
                  handleClick(!filterValue[item.id], item.id, item.value)
                }
                className={classes.listedItem}
              >
                {type === FilterType.RADIOBUTTON ? (
                  <Radio
                    className={classes.radioButton}
                    size="small"
                    color="primary"
                    checked={filterValue[item.id] !== undefined}
                    onChange={() => handleClick(true, item.id, item.value)}
                  />
                ) : (
                  <Checkbox
                    value={filterValue[item.id]}
                    onChange={(isChecked: null | boolean) =>
                      handleClick(isChecked || false, item.id, item.value)
                    }
                  />
                )}
                <div>{item.label}</div>
              </div>
            ))}
          </div>
          {!inDialog && options.length > displayCount ? (
            <div
              className={classes.bottomText}
              onClick={() => setShowAll((prev) => !prev)}
            >
              {showAll ? (
                'Zobrazit méně'
              ) : (
                <span>
                  Zobrazit další (<span>{options.length - displayCount}</span>)
                </span>
              )}
            </div>
          ) : (
            <></>
          )}
          {type === FilterType.CHECKBOX_WITH_RANGE &&
            (showRangeInput ? (
              <div className={classesLayout.flex}>
                Od:{' '}
                <TextField
                  type="number"
                  onChange={(value: string | null) =>
                    updateRange({ ...range, from: value })
                  }
                  value={range.from}
                />
                Do:{' '}
                <TextField
                  type="number"
                  onChange={(value: string | null) =>
                    updateRange({ ...range, until: value })
                  }
                  value={range.until}
                />
                <IconButton
                  color="secondary"
                  size="small"
                  onClick={() => {
                    setShowRangeInput(false);
                    updateRange({ from: null, until: null });
                  }}
                >
                  <CloseIcon />
                </IconButton>
              </div>
            ) : (
              <div
                className={classes.bottomText}
                onClick={() => setShowRangeInput(true)}
              >
                Zadat rozpětí
              </div>
            ))}
        </>
      )}
    </div>
  );
};
