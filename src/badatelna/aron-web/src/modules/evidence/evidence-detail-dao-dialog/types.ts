import React from 'react';
import { Dao, DaoFile } from '../../../types';

export interface FileObject {
  id: string;
  tile?: DaoFile;
  published?: DaoFile;
  thumbnail?: DaoFile;
}

export interface IconProps {
  title: string;
  className?: string;
  Component: (props: any) => JSX.Element;
  onClick: (e: React.MouseEvent) => void;
  disabled?: boolean;
}

export interface ActionsRenderProps {
  fullscreen?: boolean;
}

export interface ToolbarProps {
  noView?: boolean;
  noAction?: boolean;
  previous: () => void;
  next: () => void;
  zoomIn: () => void;
  zoomOut: () => void;
  reset: () => void;
  rotateLeft: () => void;
  rotateRight: () => void;
  previousEnabled: boolean;
  nextEnabled: boolean;
  previousDisabled: boolean;
  nextDisabled: boolean;
  zoomInDisabled: boolean;
  zoomOutDisabled: boolean;
  item: Dao;
  setItem: (item: Dao | null) => void;
  open: boolean;
  setOpen: (open: boolean) => void;
  file: FileObject;
  showCloseButton?: boolean;
  customActionsLeft?: React.ReactNode;
  customActionsRight?: React.ReactNode;
  customActionsCenter?: React.ReactNode;
}

export interface DetailDaoDialogProps {
  item: Dao;
  items: Dao[];
  setItem: (item: Dao | null) => void;
  embed?: boolean;
  customActionsLeft?: (props: ActionsRenderProps) => React.ReactNode;
  customActionsRight?: (props: ActionsRenderProps) => React.ReactNode;
  customActionsCenter?: (props: ActionsRenderProps) => React.ReactNode;
  apuInfo?: {
    name?: string,
    description?: string,
  };
}
