import { useCallback, useMemo } from 'react';
import { defineMessages } from 'react-intl';

export function useDefaultValidationMessages() {
  const messages = defineMessages({
    EAS_VALIDATION_MSG_REQUIRED: {
      id: 'EAS_VALIDATION_MSG_REQUIRED',
      defaultMessage: 'Musí být vyplněné',
    },
    EAS_VALIDATION_MSG_IS_THE_SAME: {
      id: 'EAS_VALIDATION_MSG_IS_THE_SAME',
      defaultMessage: 'Neshoduje se',
    },
    EAS_VALIDATION_MSG_MEETS_RULES: {
      id: 'EAS_VALIDATION_MSG_MEETS_RULES',
      defaultMessage: 'Nesplňuje pravidla',
    },
    EAS_VALIDATION_MSG_MEETS_FORMAT: {
      id: 'EAS_VALIDATION_MSG_MEETS_FORMAT',
      defaultMessage: 'Chybný formát',
    },
    EAS_VALIDATION_MSG_DOES_NOT_EXIST: {
      id: 'EAS_VALIDATION_MSG_DOES_NOT_EXIST',
      defaultMessage: 'Už existuje',
    },
    EAS_VALIDATION_MSG_MAX_LENGTH: {
      id: 'EAS_VALIDATION_MSG_MAX_LENGTH',
      defaultMessage: 'Musí byt kratší než {max} znaků',
    },
  });

  const requiredField =
    messages.EAS_VALIDATION_MSG_REQUIRED.id +
    ';;' +
    messages.EAS_VALIDATION_MSG_REQUIRED.defaultMessage; // Musí být vyplněné
  const isTheSame =
    messages.EAS_VALIDATION_MSG_IS_THE_SAME.id +
    ';;' +
    messages.EAS_VALIDATION_MSG_IS_THE_SAME.defaultMessage; // Neshoduje se
  const meetsRules =
    messages.EAS_VALIDATION_MSG_MEETS_RULES.id +
    ';;' +
    messages.EAS_VALIDATION_MSG_MEETS_RULES.defaultMessage; // Nesplňuje pravidla
  const meetsFormat =
    messages.EAS_VALIDATION_MSG_MEETS_FORMAT.id +
    ';;' +
    messages.EAS_VALIDATION_MSG_MEETS_FORMAT.defaultMessage; // Zlý formát
  const doesNotExist =
    messages.EAS_VALIDATION_MSG_DOES_NOT_EXIST.id +
    ';;' +
    messages.EAS_VALIDATION_MSG_DOES_NOT_EXIST.defaultMessage; // Už existuje
  const maxLength = useCallback(
    ({ max }: { max: number }) => ({
      key:
        messages.EAS_VALIDATION_MSG_MAX_LENGTH.id +
        ';;' +
        messages.EAS_VALIDATION_MSG_MAX_LENGTH.defaultMessage,
      values: { max },
    }),
    [
      messages.EAS_VALIDATION_MSG_MAX_LENGTH.defaultMessage,
      messages.EAS_VALIDATION_MSG_MAX_LENGTH.id,
    ]
  ); // Musí byt kratší než {length} znaků

  return useMemo(
    () => ({
      requiredField,
      maxLength,
      isTheSame,
      meetsRules,
      meetsFormat,
      doesNotExist,
    }),
    [doesNotExist, isTheSame, maxLength, meetsFormat, meetsRules, requiredField]
  );
}
