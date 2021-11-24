import React, { useContext } from 'react';
import classNames from 'classnames';
import { useIntl, FormattedMessage } from 'react-intl';
import GetAppIcon from '@material-ui/icons/GetApp';

import { SnackbarContext, SnackbarVariant } from '@eas/common-web';

import { useStyles } from './styles';
import { useLayoutStyles, useSpacingStyles } from '../../styles';
import { DetailAttachmentsProps } from './types';
import { Message } from '../../enums';
import { downloadFile } from '../../common-utils';

export function EvidenceDetailAttachments({
  items,
  setLoading,
}: DetailAttachmentsProps) {
  const classes = useStyles();
  const layoutClasses = useLayoutStyles();
  const spacingClasses = useSpacingStyles();

  const { showSnackbar } = useContext(SnackbarContext);

  const { formatMessage } = useIntl();

  return items.length ? (
    <div className={spacingClasses.marginTopBig}>
      <h3 className={spacingClasses.marginBottomSmall}>
        <FormattedMessage id={Message.ATTACHMENTS} />
      </h3>
      <div
        className={classNames(
          layoutClasses.flexAlignCenter,
          layoutClasses.flexWrap
        )}
      >
        {items.map(({ id, name, file }) => {
          const fileId = file?.file?.id;

          return (
            <div
              {...{
                key: id,
                className: classNames(
                  classes.attachment,
                  layoutClasses.flexCentered,
                  spacingClasses.padding,
                  spacingClasses.marginRight,
                  spacingClasses.marginBottom,
                  fileId && classes.attachmentDownload
                ),
                onClick: async () => {
                  if (fileId) {
                    setLoading(true);
                    const ok = await downloadFile(fileId, name);
                    if (!ok) {
                      showSnackbar(
                        formatMessage({ id: Message.ERROR_DOWNLOAD_FILE }),
                        SnackbarVariant.ERROR
                      );
                    }
                    setLoading(false);
                  }
                },
              }}
            >
              <div
                className={classNames(
                  classes.attachmentLabel,
                  spacingClasses.marginRight
                )}
              >
                {name}
              </div>
              {fileId ? <GetAppIcon /> : <></>}
            </div>
          );
        })}
      </div>
    </div>
  ) : (
    <></>
  );
}
