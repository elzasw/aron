import {
  useMemo,
  useImperativeHandle,
  Ref,
  useRef,
  useEffect,
  useState,
  useContext,
} from 'react';
import { unstable_batchedUpdates } from 'react-dom';
import * as Yup from 'yup';
import { noop } from 'lodash';
import { v4 as uuidv4 } from 'uuid';
import { useEventCallback } from 'utils/event-callback-hook';
import { DomainObject } from 'common/common-types';
import { FormHandle } from 'composite/form/form-types';
import {
  NavigationContext,
  Prompt,
} from 'composite/navigation/navigation-context';
import {
  DetailProps,
  DetailHandle,
  DetailMode,
  RefreshListener,
} from './detail-types';
import { DetailContext } from './detail-context';
import { DetailToolbar } from './detail-toolbar';
import { DetailContainer } from './detail-container';
import { useIntl } from 'react-intl';
import { SnackbarContext } from 'composite/snackbar/snackbar-context';
import { SnackbarVariant } from 'composite/snackbar/snackbar-types';
import { EmptyComponent } from 'utils/empty-component';
import { EvidenceContext } from 'composite/evidence/evidence-context';

export function useDetail<OBJECT extends DomainObject>(
  options: DetailProps<OBJECT>,
  ref: Ref<DetailHandle<OBJECT>>
) {
  const formRef = useRef<FormHandle<OBJECT>>(null);

  const props: Required<DetailProps<OBJECT>> = {
    ToolbarComponent: DetailToolbar,
    ContainerComponent: DetailContainer,
    FieldsComponent: EmptyComponent,
    GeneralFieldsComponent: EmptyComponent,
    onPersisted: noop,
    initNewItem: defaultInitNewItem,
    showErrorPanel: false,
    validationSchema: Yup.object<OBJECT>(),
    ...options,
    toolbarProps: {
      ...options.toolbarProps,
    },
  };

  const intl = useIntl();
  const { showSnackbar } = useContext(SnackbarContext);

  const confirmTitle = intl.formatMessage({
    id: 'EAS_DETAIL_LEAVE_DIALOG_TITLE',
    defaultMessage: 'Zanechání změn',
  });
  const confirmText = intl.formatMessage({
    id: 'EAS_DETAIL_LEAVE_DIALOG_TEXT',
    defaultMessage: 'Skutečně chcete opustit rozpracované změny?',
  });
  const prompt: Prompt = useMemo(
    () => ({
      title: confirmTitle,
      text: confirmText,
      clearCallback: () => {
        formRef.current?.resetValidation();
      },
    }),
    [confirmTitle, confirmText]
  );

  const { registerPrompt, unregisterPrompt } = useContext(NavigationContext);
  const { tableRef, detailRef } = useContext(EvidenceContext);

  const [mode, setMode] = useState<DetailMode>(DetailMode.NONE);
  const refreshListeners = useRef<RefreshListener[]>([]);

  const setActive = useEventCallback(async (id: string | null) => {
    if (id !== null) {
      if (id !== options.source.data?.id) {
        const data = await options.source.get(id);

        if (data !== undefined) {
          setMode(DetailMode.VIEW);
        }
      }
    } else {
      options.source.reset();
      setMode(DetailMode.NONE);
    }
  });

  const refresh = useEventCallback(() => {
    options.source.refresh();

    refreshListeners.current.forEach((l) => l());
    formRef.current?.resetValidation();
  });

  const refreshAll = useEventCallback(() => {
    if (mode === DetailMode.VIEW) {
      options.source.refresh();
      refreshListeners.current.forEach((l) => l());

      formRef.current?.resetValidation();
      props.onPersisted(options.source.data?.id ?? null);
    }
  });

  const startNew = useEventCallback((data?: OBJECT) => {
    if (options.source.data !== null) {
      options.source.reset();
    }

    // needs to do the form initialization in next frame, because the data source reset will trigger form load of empty data
    requestAnimationFrame(() => {
      formRef.current?.setFieldValues({ ...props.initNewItem(), ...data });

      setMode(DetailMode.NEW);
      formRef.current?.resetValidation();
      registerPrompt(prompt);
    });
  });

  const startEditing = useEventCallback(() => {
    if (options.source.data !== null) {
      setMode(DetailMode.EDIT);
      registerPrompt(prompt);
    }
  });

  const cancelEditing = useEventCallback(() => {
    formRef.current?.resetValidation();

    if (mode === DetailMode.NEW) {
      const id = tableRef.current?.activeRow;

      if (id) {
        setMode(DetailMode.VIEW);
        detailRef.current?.setActive(id);
      } else {
        setMode(DetailMode.NONE);
        formRef.current?.clearForm();
      }

      unregisterPrompt(prompt);
    } else if (mode === DetailMode.EDIT) {
      setMode(DetailMode.VIEW);
      unregisterPrompt(prompt);
      formRef.current?.setFieldValues(options.source.data!);
    }
  });

  const save = useEventCallback(async () => {
    if (formRef.current != null) {
      const errors = await validate();
      if (errors.length > 0) {
        return;
      }

      if (mode === DetailMode.NEW) {
        const values = formRef.current.getFieldValues();
        const data = await options.source.create(values);

        unregisterPrompt(prompt);

        if (data !== undefined) {
          unstable_batchedUpdates(() => {
            setActive(data.id);
            setMode(DetailMode.VIEW);
            props.onPersisted(data.id);
          });
        }
      } else if (mode === DetailMode.EDIT) {
        const values = formRef.current.getFieldValues();
        const data = await options.source.update(values, options.source.data!);

        unregisterPrompt(prompt);

        if (data !== undefined) {
          setMode(DetailMode.VIEW);
          props.onPersisted(data.id);
        }
      }
    }
  });

  const del = useEventCallback(async () => {
    if (mode === DetailMode.VIEW && options.source.data !== null) {
      await options.source.del(options.source.data.id);

      options.source.reset();
      formRef.current?.resetValidation();
      setMode(DetailMode.NONE);
      props.onPersisted(null);
    }
  });

  const validate = useEventCallback(async () => {
    if (formRef.current != null) {
      const errors = await formRef.current.validateForm();

      const isValid = errors.length === 0;

      if (isValid) {
        const message = intl.formatMessage({
          id: 'EAS_DETAIL_VALIDATION_MSG_SUCCESS',
          defaultMessage: 'Formulář je v pořádku.',
        });
        showSnackbar(message, SnackbarVariant.SUCCESS);
      } else {
        const message = intl.formatMessage({
          id: 'EAS_DETAIL_VALIDATION_MSG_ERROR',
          defaultMessage: 'Formulář obsahuje chyby.',
        });
        showSnackbar(message, SnackbarVariant.ERROR);
      }

      return errors;
    }

    return [];
  });

  const addRefreshListener = useEventCallback((listener: RefreshListener) => {
    refreshListeners.current = [...refreshListeners.current, listener];
  });

  const removeRefreshListener = useEventCallback(
    (listener: RefreshListener) => {
      refreshListeners.current = refreshListeners.current.filter(
        (l) => l !== listener
      );
    }
  );

  const context: DetailContext<OBJECT> = useMemo(
    () => ({
      formRef: formRef.current,
      source: props.source,
      FieldsComponent: props.FieldsComponent,
      onPersisted: props.onPersisted,
      isExisting: mode === DetailMode.VIEW || mode === DetailMode.EDIT,
      mode,
      showErrorPanel: props.showErrorPanel,
      refresh,
      refreshAll,
      setActive,
      startNew,
      startEditing,
      cancelEditing,
      del,
      save,
      validate,
      addRefreshListener,
      removeRefreshListener,
    }),
    [
      formRef,
      props.source,
      props.FieldsComponent,
      props.onPersisted,
      props.showErrorPanel,
      mode,
      refresh,
      refreshAll,
      setActive,
      startNew,
      startEditing,
      cancelEditing,
      del,
      save,
      validate,
      addRefreshListener,
      removeRefreshListener,
    ]
  );

  useEffect(() => {
    formRef.current?.setFieldValues(options.source.data ?? ({} as any));
  }, [options.source.data]);

  useImperativeHandle(ref, () => context, [context]);

  const editing = mode === DetailMode.NEW || mode === DetailMode.EDIT;

  return { props, context, formRef, editing };
}

function defaultInitNewItem(): any {
  return {
    id: uuidv4(),
  };
}
