/**
 * The viewport bands the layout is built on - one definition, so a rule in a
 * page and the decision in a component can never disagree about where a band
 * ends.
 *
 * "Stacked" is the small-screen arrangement: the pages leave the one-viewport
 * frame (see AppLayout), the document scrolls and the header scrolls away with
 * it. It is triggered by a **narrow** viewport (a phone held upright) and
 * equally by a **short** one (a phone held sideways, ~400px tall): a frame
 * that keeps header, breadcrumbs and footer on screen has nothing left for the
 * content there. Laptops stay above 600px even with the browser's own chrome.
 */
export const STACK_MAX_WIDTH = 860;
export const STACK_MAX_HEIGHT = 600;
/** From here a digitized record has room for tree | viewer | description (the old portal's lg). */
export const THREE_PANES_MIN_WIDTH = 1280;

/** Narrow or short: the stacked arrangement. */
export const STACKED = `(max-width: ${STACK_MAX_WIDTH}px), (max-height: ${STACK_MAX_HEIGHT}px)`;
/** Short, whatever the width - where a stacked pane may take the whole viewport height. */
export const SHORT = `(max-height: ${STACK_MAX_HEIGHT}px)`;
/** Narrow but tall: a phone held upright. Disjoint from SHORT, so the two never cascade. */
export const NARROW_TALL = `(max-width: ${STACK_MAX_WIDTH}px) and (min-height: ${STACK_MAX_HEIGHT + 1}px)`;
/** Wide and tall enough for panes side by side. */
export const SIDE_BY_SIDE = `(min-width: ${STACK_MAX_WIDTH + 1}px) and (min-height: ${STACK_MAX_HEIGHT + 1}px)`;
/** Side by side, but not wide enough for three panes. */
export const MEDIUM = `${SIDE_BY_SIDE} and (max-width: ${THREE_PANES_MIN_WIDTH - 1}px)`;
/** Three panes fit. */
export const THREE_PANES = `(min-width: ${THREE_PANES_MIN_WIDTH}px) and (min-height: ${STACK_MAX_HEIGHT + 1}px)`;

/** A makeStyles key for one of the queries above. */
export const media = (query: string): string => `@media ${query}`;
