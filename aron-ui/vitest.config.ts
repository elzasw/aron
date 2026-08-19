import react from "@vitejs/plugin-react";
import { defineConfig } from "vitest/config";

// Component tests run in jsdom; they exercise the parts of the UI with real
// branching (language negotiation, per-kind item rendering) rather than markup.
// Kept out of vite.config.ts so the app build carries no test configuration.
export default defineConfig({
  plugins: [react()],
  test: {
    environment: "jsdom",
    globals: true,
    setupFiles: ["./vitest.setup.ts"],
    // Fluent UI's ESM entry pulls in CommonJS focus-management packages, which
    // the ESM loader cannot read as-is. Prebundling with esbuild fixes the
    // interop and is far cheaper than transforming Fluent UI on every run.
    deps: { optimizer: { web: { enabled: true, include: ["@fluentui/react-components"] } } },
    include: ["src/**/*.test.ts", "src/**/*.test.tsx"],
    restoreMocks: true,
  },
});
