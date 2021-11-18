import React, { FC } from 'react';
import classNames from 'classnames';

import {
  ApuType,
} from '../../enums';
import { useStyles } from './styles';
import { useSpacingStyles } from '../../styles';

export const EvidenceIcon:FC<{
    type?:ApuType
}> = ({
    type
}) => {
    const classes = useStyles();
    const spacingClasses = useSpacingStyles();

    const getIcon = (type?: ApuType) => {
        switch(type){
            case ApuType.ENTITY:
            case ApuType.INSTITUTION:
                return 'fa fa-key';
            case ApuType.FUND:
                return 'fa fa-sitemap';
            case ApuType.FINDING_AID:
                return 'fa fa-book-reader';
            default:
                return null;
        }
    }

    const icon = getIcon(type)

    return icon ? <div key={type}><i
        className={classNames(
            icon,
            classes.evidenceDetailIcon,
            spacingClasses.marginBottomSmall
        )}
        /></div> : <></>
}
