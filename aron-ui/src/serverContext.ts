declare global {
  interface Window {
    /** Injected by the server into the SPA shell; absent in the Vite dev server. */
    serverContextPath?: string;
  }
}

const injected = window.serverContextPath;

/**
 * Effective public prefix of the application: "" at the URL root, "/aron" under
 * a subpath. API base URL and router basename derive from it. The token guard
 * covers a statically served build where the server never substituted the
 * shell template.
 */
export const serverContextPath =
  injected !== undefined && !injected.startsWith("__") ? injected : "";
