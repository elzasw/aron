import { Button, Tooltip, makeStyles, mergeClasses, tokens } from "@fluentui/react-components";
import { ChevronLeft16Regular, ChevronRight16Regular } from "@fluentui/react-icons";
import { useRef } from "react";

/**
 * Vertical pane separator: drag it with the pointer or move it with the arrow
 * keys. It keeps no width of its own - the owning page holds the value, which is
 * what lets the page decide where the width is remembered. With a collapse
 * handler it also carries the old portal's fold-away triangles: a chevron
 * button at the separator's middle that hides the sized pane entirely and
 * brings it back.
 *
 * Accessibility (doc/accessibility.md): a real `separator` with a value and a
 * name, focusable and keyboard-operable, so resizing never depends on a drag
 * gesture (WCAG 2.1.1, 2.5.7); the fold-away control is its own button, never
 * nested inside the separator widget.
 */

/** Pixels one arrow key moves the separator; Home/End jump to the bounds. */
const STEP = 24;

const useStyles = makeStyles({
  // the strip hosting the separator and, when the pane can fold, its chevron
  strip: {
    alignSelf: "stretch",
    position: "relative",
    display: "flex",
    justifyContent: "center",
    alignItems: "center",
    flexShrink: 0,
    width: "16px",
  },
  root: {
    // spans the whole content row, so the rule reads as the pane's border
    position: "absolute",
    inset: 0,
    display: "flex",
    justifyContent: "center",
    alignItems: "center",
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
  // the panes are as tall as the frame, so the middle of the separator is also
  // the middle of the screen - no need to follow the reader
  grip: {
    width: "4px",
    height: "36px",
    borderRadius: tokens.borderRadiusCircular,
    backgroundColor: tokens.colorNeutralStroke1,
  },
  // the fold-away triangle sits over the grip's place, mid-height like the old
  // portal's, and stays when the separator itself has nothing left to size
  collapseButton: {
    position: "relative",
    zIndex: 1,
    minWidth: "16px",
    maxWidth: "16px",
    paddingLeft: 0,
    paddingRight: 0,
    backgroundColor: tokens.colorNeutralBackground1,
    border: `1px solid ${tokens.colorNeutralStroke2}`,
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
  /**
   * The sized pane sits to the RIGHT of the separator: moving the separator
   * left then grows the value instead of shrinking it, for drag and arrow keys
   * alike - the keys keep moving the separator, not the number.
   */
  reverse?: boolean;
  /** The fold-away control: present only when a handler is given. */
  collapsed?: boolean;
  onCollapsedChange?: (collapsed: boolean) => void;
  /** Accessible names of the fold-away control's two states. */
  collapseLabel?: string;
  expandLabel?: string;
  className?: string;
}

/** Value an arrow/Home/End key leads to, or undefined for any other key. */
function moved(key: string, value: number, min: number, max: number, reverse: boolean): number | undefined {
  const step = reverse ? -STEP : STEP;
  switch (key) {
    case "ArrowLeft":
      return value - step;
    case "ArrowRight":
      return value + step;
    case "Home":
      return reverse ? max : min;
    case "End":
      return reverse ? min : max;
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
  reverse = false,
  collapsed = false,
  onCollapsedChange,
  collapseLabel,
  expandLabel,
  className,
}: SplitterProps) {
  const styles = useStyles();
  const drag = useRef<{ pointerId: number; startX: number; startValue: number }>();

  const clamp = (next: number) => Math.min(max, Math.max(min, Math.round(next)));
  const dragged = (clientX: number) =>
    clamp(drag.current!.startValue + (reverse ? -1 : 1) * (clientX - drag.current!.startX));

  // the chevron points where the pane would go: a left pane folds leftwards
  const collapseGlyph = reverse ? <ChevronRight16Regular /> : <ChevronLeft16Regular />;
  const expandGlyph = reverse ? <ChevronLeft16Regular /> : <ChevronRight16Regular />;

  const separator = (
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
      className={styles.root}
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
        const next = moved(event.key, value, min, max, reverse);
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

  return (
    <div className={mergeClasses(styles.strip, className)}>
      {/* a collapsed pane has no width to adjust, so the separator rests */}
      {!collapsed && separator}
      {onCollapsedChange !== undefined && (
        <Tooltip content={(collapsed ? expandLabel : collapseLabel) ?? label} relationship="label">
          <Button
            appearance="subtle"
            size="small"
            className={styles.collapseButton}
            icon={collapsed ? expandGlyph : collapseGlyph}
            onClick={() => onCollapsedChange(!collapsed)}
          />
        </Tooltip>
      )}
    </div>
  );
}
