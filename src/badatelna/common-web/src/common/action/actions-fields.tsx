import React from 'react';
import { FormattedMessage, useIntl } from 'react-intl';
import { FormPanel } from 'composite/form/fields/form-panel';
import { FormSelect } from 'composite/form/fields/form-select';
import { FormEditor } from 'composite/form/fields/form-editor';
import { useFormSelector } from 'composite/form/selectors/selector';
import { Action } from './actions-types';
import { ScriptType } from 'common/common-types';
import { useScriptTypes } from './actions-api';

export function ActionsFields() {
  const intl = useIntl();

  const scriptType = useFormSelector((data: Action) => data.scriptType);

  const types = useScriptTypes();

  return (
    <FormPanel
      label={
        <FormattedMessage
          id="EAS_ACTIONS_PANEL_TITLE"
          defaultMessage="Skript"
        />
      }
    >
      <FormSelect
        name="scriptType"
        required
        label={
          <FormattedMessage
            id="EAS_ACTIONS_FIELD_LABEL_SCRIPT_TYPE"
            defaultMessage="Skriptovací jazyk"
          />
        }
        helpLabel={intl.formatMessage({
          id: 'EAS_ACTIONS_FIELD_HELP_SCRIPT_TYPE',
          defaultMessage: ' ',
        })}
        source={types}
        tooltipMapper={(o) => o.name}
        valueIsId={true}
      />
      <FormEditor
        name="script"
        required
        disabled={scriptType == null}
        language={scriptType === ScriptType.GROOVY ? 'java' : 'javascript'}
        label={
          <FormattedMessage
            id="EAS_ACTIONS_FIELD_LABEL_SCRIPT"
            defaultMessage="Skript"
          />
        }
        helpLabel={intl.formatMessage({
          id: 'EAS_ACTIONS_FIELD_HELP_SCRIPT',
          defaultMessage: ' ',
        })}
      />
    </FormPanel>
  );
}
