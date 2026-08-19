import react from "@vitejs/plugin-react";
import { defineConfig, type Plugin } from "vite";

// The built index.html is not served raw: IndexController uses it as the SPA
// shell template and substitutes the effective deployment prefix per request
// into <base href> and window.serverContextPath. The tokens are injected only
// into the BUILT page so the dev server keeps a plain, working one.
function injectDeploymentPrefixTokens(): Plugin {
  const anchor = '<base href="/" />';
  // the deployment's own name and default language belong to the first paint too:
  // without them the tab shows this file's placeholder until React has loaded
  const tokens: [RegExp, string][] = [
    [/<html lang="[^"]*">/, '<html lang="__PAGE_LANG__">'],
    [/<title>[^<]*<\/title>/, "<title>__PAGE_TITLE__</title>"],
  ];
  return {
    name: "aron-context-path-tokens",
    apply: "build",
    transformIndexHtml(html) {
      if (!html.includes(anchor)) {
        throw new Error(`index.html: expected ${anchor} - cannot tokenize the shell`);
      }
      let tokenized = html.replace(anchor, '<base href="__CONTEXT_PATH_HTML__/" />');
      for (const [pattern, replacement] of tokens) {
        if (!pattern.test(tokenized)) {
          throw new Error(`index.html: expected ${pattern} - cannot tokenize the shell`);
        }
        tokenized = tokenized.replace(pattern, replacement);
      }
      return {
        html: tokenized,
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
