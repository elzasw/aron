import * as React from 'react';
import ReactMarkdown from 'react-markdown/with-html';
import HelpOutline from '@material-ui/icons/HelpOutline';
import { ContextHelpProps } from './context-help-types';
import { useStyles } from './context-help-styles';
import { Tooltip } from 'components/tooltip/tooltip';

export function ContextHelp({ label, type = 'CLICKABLE' }: ContextHelpProps) {
  const { root } = useStyles();

  if (label === undefined || label === ' ') {
    return <></>;
  } else {
    const content = <ReactMarkdown allowDangerousHtml source={label ?? ''} />;

    return (
      <Tooltip title={content} placement="top-start" type={type}>
        <HelpOutline className={root} />;
      </Tooltip>
    );
  }
}
