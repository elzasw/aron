import React, { useState } from 'react';
import classNames from 'classnames';
import { get, map, find } from 'lodash';
import Tooltip from '@material-ui/core/Tooltip';
import LinearProgress from '@material-ui/core/LinearProgress';
import CloseIcon from '@material-ui/icons/Close';
import MenuIcon from '@material-ui/icons/Menu';
import KeyboardArrowLeftIcon from '@material-ui/icons/KeyboardArrowLeft';
import KeyboardArrowRightIcon from '@material-ui/icons/KeyboardArrowRight';
import ZoomInIcon from '@material-ui/icons/ZoomIn';
import ZoomOutIcon from '@material-ui/icons/ZoomOut';
import ZoomOutMapIcon from '@material-ui/icons/ZoomOutMap';
import GetAppIcon from '@material-ui/icons/GetApp';
import InsertDriveFileIcon from '@material-ui/icons/InsertDriveFile';
import { useIntl, FormattedMessage } from 'react-intl';

import { useStyles } from './styles';
import { useLayoutStyles, useSpacingStyles } from '../../styles';
import { DetailDaoDialogProps } from './types';
import { FileViewer, FileViewerWrapper } from '../../components';
import { Dao } from '../../types';
import { Message, DaoBundleType } from '../../enums';
import { downloadFile, downloadApiFile } from '../../common-utils';

interface IconProps {
  title: string;
  className?: string;
  Component: (props: any) => JSX.Element;
  onClick: () => void;
  disabled?: boolean;
}

interface ToolbarProps {
  noView: boolean;
  previous: () => void;
  next: () => void;
  zoomIn: () => void;
  zoomOut: () => void;
  reset: () => void;
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
}

export function Icon({
  title,
  Component,
  className,
  disabled,
  onClick,
  ...props
}: IconProps) {
  const classes = useStyles();
  const spacingClasses = useSpacingStyles();

  return (
    <Tooltip {...{ key: title, title }}>
      <Component
        {...{
          ...props,
          onClick: () => !disabled && onClick(),
          className: classNames(
            classes.daoDialogIcon,
            disabled && classes.daoDialogIconDisabled,
            spacingClasses.marginHorizontalSmall,
            className
          ),
        }}
      />
    </Tooltip>
  );
}

export function Toolbar({
  noView,
  previous,
  next,
  zoomIn,
  zoomOut,
  reset,
  previousEnabled,
  nextEnabled,
  previousDisabled,
  nextDisabled,
  zoomInDisabled,
  zoomOutDisabled,
  item,
  setItem,
  open,
  setOpen,
}: ToolbarProps) {
  const classes = useStyles();
  const layoutClasses = useLayoutStyles();

  const { formatMessage } = useIntl();

  const [loading, setLoading] = useState(false);

  const published = find(
    item.files,
    ({ type }) => type === DaoBundleType.PUBLISHED
  );

  return (
    <div className={classes.daoDialogToolbar}>
      {loading ? (
        <LinearProgress />
      ) : (
        <div className={classes.loadingPlaceholder} />
      )}
      <div
        className={classNames(
          classes.daoDialogToolbarInner,
          layoutClasses.flexSpaceBetween
        )}
      >
        <Icon
          {...{
            title: formatMessage({ id: Message.CLOSE }),
            Component: CloseIcon,
            onClick: () => setItem(null),
          }}
        />
        <div className={layoutClasses.flex}>
          {[
            {
              title: Message.PREVIOUS_PAGE,
              Component: KeyboardArrowLeftIcon,
              onClick: previous,
              visible: !noView && previousEnabled,
              disabled: previousDisabled,
            },
            {
              title: Message.NEXT_PAGE,
              Component: KeyboardArrowRightIcon,
              onClick: next,
              visible: !noView && nextEnabled,
              disabled: nextDisabled,
            },
            {
              title: Message.ZOOM_IN,
              Component: ZoomInIcon,
              onClick: zoomIn,
              visible: !noView,
              disabled: zoomInDisabled,
            },
            {
              title: Message.ZOOM_OUT,
              Component: ZoomOutIcon,
              onClick: zoomOut,
              visible: !noView,
              disabled: zoomOutDisabled,
            },
            {
              title: Message.CENTER,
              Component: ZoomOutMapIcon,
              onClick: reset,
              visible: !noView,
            },
            {
              title: Message.DOWNLOAD,
              Component: GetAppIcon,
              onClick: async () => {
                setLoading(true);
                await downloadApiFile(get(published, 'file.id'), 'dao.jpg');
                setLoading(false);
              },
              visible: published,
              disabled: loading,
            },
            {
              title: Message.DOWNLOAD_ALL,
              Component: InsertDriveFileIcon,
              onClick: () => null,
            },
          ].map(
            ({ visible, title, ...icon }) =>
              visible !== false && (
                <Icon
                  {...{
                    key: title,
                    ...icon,
                    title: formatMessage({ id: title }),
                  }}
                />
              )
          )}
        </div>
        <Icon
          {...{
            title: '',
            Component: MenuIcon,
            onClick: () => setOpen(!open),
            className: classes.daoDialogMenu,
          }}
        />
        <div className={classes.daoDialogMenuPlaceholder} />
      </div>
    </div>
  );
}

const getTiles = (item: Dao) =>
  item.files.filter(({ type }) => type === DaoBundleType.TILE);

export function EvidenceDetailDaoDialog({
  item,
  items,
  setItem,
}: DetailDaoDialogProps) {
  const tiles = getTiles(item);

  const { formatMessage } = useIntl();

  const classes = useStyles();
  const layoutClasses = useLayoutStyles();
  const spacingClasses = useSpacingStyles();

  const [file, setFile] = useState(tiles[0]);
  const [open, setOpen] = useState(false);

  const { id, metadata } = file;

  return (
    <FileViewerWrapper {...{ id }}>
      {({ fileViewerProps, ...toolbarProps }) => (
        <div className={classes.daoDialog}>
          <Toolbar {...{ ...toolbarProps, item, setItem, open, setOpen }} />
          <div
            className={classNames(
              classes.daoDialogSection,
              classes.daoDialogSide,
              classes.daoDialogLeft,
              open && classes.daoDialogSideOpen
            )}
          >
            {[
              {
                label: Message.LIST_OF_DAO,
                defaultLabelId: Message.DIGITIZER,
                name: 'name',
                items,
                onClick: (item: Dao) => {
                  setItem(item);
                  setFile(getTiles(item)[0]);
                },
                active: item,
              },
              {
                label: Message.FILES_IN_DAO,
                defaultLabelId: Message.FILE,
                name: 'file.name',
                items: tiles,
                onClick: setFile,
                active: file,
              },
            ].map(
              ({ label, defaultLabelId, name, items, onClick, active }, i) => (
                <div
                  key={`${label}-${i}`}
                  className={classes.daoDialogSectionPart}
                >
                  <div
                    className={classNames(
                      classes.daoDialogSectionPartLabel,
                      layoutClasses.flexAlignCenter
                    )}
                  >
                    {formatMessage({ id: label })}
                  </div>
                  <div className={classes.daoDialogSectionPartContent}>
                    {map(items, (item: any, i) => {
                      const label = get(item, name);
                      const isActive = active && active.id === item.id;
                      return (
                        <div
                          {...{
                            key: `${label}-${i}`,
                            className: classNames(
                              isActive && classes.daoDialogSectionPartActive
                            ),
                          }}
                          onClick={() => !isActive && onClick(item)}
                        >
                          {label || <FormattedMessage id={defaultLabelId} />}
                        </div>
                      );
                    })}
                  </div>
                </div>
              )
            )}
          </div>
          <div
            className={classNames(
              classes.daoDialogSection,
              classes.daoDialogCenter,
              !open && classes.daoDialogCenterOpen
            )}
          >
            <FileViewer {...fileViewerProps} />
          </div>
          <div
            className={classNames(
              classes.daoDialogSection,
              classes.daoDialogSide,
              classes.daoDialogRight,
              open && classes.daoDialogSideOpen
            )}
          >
            <div
              className={classNames(
                classes.daoDialogMetadata,
                spacingClasses.paddingSmall
              )}
            >
              <div className={layoutClasses.flex}>
                <div className={classes.bold}>Metadata</div>
                <Tooltip
                  {...{
                    title: formatMessage({ id: Message.DOWNLOAD_METADATA }),
                  }}
                >
                  <GetAppIcon
                    className={classNames(
                      classes.icon,
                      spacingClasses.marginLeft
                    )}
                    onClick={() =>
                      downloadFile(
                        `/digitalObjectFile/${id}/metadata/csv`,
                        `${id}_metadata.csv`
                      )
                    }
                  />
                </Tooltip>
              </div>
              {metadata.map(({ value, type: { name } }, i) => (
                <div
                  {...{ key: `${value}-${i}`, className: layoutClasses.flex }}
                >
                  <div
                    className={classNames(
                      classes.bold,
                      classes.daoDialogMetadataLabel
                    )}
                  >
                    {name}:
                  </div>
                  <div>{value}</div>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}
    </FileViewerWrapper>
  );
}
