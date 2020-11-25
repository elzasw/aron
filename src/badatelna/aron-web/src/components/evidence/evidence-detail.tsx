import React, { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { flatten } from 'lodash';
import classNames from 'classnames';
import Tooltip from '@material-ui/core/Tooltip';
import DoubleArrowIcon from '@material-ui/icons/DoubleArrow';
import PersonIcon from '@material-ui/icons/Person';
import InsertPhotoIcon from '@material-ui/icons/InsertPhoto';

import { ApiUrl, ApuType } from '../../enums';
import { useStyles } from './styles';
import { useLayoutStyles, useSpacingStyles } from '../../styles';
import { useGet } from '../../hooks';
import { DetailProps } from './types';
import { ApuEntity, ApuPartType, ApuPartItemType } from '../../types';
import { formatApuPartItemValue } from '../../common-utils';
import { EvidenceWrapper } from './evidence-wrapper';
import { findApuParts, filterApuPartTypes } from './utils';
import { EvidenceDetailDao } from './evidence-detail-dao';

function Component({
  label,
  items,
  open: outterOpen,
}: {
  label: string;
  items: any[];
  open: boolean;
}) {
  const classes = useStyles();
  const layoutClasses = useLayoutStyles();
  const spacingClasses = useSpacingStyles();

  const [open, setOpen] = useState(false);

  useEffect(() => {
    setOpen(outterOpen);
  }, [outterOpen, setOpen]);

  const labelClassName = classNames(
    classes.evidenceDetailItemLabel,
    spacingClasses.marginRightBig
  );

  return (
    <div
      key={label}
      className={classNames(
        classes.evidenceDetailItem,
        spacingClasses.marginBottomSmall
      )}
    >
      <div className={layoutClasses.flex}>
        <p className={labelClassName}>{label}</p>
        <Tooltip title={`Kliknutím ${open ? 'sbalte' : 'rosbalte'}`}>
          <p
            className={classes.bold}
            style={{ cursor: 'pointer' }}
            onClick={() => setOpen(!open)}
          >
            {items
              .map(({ value, dataType }) =>
                formatApuPartItemValue(value, dataType)
              )
              .join(' ')}
          </p>
        </Tooltip>
      </div>
      {open ? (
        items.map(({ label, value, dataType }) => (
          <div key={label} className={layoutClasses.flex}>
            <p className={labelClassName}>{label}</p>
            <p>{formatApuPartItemValue(value, dataType)}</p>
          </div>
        ))
      ) : (
        <></>
      )}
    </div>
  );
}

export function EvidenceDetail({
  modulePath: path,
  label,
  apuPartTypes,
  apuPartItemTypes,
}: DetailProps) {
  const classes = useStyles();
  const layoutClasses = useLayoutStyles();
  const spacingClasses = useSpacingStyles();

  const [open, setOpen] = useState(false);

  const { id } = useParams();

  const url = `${ApiUrl.APU}/${id}`;

  const [item] = useGet(url);

  if (!item) {
    return <></>;
  }

  const { name, description, type, parts = [] } = item as ApuEntity;

  const ImageComponent = type === ApuType.ENTITY ? PersonIcon : InsertPhotoIcon;

  return (
    <EvidenceWrapper
      {...{
        items: [
          { path, label },
          {
            label: name,
          },
        ],
      }}
    >
      <div className={classes.evidenceDetail}>
        <div className={spacingClasses.paddingBig}>
          <div
            className={classNames(
              classes.evidenceDetailTop,
              layoutClasses.flexSpaceBetween,
              spacingClasses.marginBottom
            )}
          >
            <div className={layoutClasses.flex}>
              <ImageComponent
                className={classNames(
                  classes.evidenceDetailImage,
                  spacingClasses.marginRightBig
                )}
              />
              <div>
                <h3 className={spacingClasses.marginBottomSmall}>{name}</h3>
                <h4>{description}</h4>
              </div>
            </div>
            <DoubleArrowIcon
              className={classNames(
                classes.evidenceDetailTopIcon,
                open && classes.evidenceDetailTopIconOpen
              )}
              onClick={() => setOpen(!open)}
            />
          </div>
          {filterApuPartTypes(apuPartTypes, parts)
            .map(({ code, label }: ApuPartType) => {
              const items = flatten(
                findApuParts(parts, code).map(({ items }) => items)
              );

              return {
                label,
                items: flatten(
                  filterApuPartTypes(apuPartItemTypes, items).map(
                    ({ code, label, dataType }: ApuPartItemType) => {
                      return findApuParts(items, code).map((item) => ({
                        ...item,
                        label,
                        dataType,
                      }));
                    }
                  )
                ),
              };
            })
            .map((item) => (
              <Component {...{ key: item.label, ...item, open }} />
            ))}
          <div className={spacingClasses.marginTopBig} />
          <h3 className={spacingClasses.marginBottomSmall}>Digitalizáty</h3>
          <div
            className={classNames(layoutClasses.flex, layoutClasses.flexWrap)}
          >
            {[
              { id: '1', description: 'test', permalink: 'test', files: [] },
              { id: '2', description: 'test', permalink: 'test', files: [] },
              { id: '3', description: 'test', permalink: 'test', files: [] },
            ].map((item) => (
              <EvidenceDetailDao {...{ key: item.id, item }} />
            ))}
          </div>
        </div>
      </div>
    </EvidenceWrapper>
  );
}
