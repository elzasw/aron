import { SnackbarContext, SnackbarVariant } from '@eas/common-web';
import {
    Close as CloseIcon,
    GetApp as GetAppIcon, Menu as MenuIcon,
    RotateLeft,
    RotateRight,
    ZoomIn as ZoomInIcon,
    ZoomOut as ZoomOutIcon,
    ZoomOutMap as ZoomOutMapIcon,
  Share as ShareIcon,
} from "@material-ui/icons";
import KeyboardArrowLeftIcon from '@material-ui/icons/KeyboardArrowLeft';
import KeyboardArrowRightIcon from '@material-ui/icons/KeyboardArrowRight';
import classNames from 'classnames';
import { get } from 'lodash';
import React, { useContext, useState } from 'react';
import { useIntl } from 'react-intl';
import { downloadFile } from '../../../common-utils';
import { Loading, useConfiguration } from '../../../components';
import { Message } from '../../../enums';
import { useLayoutStyles } from '../../../styles';
import { useStyles } from './styles';
import { ToolbarProps } from './types';
import { getExistingFile } from './utils';
import { Icon } from './icon';

export function Toolbar({
  noView,
  noAction,
  previous,
  next,
  zoomIn,
  zoomOut,
  reset,
  rotateLeft,
  rotateRight,
  previousEnabled,
  nextEnabled,
  previousDisabled,
  nextDisabled,
  zoomInDisabled,
  zoomOutDisabled,
  item,
  setItem,
  open,
  setOpen,
  file,
  showCloseButton = true,
  customActionsLeft,
  customActionsRight,
  customActionsCenter,
}: ToolbarProps) {
  const classes = useStyles();
  const { disableDownloads } = useConfiguration();
  const layoutClasses = useLayoutStyles();

  const { showSnackbar } = useContext(SnackbarContext);

  const { formatMessage } = useIntl();

  const [loading, setLoading] = useState(false);

  const isReferencedFile = !!file?.published?.name;
  const fileDownload = get(getExistingFile(file, true), isReferencedFile ? 'id' : 'file.id');

  return (
    <div className={classes.daoDialogToolbar}>
      <Loading loading={loading} />
      <div
        className={classNames(
          classes.daoDialogToolbarInner,
          layoutClasses.flexSpaceBetween
        )}
      >
        {showCloseButton &&
          <Icon
            {...{
              title: formatMessage({ id: Message.CLOSE }),
              Component: CloseIcon,
              onClick: () => setItem(null),
            }}
            />
        }
        {customActionsLeft}
        <div style={{flex: 1}}/>
        <div className={layoutClasses.flex}>
          {[
            {
              title: Message.PREVIOUS_PAGE,
              Component: KeyboardArrowLeftIcon,
              onClick: previous,
              visible: !noView && previousEnabled,
              disabled: previousDisabled,
            },
            {
              title: Message.NEXT_PAGE,
              Component: KeyboardArrowRightIcon,
              onClick: next,
              visible: !noView && nextEnabled,
              disabled: nextDisabled,
            },
            {
              title: Message.ROTATE_LEFT,
              Component: RotateLeft,
              onClick: rotateLeft,
              visible: !noView && !noAction,
              disabled: zoomInDisabled,
            },
            {
              title: Message.ROTATE_RIGHT,
              Component: RotateRight,
              onClick: rotateRight,
              visible: !noView && !noAction,
              disabled: zoomInDisabled,
            },
            {
              title: Message.ZOOM_IN,
              Component: ZoomInIcon,
              onClick: zoomIn,
              visible: !noView && !noAction,
              disabled: zoomInDisabled,
            },
            {
              title: Message.ZOOM_OUT,
              Component: ZoomOutIcon,
              onClick: zoomOut,
              visible: !noView && !noAction,
              disabled: zoomOutDisabled,
            },
            {
              title: Message.CENTER,
              Component: ZoomOutMapIcon,
              onClick: reset,
              visible: !noView && !noAction,
            },
            {
              title: Message.COPY_LINK,
              Component: ShareIcon,
              onClick: async () => {
                try {
                  await navigator.clipboard.writeText(location.toString());
                  showSnackbar(formatMessage({ id: Message.COPY_LINK_SUCCESS }), SnackbarVariant.INFO, true);
                } catch(error) {
                  showSnackbar(formatMessage({ id: Message.COPY_LINK_ERROR }), SnackbarVariant.ERROR, false);
                }
              },
              visible: !noView && !noAction,
            },
            {
              title: Message.DOWNLOAD,
              Component: GetAppIcon,
              onClick: async () => {
                setLoading(true);
                const ok = await downloadFile(
                  fileDownload,
                  `dao_${item.id}.jpg`,
                  isReferencedFile,
                );
                if (!ok) {
                  showSnackbar(
                    formatMessage({ id: Message.ERROR_DOWNLOAD_FILE }),
                    SnackbarVariant.ERROR
                  );
                }
                setLoading(false);
              },
              visible: fileDownload,
              disabled: disableDownloads ? true : loading,
            },
            // {
            //   title: Message.DOWNLOAD_ALL,
            //   Component: InsertDriveFileIcon,
            //   onClick: () => null,
            // },
          ].map(
            ({ visible, title, ...icon }) =>
              visible && (
                <Icon
                  {...{
                    key: title,
                    ...icon,
                    title: formatMessage({ id: title }),
                  }}
                />
              )
          )}
          {customActionsCenter}
        </div>
        <div style={{flex: 1}}/>
        {customActionsRight}
        <Icon
          {...{
            title: '',
            Component: MenuIcon,
            onClick: () => setOpen(!open),
            className: classes.daoDialogMenu,
          }}
        />
      </div>
    </div>
  );
}
