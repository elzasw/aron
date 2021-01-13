import React, { useState, useCallback } from 'react';
import { isEmpty } from 'lodash';
import CloseIcon from '@material-ui/icons/Close';
import IconButton from '@material-ui/core/IconButton';
import Radio from '@material-ui/core/Radio';
import { FormattedMessage } from 'react-intl';
import classNames from 'classnames';

import { TextField } from '@eas/common-web';
import { Checkbox } from '@eas/common-web';

import { useStyles } from './styles';
import { useLayoutStyles, useSpacingStyles } from '../../../styles';
import { FilterType, Message } from '../../../enums';
import { Select } from '../../../components';
import { FilterObject, FilterChangeCallBack } from '../types';
import { ApiFilterOperation, Filter } from '../../../types';

interface Range {
  from: string | null;
  to: string | null;
}

export interface SelectionFilterOption {
  label: string;
  value: any;
  id: string;
}

interface SelectionFilterProps {
  type: FilterType;
  field: string;
  label?: string;
  options: SelectionFilterOption[];
  displayCount?: number;
  onChange: FilterChangeCallBack;
  value: FilterObject;
  inDialog?: boolean;
  operation?: ApiFilterOperation;
  filters: Filter[];
}

export const SelectionFilter: React.FC<SelectionFilterProps> = ({
  type,
  field,
  label,
  options,
  displayCount = 5,
  onChange,
  value: filterValue = {},
  inDialog,
  operation,
  filters,
}) => {
  const classes = useStyles();
  const classesLayout = useLayoutStyles();
  const classesSpacing = useSpacingStyles();

  const updateFilterValue = useCallback(
    (newFilterValue: FilterObject) => {
      onChange(
        field,
        !isEmpty(newFilterValue) ? newFilterValue : null,
        operation,
        filters,
        type
      );
    },
    [onChange, field, operation, filters, type]
  );

  const handleClick = (isSelected: boolean, id: string, value: any) => {
    if (isSelected) {
      updateFilterValue(
        type !== FilterType.RADIO
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

  const [showRangeInput, setShowRangeInput] = useState(false);
  const [range, setRange] = useState<Range>({
    from: null,
    to: null,
  });

  const updateRange = (newRange: Range) => {
    setRange(newRange);
    const rangeInISO = {
      from: yearInISO(newRange.from),
      to: yearInISO(newRange.to),
    };
    if (newRange.from === null && newRange.to === null) {
      const newFilterValue = filterValue;
      delete newFilterValue['_RANGE'];
      updateFilterValue(newFilterValue);
    } else {
      updateFilterValue({ ...filterValue, _RANGE: rangeInISO });
    }
  };
  const yearInISO = (value: string | null) =>
    parseInt(value || '')
      ? new Date(parseInt(value || ''), 1).toISOString()
      : null;

  return (
    <div>
      <div className={classes.filterTitle}>{label}</div>
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
            {data.map((item, index) => (
              <div
                key={index}
                onClick={() =>
                  handleClick(!filterValue[item.id], item.id, item.value)
                }
                className={classes.listedItem}
              >
                {type === FilterType.RADIO ? (
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
                <FormattedMessage id={Message.SHOW_LESS} />
              ) : (
                <span>
                  <FormattedMessage id={Message.SHOW_MORE} />
                  <span>( {options.length - displayCount} )</span>
                </span>
              )}
            </div>
          ) : (
            <></>
          )}
          {type === FilterType.CHECKBOX_WITH_RANGE &&
            (showRangeInput ? (
              <div
                className={classNames(
                  classesLayout.flexAlignCenter,
                  classesSpacing.paddingTopSmall
                )}
              >
                <FormattedMessage id={Message.FROM} />
                :&nbsp;&nbsp;&nbsp;
                <TextField
                  type="number"
                  onChange={(value: string | null) =>
                    updateRange({ ...range, from: value })
                  }
                  value={range.from}
                />
                &nbsp;&nbsp;&nbsp;
                <FormattedMessage id={Message.TO} />
                :&nbsp;&nbsp;&nbsp;
                <TextField
                  type="number"
                  onChange={(value: string | null) =>
                    updateRange({ ...range, to: value })
                  }
                  value={range.to}
                />
                <IconButton
                  color="secondary"
                  size="small"
                  onClick={() => {
                    setShowRangeInput(false);
                    updateRange({ from: null, to: null });
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
                <FormattedMessage id={Message.ENTER_THE_RANGE} />
              </div>
            ))}
        </>
      )}
    </div>
  );
};
