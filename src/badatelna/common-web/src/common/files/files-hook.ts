import { useMemo, useState, useContext } from 'react';
import { useIntl } from 'react-intl';
import { FileRef } from 'common/common-types';
import { SnackbarContext } from 'composite/snackbar/snackbar-context';
import { SnackbarVariant } from 'composite/snackbar/snackbar-types';
import { useEventCallback } from 'utils/event-callback-hook';
import { FilesContext } from './files-context';
import { callUploadFile } from './files-api';

export function useFiles(url: string, maxUploadSize?: number) {
  const intl = useIntl();
  const { showSnackbar } = useContext(SnackbarContext);

  const [loading, setLoading] = useState(false);

  const getFileUrl = useEventCallback((id: string) => `${url}/${id}`);

  const uploadFile = useEventCallback(async (file: File) => {
    if (maxUploadSize !== undefined) {
      if (file.size > maxUploadSize) {
        const message = intl.formatMessage({
          id: 'EAS_FILES_MSG_ERROR_UPLOAD_SIZE',
          defaultMessage: 'Byla překročena maximální povolena velikost souboru',
        });

        showSnackbar(message, SnackbarVariant.ERROR);

        throw new Error('Max file size error');
      }
    }

    try {
      const fileRef: FileRef = await callUploadFile(url, file).json();

      const message = intl.formatMessage({
        id: 'EAS_FILES_MSG_SUCCESS',
        defaultMessage: 'Nahrávaní souboru dokončeno',
      });
      showSnackbar(message, SnackbarVariant.SUCCESS);

      setLoading(false);

      return fileRef;
    } catch (err) {
      setLoading(false);
      if (err.name !== 'AbortError') {
        let message = '';
        if (
          err.exception ===
          'org.springframework.web.multipart.MaxUploadSizeExceededException'
        ) {
          message = intl.formatMessage({
            id: 'EAS_FILES_MSG_ERROR_UPLOAD_SIZE',
            defaultMessage:
              'Byla překročena maximální povolena velikost souboru',
          });
        } else if (
          err.exception === 'cz.inqool.eas.common.exception.VirusFoundException'
        ) {
          message = intl.formatMessage({
            id: 'EAS_FILES_MSG_ERROR_VIRUS',
            defaultMessage: 'Soubor nebyl nahrán protože obsahuje virus',
          });
        } else if (
          err.exception ===
          'cz.inqool.eas.common.exception.ExtensionNotAllowedException'
        ) {
          message = intl.formatMessage({
            id: 'EAS_FILES_MSG_ERROR_NOT_ALLOWED',
            defaultMessage:
              'Soubor nebyl nahrán protože má nepovolenou příponu ',
          });
        } else {
          message = intl.formatMessage(
            {
              id: 'EAS_FILES_MSG_ERROR',
              defaultMessage: 'Chyba nahrávaní souboru: {detail}',
            },
            { detail: err.message }
          );
        }

        showSnackbar(message, SnackbarVariant.ERROR);

        throw err;
      }
    }
  });

  const context: FilesContext = useMemo(
    () => ({
      loading,
      url,
      getFileUrl,
      uploadFile,
    }),
    [loading, url, getFileUrl, uploadFile]
  );

  return { context };
}
