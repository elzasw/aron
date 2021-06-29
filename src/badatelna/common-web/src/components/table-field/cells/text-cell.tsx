import React, {
  memo,
  ReactElement,
  useRef,
  useState,
  useLayoutEffect,
  RefObject,
} from 'react';
import Typography from '@material-ui/core/Typography';
import { Tooltip } from 'components/tooltip/tooltip';
import { TableFieldCellProps } from '../table-field-types';
import { useStyles } from '../table-field-styles';
import { useComponentSize } from 'utils/component-size';
import { useDebounce } from 'use-debounce/lib';

export const TextCell = memo(function TextCell<OBJECT>({
  value,
}: TableFieldCellProps<OBJECT>) {
  const classes = useStyles();

  const textRef = useRef<HTMLInputElement>(null);
  const [useTooltip, setUseTooltip] = useState(false);

  const { width } = useComponentSize(textRef);
  const [debouncedWidth] = useDebounce(width, 500);

  useLayoutEffect(() => {
    setUseTooltip(isEllipsisActive(textRef));
  }, [value, debouncedWidth]);

  const content = (
    <Typography className={classes.tableCell} ref={textRef}>
      {value}
    </Typography>
  );

  return useTooltip ? (
    <Tooltip title={value} placement="top-start" type="HOVER">
      {content}
    </Tooltip>
  ) : (
    <>{content}</>
  );
}) as <OBJECT>(p: TableFieldCellProps<OBJECT>) => ReactElement;

function isEllipsisActive(e: RefObject<HTMLInputElement>) {
  const current = e.current;
  return current !== null ? current.offsetWidth < current.scrollWidth : false;
}
