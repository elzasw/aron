import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, type RenderResult } from "@testing-library/react";
import type { ReactNode } from "react";
import { MemoryRouter } from "react-router-dom";

/**
 * The providers every rendered component needs: a react-query client and a
 * router. Retries are off, so a test asserting an error state reaches it at
 * once instead of waiting out the default backoff, and each render gets its own
 * client, so nothing is served from the previous test's cache.
 *
 * A test that needs its own route table renders the {@link Routes} itself and
 * passes it as `ui`; `initialEntries` is the router's, verbatim.
 */
export function renderWithProviders(
  ui: ReactNode,
  { initialEntries }: { initialEntries?: string[] } = {},
): RenderResult {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={initialEntries}>{ui}</MemoryRouter>
    </QueryClientProvider>,
  );
}
