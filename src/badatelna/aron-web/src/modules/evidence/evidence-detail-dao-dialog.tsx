import React, { useState, useContext } from 'react';
import classNames from 'classnames';
import { get, map, find, sortBy, findIndex } from 'lodash';
import Tooltip from '@material-ui/core/Tooltip';
import KeyboardArrowLeftIcon from '@material-ui/icons/KeyboardArrowLeft';
import KeyboardArrowRightIcon from '@material-ui/icons/KeyboardArrowRight';
import { 
    Close as CloseIcon,
    Menu as MenuIcon,
    RotateLeft,
    RotateRight,
    ZoomIn as ZoomInIcon, 
    ZoomOut as ZoomOutIcon, 
    ZoomOutMap as ZoomOutMapIcon,
    GetApp as GetAppIcon,
} from "@material-ui/icons";
import { useIntl, FormattedMessage } from 'react-intl';

import { SnackbarContext, SnackbarVariant } from '@eas/common-web';

import { useStyles } from './styles';
import { useLayoutStyles, useSpacingStyles } from '../../styles';
import { DetailDaoDialogProps } from './types';
import { FileViewer, FileViewerWrapper, Loading, useConfiguration } from '../../components';
import { Dao, DaoFile } from '../../types';
import { Message, DaoBundleType } from '../../enums';
import {
  downloadFile,
  downloadFileByUrl,
  useWindowSize,
  isUUID,
} from '../../common-utils';

interface FileObject {
  id: string;
  tile?: DaoFile;
  published?: DaoFile;
  thumbnail?: DaoFile;
}

interface IconProps {
  title: string;
  className?: string;
  Component: (props: any) => JSX.Element;
  onClick: (e: React.MouseEvent) => void;
  disabled?: boolean;
}

interface ToolbarProps {
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
}

const getExistingFile = (fileObject: FileObject, publishedFirst = false) => {
  if (fileObject) {
    const { tile, published, thumbnail } = fileObject;

    return (
      (publishedFirst ? published : tile) ||
      (publishedFirst ? tile : published) ||
      thumbnail ||
      null
    );
  }

  return null;
};

const getFilesByType = (item: Dao, bundleType: DaoBundleType) =>
  sortBy(
    item.files.filter(({ type }) => type === bundleType),
    'order'
  );

const getFileByOrder = (files: DaoFile[], order: number) =>
  find(files, (file) => file.order === order);

const getFiles = (item: Dao) => {
  if (!item.files || !item.files.length) {
    return [];
  }

  const publishedItems = getFilesByType(item, DaoBundleType.PUBLISHED);
  const thumbnails = getFilesByType(item, DaoBundleType.THUMBNAIL);
  const tiles = getFilesByType(item, DaoBundleType.TILE);

  const files: FileObject[] = [];

  let i = 1;
  let flag = true;

  while (flag) {
    const published = getFileByOrder(publishedItems, i);
    const thumbnail = getFileByOrder(thumbnails, i);
    const tile = getFileByOrder(tiles, i);

    if (published || thumbnail || tile) {
      files.push({
        id: tile?.id || published?.id || thumbnail?.id || '',
        tile,
        published,
        thumbnail,
      });

      i++;
    } else {
      flag = false;
    }
  }

  return files;
};

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
          onClick: (e: React.MouseEvent) => !disabled && onClick(e),
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
  noAction,
  previous,
  next,
  zoomIn,
  zoomOut,
  reset,
  rotateLeft,
  rotateRight,
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
  file,
}: ToolbarProps) {
  const classes = useStyles();
  const layoutClasses = useLayoutStyles();

  const { showSnackbar } = useContext(SnackbarContext);

  const { formatMessage } = useIntl();

  const [loading, setLoading] = useState(false);

  const fileDownload = get(getExistingFile(file, true), 'file.id');

  return (
    <div className={classes.daoDialogToolbar}>
      <Loading loading={loading} />
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
              title: "rotate left",
              Component: RotateLeft,
              onClick: rotateLeft,
              visible: !noView && !noAction,
              disabled: zoomInDisabled,
            },
            {
              title: "rotate right",
              Component: RotateRight,
              onClick: rotateRight,
              visible: !noView && !noAction,
              disabled: zoomInDisabled,
            },
            {
              title: Message.ZOOM_IN,
              Component: ZoomInIcon,
              onClick: zoomIn,
              visible: !noView && !noAction,
              disabled: zoomInDisabled,
            },
            {
              title: Message.ZOOM_OUT,
              Component: ZoomOutIcon,
              onClick: zoomOut,
              visible: !noView && !noAction,
              disabled: zoomOutDisabled,
            },
            {
              title: Message.CENTER,
              Component: ZoomOutMapIcon,
              onClick: reset,
              visible: !noView && !noAction,
            },
            {
              title: Message.DOWNLOAD,
              Component: GetAppIcon,
              onClick: async () => {
                setLoading(true);
                const ok = await downloadFile(
                  fileDownload,
                  `dao_${item.id}.jpg`
                );
                if (!ok) {
                  showSnackbar(
                    formatMessage({ id: Message.ERROR_DOWNLOAD_FILE }),
                    SnackbarVariant.ERROR
                  );
                }
                setLoading(false);
              },
              visible: fileDownload,
              disabled: loading,
            },
            // {
            //   title: Message.DOWNLOAD_ALL,
            //   Component: InsertDriveFileIcon,
            //   onClick: () => null,
            // },
          ].map(
            ({ visible, title, ...icon }) =>
              visible && (
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

export function EvidenceDetailDaoDialog({
  item,
  items,
  setItem,
}: DetailDaoDialogProps) {
  const files = getFiles(item);

  const { formatMessage } = useIntl();
  const configuration = useConfiguration();

  const classes = useStyles();
  const layoutClasses = useLayoutStyles();
  const spacingClasses = useSpacingStyles();

  const { height } = useWindowSize();

  const [file, setFile] = useState(files[0]);
  const [open, setOpen] = useState(false);

  const fileIndex = findIndex(files, ({ id }) => id === file.id);

  const existingFile = getExistingFile(file);

  const metadataId = file?.published?.id;
  const metadata = file?.published?.metadata;
  const showMetadata = metadata?.length && configuration.showMetadataInImageViewer;

  const maxHeight = height - 120; // - titles, toolbar, bottom space

  const getSectionHeight = (count: number, restCount: number) => {
    const fullHeight = count * 30;
    const restFullHeight = restCount * 30;

    return fullHeight < maxHeight / 2 || fullHeight + restFullHeight < maxHeight
      ? fullHeight
      : restFullHeight < maxHeight / 2
      ? maxHeight - restFullHeight
      : maxHeight / 2;
  };

  const isTile = !!(file && file.tile);

  return (
    <FileViewerWrapper
      {...{
        id: isTile ? existingFile?.id : existingFile?.file.id,
        highResImage: isTile,
      }}
    >
      {({ fileViewerProps, ...toolbarProps }) => (
        <div className={classes.daoDialog}>
          <Toolbar
            {...{
              ...toolbarProps,
              previousEnabled: true,
              nextEnabled: true,
              previousDisabled: fileIndex <= 0,
              nextDisabled: fileIndex < 0 || fileIndex >= files.length - 1,
              previous: () => setFile(files[fileIndex - 1]),
              next: () => setFile(files[fileIndex + 1]),
              item,
              setItem,
              open,
              setOpen,
              file,
            }}
          />
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
                defaultId: Message.DAO,
                items,
                onClick: (item: Dao) => {
                  setItem(item);
                  setFile(getFiles(item)[0]);
                },
                active: item,
                height: getSectionHeight(items.length, files.length),
                visible: true,
              },
              {
                label: Message.FILES_IN_DAO,
                defaultId: Message.FILE,
                mapper: (item: FileObject) =>
                  `${
                    item.tile
                      ? 'tile'
                      : item.published
                      ? 'published'
                      : 'thumbnail'
                  }.file.name`,
                items: files,
                onClick: setFile,
                active: file,
                height: getSectionHeight(files.length, items.length),
                visible: files.length,
              },
            ]
              .filter(({ visible }) => visible)
              .map(
                (
                  { label, defaultId, items, onClick, active, height, mapper },
                  i
                ) => (
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
                    <div
                      className={classes.daoDialogSectionPartContent}
                      style={{ height, maxHeight: height }}
                    >
                      {map(items, (item: any, i) => {
                        const isActive = active && active.id === item.id;

                        const value = get(item, mapper ? mapper(item) : 'name');

                        return (
                          <div
                            {...{
                              key: item.id,
                              className: classNames(
                                isActive && classes.daoDialogSectionPartActive
                              ),
                            }}
                            onClick={() => !isActive && onClick(item)}
                          >
                            {value && !isUUID(value) ? (
                              value
                            ) : (
                              <span>
                                {i + 1}. <FormattedMessage id={defaultId} />
                              </span>
                            )}
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
              !open && classes.daoDialogCenterOpen,
              !showMetadata && classes.daoDialogCenterNoSidebar
            )}
          >
            {files.length ? (
              <FileViewer {...fileViewerProps} />
            ) : (
              <div
                className={classNames(
                  classes.daoDialogNoFiles,
                  layoutClasses.flexCentered
                )}
              >
                <FormattedMessage id={Message.NO_FILES_TO_DISPLAY} />
              </div>
            )}
          </div>
          {showMetadata && <div
            className={classNames(
              classes.daoDialogSection,
              classes.daoDialogSide,
              showMetadata && classes.daoDialogRight,
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
                      downloadFileByUrl(
                        `/digitalObjectFile/${metadataId}/metadata/csv`,
                        `dao_${item.id}_metadata.csv`
                      )
                  }
                    />
                </Tooltip>
              </div>
              {metadata?.map(({ id, value, type }) => (
                <div {...{ key: id, className: layoutClasses.flex }}>
                  <div
                    className={classNames(
                      classes.bold,
                      classes.daoDialogMetadataLabel
                    )}
                  >
                    {type}:
                  </div>
                  <div>{value}</div>
                </div>
              ))}
            </div>
          </div>}
        </div>
      )}
    </FileViewerWrapper>
  );
}
