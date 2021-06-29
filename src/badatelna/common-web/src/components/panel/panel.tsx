import React, {
  PropsWithChildren,
  useState,
  ChangeEvent,
  forwardRef,
  useImperativeHandle,
  useRef,
} from 'react';
import clsx from 'clsx';
import ExpansionPanel from '@material-ui/core/ExpansionPanel';
import Typography from '@material-ui/core/Typography/Typography';
import ExpansionPanelSummary from '@material-ui/core/ExpansionPanelSummary';
import ExpansionPanelDetails from '@material-ui/core/ExpansionPanelDetails';
import ExpandMoreIcon from '@material-ui/icons/ExpandMore';

//import ExpandMoreIcon from '@material-ui/icons/ExpandMore';
import { PanelProps, PanelHandle } from './panel-types';
import { useStyles } from './panel-styles';
import { useEventCallback } from 'utils/event-callback-hook';
import { composeRefs } from 'utils/compose-refs';

export const Panel = forwardRef<PanelHandle, PropsWithChildren<PanelProps>>(
  function Panel(
    {
      children,
      label,
      summary,
      expandable = false,
      defaultExpanded = true,
      sideBorder = false,
      className,
      fitHeight,
    },
    ref
  ) {
    const {
      root,
      fullHeight,
      summaryRoot,
      summaryFixed,
      detailsRoot,
      detailRootWithSideBorder,
      labelRoot,
      summaryContent,
      summaryRootWithBorder,
      sumaryFullHeight,
      formPanelSummary,
    } = useStyles();

    const anchorRef = useRef<PanelHandle>(null);
    const composedRef = composeRefs(ref, anchorRef);

    const [expanded, setExpanded] = useState(defaultExpanded);

    useImperativeHandle(ref, () => ({
      setExpanded,
      scrollIntoView: () => {
        anchorRef.current?.scrollIntoView();
      },
    }));

    const handleChange = useEventCallback(
      (event: ChangeEvent<any>, expanded) => {
        if (expandable) {
          setExpanded(expanded);
        }
      }
    );

    return (
      <ExpansionPanel
        className={className}
        classes={{ root: clsx(root, { [fullHeight]: fitHeight }) }}
        square={true}
        expanded={expanded}
        onChange={handleChange}
        innerRef={composedRef}
        TransitionProps={{
          style: fitHeight
            ? {
                flex: '1 1 auto',
                overflowY: 'auto',
              }
            : undefined,
        }}
      >
        <ExpansionPanelSummary
          classes={{
            root: clsx(summaryRoot, {
              [summaryFixed]: !expandable,
              [summaryRootWithBorder]: sideBorder,
              [sumaryFullHeight]: fitHeight,
            }),
            content: summaryContent,
          }}
          expandIcon={expandable ? <ExpandMoreIcon /> : undefined}
        >
          <Typography classes={{ root: labelRoot }} variant="h6">
            {label}
          </Typography>
          {summary && (
            <Typography component="div" className={formPanelSummary}>
              {summary}
            </Typography>
          )}
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
);
