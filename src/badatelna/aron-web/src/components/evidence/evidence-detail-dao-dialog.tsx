import React from 'react';
// import classNames from 'classnames';
import Tooltip from '@material-ui/core/Tooltip';
import CloseIcon from '@material-ui/icons/Close';
import MenuIcon from '@material-ui/icons/Menu';
import ZoomInIcon from '@material-ui/icons/ZoomIn';
import ZoomOutIcon from '@material-ui/icons/ZoomOut';
import ZoomOutMapIcon from '@material-ui/icons/ZoomOutMap';
import PanToolIcon from '@material-ui/icons/PanTool';
import GetAppIcon from '@material-ui/icons/GetApp';
import InsertDriveFileIcon from '@material-ui/icons/InsertDriveFile';

import { useStyles } from './styles';
import {
  useLayoutStyles,
  // useSpacingStyles
} from '../../styles';
import { DetailDaoDialogProps } from './types';

export function EvidenceDetailDaoDialog({
  open,
  onClose,
}: DetailDaoDialogProps) {
  const classes = useStyles();
  const layoutClasses = useLayoutStyles();
  // const spacingClasses = useSpacingStyles();

  return open ? (
    <div className={classes.daoDialog}>
      <div className={layoutClasses.flexSpaceBetween}>
        <CloseIcon
          {...{ onClick: onClose, className: classes.daoDialogIcon }}
        />
        <div className={layoutClasses.flex}>
          {[
            { title: 'Přiblížit', Icon: ZoomInIcon, onClick: () => null },
            { title: 'Oddálit', Icon: ZoomOutIcon, onClick: () => null },
            { title: '', Icon: ZoomOutMapIcon, onClick: () => null }, // TODO:
            { title: '', Icon: PanToolIcon, onClick: () => null },
            { title: 'Stáhnout', Icon: GetAppIcon, onClick: () => null },
            {
              title: 'Stáhnout metadata',
              Icon: InsertDriveFileIcon,
              onClick: () => null,
            },
          ].map(({ title, Icon }) => (
            <Tooltip {...{ key: title, title }}>
              <Icon {...{ className: classes.daoDialogIcon }} />
            </Tooltip>
          ))}
        </div>
        <MenuIcon onClick={() => null} />
      </div>
    </div>
  ) : (
    <></>
  );
}
