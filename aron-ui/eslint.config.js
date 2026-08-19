import js from "@eslint/js";
import jsxA11y from "eslint-plugin-jsx-a11y";
import reactHooks from "eslint-plugin-react-hooks";
import globals from "globals";
import tseslint from "typescript-eslint";

/**
 * Lint is the accessibility gate of the UI (doc/accessibility.md §5): a missing
 * label, a role misuse or a click handler on a non-interactive element fails
 * the build instead of a review. Formatting is deliberately not linted - it is
 * not what breaks a screen reader.
 */
export default tseslint.config(
  {
    // generated client (regenerated on every build), build output, installed Node
    ignores: ["src/api/generated/**", "dist/**", "target/**", ".node/**"],
  },
  js.configs.recommended,
  ...tseslint.configs.recommended,
  jsxA11y.flatConfigs.recommended,
  reactHooks.configs.flat.recommended,
  {
    files: ["**/*.{ts,tsx}"],
    languageOptions: {
      globals: { ...globals.browser },
    },
    rules: {
      // an unused import is dead weight; an unused argument prefixed with _ is intent
      "@typescript-eslint/no-unused-vars": ["error", { argsIgnorePattern: "^_" }],
    },
  },
  {
    files: ["**/*.test.{ts,tsx}", "vitest.setup.ts"],
    languageOptions: {
      globals: { ...globals.node },
    },
  },
  {
    files: ["*.config.{ts,js}"],
    languageOptions: {
      globals: { ...globals.node },
    },
  },
);
