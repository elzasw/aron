import React from 'react';
import classNames from 'classnames';
import InsertDriveFileIcon from '@material-ui/icons/InsertDriveFile';
import { FormattedMessage } from 'react-intl';

import { useStyles } from './styles';
import { useLayoutStyles, useSpacingStyles } from '../../styles';
import { DetailDaoProps } from './types';
import { EvidenceDetailDaoDialog } from './evidence-detail-dao-dialog';
import { DaoFile } from '../../types';
import { Message, DaoBundleType } from '../../enums';

import { ImageLoad } from '../../components/image-load/';

const ID = 'evidence-detail-dao';

export function EvidenceDetailDao({ 
  items, 
  apuInfo,
  item,
  setItem,
}: DetailDaoProps) {
  const classes = useStyles();
  const layoutClasses = useLayoutStyles();
  const spacingClasses = useSpacingStyles();

  const maxCount = 5;

  return items.length ? (
    <div id={ID} style={{height: "100%"}} className={classes.daoContainer}>
      <h3 className={spacingClasses.marginBottomSmall}>
        <FormattedMessage id={Message.DAOS} />
      </h3>
      <div className={layoutClasses.flex} style={{flexDirection: "column"}}>
        {maxCount ? (
          items.slice(0, maxCount).map((item) => {
            const thumbnailID = item.files
              .filter((item) => item.type === DaoBundleType.THUMBNAIL)
              .reduce(
                (minDAOFile: DaoFile | undefined, current: DaoFile) =>
                  !minDAOFile || minDAOFile.order > current.order
                    ? current
                    : minDAOFile,
                undefined
              )?.file?.id;

            const onClick = () => setItem(item);

            return (
              <div
                key={item.id}
                className={classNames(
                  classes.dao,
                  spacingClasses.marginRightSmall
                )}
              >
                <div
                  className={classNames(
                    classes.daoPreview,
                    spacingClasses.marginBottomSmall
                  )}
                  onClick={onClick}
                >
                  <ImageLoad
                    id={thumbnailID}
                    className={classNames(
                      classes.daoPreviewImage,
                      layoutClasses.flexCentered
                    )}
                    alternativeImage={<InsertDriveFileIcon />}
                  />
                </div>
                <div className={classes.link} onClick={onClick}>
                  <FormattedMessage id={Message.DISPLAY} />
                </div>
              </div>
            );
          })
        ) : (
          <></>
        )}
        {items.length > maxCount && (
          <div
            className={classNames(
              classes.daoShowAll,
              layoutClasses.flexCentered
            )}
            onClick={() => setItem(items[0])}
          >
            <FormattedMessage id={Message.OTHER_DAOS} />
          </div>
        )}
      </div>
      {item ? (
        <EvidenceDetailDaoDialog
          {...{
            item,
            items,
            setItem,
            apuInfo,
          }}
        />
      ) : (
        <></>
      )}
    </div>
  ) : (
    <></>
  );
}
