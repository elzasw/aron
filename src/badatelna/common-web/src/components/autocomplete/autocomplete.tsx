import React, {
  useState,
  useRef,
  KeyboardEvent,
  useEffect,
  MouseEvent,
} from 'react';
import clsx from 'clsx';
import { useDebouncedCallback } from 'use-debounce';
import ArrowDropDownIcon from '@material-ui/icons/ArrowDropDown';
import IconButton from '@material-ui/core/IconButton';
import ClearIcon from '@material-ui/icons/Clear';
import Popover from '@material-ui/core/Popper';
import Paper from '@material-ui/core/Paper';
import Chip from '@material-ui/core/Chip';
import ClickAwayListener from '@material-ui/core/ClickAwayListener';
import { useEventCallback } from 'utils/event-callback-hook';
import { DomainObject } from 'common/common-types';
import { TextField } from 'components/text-field/text-field';
import { InfiniteList } from 'components/infinite-list/infinite-list';
import { AutocompleteProps } from './autocomplete-types';
import { useStyles } from './autocomplate-styles';
import { InfiniteListHandle } from 'components/infinite-list/infinite-list-types';

export function Autocomplete<OPTION extends DomainObject>({
  disabled,
  value,
  onChange,
  source,
  clearable = true,
  multiple = false,
  labelMapper = (option: OPTION) => (option as any).name, // fixme: memorize
  tooltipMapper,
  showTooltip,
}: AutocompleteProps<OPTION>) {
  // fix undefined value
  value = value ?? null;
  const singleValue = value as OPTION;
  const multipleValue = (value as OPTION[]) ?? [];
  const multipleIds = multiple ? multipleValue.map((item) => item.id) : [];
  const singleLabel = singleValue != null ? labelMapper(singleValue) : '';
  const multiLabel = multiple ? multipleValue.map(labelMapper).join(', ') : '';

  const {
    paper,
    popper,
    icon,
    iconOpened,
    wrapper,
    wrapperDisabled,
    chips,
    chip,
    input,
    clearButton,
  } = useStyles();

  const [popupOpen, setPopupOpen] = useState(false);
  const anchorEl = useRef<HTMLDivElement>(null);
  const [textValue, setTextValue] = useState<string | null>(null);
  const infiniteList = useRef<InfiniteListHandle<OPTION>>(null);

  const handleItemClick = useEventCallback(async (option: OPTION) => {
    if (multiple) {
      // toggle item
      if (multipleIds.includes(option.id)) {
        onChange(multipleValue.filter((v) => v.id !== option.id));
      } else {
        option = await source.loadDetail(option);
        onChange([...multipleValue, option]);
      }
    } else {
      // select item and close popup
      option = await source.loadDetail(option);
      onChange(option);
      setPopupOpen(false);

      const value = labelMapper(option);
      setTextValue(value);
      setSearchQuery(value);
    }
  });

  const handleClickAway = useEventCallback(() => {
    setPopupOpen(false);
  });

  const [setSearchQuery] = useDebouncedCallback((value: string | null) => {
    source.setSearchQuery(value ?? '');
    source.reset();

    infiniteList.current?.reset();
  }, 500);

  const handleChange = useEventCallback((value: string | null) => {
    setTextValue(value);
    setSearchQuery(value);
  });

  const handleClear = useEventCallback((e: MouseEvent) => {
    e.stopPropagation();
    onChange(null);
    setTextValue(null);
    setSearchQuery(null);
  });

  const handleDelete = useEventCallback((value: OPTION) => {
    onChange(multipleValue.filter((v) => v !== value));
  });

  const handleClick = useEventCallback(() => {
    if (!disabled) {
      setSearchQuery(textValue);
      setPopupOpen(true);
    }
  });

  const handleKeyDown = useEventCallback((e: KeyboardEvent) => {
    if (e.key !== 'Tab' && !popupOpen) {
      setPopupOpen(true);
    }
    if (e.key === 'ArrowUp') {
      e.preventDefault();
      infiniteList.current?.focusPrevious();
    } else if (e.key === 'ArrowDown') {
      e.preventDefault();
      infiniteList.current?.focusNext();
    } else if (e.key === 'Enter') {
      e.preventDefault();
      const item = infiniteList.current?.getFocusedItem();
      if (item !== undefined) {
        handleItemClick(item);
      }
    } else if (e.key === 'Escape') {
      setPopupOpen(false);
      setTextValue('');
      setSearchQuery('');
    }
  });

  useEffect(() => {
    if (!multiple) {
      setTextValue(singleValue !== null ? labelMapper(singleValue) : null);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [singleValue, multiple]);

  const error =
    !multiple &&
    textValue !== (value !== null ? labelMapper(singleValue) : null);

  return (
    <>
      <div
        className={clsx('EasInput-root', wrapper, {
          [wrapperDisabled]: disabled,
        })}
      >
        {disabled ? (
          <TextField
            disabled={true}
            value={multiple ? multiLabel : singleLabel}
          />
        ) : (
          <>
            <div className={input} onClick={handleClick}>
              <TextField
                ref={anchorEl}
                error={error}
                endAdornment={
                  <>
                    {clearable && value !== null && (
                      <IconButton
                        className={clearButton}
                        size="small"
                        onClick={handleClear}
                      >
                        <ClearIcon fontSize="small" />
                      </IconButton>
                    )}
                    <ArrowDropDownIcon
                      classes={{
                        root: clsx(icon, { [iconOpened]: popupOpen }),
                      }}
                    />
                  </>
                }
                value={textValue}
                onChange={handleChange}
                onKeyDown={handleKeyDown}
              />
            </div>

            {multiple && multipleValue.length > 0 && (
              <div className={chips}>
                {multipleValue.map((value) => (
                  <Chip
                    key={value.id}
                    label={labelMapper(value)}
                    className={chip}
                    variant="outlined"
                    size="small"
                    onDelete={!disabled ? () => handleDelete(value) : undefined}
                  />
                ))}
              </div>
            )}
          </>
        )}
      </div>
      {popupOpen && anchorEl && (
        <Popover
          className={popper}
          role="presentation"
          anchorEl={anchorEl.current}
          placement="bottom-start"
          open
        >
          <ClickAwayListener onClickAway={handleClickAway}>
            <Paper className={paper} square={true} elevation={8}>
              <InfiniteList
                ref={infiniteList}
                source={source}
                onItemClick={handleItemClick}
                showTooltip={showTooltip}
                tooltipMapper={tooltipMapper}
                selectedIds={multipleIds}
              />
            </Paper>
          </ClickAwayListener>
        </Popover>
      )}
    </>
  );
}
