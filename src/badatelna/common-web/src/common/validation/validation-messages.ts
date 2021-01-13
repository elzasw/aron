import { useCallback, useMemo } from 'react';
import { defineMessages } from 'react-intl';

export function useDefaultValidationMessages() {
  const requiredField = 'EAS_VALIDATION_MSG_REQUIRED'; // Musí být vyplněné
  const isTheSame = 'EAS_VALIDATION_MSG_IS_THE_SAME'; // Neshoduje se
  const meetsRules = 'EAS_VALIDATION_MSG_MEETS_RULES'; // Nesplňuje pravidla
  const meetsFormat = 'EAS_VALIDATION_MSG_MEETS_FORMAT'; // Zlý formát
  const doesNotExist = 'EAS_VALIDATION_MSG_DOES_NOT_EXIST'; // Už existuje
  const maxLength = useCallback(
    ({ max }: { max: number }) => ({
      key: 'EAS_VALIDATION_MSG_MAX_LENGTH',
      values: { max },
    }),
    []
  ); // Musí byt kratší než {length} znaků

  defineMessages({
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
      defaultMessage: 'Zlý formát',
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
