import { FluentProvider, webLightTheme } from "@fluentui/react-components";
import { MutationCache, QueryCache, QueryClient, QueryClientProvider } from "@tanstack/react-query";
import React from "react";
import ReactDOM from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import App from "./App";
import { reportApiError } from "./errors/apiErrors";
import ErrorBoundary from "./errors/ErrorBoundary";
import "./i18n";
import "./index.css";
import { serverContextPath } from "./serverContext";

// every failed request surfaces visibly (ApiErrorBar) - never swallowed silently
const queryClient = new QueryClient({
  queryCache: new QueryCache({ onError: reportApiError }),
  mutationCache: new MutationCache({ onError: reportApiError }),
});

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <FluentProvider theme={webLightTheme}>
      <ErrorBoundary>
        <QueryClientProvider client={queryClient}>
          <BrowserRouter basename={serverContextPath}>
            <App />
          </BrowserRouter>
        </QueryClientProvider>
      </ErrorBoundary>
    </FluentProvider>
  </React.StrictMode>,
);
