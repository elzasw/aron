import {
  Button,
  Input,
  Menu,
  MenuItem,
  MenuList,
  MenuPopover,
  MenuTrigger,
  Overflow,
  OverflowDivider,
  OverflowItem,
  Text,
  Tooltip,
  makeStyles,
  tokens,
  useIsOverflowItemVisible,
  useOverflowMenu,
} from "@fluentui/react-components";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";

const useStyles = makeStyles({
  // the Overflow contract: the container itself clips and may shrink, or the
  // bar would spill over the splitter instead of collapsing into the menu
  bar: {
    display: "flex",
    flexWrap: "nowrap",
    alignItems: "center",
    columnGap: "2px",
    padding: `${tokens.spacingVerticalXXS} 0`,
    whiteSpace: "nowrap",
    overflow: "hidden",
    minWidth: 0,
  },
  // a glyph needs a square button, not Fluent's 64px text-button minimum
  iconButton: {
    minWidth: "28px",
    maxWidth: "28px",
    paddingLeft: 0,
    paddingRight: 0,
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
});

/** One toolbar command: rendered as an icon button inline, as a labelled menu item when overflowed. */
interface Command {
  id: string;
  label: string;
  glyph: string;
  action: () => void;
  disabled?: boolean;
  /** Higher survives longer when space runs out. */
  priority: number;
  group: string;
  /** Inline anchor semantics (download/fullscreen) instead of a plain button. */
  href?: string;
  download?: string;
}

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
}

function CommandButton({ command }: { command: Command }) {
  const styles = useStyles();
  const button =
    command.href !== undefined ? (
      <Button
        as="a"
        appearance="subtle"
        size="small"
        className={styles.iconButton}
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
      >
        {command.glyph}
      </Button>
    ) : (
      <Button
        appearance="subtle"
        size="small"
        className={styles.iconButton}
        disabled={command.disabled}
        onClick={command.action}
      >
        {command.glyph}
      </Button>
    );
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
    <MenuItem disabled={command.disabled} onClick={command.action}>
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
          aria-label={t("dao.moreControls")}
        >
          ⋯
        </Button>
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

/**
 * The viewer's own controls, one line always - real buttons whose accessible
 * names double as tooltips, no library chrome. When the hosting pane is too
 * narrow, the less essential commands collapse into a "⋯" menu (the mobile
 * navigation pattern) instead of wrapping the bar into a second row; the page
 * input and total never collapse, so the reader always knows where they are.
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
  const commands: Command[] = [
    { id: "first", group: "nav", priority: 60, label: t("dao.firstPage"), glyph: "⇤",
      disabled: atStart, action: () => onGoTo(0) },
    { id: "prev", group: "nav", priority: 90, label: t("dao.prevPage"), glyph: "←",
      disabled: atStart, action: () => onGoTo(currentIndex - 1) },
    { id: "next", group: "nav", priority: 90, label: t("dao.nextPage"), glyph: "→",
      disabled: atEnd, action: () => onGoTo(currentIndex + 1) },
    { id: "last", group: "nav", priority: 60, label: t("dao.lastPage"), glyph: "⇥",
      disabled: atEnd, action: () => onGoTo(pageCount - 1) },
    { id: "zoom-in", group: "zoom", priority: 80, label: t("dao.zoomIn"), glyph: "＋", action: onZoomIn },
    { id: "zoom-out", group: "zoom", priority: 80, label: t("dao.zoomOut"), glyph: "−", action: onZoomOut },
    { id: "zoom-fit", group: "zoom", priority: 50, label: t("dao.zoomFit"), glyph: "▣", action: onZoomFit },
    { id: "rotate-left", group: "zoom", priority: 40, label: t("dao.rotateLeft"), glyph: "⟲",
      action: onRotateLeft },
    { id: "rotate-right", group: "zoom", priority: 40, label: t("dao.rotateRight"), glyph: "⟳",
      action: onRotateRight },
  ];
  if (downloadUrl !== undefined) {
    commands.push({
      id: "download", group: "actions", priority: 70, label: t("dao.download"), glyph: "⤓",
      href: downloadUrl, download: downloadName ?? "",
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
    id: "copy-link", group: "actions", priority: 30, label: t("dao.copyLink"), glyph: "⧉",
    action: onCopyLink,
  });
  if (fullscreenUrl !== undefined) {
    commands.push({
      id: "fullscreen", group: "actions", priority: 55, label: t("dao.fullscreen"), glyph: "⛶",
      href: fullscreenUrl, action: () => navigate(fullscreenUrl),
    });
  }

  const groupEnds = ["last", "rotate-right"];
  return (
    <Overflow padding={8}>
      <div role="toolbar" aria-label={t("dao.viewerControls")} className={styles.bar}>
        {commands.slice(0, 2).map((command) => (
          <CommandButton key={command.id} command={command} />
        ))}
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
        {commands.slice(2).map((command) => (
          <span key={command.id} style={{ display: "contents" }}>
            <CommandButton command={command} />
            {groupEnds.includes(command.id) && (
              <OverflowDivider groupId={command.group}>
                <div className={styles.divider} aria-hidden="true" />
              </OverflowDivider>
            )}
          </span>
        ))}
        <OverflowMenu commands={commands} />
      </div>
    </Overflow>
  );
}
