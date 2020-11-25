import React from 'react';
import { FormattedMessage } from 'react-intl';
import { FormPanel } from 'composite/form/fields/form-panel';
import { FormSelect } from 'composite/form/fields/form-select';
import { useLanguages } from './translations-api';
import { FormFileField } from 'composite/form/fields/form-file-field';

export function TranslationsFields() {
  const languages = useLanguages();

  return (
    <FormPanel
      label={
        <FormattedMessage
          id="EAS_TRANSLATIONS_PANEL_TITLE"
          defaultMessage="Překlad"
        />
      }
    >
      <FormSelect
        name="language"
        required
        label={
          <FormattedMessage
            id="EAS_TRANSLATIONS_FIELD_LANGUAGE"
            defaultMessage="Jazyk"
          />
        }
        source={languages}
        tooltipMapper={(o) => o.name}
        valueIsId={true}
      />
      <FormFileField
        name="content"
        required
        label={
          <FormattedMessage
            id="EAS_TRANSLATIONS_FIELD_CONTENT"
            defaultMessage="Soubor"
          />
        }
      />
    </FormPanel>
  );
}
