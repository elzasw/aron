import { Fullscreen, FullscreenExit, GetApp as GetAppIcon, InfoOutlined } from "@material-ui/icons";
import classNames from 'classnames';
import { findIndex } from 'lodash';
import React, { useEffect, useRef, useState, useContext } from 'react';
import { FormattedMessage, useIntl } from 'react-intl';
import { downloadFileByUrl, createUrlParams } from '../../../common-utils';
import { useConfiguration } from '../../../components';
import { ImageViewer, ImageViewerExposedFunctions } from '../../../components/file-viewer/image-viewer';
import { ImageLoad } from '../../../components/image-load';
import { Message, API_URL } from '../../../enums';
import { useLayoutStyles, useSpacingStyles } from '../../../styles';
import { Icon } from './icon';
import { useStyles } from './styles';
import { Toolbar } from './toolbar';
import { DetailDaoDialogProps, FileObject } from './types';
import { getExistingFile, getFiles } from './utils';
import { useParams } from "react-router-dom";
import { NavigationContext } from "@eas/common-web";
import { createApuDaoFileUrl, ApuPathParams } from "../evidence";
import { DaoNamePlacement } from "../../../enums/dao-name-placement";
import { FixedSizeList, ListChildComponentProps } from "react-window";
import AutoSizer from "react-virtualized-auto-sizer"
import { useKeyPress } from "../../../utils/useKeyPress";

function Thumbnail({
  file,
  index,
  isActive,
  onClick = () => console.error('"onClick" not defined in Thumbnail'),
}: {
  file: FileObject;
  index: number;
  isActive?: boolean;
  onClick?: (file: FileObject) => void;
}) {
  const classes = useStyles();
  const name = file.published?.metadata?.find((item) => item.type === "name")?.value;
  const isReferencedFile = !!file?.thumbnail?.name;

  return (
    <div
      {...{
        key: file.id,
      }}
      onClick={() => !isActive && onClick(file)}
      className={classNames(
        isActive && classes.daoDialogSectionPartActive,
        classes.daoThumbnailContainer
      )}
    >
      <div
        title={name}
        className={classes.daoThumbnailTitle}
      >
        {index + 1}
        {name && ` - ${name}`}
      </div>
      <ImageLoad
        key={file.id}
        id={isReferencedFile ? file?.thumbnail?.id : file?.thumbnail?.file?.id}
        referencedFile={isReferencedFile}
        alternativeImage={<div />}
        className={classNames(classes.daoThumbnail)}
      />
    </div>
  );
}

function ListItem({ index, style, data }: ListChildComponentProps) {
  const item = data.files[index];
  const isActive = data.activeFile && data.activeFile.id === item.id;
  return <div
    key={index}
    style={{ ...style, overflow: "hidden", padding: "3px 8px" }}
  >
    <Thumbnail
      key={index}
      isActive={isActive}
      index={index}
      file={item}
      onClick={data.onClick}
    />
  </div>
}

function ImageList({
  activeFile,
  files,
  label,
  onClick = () => console.error('"onClick" not defined in ImageList'),
}: {
  activeFile: FileObject;
  files: FileObject[];
  label?: string;
  onClick?: (file: FileObject) => void;
}) {
  const classes = useStyles();
  return <div
    key={`${label}`}
    className={classes.daoDialogSectionPart}
  >
    <div
      className={classes.daoDialogSectionPartContent}
    >
      <AutoSizer>
        {({ width, height }) => (
          <FixedSizeList
            width={width}
            height={height}
            itemCount={files.length}
            itemSize={140}
            overscanCount={2}
            itemData={{
              files, activeFile, onClick
            }}
          >
            {ListItem}
          </FixedSizeList>
        )}
      </AutoSizer>
    </div>
  </div>
}

const FULLSCREEN = "fullscreen"

const getObjectFromUrlParams = (map: URLSearchParams) => {
  const object: Record<string, string> = {};
  map.forEach((value, key) => {
    object[key] = value.toString();
  })
  return object;
}

const getDaoPlacementStyle = (placement?: DaoNamePlacement) => {
  if (!placement) { placement = DaoNamePlacement.TOP_RIGHT }

  const placementStyle = {
    [DaoNamePlacement.TOP_LEFT]: { top: '50px', left: 0 },
    [DaoNamePlacement.TOP_RIGHT]: { top: '50px', right: 0 },
    [DaoNamePlacement.BOTTOM_RIGHT]: { bottom: 0, right: 0 },
    [DaoNamePlacement.BOTTOM_LEFT]: { bottom: 0, left: 0 },
  }[placement];

  if (!placementStyle) { throw `Undefined placement name: ${placement}` }

  return placementStyle || {};
}

export function EvidenceDetailDaoDialog({
  // item,
  // items,
  dao,
  file,
  setItem,
  embed = false,
  customActionsLeft,
  customActionsRight,
  customActionsCenter,
  apuInfo,
  showInfo,
}: DetailDaoDialogProps) {
  const files = getFiles(dao);
  const viewerRef = useRef<ImageViewerExposedFunctions>(null);
  const daoDialogElement = useRef<HTMLDivElement>(null);

  const { formatMessage } = useIntl();
  const {
    showMetadataInImageViewer,
    daoFooter,
    daoNamePlacement,
    hideThumbnails,
  } = useConfiguration();

  const classes = useStyles();
  const layoutClasses = useLayoutStyles();
  const spacingClasses = useSpacingStyles();
  const urlParams = getObjectFromUrlParams(new URLSearchParams(location.search));
  const isFullscreen = urlParams[FULLSCREEN] === "true";

  const [open, setOpen] = useState(false);
  const [showMetadata, setShowMetadata] = useState(false);
  const [fullscreen, setFullscreen] = useState(!embed || isFullscreen);

  const { daoId, fileId } = useParams<ApuPathParams>();
  const { navigate } = useContext(NavigationContext);

  useEffect(() => {
    if (!daoId || !fileId) {
      navigate(createApuDaoFileUrl(apuInfo.id, dao.id, file.id));
    }
  }, [dao, file])

  useEffect(() => {
    navigate(`${location.pathname}${createUrlParams({ ...urlParams, [FULLSCREEN]: fullscreen && embed })}`);
  }, [fullscreen])

  const fileIndex = findIndex(files, ({ id }) => id === file.id);

  const existingFile = getExistingFile(file);

  const metadataId = file?.published?.id;
  const metadata = file?.published?.metadata;

  const isTile = !!(file && file.tile);
  const fileUuid = isTile ? existingFile?.id : existingFile?.file.id

  useKeyPress([{ key: "ArrowUp" }, { key: "ArrowLeft" }], () => handleSelectRelative(-1), daoDialogElement.current);
  useKeyPress([{ key: "ArrowDown" }, { key: "ArrowRight" }], () => handleSelectRelative(1), daoDialogElement.current);
  useKeyPress([{ key: "Home" }], () => handleShowFirst(), daoDialogElement.current);
  useKeyPress([{ key: "End" }], () => handleShowLast(), daoDialogElement.current);
  useKeyPress([{ key: "PageUp" }], () => handleSelectRelative(-10), daoDialogElement.current);
  useKeyPress([{ key: "PageDown" }], () => handleSelectRelative(10), daoDialogElement.current);

  const handleClickThumbnail = (file: FileObject) => {
    navigate(`${createApuDaoFileUrl(apuInfo.id, dao.id, file.id)}${createUrlParams(urlParams)}`);
    if (open) { setOpen(false); }
  }

  const handleShowMetadata = () => {
    if (metadata?.length) { setShowMetadata(!showMetadata) }
  }

  const showFileByIndex = (index: number) => navigate(createApuDaoFileUrl(apuInfo.id, dao.id, files[index]?.id));

  const handleSelectRelative = (step: number) => {
    if (fileIndex + step >= files.length) {
      handleShowLast();
    }
    else if (fileIndex + step < 0) {
      handleShowFirst();
    }
    else {
      showFileByIndex(fileIndex + step)
    }
  };

  const handleShowFirst = () => showFileByIndex(0);
  const handleShowLast = () => showFileByIndex(files.length - 1);

  const createUrlsFromFiles = (files: FileObject[]) => {
    return files.map((file) => {
      const _file = getExistingFile(file);
      const isTile = !!(file && file.tile);
      const uuid = isTile ? _file?.id : _file?.file.id
      return file.tile?.referencedFile?.startsWith("http") ? file.tile.referencedFile : `${API_URL}/tile/${uuid}/image.dzi`
    })
  }

  return (
    <div tabIndex={0} ref={daoDialogElement} className={classNames(
      classes.daoDialog,
      fullscreen && classes.daoDialogFixed
    )}>
      <>
        {viewerRef.current &&
          <Toolbar
            {...{
              zoomIn: () => viewerRef.current?.zoomIn(),
              zoomOut: () => viewerRef.current?.zoomOut(),
              zoomInDisabled: false,
              zoomOutDisabled: false,
              rotateLeft: () => viewerRef.current?.rotateLeft(),
              rotateRight: () => viewerRef.current?.rotateRight(),
              reset: () => viewerRef.current?.reset(),
              previousEnabled: true,
              nextEnabled: true,
              previousDisabled: fileIndex <= 0,
              nextDisabled: fileIndex < 0 || fileIndex >= files.length - 1,
              previous: () => handleSelectRelative(-1),
              next: () => handleSelectRelative(1),
              item: dao,
              setItem,
              open,
              setOpen: hideThumbnails ? undefined : setOpen,
              file,
              showCloseButton: !embed,
              customActionsLeft: customActionsLeft?.({ fullscreen }),
              customActionsRight: customActionsRight?.({ fullscreen }),
              customActionsCenter: <>
                {showMetadataInImageViewer && <Icon Component={InfoOutlined} title={"info"} onClick={handleShowMetadata} />}
                {embed && <Icon
                  Component={fullscreen ? FullscreenExit : Fullscreen}
                  onClick={() => setFullscreen(!fullscreen)}
                  title={formatMessage({ id: fullscreen ? Message.FULLSCREEN_EXIT : Message.FULLSCREEN })}
                />}
                {customActionsCenter?.({ fullscreen })}
              </>,
            }}
          />
        }
        <div>
          <div
            className={classNames(
              classes.daoDialogSection,
              classes.daoDialogCenter,
              classes.daoDialogCenterNoSidebar,
              hideThumbnails && classes.daoDialogCenterNoThumbnails
            )}
          >
            {files.length && fileUuid ? (
              <ImageViewer
                ref={viewerRef}
                parentId={dao.id}
                urls={createUrlsFromFiles(files)}
                page={files.findIndex((file) => {
                  return fileUuid === file?.tile?.id || fileUuid === file?.id || fileUuid === file?.published?.id;
                })}
              />
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
            <div
              className={classes.daoDialogFloatingOverlay}
              style={{ ...getDaoPlacementStyle(daoNamePlacement), padding: '10px' }}
            >
              {showInfo && <>
                <div className={classes.bold}>
                  {apuInfo.name}
                </div>
                {apuInfo.description && <div style={{
                  marginTop: '5px',
                }}>
                  {apuInfo.description.length > 150
                    ? `${apuInfo.description?.slice(0, 150)}...`
                    : apuInfo.description
                  }
                </div>}
              </>
              }
              <div style={{ marginTop: '5px' }}>
                {fileIndex + 1}/{files.length} - {file.published?.metadata?.find((item) => item.type === "name")?.value}
              </div>
            </div>
            {daoFooter &&
              <div
                className={classes.daoDialogFloatingOverlay}
                style={{ bottom: 0, right: 0 }}
              >
                <span style={{
                  paddingRight: '10px',
                  borderRight: '1px solid currentColor',
                  margin: '0 5px'
                }}>
                  {daoFooter.copyrightText}
                </span>
                {daoFooter.links?.map((link, index) => {
                  return <a
                    key={index}
                    target='_'
                    style={{
                      margin: '0 5px',
                      color: 'white'
                    }}
                    href={link.url}
                  >
                    {link.text}
                  </a>
                })}
              </div>
            }
          </div>
        </div>
        {showMetadata && <div
          className={classNames(
            classes.daoDialogSection,
            classes.daoDialogMetadataContainer,
          )}
        >
          <div
            className={classNames(
              classes.daoDialogMetadata,
              spacingClasses.paddingSmall
            )}
          >
            <div
              onClick={() =>
                downloadFileByUrl(
                  `/digitalObjectFile/${metadataId}/metadata/csv`,
                  `dao_${dao.id}_metadata.csv`
                )}
              className={classes.daoDialogMetadataButton}
            >
              <GetAppIcon className={classNames(classes.icon)} />
              {formatMessage({ id: Message.DOWNLOAD_METADATA })}
            </div>
            <div className={layoutClasses.flex}>
              <div className={classes.bold}>{formatMessage({ id: Message.METADATA })}</div>
            </div>
            <div>
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
          </div>
        </div>}
      </>
      {!hideThumbnails && <div
        className={classNames(
          classes.daoDialogSection,
          classes.daoDialogSide,
          classes.daoDialogLeft,
          open && classes.daoDialogSideOpen
        )}
      >
        <ImageList files={files} activeFile={file} onClick={handleClickThumbnail} />
      </div>}
    </div>
  );
}
