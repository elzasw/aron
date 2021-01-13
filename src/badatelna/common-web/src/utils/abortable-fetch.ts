export interface AbortableFetch {
  response: Promise<Response>;
  json: () => Promise<any>;
  text: () => Promise<any>;
  none: () => Promise<void>;
  abort: () => void;
}

export function abortableFetch(
  request: RequestInfo,
  opts?: RequestInit
): AbortableFetch {
  const controller = new AbortController();
  const signal = controller.signal;

  const response = fetch(request, { ...opts, signal });

  return {
    abort: () => controller.abort(),
    response,
    json: async () => {
      const res = await response;
      const data = await res.json();
      if (!res.ok) {
        throw data;
      }

      return data;
    },
    text: async () => {
      const res = await response;
      const data = await res.text();
      if (!res.ok) {
        throw data;
      }

      return data;
    },
    none: async () => {
      const res = await response;
      if (res.status === 403) {
        throw { message: 'Přístup odepřen' };
      } else if (!res.ok) {
        throw await res.json();
      }

      return;
    },
  };
}
