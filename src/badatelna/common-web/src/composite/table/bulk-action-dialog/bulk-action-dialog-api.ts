import { abortableFetch } from 'utils/abortable-fetch';
import { Filter } from 'common/common-types';

export interface ResultStatus {
  status: boolean;
  error?: string;
}

export async function bulkActionDialogApi(
  url: string,
  filters: Filter[],
  ids: string[],
  params?: Record<string, any>
): Promise<ResultStatus> {
  try {
    if (ids.length > 0) {
      await abortableFetch(url, {
        method: 'POST',
        headers: new Headers({
          'Content-Type': 'application/json',
        }),
        body: JSON.stringify({
          ids,
          ...params,
        }),
      }).json();
    } else {
      await abortableFetch(url, {
        method: 'POST',
        headers: new Headers({
          'Content-Type': 'application/json',
        }),
        body: JSON.stringify({
          filters,
          ...params,
        }),
      }).json();
    }

    return {
      status: true,
    };
  } catch (err) {
    console.log('Bulk action error: ', err);

    return {
      status: false,
      error: err.message ?? err,
    };
  }
}
