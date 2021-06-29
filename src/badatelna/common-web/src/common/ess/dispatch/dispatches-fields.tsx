import React from 'react';
import { FormattedMessage } from 'react-intl';
import { FormPanel } from 'composite/form/fields/form-panel';
import { Identifier } from '../common/identifier';
import { useDispatchMethods, useDispatchStates } from '../ess-api';
import { FormSelect } from 'composite/form/fields/form-select';

/**
 * fixme: add document, subject
 */
export function DispatchesFields() {
  const methods = useDispatchMethods();
  const states = useDispatchStates();

  return (
    <>
      <FormPanel
        label={
          <FormattedMessage
            id="ESS_DISPATCHES_PANEL_TITLE"
            defaultMessage="Vypravení"
          />
        }
      >
        <FormSelect
          name="state"
          label={
            <FormattedMessage
              id="ESS_DISPATCHES_FIELD_LABEL_STATE"
              defaultMessage="Stav"
            />
          }
          valueIsId={true}
          source={states}
          tooltipMapper={(o) => o.name}
        />
        <FormSelect
          name="method"
          label={
            <FormattedMessage
              id="ESS_DISPATCHES_FIELD_LABEL_METHOD"
              defaultMessage="Type"
            />
          }
          valueIsId={true}
          source={methods}
          tooltipMapper={(o) => o.name}
        />
      </FormPanel>
      <Identifier />
    </>
  );
}
