import React from 'react';
import Paper, { PaperProps } from '@material-ui/core/Paper';
import Draggable from 'react-draggable';

export function DragablePaper(props: PaperProps) {
  return (
    <Draggable
      cancel={
        '[class*="MuiDialogContent-root"], [class*="MuiDialogActions-root"]'
      }
    >
      <Paper {...props} />
    </Draggable>
  );
}
