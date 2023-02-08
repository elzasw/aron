import { Fullscreen, FullscreenExit, GetApp as GetAppIcon, InfoOutlined } from "@material-ui/icons";
import classNames from 'classnames';
import { findIndex } from 'lodash';
import React, { useEffect, useRef, useState, useContext } from 'react';
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
import { useParams } from "react-router-dom";
import { NavigationContext } from "@eas/common-web";
import { createApuDaoFileUrl, ApuPathParams } from "../evidence";

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

  const { formatMessage } = useIntl();
  const {
    showMetadataInImageViewer,
    daoFooter,
  } = useConfiguration();

  const classes = useStyles();
  const layoutClasses = useLayoutStyles();
  const spacingClasses = useSpacingStyles();

  const [open, setOpen] = useState(false);
  const [showMetadata, setShowMetadata] = useState(false);
  const [fullscreen, setFullscreen] = useState(!embed);

  const { daoId, fileId } = useParams<ApuPathParams>();
  const { navigate } = useContext(NavigationContext);
  
  useEffect(() => {
    if(!daoId || !fileId){
      navigate(createApuDaoFileUrl(apuInfo.id, dao.id, file.id));
    }
  },[dao, file])

  const fileIndex = findIndex(files, ({ id }) => id === file.id);

  const existingFile = getExistingFile(file);

  const metadataId = file?.published?.id;
  const metadata = file?.published?.metadata;

  const isTile = !!(file && file.tile);
  const fileUuid = isTile ? existingFile?.id : existingFile?.file.id

  const handleClickThumbnail = (file: FileObject) => {
    navigate(createApuDaoFileUrl(apuInfo.id, dao.id, file.id));
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
              previous: () => navigate(createApuDaoFileUrl(apuInfo.id, dao.id, files[fileIndex - 1]?.id)),
              next: () => navigate(createApuDaoFileUrl(apuInfo.id, dao.id, files[fileIndex + 1]?.id)),
              item: dao,
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
            {files.length && fileUuid ? (
              <ImageViewer ref={viewerRef} id={fileUuid} />
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
            {showInfo && 
              <div 
                className={classes.daoDialogFloatingOverlay} 
                style={{ top: 0, right: 0 }}
              >
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
              </div>
          }
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
                  `dao_${dao.id}_metadata.csv`
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
