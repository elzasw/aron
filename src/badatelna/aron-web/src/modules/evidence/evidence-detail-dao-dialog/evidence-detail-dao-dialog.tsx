import Tooltip from '@material-ui/core/Tooltip';
import { GetApp as GetAppIcon, Fullscreen, FullscreenExit } from "@material-ui/icons";
import classNames from 'classnames';
import { findIndex, map } from 'lodash';
import React, { useEffect, useState, useRef } from 'react';
import { FormattedMessage, useIntl } from 'react-intl';
import {
    downloadFileByUrl,
    useWindowSize
} from '../../../common-utils';
import { useConfiguration } from '../../../components';
import { ImageViewer, ImageViewerExposedFunctions } from '../../../components/file-viewer/image-viewer';
import { ImageLoad } from '../../../components/image-load';
import { Message } from '../../../enums';
import { useLayoutStyles, useSpacingStyles } from '../../../styles';
import { Dao } from '../../../types';
import { Icon } from './icon';
import { useStyles } from './styles';
import { Toolbar } from './toolbar';
import { DetailDaoDialogProps, FileObject } from './types';
import { getExistingFile, getFiles } from './utils';

export function EvidenceDetailDaoDialog({
  item,
  items,
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

  const { height } = useWindowSize();

  const [file, setFile] = useState(files[0]);
  const [open, setOpen] = useState(false);
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
  const showMetadata = metadata?.length && showMetadataInImageViewer;

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
  const fileId = isTile ? existingFile?.id : existingFile?.file.id

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
              !open && classes.daoDialogCenterOpen,
              !showMetadata && classes.daoDialogCenterNoSidebar
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
        </>
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
            visible: false,
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
                  className={classes.daoDialogSectionPartContent}
                >
                  {map(items, (item: any, i) => {
                    const isActive = active && active.id === item.id;
                    const name = item.published?.metadata?.find((item: any) => item.type === "name")?.value;
                    const isReferencedFile = !!item?.thumbnail?.name;

                    return (
                      <div
                        {...{
                          key: item.id,
                        }}
                        onClick={() => !isActive && onClick(item)}
                        className={classNames(
                          isActive && classes.daoDialogSectionPartActive,
                          classes.daoThumbnailContainer
                        )}
                      >
                        <div title={name} style={{
                          color: 'white',
                          position: 'absolute',
                          zIndex: 10,
                          padding: '5px 10px',
                          textShadow: '0px 0px 8px black',
                          bottom: 0,
                          lineHeight: '1em',
                          width: '100%',
                          background: '#0007',
                          maxHeight: '100%',
                          whiteSpace: 'nowrap',
                          overflow: 'hidden',
                          textOverflow: 'ellipsis',
                        }}>
                          {i+1}
                          {name && ` - ${name}`}
                        </div>
                        <ImageLoad
                          key={item.id}
                          id={isReferencedFile ? item?.thumbnail?.id : item?.thumbnail?.file?.id}
                          referencedFile={isReferencedFile}
                          alternativeImage={<div/>}
                          className={classNames( classes.daoThumbnail)}
                          />
                      </div>
                    );
                  })}
                </div>
              </div>
            )
        )}
      </div>
    </div>
  );
}
