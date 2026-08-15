import { FluentProvider, webLightTheme } from "@fluentui/react-components";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import React from "react";
import ReactDOM from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import App from "./App";
import "./i18n";
import "./index.css";
import { serverContextPath } from "./serverContext";

const queryClient = new QueryClient();

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <FluentProvider theme={webLightTheme}>
      <QueryClientProvider client={queryClient}>
        <BrowserRouter basename={serverContextPath}>
          <App />
        </BrowserRouter>
      </QueryClientProvider>
    </FluentProvider>
  </React.StrictMode>,
);
