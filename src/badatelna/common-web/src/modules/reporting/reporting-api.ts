import { abortableFetch } from 'utils/abortable-fetch';

export function generateReport(url: string, code: string, input: any) {
  return abortableFetch(`${url}/${code}/generate`, {
    headers: new Headers({
      'Content-Type': 'application/json',
    }),
    method: 'POST',
    body: JSON.stringify(input),
  });
}

export function listReportDefinitions(url: string) {
  return abortableFetch(`${url}/definitions`, {
    headers: new Headers({
      'Content-Type': 'application/json',
    }),
    method: 'POST',
  });
}
