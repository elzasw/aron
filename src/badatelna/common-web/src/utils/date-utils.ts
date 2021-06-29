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

export function formatISOTime(date: Date) {
  const time = formatISO(date, { representation: 'time' });
  if (time.endsWith('Z')) {
    return time.slice(0, -1);
  } else if (time.includes('+')) {
    return time.slice(0, time.indexOf('+'));
  } else {
    return time;
  }
}

export function parseISOTimeSafe(date: string | null | undefined) {
  if (date == null) {
    return date;
  } else {
    return parseISOTime(date);
  }
}
