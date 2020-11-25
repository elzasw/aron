import React, { PropsWithChildren, useState, ChangeEvent } from 'react';
import clsx from 'clsx';
import ExpansionPanel from '@material-ui/core/ExpansionPanel';
import Typography from '@material-ui/core/Typography/Typography';
import ExpansionPanelSummary from '@material-ui/core/ExpansionPanelSummary';
import ExpansionPanelDetails from '@material-ui/core/ExpansionPanelDetails';
import ExpandMoreIcon from '@material-ui/icons/ExpandMore';

//import ExpandMoreIcon from '@material-ui/icons/ExpandMore';
import { PanelProps } from './panel-types';
import { useStyles } from './panel-styles';
import { useEventCallback } from 'utils/event-callback-hook';

export function Panel({
  children,
  label,
  expandable = false,
  defaultExpanded = true,
  sideBorder = false,
}: PropsWithChildren<PanelProps>) {
  const {
    root,
    summaryRoot,
    summaryFixed,
    detailsRoot,
    detailRootWithSideBorder,
    labelRoot,
    summaryRootWithBorder,
  } = useStyles();

  const [expanded, setExpanded] = useState(defaultExpanded);

  const handleChange = useEventCallback((event: ChangeEvent<any>, expanded) => {
    if (expandable) {
      setExpanded(expanded);
    }
  });

  return (
    <ExpansionPanel
      classes={{ root }}
      square={true}
      expanded={expanded}
      onChange={handleChange}
    >
      <ExpansionPanelSummary
        classes={{
          root: clsx(summaryRoot, {
            [summaryFixed]: !expandable,
            [summaryRootWithBorder]: sideBorder,
          }),
        }}
        expandIcon={expandable ? <ExpandMoreIcon /> : undefined}
      >
        <Typography classes={{ root: labelRoot }} variant="h6">
          {label}
        </Typography>
      </ExpansionPanelSummary>
      <ExpansionPanelDetails
        classes={{
          root: clsx(detailsRoot, {
            [detailRootWithSideBorder]: sideBorder,
          }),
        }}
      >
        {children}
      </ExpansionPanelDetails>
    </ExpansionPanel>
  );
}
