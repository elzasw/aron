import { ApuPartItemDataType } from '../enums';
import { formatDate, formatDateTime } from './date';

export const formatApuPartItemValue = (
  value: string,
  dataType: ApuPartItemDataType
) => {
  if (dataType === ApuPartItemDataType.UNITDATE) {
    let result;

    try {
      result = JSON.parse(value);
    } catch (e) {
      console.log(e);
      result = undefined;
    }

    if (result) {
      const { from, to, format } = result;
      const fn = format === 'D' ? formatDate : formatDateTime;
      return from !== to ? `${fn(from)} - ${fn(to)}` : fn(from);
    }

    return '';
  }
  return value;
};
