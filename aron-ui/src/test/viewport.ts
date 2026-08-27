/**
 * Sets the jsdom viewport width for a test. The matchMedia shim in
 * vitest.setup.ts hears the resize event, so a component reacting to a width
 * query re-renders like in a browser; wrap the call in `act` when a rendered
 * component is expected to react.
 */
export function setViewportWidth(width: number): void {
  Object.defineProperty(window, "innerWidth", { configurable: true, writable: true, value: width });
  window.dispatchEvent(new Event("resize"));
}
