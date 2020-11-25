import { parseISO, formatISO } from 'date-fns';

export function parseISOSafe(date: string | null | undefined) {
  if (date == null) {
    return date;
  } else {
    return parseISO(date);
  }
}

export function parseISOTime(time: string) {
  const now = new Date();
  const dateTime = formatISO(now, { representation: 'date' }) + 'T' + time;
  return parseISO(dateTime);
}

export function parseISOTimeSafe(date: string | null | undefined) {
  if (date == null) {
    return date;
  } else {
    return parseISOTime(date);
  }
}
