import { makeStyles, tokens } from "@fluentui/react-components";
import { useTranslation } from "react-i18next";
import { Link, useLocation } from "react-router-dom";
import { useApuDetail } from "../apu/useApuDetail";
import { SECTION_OF_APU_TYPE, SECTIONS } from "../sections";

const useStyles = makeStyles({
  bar: {
    padding: `${tokens.spacingVerticalM} ${tokens.spacingHorizontalXXL}`,
    backgroundColor: tokens.colorNeutralBackground1,
    borderBottom: `1px solid ${tokens.colorNeutralStroke2}`,
    fontSize: tokens.fontSizeBase300,
    "@media (max-width: 860px)": {
      padding: `${tokens.spacingVerticalS} ${tokens.spacingHorizontalM}`,
    },
  },
  trail: {
    listStyleType: "none",
    margin: 0,
    padding: 0,
    display: "flex",
    // a record deep in a fund makes a long trail; it wraps rather than clips
    flexWrap: "wrap",
    alignItems: "center",
    gap: tokens.spacingHorizontalS,
  },
  crumb: {
    display: "flex",
    alignItems: "center",
    gap: tokens.spacingHorizontalS,
    color: tokens.colorNeutralForeground2,
  },
  link: {
    color: tokens.colorBrandForegroundLink,
    textDecorationLine: "none",
    ":hover": {
      textDecorationLine: "underline",
    },
  },
});

/** One step of the trail; without a route it is where the reader stands. */
interface Crumb {
  label: string;
  to?: string;
}

/**
 * The portal's single breadcrumb strip, under the header. On a section page it
 * is "home / section"; on a record it continues into the record's own place in
 * the archival description - "home / section / fund / … / record" - because a
 * reader has one location, not two, and the old portal showed it as one trail.
 * Hidden on the home page.
 *
 * The record comes from the same query as the page below it (useApuDetail), so
 * the strip costs no extra request and can never disagree with the page.
 */
export default function Breadcrumbs() {
  const styles = useStyles();
  const { t } = useTranslation();
  const { pathname } = useLocation();
  // a record, or a digital object opened in its viewer under the record's route
  const recordMatch = /^\/apu\/([^/]+)(?:\/dao\/([^/]+))?$/.exec(pathname);
  const recordUuid = recordMatch?.[1];
  const daoUuid = recordMatch?.[2];
  const { data: record } = useApuDetail(recordUuid);

  if (pathname === "/") {
    return null;
  }

  const crumbs: Crumb[] = [];
  if (record !== undefined) {
    const sectionCode = SECTION_OF_APU_TYPE[record.apuType];
    const section = sectionCode === undefined ? undefined : SECTIONS[sectionCode];
    if (section?.route !== undefined) {
      crumbs.push({ label: t(section.labelKey), to: section.route });
    }
    // the path includes the record itself as its last element
    for (const ancestor of record.treePath.slice(0, -1)) {
      crumbs.push({ label: ancestor.name, to: `/apu/${ancestor.uuid}` });
    }
    if (daoUuid !== undefined) {
      // in the viewer the record becomes a link and the digital object the terminal crumb
      crumbs.push({ label: record.name, to: `/apu/${record.uuid}` });
      const dao = record.digitalObjects.find((candidate) => candidate.uuid === daoUuid);
      crumbs.push({ label: dao?.name ?? t("apu.digitalObjects") });
    } else {
      crumbs.push({ label: record.name });
    }
  } else if (recordUuid === undefined) {
    // a section or search page: the route names itself
    const firstSegment = "/" + pathname.split("/")[1];
    const section = Object.values(SECTIONS).find((candidate) => candidate.route === firstSegment);
    crumbs.push({
      label: t(section?.labelKey ?? (firstSegment === "/apu" ? "nav.search" : "notFound.heading")),
    });
  }

  return (
    <nav aria-label={t("nav.breadcrumbs")} className={styles.bar}>
      <ol className={styles.trail}>
        <li className={styles.crumb}>
          <Link to="/" className={styles.link}>
            {t("nav.home")}
          </Link>
        </li>
        {crumbs.map((crumb, index) => (
          <li key={`${crumb.to ?? ""}-${index}`} className={styles.crumb}>
            <span aria-hidden="true">/</span>
            {crumb.to === undefined ? (
              <span aria-current="page">{crumb.label}</span>
            ) : (
              <Link to={crumb.to} className={styles.link}>
                {crumb.label}
              </Link>
            )}
          </li>
        ))}
      </ol>
    </nav>
  );
}
