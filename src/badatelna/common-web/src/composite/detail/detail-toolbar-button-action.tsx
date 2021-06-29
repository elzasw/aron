import React, { useContext, useRef } from 'react';
import { useEventCallback } from 'utils/event-callback-hook';
import { DetailContext } from 'composite/detail/detail-context';
import { usePrompts } from 'composite/prompt/prompt-register-hook';
import { PromptContext } from 'composite/prompt/prompt-context';
import { DetailToolbarButton } from './detail-toolbar-button';
import { DetailMode, DetailToolbarButtonActionProps } from './detail-types';
import { stubTrue } from 'lodash';
import { useIntl } from 'react-intl';
import { SnackbarContext } from 'composite/snackbar/snackbar-context';
import { AbortableFetch } from 'utils/abortable-fetch';
import { SnackbarVariant } from 'composite/snackbar/snackbar-types';

export function DetailToolbarButtonAction<T = unknown>({
  promptKey,
  apiCall,
  buttonLabel,
  buttonTooltip,
  buttonDisabled = false,
  dialogText,
  dialogTitle,
  dialogWidth,
  FormFields,
  formValidationSchema,
  formInitialValues,
  successMessage,
  errorMessage,
  modes = [DetailMode.VIEW],
  onSuccess: providedOnSuccess,
  onError: providedOnError,
  onResult: providedOnResult,
  onShouldShow = stubTrue,
  ButtonComponent = DetailToolbarButton,
  buttonProps = {},
  ...restProps
}: DetailToolbarButtonActionProps<T>) {
  const intl = useIntl();
  const { showSnackbar } = useContext(SnackbarContext);
  const { testPrompt } = useContext(PromptContext);
  const { source, onPersisted, mode } = useContext(DetailContext);

  const fetch = useRef<AbortableFetch | null>(null);

  usePrompts(
    [
      {
        key: promptKey,
        dialogTitle,
        dialogText,
        dialogWidth,
        FormFields,
        formValidationSchema,
        formInitialValues,
      },
    ],
    [
      promptKey,
      dialogTitle,
      dialogText,
      dialogWidth,
      FormFields,
      formValidationSchema,
      formInitialValues,
    ]
  );

  const defaultOnSuccess = useEventCallback(async () => {
    onPersisted(source.data!.id);
    await source.refresh();
  });

  const defaultOnResult = useEventCallback(() => {
    const message =
      successMessage ??
      intl.formatMessage({
        id: 'EAS_DETAIL_ACTION_MSG_SUCCESS',
        defaultMessage: 'Akce byla úspěšně vykonána.',
      });

    showSnackbar(message, SnackbarVariant.SUCCESS);
  });

  const onSuccess = providedOnSuccess ?? defaultOnSuccess;
  const onError = providedOnError ?? defaultOnError;

  const wrappedApiCall = useEventCallback(async (formData: any) => {
    try {
      source.setLoading(true);
      if (fetch.current !== null) {
        fetch.current.abort();
      }

      fetch.current = apiCall(source.data.id, formData);

      const response = await fetch.current.raw();

      if (providedOnResult !== undefined) {
        const json = await response.json();
        await providedOnResult(json);
      } else {
        defaultOnResult();
      }

      source.setLoading(false);

      await onSuccess();
    } catch (err) {
      source.setLoading(false);

      if (err.name !== 'AbortError') {
        const message =
          errorMessage !== undefined
            ? errorMessage.replace('{detail}', err.message)
            : intl.formatMessage(
                {
                  id: 'EAS_DETAIL_ACTION_FINISH_ERROR',
                  defaultMessage: 'Chyba volání akce: {detail}',
                },
                { detail: err.message }
              );

        showSnackbar(message, SnackbarVariant.ERROR);
        await onError(err);
      }
      return undefined;
    }
  });

  const handleConfirm = useEventCallback(async () => {
    testPrompt({
      key: promptKey,
      callback: wrappedApiCall,
    });
  });

  return (
    <>
      {modes.includes(mode) && onShouldShow() && (
        <ButtonComponent
          {...buttonProps}
          disabled={buttonDisabled}
          label={buttonLabel}
          tooltip={buttonTooltip}
          onClick={handleConfirm}
          {...restProps}
        />
      )}
    </>
  );
}

async function defaultOnError(err: Error) {
  throw err;
}
