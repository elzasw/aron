import { makeStyles, mergeClasses, tokens } from "@fluentui/react-components";
import { useRef } from "react";

/**
 * Vertical pane separator: drag it with the pointer or move it with the arrow
 * keys. It keeps no width of its own - the owning page holds the value, which is
 * what lets the page decide where the width is remembered.
 *
 * Accessibility (doc/accessibility.md): a real `separator` with a value and a
 * name, focusable and keyboard-operable, so resizing never depends on a drag
 * gesture (WCAG 2.1.1, 2.5.7).
 */

/** Pixels one arrow key moves the separator; Home/End jump to the bounds. */
const STEP = 24;

const useStyles = makeStyles({
  root: {
    // spans the whole content row, so the rule reads as the pane's border
    alignSelf: "stretch",
    position: "relative",
    display: "flex",
    justifyContent: "center",
    flexShrink: 0,
    width: "12px",
    padding: 0,
    border: "none",
    backgroundColor: "transparent",
    cursor: "col-resize",
    // a drag must move the separator, not pan the page on a touch screen
    touchAction: "none",
    "::before": {
      content: '""',
      position: "absolute",
      top: 0,
      bottom: 0,
      width: "1px",
      backgroundColor: tokens.colorNeutralStroke2,
    },
    ":hover": {
      backgroundColor: tokens.colorNeutralBackground3Hover,
    },
    ":focus-visible": {
      outline: `2px solid ${tokens.colorStrokeFocus2}`,
      outlineOffset: "-2px",
    },
  },
  // the row is far taller than the viewport, so the grip sticks near the middle
  // of the screen instead of scrolling out of reach
  grip: {
    position: "sticky",
    top: "45vh",
    width: "4px",
    height: "36px",
    borderRadius: tokens.borderRadiusCircular,
    backgroundColor: tokens.colorNeutralStroke1,
  },
});

interface SplitterProps {
  /** Accessible name: what the value means, e.g. the pane it sizes. */
  label: string;
  value: number;
  min: number;
  max: number;
  /** Every intermediate value, i.e. also each step of a drag. */
  onChange: (value: number) => void;
  /** The value the reader settled on - a released drag or one key press. */
  onCommit?: (value: number) => void;
  className?: string;
}

/** Value an arrow/Home/End key leads to, or undefined for any other key. */
function moved(key: string, value: number, min: number, max: number): number | undefined {
  switch (key) {
    case "ArrowLeft":
      return value - STEP;
    case "ArrowRight":
      return value + STEP;
    case "Home":
      return min;
    case "End":
      return max;
    default:
      return undefined;
  }
}

export default function Splitter({
  label,
  value,
  min,
  max,
  onChange,
  onCommit,
  className,
}: SplitterProps) {
  const styles = useStyles();
  const drag = useRef<{ pointerId: number; startX: number; startValue: number }>();

  const clamp = (next: number) => Math.min(max, Math.max(min, Math.round(next)));
  const dragged = (clientX: number) =>
    clamp(drag.current!.startValue + clientX - drag.current!.startX);

  return (
    // ARIA makes a separator a widget as soon as it is focusable - that is the
    // window-splitter pattern, and it is why this one takes a tab stop, a value
    // and key handling. The plugin's role table knows only the structural
    // separator, so both suppressions below are the pattern, not a shortcut.
    // eslint-disable-next-line jsx-a11y/no-noninteractive-element-interactions
    <div
      role="separator"
      aria-orientation="vertical"
      aria-label={label}
      aria-valuenow={value}
      aria-valuemin={min}
      aria-valuemax={max}
      // eslint-disable-next-line jsx-a11y/no-noninteractive-tabindex
      tabIndex={0}
      className={mergeClasses(styles.root, className)}
      onPointerDown={(event) => {
        // capture keeps the drag alive when the pointer outruns the 12px strip
        event.currentTarget.setPointerCapture(event.pointerId);
        drag.current = { pointerId: event.pointerId, startX: event.clientX, startValue: value };
      }}
      onPointerMove={(event) => {
        if (drag.current?.pointerId === event.pointerId) {
          onChange(dragged(event.clientX));
        }
      }}
      onPointerUp={(event) => {
        if (drag.current?.pointerId !== event.pointerId) {
          return;
        }
        const settled = dragged(event.clientX);
        event.currentTarget.releasePointerCapture(event.pointerId);
        drag.current = undefined;
        onChange(settled);
        onCommit?.(settled);
      }}
      // a cancelled gesture (a system swipe, a lost capture) must not leave the
      // separator following an unpressed pointer
      onPointerCancel={() => {
        drag.current = undefined;
      }}
      onKeyDown={(event) => {
        const next = moved(event.key, value, min, max);
        if (next === undefined) {
          return;
        }
        // Home/End would otherwise scroll the page under the reader's hands
        event.preventDefault();
        const settled = clamp(next);
        onChange(settled);
        onCommit?.(settled);
      }}
    >
      <span aria-hidden="true" className={styles.grip} />
    </div>
  );
}
