import { ResponseError } from "../api/generated";

/**
 * Global collector of failed API requests, wired to the query client's error
 * callbacks (main.tsx) and rendered by ApiErrorBar. The working principle:
 * every failed request must surface visibly so it gets resolved - errors are
 * never swallowed silently. A future deployment switch (ui/config) may reduce
 * production display to a generic notice; the collection itself stays.
 */
export interface ApiError {
  id: number;
  /** Failing endpoint (method + URL path) when known. */
  request?: string;
  /** HTTP status, when the server answered. */
  status?: number;
  /** Technical detail: the server's error message or the exception text. */
  detail: string;
}

let nextId = 1;
/** Immutable snapshot - replaced on every change (useSyncExternalStore). */
let errors: ApiError[] = [];
const listeners = new Set<() => void>();

function notify(): void {
  listeners.forEach((listener) => listener());
}

export function subscribeApiErrors(listener: () => void): () => void {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

export function getApiErrors(): ApiError[] {
  return errors;
}

export function dismissApiError(id: number): void {
  errors = errors.filter((error) => error.id !== id);
  notify();
}

/** Error callback of the query/mutation caches. */
export function reportApiError(error: unknown): void {
  if (error instanceof ResponseError) {
    const request = urlPath(error.response.url);
    const entry = addOrReplace({
      request,
      status: error.response.status,
      detail: `HTTP ${error.response.status}`,
    });
    // best effort: enrich with the server's error message (async body read)
    void error.response
      .clone()
      .json()
      .then((body: unknown) => {
        const message = (body as { message?: string; error?: string } | null)?.message
          ?? (body as { error?: string } | null)?.error;
        if (message) {
          update(entry.id, `HTTP ${error.response.status} - ${message}`);
        }
      })
      .catch(() => {
        /* no JSON body - the status alone stays */
      });
    return;
  }
  addOrReplace({ detail: error instanceof Error ? error.message : String(error) });
}

/** One entry per (request, status): a re-failing request updates instead of stacking. */
function addOrReplace(candidate: Omit<ApiError, "id">): ApiError {
  const existing = errors.find(
    (error) => error.request === candidate.request && error.status === candidate.status,
  );
  if (existing) {
    return existing;
  }
  const entry: ApiError = { id: nextId++, ...candidate };
  errors = [...errors, entry];
  notify();
  return entry;
}

function update(id: number, detail: string): void {
  errors = errors.map((error) => (error.id === id ? { ...error, detail } : error));
  notify();
}

function urlPath(url: string): string {
  try {
    return new URL(url, window.location.origin).pathname;
  } catch {
    return url;
  }
}
