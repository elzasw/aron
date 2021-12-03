import React, { useState, useEffect } from 'react';
import { find } from 'lodash';
import classNames from 'classnames';
import Tooltip from '@material-ui/core/Tooltip';
import { FormattedMessage } from 'react-intl';

import { ApuPartViewType, ApuPartItemDataType, Message } from '../../enums';
import { useStyles } from './styles';
import { useLayoutStyles, useSpacingStyles } from '../../styles';
import { usePrevious } from '../../common-utils';
import { ApuEntity } from '../../types';
import { EvidenceDetailItemValue } from './evidence-detail-item-value';
import { useConfiguration } from '../../components';

export function EvidenceDetailItem({
  name,
  viewType,
  items,
  open: outterOpen,
  index,
  apus,
}: {
  name: string;
  viewType: ApuPartViewType;
  items: any[];
  open: boolean;
  index: number;
  apus: ApuEntity[];
}) {
  const configuration = useConfiguration();
  const classes = useStyles({alternativeItemLabel: configuration.alternativeItemLabel});
  const layoutClasses = useLayoutStyles();
  const spacingClasses = useSpacingStyles();

  const [open, setOpen] = useState(viewType === ApuPartViewType.STANDALONE);

  const previousOutterOpen = usePrevious(outterOpen);

  useEffect(() => {
    if (
      viewType === ApuPartViewType.GROUPED &&
      previousOutterOpen !== undefined &&
      previousOutterOpen !== outterOpen
    ) {
      setOpen(outterOpen);
    }
  }, [viewType, outterOpen, previousOutterOpen]);

  const labelClassName = classNames(
    classes.evidenceDetailItemText,
    classes.evidenceDetailItemLabel,
    spacingClasses.marginRight,
    spacingClasses.paddingRight
  );

  const isGrouped = viewType === ApuPartViewType.GROUPED;

  const renderGroupedItems = () => {
    return <div
      className={classNames(
        classes.evidenceDetailItemText,
        classes.bold,
        open && spacingClasses.paddingBottomSmall
      )}
      style={configuration.allowDetailExpand ? { cursor: 'pointer' } : {}}
      onClick={() => configuration.allowDetailExpand && setOpen(!open)}
    >
      {items
        .filter(({ type }) => type !== ApuPartItemDataType.APU_REF)
        .map(({ value, type }, i) => (
          <React.Fragment key={`${value}-${i}`}>
            <EvidenceDetailItemValue {...{ value, type }} />{' '}
          </React.Fragment>
        ))}
    </div>
  }

  return (
    <div
      className={classNames(
        classes.evidenceDetailItem,
        spacingClasses.paddingTopSmall,
        index && classes.evidenceDetailItemNotFirst
      )}
    >
      <div
        className={classNames(
          layoutClasses.flex,
          (!isGrouped || !open) && spacingClasses.paddingBottomSmall
        )}
      >
        {(isGrouped || configuration.showStandalonePartName) && 
            <div
                className={classNames(
                    labelClassName,
                    isGrouped && classes.evidenceDetailItemLabelBorder
                )}
            >
                {name}
            </div>
        }
        {isGrouped && (
          configuration.allowDetailExpand ? <Tooltip
            title={
              <FormattedMessage
                id={open ? Message.CLICK_TO_COLLAPSE : Message.CLICK_TO_EXPAND}
              />
            }
          >
            {renderGroupedItems()}
          </Tooltip> : renderGroupedItems()
        )}
      </div>
      {open ? (
        <div className={spacingClasses.paddingBottomSmall}>
          {items.map((item, i) => {
            const prevItem = items[i-1];
            const isSameTypeAsPrev = prevItem && prevItem.code === item.code;

            return item.type !== ApuPartItemDataType.APU_REF ||
            find(apus, ({ id }) => id === item.value) ? (
              <div key={`${item.name}-${i}`} className={layoutClasses.flex}>
                <div
                  className={classNames(
                    labelClassName,
                    classes.evidenceDetailItemLabelBorder,
                    i && spacingClasses.paddingTopSmall
                  )}
                >
                  {!isSameTypeAsPrev && item.name}
                </div>
                <div
                  className={classNames(
                    classes.evidenceDetailItemText,
                    i && spacingClasses.paddingTopSmall
                  )}
                >
                  <EvidenceDetailItemValue {...{ ...item, apus }} />
                </div>
              </div>
            ) : (
              <div key={`${item.name}-${i}`} />
            )
            }
          )}
        </div>
      ) : (
        <></>
      )}
    </div>
  );
}
