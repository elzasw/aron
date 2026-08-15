import react from "@vitejs/plugin-react";
import { defineConfig, type Plugin } from "vite";

// The built index.html is not served raw: IndexController uses it as the SPA
// shell template and substitutes the effective deployment prefix per request
// into <base href> and window.serverContextPath. The tokens are injected only
// into the BUILT page so the dev server keeps a plain, working one.
function injectDeploymentPrefixTokens(): Plugin {
  const anchor = '<base href="/" />';
  return {
    name: "aron-context-path-tokens",
    apply: "build",
    transformIndexHtml(html) {
      if (!html.includes(anchor)) {
        throw new Error(`index.html: expected ${anchor} - cannot tokenize the shell`);
      }
      return {
        html: html.replace(anchor, '<base href="__CONTEXT_PATH_HTML__/" />'),
        tags: [
          {
            tag: "script",
            children: 'window.serverContextPath = "__CONTEXT_PATH_JS__";',
            injectTo: "head-prepend",
          },
        ],
      };
    },
  };
}

// Dev server proxies the API to the Spring Boot backend so the SPA and API
// share an origin - ideally the dev mode (`mvn spring-boot:run -Pdev` in
// aron-core), which needs no external services.
export default defineConfig({
  // Relative asset URLs in the built index.html; they resolve against the
  // server-injected <base href>, so one build works at "/" and under a subpath.
  base: "./",
  plugins: [react(), injectDeploymentPrefixTokens()],
  server: {
    port: 5173,
    proxy: {
      "/api": "http://localhost:8080",
    },
  },
});
