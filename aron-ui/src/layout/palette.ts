/**
 * Default primary palette of the original portal (its theme's primary.dark and
 * primary.main): the slate of the header, the result-card icon tiles and the
 * primary buttons. Section accents are configured separately (menu colors of
 * /api/v1/ui/config).
 *
 * A deployment can have its own primary colour (pageTemplate.yaml
 * `primaryColor`): the server injects the two custom properties into the SPA
 * shell, so the first paint is already the deployment's colour - the values
 * here are the fallback, i.e. the portal default. Written as var() references
 * because they are read at paint time; the constants themselves are static, as
 * Fluent's makeStyles requires.
 *
 * Its own module so header parts can share it without importing each other.
 */
export const PRIMARY_DARK = "var(--aron-primary-dark, hsl(210, 20%, 20%))";

export const PRIMARY_MAIN = "var(--aron-primary-main, hsl(210, 20%, 30%))";
