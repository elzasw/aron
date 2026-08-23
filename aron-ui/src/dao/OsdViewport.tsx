import { forwardRef, useEffect, useImperativeHandle, useRef } from "react";
import type OpenSeadragon from "openseadragon";

/** What OpenSeadragon opens: a DZI descriptor URL, or a plain image for pages without a pyramid. */
export type OsdSource = string | { type: "image"; url: string };

export interface OsdViewportHandle {
  zoomIn(): void;
  zoomOut(): void;
  home(): void;
  rotateBy(degrees: number): void;
}

interface OsdViewportProps {
  source: OsdSource;
  /** Accessible name of the canvas region - the current page's label. */
  label: string;
  /** Id of the visually hidden keyboard instructions. */
  describedBy?: string;
  /** The source could not be opened (missing tiles, unreachable server). */
  onOpenFailed?: () => void;
}

/**
 * The one component that touches OpenSeadragon. Chrome-less on purpose: no OSD
 * buttons, no OSD image assets - every control is the toolbar's own Fluent
 * button driving this handle, which is what makes the viewer themable,
 * localized and accessible without fighting the library. The library itself is
 * imported lazily, so only readers who open a viewer download it.
 *
 * The container keeps OpenSeadragon's built-in keyboard navigation (arrows pan,
 * +/- zoom while it has focus) - panning must be keyboard-operable (WCAG 2.1.1)
 * and reimplementing it would duplicate the library.
 */
const OsdViewport = forwardRef<OsdViewportHandle, OsdViewportProps>(function OsdViewport(
  { source, label, describedBy, onOpenFailed },
  ref,
) {
  const containerRef = useRef<HTMLDivElement>(null);
  const viewerRef = useRef<OpenSeadragon.Viewer | null>(null);
  // the latest open request wins; an unmount or a page turn abandons older ones
  const openRequestRef = useRef(0);
  const onOpenFailedRef = useRef(onOpenFailed);
  onOpenFailedRef.current = onOpenFailed;

  useEffect(() => {
    const request = ++openRequestRef.current;
    let cancelled = false;

    async function openSource() {
      const OpenSeadragon = (await import("openseadragon")).default;
      if (cancelled || containerRef.current === null) {
        return;
      }
      if (viewerRef.current === null) {
        const reducedMotion = window.matchMedia?.("(prefers-reduced-motion: reduce)").matches === true;
        // no preserveImageSizeOnResize: the panes around the viewer are resizable,
        // and the image must refit the canvas instead of keeping its old scale
        viewerRef.current = OpenSeadragon({
          element: containerRef.current,
          showNavigationControl: false,
          animationTime: reducedMotion ? 0 : undefined,
        });
        viewerRef.current.addHandler("open-failed", () => onOpenFailedRef.current?.());
      }
      if (openRequestRef.current === request) {
        // a descriptor URL string and an inline image source are legal at
        // runtime; the installed typings model neither
        viewerRef.current.open(source as unknown as OpenSeadragon.TileSourceSpecifier);
      }
    }

    void openSource();
    return () => {
      cancelled = true;
    };
  }, [source]);

  // one viewer per mounted component; destroying releases the canvas and handlers
  useEffect(
    () => () => {
      viewerRef.current?.destroy();
      viewerRef.current = null;
    },
    [],
  );

  useImperativeHandle(ref, () => ({
    zoomIn: () => viewerRef.current?.viewport.zoomBy(1.5).applyConstraints(),
    zoomOut: () => viewerRef.current?.viewport.zoomBy(1 / 1.5).applyConstraints(),
    home: () => viewerRef.current?.viewport.goHome(),
    rotateBy: (degrees) => {
      const viewport = viewerRef.current?.viewport;
      viewport?.setRotation(viewport.getRotation() + degrees);
    },
  }));

  return (
    <div
      ref={containerRef}
      role="application"
      aria-label={label}
      aria-describedby={describedBy}
      style={{ width: "100%", height: "100%" }}
    />
  );
});

export default OsdViewport;
