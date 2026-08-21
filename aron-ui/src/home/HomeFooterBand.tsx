import { makeStyles, tokens } from "@fluentui/react-components";
import { useTranslation } from "react-i18next";
import type { FooterLink, FooterParagraph, HomeFooter } from "../api/generated";
import { PRIMARY_DARK } from "../layout/palette";

const useStyles = makeStyles({
  // full-bleed band, like the original portal's: it ends the home page rather
  // than sitting inside its column
  band: {
    width: "100%",
    backgroundColor: PRIMARY_DARK,
    color: "#ffffff",
    marginTop: tokens.spacingVerticalXXXL,
    padding: `${tokens.spacingVerticalXXL} ${tokens.spacingHorizontalXXL}`,
    boxSizing: "border-box",
  },
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
    color: "#ffffff",
    fontSize: tokens.fontSizeBase300,
  },
  mark: {
    height: "1.25em",
    width: "auto",
    flexShrink: 0,
  },
});

/**
 * One paragraph. The runs come from the server: the deployment wrote one
 * sentence with named placeholders and the server resolved them, so the links
 * inside the prose are anchors this component renders - configured markup never
 * reaches the page.
 */
function Paragraph({ paragraph, className }: { paragraph: FooterParagraph; className: string }) {
  return (
    <p className={className}>
      {paragraph.runs.map((run, index) =>
        run.url ? (
          <a key={index} href={run.url} style={{ color: "inherit" }}>
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
function ColumnLink({ link, styles }: { link: FooterLink; styles: ReturnType<typeof useStyles> }) {
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
 * The deployment's own band at the foot of the home page: who runs the portal,
 * what it presents, how to reach them. Only here and not in the application
 * frame - the frame is one viewport tall and every row of it comes out of the
 * routed content, so a band on every page would cost the record detail its
 * pane height for good.
 *
 * Not a `<footer>` either: the frame already has the page's one `contentinfo`
 * landmark, which is where the links a deployment must publish live.
 */
export default function HomeFooterBand({ footer }: { footer: HomeFooter }) {
  const styles = useStyles();
  const { t } = useTranslation();

  return (
    <section className={styles.band} aria-label={t("home.about")}>
      <div className={styles.columns}>
        {footer.columns.map((column, index) => (
          <div className={styles.column} key={index}>
            {column.heading && <h2 className={styles.heading}>{column.heading}</h2>}
            {column.paragraphs.map((paragraph, paragraphIndex) => (
              <Paragraph key={paragraphIndex} paragraph={paragraph} className={styles.paragraph} />
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
    </section>
  );
}
