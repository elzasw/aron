import React, { useState, useCallback } from 'react';
import { find } from 'lodash';
import Radio from '@material-ui/core/Radio';
import { FormattedMessage } from 'react-intl';
import classNames from 'classnames';

import { Checkbox } from '@eas/common-web';

import { Tooltip, TextWithCount } from '../../../components';
import { Message, FacetType } from '../../../enums';
import { useStyles } from './styles';
import { SelectionFilterProps } from '.';

export const SelectionFilter: React.FC<SelectionFilterProps> = ({
  type,
  source,
  label,
  tooltip,
  description,
  inDialog,
  options,
  displayedItems,
  maxDisplayedItems,
  onChange,
  value: filterValue,
}) => {
  const classes = useStyles();

  const updateFilterValue = useCallback(
    (value: string[]) => {
      onChange({ source, value });
    },
    [onChange, source]
  );

  const onClick = (value: string) => {
    updateFilterValue(
      type === FacetType.ENUM_SINGLE
        ? [value]
        : find(filterValue, (f) => f === value)
        ? filterValue.filter((f) => f !== value)
        : [...filterValue, value]
    );
  };

  const [showAll, setShowAll] = useState(false);

  const allOptions = maxDisplayedItems
    ? options.slice(0, maxDisplayedItems)
    : options;

  const displayCount = displayedItems || 0;

  const visibleOptions =
    inDialog || showAll || !displayCount
      ? allOptions
      : allOptions.slice(0, displayCount);

  return (
    <div>
      <div className={classes.filterTitle}>
        <Tooltip title={tooltip}>
          <div>{label}</div>
        </Tooltip>
      </div>
      {inDialog && description ? (
        <div className={classes.filterDescription}>{description}</div>
      ) : (
        <></>
      )}
      <div>
        {visibleOptions.map(({ value, label, tooltip }, index) => {
          const isChecked = !!find(filterValue, (f) => f === value);

          const handleClick = () => onClick(value);

          return (
            <div
              key={index}
              onClick={handleClick}
              className={classNames(
                classes.listedItem,
                classes.listedItemCheckbox
              )}
            >
              {type === FacetType.ENUM_SINGLE ? (
                <Radio
                  className={classes.radioButton}
                  size="small"
                  color="primary"
                  checked={isChecked}
                  onChange={handleClick}
                />
              ) : (
                <Checkbox value={isChecked} onChange={handleClick} />
              )}
              <Tooltip title={tooltip}>
                <div>{label}</div>
              </Tooltip>
            </div>
          );
        })}
      </div>
      {!inDialog && displayedItems && options.length > displayCount ? (
        <div
          className={classes.bottomText}
          onClick={() => setShowAll((prev) => !prev)}
        >
          {showAll ? (
            <FormattedMessage id={Message.SHOW_LESS} />
          ) : (
            <TextWithCount
              text={<FormattedMessage id={Message.SHOW_MORE} />}
              count={options.length - displayCount}
            />
          )}
        </div>
      ) : (
        <></>
      )}
    </div>
  );
};
