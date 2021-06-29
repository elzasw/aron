import React, { ReactElement, useState, useCallback, useEffect } from 'react';
import classNames from 'classnames';
import { debounce, isEqual, isArray, compact, isEmpty } from 'lodash';
import { FormattedMessage } from 'react-intl';
import IconButton from '@material-ui/core/IconButton';
import RefreshIcon from '@material-ui/icons/Refresh';

import { Message, DEFAULT_RANGE } from '../../../enums';
import { Slider } from '../../../components/slider';
import { useStyles as useEvidenceStyles } from '../styles';
import { useLayoutStyles, useSpacingStyles } from '../../../styles';
import { useStyles } from './styles';
import { TextField, Tooltip } from '../../../components';
import {
  toStrRange,
  isOutsideRange,
  RangeFilterProps,
  useGetCountRange,
} from '.';
import { useGetRangeFilterInterval, usePrevious } from '../../../common-utils';

export function RangeFilter({
  onChange,
  source,
  value,
  label,
  tooltip,
  description,
  inDialog,
  apiFilters,
}: RangeFilterProps): ReactElement {
  const classes = useStyles();
  const classesEvidence = useEvidenceStyles();
  const classesLayout = useLayoutStyles();
  const classesSpacing = useSpacingStyles();

  const filterActive = isArray(value) && value.length;

  const prevValue = usePrevious(value);

  const [rangeInterval, loadingInterval] = useGetRangeFilterInterval(
    apiFilters,
    source
  );

  const interval: [number, number] =
    compact(rangeInterval).length === 2
      ? (rangeInterval as [number, number])
      : DEFAULT_RANGE;

  const prevInterval = usePrevious(interval);

  const [range, setRange] = useState<[number, number]>(interval);

  const [textFieldRange, setTextFieldRange] = useState<[string, string]>(
    toStrRange(interval)
  );

  const updateFilterValue = useCallback(
    (newRange: [number, number]) => {
      if (
        (newRange[0] === interval[0] && newRange[1] === interval[1]) ||
        (newRange[0] === DEFAULT_RANGE[0] && newRange[1] === DEFAULT_RANGE[1])
      ) {
        onChange({
          source,
          value: [],
        });
      } else if (newRange[0] && newRange[1]) {
        onChange({
          source,
          value: [`${newRange[0]}`, `${newRange[1]}`],
        });
      }
    },
    [onChange, source, interval]
  );

  useEffect(() => {
    const [intervalFrom, intervalTo] = interval;

    if (
      value &&
      value.length > 1 &&
      value[0] &&
      value[1] &&
      value[0].length >= 3 &&
      value[1].length >= 3 &&
      !isEqual(prevValue, value)
    ) {
      const from = Number(value[0]);
      const to = Number(value[1]);

      if (from && to && !isNaN(from) && !isNaN(to)) {
        const newRange: [number, number] = [
          intervalFrom && from < intervalFrom ? intervalFrom : from,
          intervalTo && to > intervalTo ? intervalTo : to,
        ];

        if (from !== range[0] || to !== range[1]) {
          setRange(newRange);
        }

        const textFrom = Number(textFieldRange[0]);
        const textTo = Number(textFieldRange[1]);

        if (
          isNaN(textFrom) ||
          isNaN(textTo) ||
          from !== textFrom ||
          to !== textTo
        ) {
          setTextFieldRange(toStrRange(newRange));
        }
      }
    } else {
      if (isEmpty(value)) {
        setRange(interval);
        setTextFieldRange(toStrRange(interval));
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [prevValue, value, interval]);

  const [result, loading] = useGetCountRange(source, range, apiFilters);

  const updateRange = (newRange: [number, number]) => {
    setTextFieldRange(toStrRange(newRange));
    setRange(newRange);

    updateFilterValue(newRange);
  };

  // TODO: change year in url if out of interval
  useEffect(() => {
    if (
      interval &&
      interval[0] &&
      interval[1] &&
      !isEqual(prevInterval, interval)
    ) {
      if (value && value.length > 1 && value[0] && value[1]) {
        const [intervalFrom, intervalTo] = interval;
        const [from, to] = range;

        const changeFrom = intervalFrom > from;
        const changeTo = intervalTo < to;

        if (changeFrom || changeTo) {
          const newRange: [number, number] = [
            changeFrom ? intervalFrom : from,
            changeTo ? intervalTo : to,
          ];

          setRange(newRange);
          setTextFieldRange(toStrRange(newRange));
        } else {
          const fromNumber = Number(textFieldRange[0]);
          const toNumber = Number(textFieldRange[1]);

          const changeFrom = isNaN(fromNumber) || intervalFrom > fromNumber;
          const changeTo = isNaN(toNumber) || intervalTo < toNumber;

          if (changeFrom || changeTo) {
            setTextFieldRange(
              toStrRange([
                changeFrom ? from : fromNumber,
                changeTo ? to : toNumber,
              ])
            );
          }
        }
      } else {
        setRange(interval);
        setTextFieldRange(toStrRange(interval));
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [prevInterval, interval, value]);

  // eslint-disable-next-line react-hooks/exhaustive-deps
  const updateRangeDebounced = useCallback(
    debounce((from: number, to: number) => {
      const realFrom =
        from < interval[0]
          ? interval[0]
          : from > interval[1]
          ? interval[1]
          : from;

      const newRange: [number, number] = [
        realFrom,
        to < realFrom
          ? realFrom
          : to > interval[1]
          ? interval[1]
          : to < interval[0]
          ? interval[0]
          : to,
      ];

      setRange(newRange);
      setTextFieldRange(toStrRange(newRange));

      if (!isEqual(range, newRange)) {
        updateFilterValue(newRange);
      }
    }, 800),
    [range, interval]
  );

  const updateTextFieldRange = (newTextFieldRange: [string, string]) => {
    const fromString = newTextFieldRange[0];
    const toString = newTextFieldRange[1];

    const from = Number(fromString);
    const to = Number(toString);

    setTextFieldRange(newTextFieldRange);

    if (
      !isNaN(from) &&
      !isNaN(to) &&
      fromString !== '' &&
      toString !== '' &&
      fromString.length >= 3 &&
      toString.length >= 3
    ) {
      updateRangeDebounced(from, to);
    }
  };

  const safeInterval =
    interval && interval[0] && interval[1] ? interval : DEFAULT_RANGE;

  return (
    <div>
      <div className={classes.filterTitle}>
        <div
          className={
            filterActive && !loading ? classes.rangeFilterTitle : undefined
          }
        >
          <Tooltip title={tooltip}>
            <span>
              <span className={classesSpacing.marginRightSmall}>{label}</span>
              {filterActive && !loading ? (
                <span className={classesEvidence.itemsCount}>
                  <span>(</span>
                  {result?.count || 0}
                  <span>)</span>
                </span>
              ) : (
                <></>
              )}
            </span>
          </Tooltip>
        </div>
        {filterActive && !loading ? (
          <IconButton
            onClick={() => updateRange(safeInterval)}
            className={classes.rangeFilterRefreshButton}
            size="small"
          >
            <RefreshIcon />
          </IconButton>
        ) : (
          <></>
        )}
      </div>
      {inDialog && description ? (
        <div className={classes.filterDescription}>{description}</div>
      ) : (
        <></>
      )}
      <div className={classes.rangeFilterSliderWrapper}>
        <Slider
          disabled={loadingInterval}
          className={classes.rangeFilterSlider}
          onChange={(range: [number, number]) => updateRange(range)}
          interval={safeInterval}
          controlledValue={range}
        />
      </div>
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
          size="small"
          error={isOutsideRange(textFieldRange[0])}
          onChange={(value: string) =>
            updateTextFieldRange([value, textFieldRange[1]])
          }
          value={textFieldRange[0]}
          onKeyDown={(e) => {
            const { key } = e;
            if (key === 'Backspace' || key === 'Delete') {
              updateTextFieldRange(['', textFieldRange[1]]);
            }
          }}
        />
        &nbsp;&nbsp;&nbsp;
        <FormattedMessage id={Message.TO} />
        :&nbsp;&nbsp;&nbsp;
        <TextField
          type="number"
          size="small"
          variant="outlined"
          error={isOutsideRange(textFieldRange[1])}
          onChange={(value: string) =>
            updateTextFieldRange([textFieldRange[0], value])
          }
          value={textFieldRange[1]}
          onKeyDown={(e) => {
            const { key } = e;
            if (key === 'Backspace' || key === 'Delete') {
              updateTextFieldRange([textFieldRange[0], '']);
            }
          }}
        />
      </div>
    </div>
  );
}
