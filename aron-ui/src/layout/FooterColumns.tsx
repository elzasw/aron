import { makeStyles, tokens } from "@fluentui/react-components";
import { useTranslation } from "react-i18next";
import type { FooterLink, FooterParagraph, HomeFooter } from "../api/generated";

const useStyles = makeStyles({
  columns: {
    display: "flex",
    flexWrap: "wrap",
    gap: tokens.spacingHorizontalXXXL,
    maxWidth: "1100px",
    margin: "0 auto",
  },
  // the first column takes the free space, so prose gets the width and a
  // contact column stays as narrow as its content
  column: {
    flexGrow: 1,
    flexBasis: "0",
    minWidth: "min(16rem, 100%)",
    display: "flex",
    flexDirection: "column",
    gap: tokens.spacingVerticalS,
  },
  heading: {
    margin: "0",
    fontSize: tokens.fontSizeBase400,
    fontWeight: tokens.fontWeightSemibold,
  },
  paragraph: {
    margin: "0",
    fontSize: tokens.fontSizeBase300,
    lineHeight: tokens.lineHeightBase400,
  },
  list: {
    listStyleType: "none",
    margin: "0",
    padding: "0",
    display: "flex",
    flexWrap: "wrap",
    gap: `${tokens.spacingVerticalXS} ${tokens.spacingHorizontalL}`,
  },
  link: {
    display: "inline-flex",
    alignItems: "center",
    gap: tokens.spacingHorizontalXS,
    color: "inherit",
    fontSize: tokens.fontSizeBase300,
  },
  mark: {
    height: "1.25em",
    width: "auto",
    flexShrink: 0,
  },
  // a link inside a sentence keeps the prose's colour; the underline carries it
  inlineLink: {
    color: "inherit",
  },
});

type Styles = ReturnType<typeof useStyles>;

/**
 * One paragraph. The runs come from the server: the deployment wrote one
 * sentence with named placeholders and the server resolved them, so the links
 * inside the prose are anchors this component renders - configured markup never
 * reaches the page.
 */
function Paragraph({ paragraph, styles }: { paragraph: FooterParagraph; styles: Styles }) {
  return (
    <p className={styles.paragraph}>
      {paragraph.runs.map((run, index) =>
        run.url ? (
          <a key={index} href={run.url} className={styles.inlineLink}>
            {run.text}
          </a>
        ) : (
          <span key={index}>{run.text}</span>
        ),
      )}
    </p>
  );
}

/** A link of a column; a configured mark is decoration, the label names it. */
function ColumnLink({ link, styles }: { link: FooterLink; styles: Styles }) {
  const { t } = useTranslation();
  const label = link.label ?? (link.code ? t(`app.footer.link.${link.code}`) : link.url);
  return (
    <li>
      <a className={styles.link} href={link.url}>
        {link.imageUrl && <img className={styles.mark} src={link.imageUrl} alt="" />}
        {label}
      </a>
    </li>
  );
}

/**
 * The deployment's own columns of the page footer: who runs the portal, what it
 * presents, how to reach them. Rendered by `AppLayout` inside the frame's one
 * `<footer>` - that is where a reader expects this, and it needs no landmark or
 * label of its own there.
 *
 * Shown on the home page only. Not because it belongs to that page, but because
 * the frame is one viewport tall and every row of footer comes out of the routed
 * content: columns on every page would cost the record detail its pane height
 * for good.
 */
export default function FooterColumns({ footer }: { footer: HomeFooter }) {
  const styles = useStyles();

  return (
    <div className={styles.columns}>
      {footer.columns.map((column, index) => (
        <div className={styles.column} key={index}>
          {column.heading && <h2 className={styles.heading}>{column.heading}</h2>}
          {column.paragraphs.map((paragraph, paragraphIndex) => (
            <Paragraph key={paragraphIndex} paragraph={paragraph} styles={styles} />
          ))}
          {column.links.length > 0 && (
            <ul className={styles.list}>
              {column.links.map((link) => (
                <ColumnLink key={link.url} link={link} styles={styles} />
              ))}
            </ul>
          )}
        </div>
      ))}
    </div>
  );
}
