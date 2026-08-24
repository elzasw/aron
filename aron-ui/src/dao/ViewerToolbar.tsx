import {
  Button,
  Input,
  Label,
  Menu,
  MenuItem,
  MenuList,
  MenuPopover,
  MenuTrigger,
  Overflow,
  OverflowDivider,
  OverflowItem,
  Popover,
  PopoverSurface,
  PopoverTrigger,
  Slider,
  Text,
  ToggleButton,
  Tooltip,
  makeStyles,
  tokens,
  useIsOverflowItemVisible,
  useOverflowMenu,
} from "@fluentui/react-components";
import {
  ArrowDownload20Regular,
  ArrowNext20Regular,
  ArrowPrevious20Regular,
  ArrowRotateClockwise20Regular,
  ArrowRotateCounterclockwise20Regular,
  ChevronDoubleLeft20Regular,
  ChevronDoubleRight20Regular,
  ChevronLeft20Regular,
  ChevronRight20Regular,
  CompassNorthwest20Regular,
  FullScreenMaximize20Regular,
  LockClosed20Regular,
  LockOpen20Regular,
  MoreHorizontal20Regular,
  Options20Regular,
  Share20Regular,
  ZoomFit20Regular,
  ZoomIn20Regular,
  ZoomOut20Regular,
} from "@fluentui/react-icons";
import { useId, useState, type ReactElement } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";

/** How far the double chevrons jump, the old portal's step. */
export const PAGE_JUMP = 10;

const useStyles = makeStyles({
  // the Overflow contract: the container itself clips and may shrink, or the
  // bar would spill over the splitter instead of collapsing into the menu
  bar: {
    display: "flex",
    flexWrap: "nowrap",
    alignItems: "center",
    // centered over the canvas, the old portal's arrangement
    justifyContent: "center",
    columnGap: "2px",
    padding: `${tokens.spacingVerticalXXS} 0`,
    whiteSpace: "nowrap",
    overflow: "hidden",
    minWidth: 0,
  },
  // an icon needs a square button, not Fluent's 64px text-button minimum
  iconButton: {
    minWidth: "28px",
    maxWidth: "28px",
    paddingLeft: 0,
    paddingRight: 0,
  },
  pageGroup: {
    display: "inline-flex",
    alignItems: "center",
    columnGap: "2px",
  },
  pageInput: {
    width: "56px",
  },
  pageTotal: {
    whiteSpace: "nowrap",
    paddingRight: tokens.spacingHorizontalXS,
    paddingLeft: tokens.spacingHorizontalXXS,
  },
  divider: {
    width: "1px",
    height: "20px",
    backgroundColor: tokens.colorNeutralStroke2,
    marginLeft: tokens.spacingHorizontalXS,
    marginRight: tokens.spacingHorizontalXS,
  },
  settings: {
    display: "flex",
    flexDirection: "column",
    gap: tokens.spacingVerticalXS,
    minWidth: "220px",
  },
  settingRow: {
    display: "flex",
    flexDirection: "column",
  },
});

/** One toolbar command: an icon button inline, a labelled menu item when overflowed. */
interface Command {
  id: string;
  label: string;
  icon: ReactElement;
  action: () => void;
  disabled?: boolean;
  /** A toggle: pressed state shown on the button and ticked in the menu. */
  active?: boolean;
  /** Higher survives longer when space runs out. */
  priority: number;
  group: string;
  /** Inline anchor semantics (download/fullscreen) instead of a plain button. */
  href?: string;
  download?: string;
}

export interface ImageAdjustments {
  /** Percent, 100 = neutral. */
  brightness: number;
  contrast: number;
}

export const NEUTRAL_ADJUSTMENTS: ImageAdjustments = { brightness: 100, contrast: 100 };

interface ViewerToolbarProps {
  pageCount: number;
  currentIndex: number;
  onGoTo: (index: number) => void;
  onZoomIn: () => void;
  onZoomOut: () => void;
  onZoomFit: () => void;
  onRotateLeft: () => void;
  onRotateRight: () => void;
  /** Download of the current page's published file; absent = nothing to download. */
  downloadUrl?: string;
  downloadName?: string;
  onCopyLink: () => void;
  /** In-app route of the fullscreen viewer; absent when the viewer already fills the frame. */
  fullscreenUrl?: string;
  navigatorShown: boolean;
  onToggleNavigator: () => void;
  viewportLocked: boolean;
  onToggleViewportLock: () => void;
  adjustments: ImageAdjustments;
  onAdjust: (adjustments: ImageAdjustments) => void;
}

function CommandButton({ command }: { command: Command }) {
  const styles = useStyles();
  let button;
  if (command.href !== undefined) {
    button = (
      <Button
        as="a"
        appearance="subtle"
        size="small"
        className={styles.iconButton}
        icon={command.icon}
        href={command.href}
        download={command.download}
        onClick={
          command.download === undefined
            ? (event) => {
                // an in-app route: the router navigates, the browser must not reload
                event.preventDefault();
                command.action();
              }
            : undefined
        }
      />
    );
  } else if (command.active !== undefined) {
    button = (
      <ToggleButton
        appearance="subtle"
        size="small"
        className={styles.iconButton}
        icon={command.icon}
        checked={command.active}
        onClick={command.action}
      />
    );
  } else {
    button = (
      <Button
        appearance="subtle"
        size="small"
        className={styles.iconButton}
        icon={command.icon}
        disabled={command.disabled}
        onClick={command.action}
      />
    );
  }
  return (
    <OverflowItem id={command.id} priority={command.priority} groupId={command.group}>
      <span>
        <Tooltip content={command.label} relationship="label">
          {button}
        </Tooltip>
      </span>
    </OverflowItem>
  );
}

/** A command that did not fit inline, offered from the "⋯" menu by its full label. */
function OverflowedCommand({ command }: { command: Command }) {
  const isVisible = useIsOverflowItemVisible(command.id);
  if (isVisible) {
    return null;
  }
  return (
    <MenuItem
      icon={command.icon}
      disabled={command.disabled}
      onClick={command.action}
      // a toggle stays a toggle in the menu
      aria-checked={command.active}
      role={command.active !== undefined ? "menuitemcheckbox" : undefined}
    >
      {command.label}
    </MenuItem>
  );
}

function OverflowMenu({ commands }: { commands: Command[] }) {
  const styles = useStyles();
  const { t } = useTranslation();
  const { ref, isOverflowing } = useOverflowMenu<HTMLButtonElement>();
  if (!isOverflowing) {
    return null;
  }
  return (
    <Menu>
      <MenuTrigger disableButtonEnhancement>
        <Button
          ref={ref}
          appearance="subtle"
          size="small"
          className={styles.iconButton}
          icon={<MoreHorizontal20Regular />}
          aria-label={t("dao.moreControls")}
        />
      </MenuTrigger>
      <MenuPopover>
        <MenuList>
          {commands.map((command) => (
            <OverflowedCommand key={command.id} command={command} />
          ))}
        </MenuList>
      </MenuPopover>
    </Menu>
  );
}

/** The sliders panel: brightness and contrast as CSS filters, reset back to neutral. */
function ImageSettings({
  adjustments,
  onAdjust,
}: {
  adjustments: ImageAdjustments;
  onAdjust: (adjustments: ImageAdjustments) => void;
}) {
  const styles = useStyles();
  const { t } = useTranslation();
  const brightnessId = useId();
  const contrastId = useId();
  return (
    <Popover>
      <PopoverTrigger disableButtonEnhancement>
        <Tooltip content={t("dao.imageSettings")} relationship="label">
          <ToggleButton
            appearance="subtle"
            size="small"
            className={styles.iconButton}
            icon={<Options20Regular />}
            checked={
              adjustments.brightness !== NEUTRAL_ADJUSTMENTS.brightness ||
              adjustments.contrast !== NEUTRAL_ADJUSTMENTS.contrast
            }
          />
        </Tooltip>
      </PopoverTrigger>
      <PopoverSurface aria-label={t("dao.imageSettings")}>
        <div className={styles.settings}>
          <div className={styles.settingRow}>
            <Label htmlFor={brightnessId}>{t("dao.brightness")}</Label>
            <Slider
              id={brightnessId}
              min={25}
              max={200}
              value={adjustments.brightness}
              onChange={(_, data) => onAdjust({ ...adjustments, brightness: data.value })}
            />
          </div>
          <div className={styles.settingRow}>
            <Label htmlFor={contrastId}>{t("dao.contrast")}</Label>
            <Slider
              id={contrastId}
              min={25}
              max={200}
              value={adjustments.contrast}
              onChange={(_, data) => onAdjust({ ...adjustments, contrast: data.value })}
            />
          </div>
          <Button size="small" onClick={() => onAdjust(NEUTRAL_ADJUSTMENTS)}>
            {t("dao.resetImage")}
          </Button>
        </div>
      </PopoverSurface>
    </Popover>
  );
}

/**
 * The viewer's own controls, one line always - real buttons whose accessible
 * names double as tooltips, no library chrome. When the hosting pane is too
 * narrow, the less essential commands collapse into a "⋯" menu (the mobile
 * navigation pattern) instead of wrapping the bar into a second row; the page
 * input, the settings panel and the menu itself never collapse, so the reader
 * always knows where they are.
 */
export default function ViewerToolbar({
  pageCount,
  currentIndex,
  onGoTo,
  onZoomIn,
  onZoomOut,
  onZoomFit,
  onRotateLeft,
  onRotateRight,
  downloadUrl,
  downloadName,
  onCopyLink,
  fullscreenUrl,
  navigatorShown,
  onToggleNavigator,
  viewportLocked,
  onToggleViewportLock,
  adjustments,
  onAdjust,
}: ViewerToolbarProps) {
  const styles = useStyles();
  const { t } = useTranslation();
  const navigate = useNavigate();
  // the draft exists only while the reader is typing; otherwise the input
  // simply shows the current page, so turns from anywhere refresh it
  const [pageDraft, setPageDraft] = useState<string | null>(null);
  const pageValue = pageDraft ?? String(currentIndex + 1);

  const commitDraft = () => {
    setPageDraft(null);
    const page = Number(pageDraft);
    if (Number.isInteger(page) && page >= 1 && page <= pageCount) {
      onGoTo(page - 1);
    }
  };

  const atStart = currentIndex === 0;
  const atEnd = currentIndex >= pageCount - 1;
  const clampedJump = (step: number) => onGoTo(Math.min(Math.max(currentIndex + step, 0), pageCount - 1));
  const commands: Command[] = [
    { id: "first", group: "nav", priority: 60, label: t("dao.firstPage"),
      icon: <ArrowPrevious20Regular />, disabled: atStart, action: () => onGoTo(0) },
    { id: "back10", group: "nav", priority: 45, label: t("dao.back10"),
      icon: <ChevronDoubleLeft20Regular />, disabled: atStart, action: () => clampedJump(-PAGE_JUMP) },
    { id: "prev", group: "nav", priority: 90, label: t("dao.prevPage"),
      icon: <ChevronLeft20Regular />, disabled: atStart, action: () => onGoTo(currentIndex - 1) },
    { id: "next", group: "nav", priority: 90, label: t("dao.nextPage"),
      icon: <ChevronRight20Regular />, disabled: atEnd, action: () => onGoTo(currentIndex + 1) },
    { id: "forward10", group: "nav", priority: 45, label: t("dao.forward10"),
      icon: <ChevronDoubleRight20Regular />, disabled: atEnd, action: () => clampedJump(PAGE_JUMP) },
    { id: "last", group: "nav", priority: 60, label: t("dao.lastPage"),
      icon: <ArrowNext20Regular />, disabled: atEnd, action: () => onGoTo(pageCount - 1) },
    { id: "zoom-in", group: "zoom", priority: 80, label: t("dao.zoomIn"),
      icon: <ZoomIn20Regular />, action: onZoomIn },
    { id: "zoom-out", group: "zoom", priority: 80, label: t("dao.zoomOut"),
      icon: <ZoomOut20Regular />, action: onZoomOut },
    { id: "zoom-fit", group: "zoom", priority: 50, label: t("dao.zoomFit"),
      icon: <ZoomFit20Regular />, action: onZoomFit },
    { id: "rotate-left", group: "zoom", priority: 40, label: t("dao.rotateLeft"),
      icon: <ArrowRotateCounterclockwise20Regular />, action: onRotateLeft },
    { id: "rotate-right", group: "zoom", priority: 40, label: t("dao.rotateRight"),
      icon: <ArrowRotateClockwise20Regular />, action: onRotateRight },
    { id: "navigator", group: "view", priority: 35, label: t("dao.navigator"),
      icon: <CompassNorthwest20Regular />, active: navigatorShown, action: onToggleNavigator },
    { id: "lock", group: "view", priority: 35, label: t("dao.lockViewport"),
      icon: viewportLocked ? <LockClosed20Regular /> : <LockOpen20Regular />,
      active: viewportLocked, action: onToggleViewportLock },
  ];
  if (downloadUrl !== undefined) {
    commands.push({
      id: "download", group: "actions", priority: 70, label: t("dao.download"),
      icon: <ArrowDownload20Regular />, href: downloadUrl, download: downloadName ?? "",
      // from the overflow menu the anchor is not rendered, so click one on the fly
      action: () => {
        const anchor = document.createElement("a");
        anchor.href = downloadUrl;
        anchor.download = downloadName ?? "";
        anchor.click();
      },
    });
  }
  commands.push({
    id: "copy-link", group: "actions", priority: 30, label: t("dao.copyLink"),
    icon: <Share20Regular />, action: onCopyLink,
  });
  if (fullscreenUrl !== undefined) {
    commands.push({
      id: "fullscreen", group: "actions", priority: 55, label: t("dao.fullscreen"),
      icon: <FullScreenMaximize20Regular />, href: fullscreenUrl, action: () => navigate(fullscreenUrl),
    });
  }

  // every child the overflow manager should count must be a registered item:
  // unregistered content is invisible to its math, which then never overflows
  // and the clipped commands lose their menu. The page input and the settings
  // panel are pinned - measured, never hidden.
  const groupEnds = ["last", "rotate-right", "lock"];
  const children: ReactElement[] = [];
  for (const command of commands) {
    children.push(<CommandButton key={command.id} command={command} />);
    if (command.id === "prev") {
      children.push(
        <OverflowItem key="page" id="page" pinned>
          <span className={styles.pageGroup}>
            <Input
              aria-label={t("dao.goToPage")}
              className={styles.pageInput}
              size="small"
              type="number"
              min={1}
              max={pageCount}
              value={pageValue}
              onChange={(_, data) => setPageDraft(data.value)}
              onBlur={commitDraft}
              onKeyDown={(event) => {
                if (event.key === "Enter") {
                  commitDraft();
                }
              }}
            />
            <Text size={200} className={styles.pageTotal}>
              {t("dao.pageOf", { total: pageCount })}
            </Text>
          </span>
        </OverflowItem>,
      );
    }
    if (groupEnds.includes(command.id)) {
      children.push(
        <OverflowDivider key={`divider-${command.group}`} groupId={command.group}>
          <div className={styles.divider} aria-hidden="true" />
        </OverflowDivider>,
      );
    }
  }
  children.push(
    <OverflowItem key="settings" id="settings" pinned>
      <span>
        <ImageSettings adjustments={adjustments} onAdjust={onAdjust} />
      </span>
    </OverflowItem>,
  );

  return (
    // the padding covers what the manager cannot measure: the column gaps
    <Overflow padding={56}>
      <div role="toolbar" aria-label={t("dao.viewerControls")} className={styles.bar}>
        {children}
        <OverflowMenu commands={commands} />
      </div>
    </Overflow>
  );
}
