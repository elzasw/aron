import Tooltip from '@material-ui/core/Tooltip';
import React from 'react';
import { useIntl } from 'react-intl';
import { EmailIcon, EmailShareButton, FacebookIcon, FacebookShareButton, TwitterIcon, TwitterShareButton } from 'react-share';
import { useConfiguration } from '../../components';
import { Message } from '../../enums';
import { ApuEntity } from '../../types';
import { useStyles } from './styles';

interface EvidenceShareButtonsProps {
    item: ApuEntity;
}

export function EvidenceShareButtons({item}:EvidenceShareButtonsProps){
  const classes = useStyles();
  const configuration = useConfiguration();
  const { formatMessage } = useIntl();

  if(!configuration.showShareButtons){return <></>}

  return <div className={classes.shareButtonsContainer}>
    <Tooltip title={formatMessage({ id: Message.SHARE_THROUGH_EMAIL })}>
      <div className={classes.shareButton}>
        <EmailShareButton subject={item.name} body={item.description} url={location.href}>
          <EmailIcon className={classes.shareIcon}/>
        </EmailShareButton>
      </div>
    </Tooltip>
    <Tooltip title={formatMessage({ id: Message.SHARE_THROUGH_FACEBOOK })}>
      <div className={classes.shareButton}>
        <FacebookShareButton quote={item.name} url={location.href}>
          <FacebookIcon className={classes.shareIcon}/>
        </FacebookShareButton>
      </div>
    </Tooltip>
    <Tooltip title={formatMessage({ id: Message.SHARE_THROUGH_TWITTER })}>
      <div className={classes.shareButton}>
        <TwitterShareButton title={item.name} url={location.href}>
          <TwitterIcon className={classes.shareIcon}/>
        </TwitterShareButton>
      </div>
    </Tooltip>
  </div>
}
