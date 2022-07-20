import { Fullscreen, FullscreenExit, GetApp as GetAppIcon, InfoOutlined } from "@material-ui/icons";
import classNames from 'classnames';
import { findIndex } from 'lodash';
import React, { useEffect, useRef, useState } from 'react';
import { FormattedMessage, useIntl } from 'react-intl';
import { downloadFileByUrl } from '../../../common-utils';
import { useConfiguration } from '../../../components';
import { ImageViewer, ImageViewerExposedFunctions } from '../../../components/file-viewer/image-viewer';
import { ImageLoad } from '../../../components/image-load';
import { Message } from '../../../enums';
import { useLayoutStyles, useSpacingStyles } from '../../../styles';
import { Icon } from './icon';
import { useStyles } from './styles';
import { Toolbar } from './toolbar';
import { DetailDaoDialogProps, FileObject } from './types';
import { getExistingFile, getFiles } from './utils';

function Thumbnail({
  file,
  index,
  isActive,
  onClick = () => console.error('"onClick" not defined in Thumbnail'),
}:{
  file: FileObject;
  index: number;
  isActive?: boolean;
  onClick?: (file: FileObject) => void;
}){
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
        {index+1}
        {name && ` - ${name}`}
      </div>
      <ImageLoad
        key={file.id}
        id={isReferencedFile ? file?.thumbnail?.id : file?.thumbnail?.file?.id}
        referencedFile={isReferencedFile}
        alternativeImage={<div/>}
        className={classNames( classes.daoThumbnail)}
        />
    </div>
  );
}

function ImageList({
  activeFile,
  files,
  label,
  onClick = () => console.error('"onClick" not defined in ImageList'),
}:{
  activeFile: FileObject;
  files: FileObject[];
  label?: string;
  onClick?: (file: FileObject) => void;
}){
  const classes = useStyles();
  return <div
    key={`${label}`}
    className={classes.daoDialogSectionPart}
  >
    <div
      className={classes.daoDialogSectionPartContent}
    >
      {files.map((item, i) => {
        const isActive = activeFile && activeFile.id === item.id;

        return <Thumbnail key={i} isActive={isActive} index={i} file={item} onClick={onClick}/>
      })}
    </div>
  </div>
}

export function EvidenceDetailDaoDialog({
  item,
  // items,
  setItem,
  embed = false,
  customActionsLeft,
  customActionsRight,
  customActionsCenter,
}: DetailDaoDialogProps) {
  const [files, setFiles] = useState(getFiles(item));
  const viewerRef = useRef<ImageViewerExposedFunctions>(null);

  const { formatMessage } = useIntl();
  const {
    showMetadataInImageViewer,
    daoFooter,
  } = useConfiguration();

  const classes = useStyles();
  const layoutClasses = useLayoutStyles();
  const spacingClasses = useSpacingStyles();

  const [file, setFile] = useState(files[0]);
  const [open, setOpen] = useState(false);
  const [showMetadata, setShowMetadata] = useState(false);
  const [fullscreen, setFullscreen] = useState(!embed);

  useEffect(() => {
    const files = getFiles(item);
    setFiles(files)
    if(files.length > 0) {setFile(files[0])}
  }, [item])

  const fileIndex = findIndex(files, ({ id }) => id === file.id);

  const existingFile = getExistingFile(file);

  const metadataId = file?.published?.id;
  const metadata = file?.published?.metadata;

  const isTile = !!(file && file.tile);
  const fileId = isTile ? existingFile?.id : existingFile?.file.id

  const handleClickThumbnail = (file: FileObject) => {
    setFile(file);
    if(open){setOpen(false);}
  }

  const handleShowMetadata = () => {
    if(metadata?.length){setShowMetadata(!showMetadata)}
  }

  return (
    <div className={classNames(
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
              previous: () => setFile(files[fileIndex - 1]),
              next: () => setFile(files[fileIndex + 1]),
              item,
              setItem,
              open,
              setOpen,
              file,
              showCloseButton: !embed,
              customActionsLeft: customActionsLeft?.({fullscreen}),
              customActionsRight: customActionsRight?.({fullscreen}),
              customActionsCenter: <>
                {showMetadataInImageViewer && <Icon Component={InfoOutlined} title={"info"} onClick={handleShowMetadata}/>}
                {embed && <Icon 
                  Component={fullscreen ? FullscreenExit : Fullscreen} 
                  onClick={() => setFullscreen(!fullscreen)}
                  title={formatMessage({id: fullscreen ? Message.FULLSCREEN_EXIT : Message.FULLSCREEN})}
                  />}
                {customActionsCenter?.({fullscreen})}
                </>,
            }}
            />
      }
        <div>
          <div
            className={classNames(
              classes.daoDialogSection,
              classes.daoDialogCenter,
              classes.daoDialogCenterNoSidebar
            )}
          >
            {files.length && fileId ? (
              <ImageViewer ref={viewerRef} id={fileId} />
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
            {daoFooter && 
              <div style={{
                position: 'absolute', 
                bottom: 0,
                right: 0,
                color: 'white',
                background: '#0008',
                padding: '8px 5px',
              }}>
                <span style={{
                  paddingRight: '10px', 
                  borderRight: '1px solid currentColor', 
                  margin: '0 5px'
                }}>
                  {daoFooter.copyrightText}
                </span>
                {daoFooter.links?.map((link, index)=>{
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
                  `dao_${item.id}_metadata.csv`
                )}
              className={classes.daoDialogMetadataButton}
            >
              <GetAppIcon className={classNames( classes.icon)} />
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
      <div
        className={classNames(
          classes.daoDialogSection,
          classes.daoDialogSide,
          classes.daoDialogLeft,
          open && classes.daoDialogSideOpen
        )}
      >
        <ImageList files={files} activeFile={file} onClick={handleClickThumbnail} />
      </div>
    </div>
  );
}
