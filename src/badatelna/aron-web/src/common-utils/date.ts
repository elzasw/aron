import { format } from 'date-fns';

const dateFormat = 'dd. MM. yyyy';

const formatFn = (dateString: string, printFormat: string) =>
  format(new Date(dateString), printFormat);

export const formatDate = (dateString: string) =>
  formatFn(dateString, dateFormat);

export const formatDateTime = (dateString: string) =>
  formatFn(dateString, `${dateFormat} HH:mm`);
