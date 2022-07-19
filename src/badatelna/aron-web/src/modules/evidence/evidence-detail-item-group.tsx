import classNames from 'classnames';
import { compact } from 'lodash';
import React, { useEffect, useState } from 'react';
import { useIntl } from 'react-intl';
import { getApu } from '../../common-utils';
import { ApuPartItemDataType, Message } from '../../enums';
import { useLayoutStyles, useSpacingStyles } from '../../styles';
import { ApuEntitySimplified } from '../../types';
import { EvidenceDetailItemValue } from './evidence-detail-item-value';
import { useStyles } from './styles';

interface EvidenceDetailItem {
  name?: string;
  type: ApuPartItemDataType;
  href?: string;
  value: string;
  code?: string;
}
export function EvidenceDetailItemGroup({
  items,
  initialItemLimit = 10,
}: {
  items: EvidenceDetailItem[];
  initialItemLimit?: number;
}) {
  const { formatMessage } = useIntl();

  const classes = useStyles();
  const layoutClasses = useLayoutStyles();
  const spacingClasses = useSpacingStyles();

  const type = items[0]?.type;
  const code = items[0]?.code;
  const name = items[0]?.name;
  const hasApus = type === ApuPartItemDataType.APU_REF;

  const [apus, setApus] = useState<ApuEntitySimplified[]>([]);
  const [apusLoading, setApusLoading] = useState(false);
  const [apusLoaded, setApusLoaded] = useState(false);
  const [apusAllLoaded, setApusAllLoaded] = useState(false);
  const [showAllItems, setShowAllItems] = useState(false);
  
  useEffect(()=>{
    if(!hasApus){return;}

    const loadApus = async () => {
      setApusLoading(true);
      const start = apus?.length || 0; // leave out existing apus
      const end = showAllItems ? items.length : initialItemLimit;
      const newApus = await Promise.all(
        items.slice(start, end)
          .map((item) => getApu(item.value))
      );
      setApus(compact([...apus, ...newApus]));
      setApusLoaded(true);
      setApusLoading(false);
      setApusAllLoaded(showAllItems);
    }
    if((!apusLoaded || !apusAllLoaded) && !apusLoading){
      loadApus();
    }
  }, [items, showAllItems])

  const handleShowAllItems = () => {
    if(!apusLoading){setShowAllItems(!showAllItems);}
  }

  const labelClassName = classNames(
    classes.evidenceDetailItemText,
    classes.evidenceDetailItemLabel,
    spacingClasses.marginRight,
    spacingClasses.paddingRight
  );

  // hide the whole group when no apus in apu_ref
  if(hasApus && apus.length === 0){
    return <></>; 
  }

  return <div key={`${code}`} className={layoutClasses.flex}>
    <div
      className={classNames(
        labelClassName,
        classes.evidenceDetailItemLabelBorder,
        spacingClasses.paddingTopSmall
      )}
    >
      {name}
    </div>
    <div
      className={classNames(
        classes.evidenceDetailItemText,
        spacingClasses.paddingTopSmall
      )}
    >
      {(!hasApus || apusLoaded) && items.slice(0, showAllItems && apusAllLoaded ? items.length : initialItemLimit).map((item, i) => 
        hasApus && !apus.find((apu)=> apu.id === item.value) // hide missing apus
          ? <></> 
          : <EvidenceDetailItemValue key={i} {...item} apus={apus}/>
      )}
      {apusLoading && `${formatMessage({id: Message.LOADING})}...`}
      {items.length > initialItemLimit && 
        <div className={classes.evidenceDetailItemExpandButtonWrapper}>
        <button 
          onClick={handleShowAllItems}
          className={classes.evidenceDetailItemExpandButton}
        >
          {showAllItems 
              ? formatMessage({id: Message.APU_HIDE}) 
              : `${formatMessage({id: Message.APU_SHOW_ALL})} (${items.length})`}
        </button>
        </div>
        }
    </div>
  </div>
}
