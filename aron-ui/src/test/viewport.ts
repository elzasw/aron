/**
 * Sets the jsdom viewport for a test. The matchMedia shim in vitest.setup.ts
 * hears the resize event, so a component reacting to a size query re-renders
 * like in a browser; wrap the call in `act` when a rendered component is
 * expected to react. Dimensions persist across tests of one file, so a suite
 * that varies them resets both in `beforeEach`.
 */
export function setViewport(width: number, height: number): void {
  Object.defineProperty(window, "innerWidth", { configurable: true, writable: true, value: width });
  Object.defineProperty(window, "innerHeight", { configurable: true, writable: true, value: height });
  window.dispatchEvent(new Event("resize"));
}

/** The width alone; the height stays as it is (jsdom's default is a tall 768). */
export function setViewportWidth(width: number): void {
  setViewport(width, window.innerHeight);
}
