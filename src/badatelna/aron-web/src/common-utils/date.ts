import { get } from 'lodash';

import { DateFormat } from '../enums';

const locale = 'cs'

export const getISOStringFromYear = (year: number) =>
  new Date(year, 1).toISOString();

export const getYearFromISOString = (ISO: string) =>
  new Date(ISO).getFullYear();

export const formatDate = (dateString: string) =>
  new Date(dateString).toLocaleDateString(locale)

export const formatDateTime = (dateString: string) =>
  new Date(dateString).toLocaleString(locale, {
    year: 'numeric', 
    month: 'numeric', 
    day: 'numeric', 
    hour: 'numeric', 
    minute: 'numeric',
  })

export const formatYear = (dateString: string) =>
  new Date(dateString).getFullYear().toString()

export const formatYearMonth = (dateString: string) =>
  new Date(dateString).toLocaleDateString(locale, {
    month: 'numeric', 
    year: 'numeric'
  })

export const formatCentury = (dateString: string) =>
  `${Math.ceil(new Date(dateString).getFullYear() / 100)}. století`;

const formatUnitDatePart = (value: string, format: string) => {
  switch (format) {
    case DateFormat.D:
      return formatDate(value);
    case DateFormat.YM:
      return formatYearMonth(value);
    case DateFormat.Y:
      return formatYear(value);
    case DateFormat.C:
      return formatCentury(value);
    default:
      return formatDateTime(value);
  }
};

const defaultUnitDateFormat = (format: any) => format || DateFormat.D;

const applyEstimated = (value: string, estimated: boolean) =>
  `${estimated ? '[' : ''}${value}${estimated ? ']' : ''}`;

export const formatUnitDate = (value: string) => {
  try {
    const parsed = JSON.parse(value);

    const { from, to, format, valueFromEstimated, valueToEstimated } = parsed;

    let format1;
    let format2;

    if (/-/.test(format)) {
      const matched = format.match(/^(.+)-(.+)/);

      format1 = defaultUnitDateFormat(matched[1]);
      format2 = defaultUnitDateFormat(matched[2]);
    } else {
      format1 = defaultUnitDateFormat(format);
      format2 = defaultUnitDateFormat(format);
    }

    const fromText = applyEstimated(
      formatUnitDatePart(from, format1),
      valueFromEstimated
    );
    const toText = applyEstimated(
      formatUnitDatePart(to, format2),
      valueToEstimated
    );

    return `${fromText !== toText ? `${fromText} - ${toText}` : fromText}`;
  } catch (e) {
    console.log(e);
    return '';
  }
};

export const getUnitDatePart = (value: string, part: string) => {
  try {
    const parsed = JSON.parse(value);

    return get(parsed, part);
  } catch (e) {
    console.log(e);
    return null;
  }
};
