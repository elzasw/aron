import { Text, makeStyles, tokens } from "@fluentui/react-components";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import type { DigitalObjectInfo } from "../api/generated";
import { buildPages, pageFileId } from "./pages";

/** More thumbnails than this only repeat themselves - the viewer has the rail. */
const STRIP_LIMIT = 12;

const useStyles = makeStyles({
  object: {
    display: "flex",
    flexDirection: "column",
    gap: tokens.spacingVerticalXS,
  },
  meta: {
    display: "flex",
    flexWrap: "wrap",
    alignItems: "baseline",
    columnGap: tokens.spacingHorizontalM,
  },
  link: {
    color: tokens.colorBrandForegroundLink,
    textDecorationLine: "none",
    ":hover": { textDecorationLine: "underline" },
  },
  strip: {
    listStyleType: "none",
    margin: 0,
    padding: 0,
    display: "flex",
    flexWrap: "wrap",
    gap: tokens.spacingHorizontalXS,
  },
  thumbLink: {
    display: "block",
    borderRadius: tokens.borderRadiusSmall,
  },
  thumb: {
    width: "88px",
    height: "88px",
    objectFit: "contain",
    backgroundColor: tokens.colorNeutralBackground3,
    borderRadius: tokens.borderRadiusSmall,
    display: "block",
  },
});

/**
 * The record page's digital-object section: per object a link into the viewer,
 * the license, and a strip of the first page thumbnails - each thumbnail a
 * deep link to its page. The strip is a preview, not the navigation; long
 * documents are the viewer's business.
 */
export default function DaoGallery({
  apuUuid,
  objects,
}: {
  apuUuid: string;
  objects: DigitalObjectInfo[];
}) {
  const styles = useStyles();
  const { t } = useTranslation();

  return (
    <>
      {objects.map((dao) => {
        const pages = buildPages(dao.files);
        const viewerUrl = `/apu/${apuUuid}/dao/${dao.uuid}`;
        const thumbnails = pages.filter((page) => page.thumbnail?.url !== undefined).slice(0, STRIP_LIMIT);
        return (
          <div key={dao.uuid} className={styles.object}>
            <div className={styles.meta}>
              <Link to={viewerUrl} className={styles.link}>
                {t("dao.openViewerNamed", { name: dao.name ?? t("apu.digitalObjects") })}
              </Link>
              <Text size={200}>{t("dao.pages", { count: pages.length })}</Text>
            </div>
            {thumbnails.length > 0 && (
              <ul className={styles.strip}>
                {thumbnails.map((page) => (
                  <li key={page.index}>
                    <Link
                      to={`${viewerUrl}?file=${pageFileId(page) ?? ""}`}
                      className={styles.thumbLink}
                      aria-label={t("dao.thumbnailOf", {
                        page: page.index + 1,
                        name: dao.name ?? "",
                      })}
                    >
                      <img src={page.thumbnail?.url} alt="" loading="lazy" className={styles.thumb} />
                    </Link>
                  </li>
                ))}
              </ul>
            )}
          </div>
        );
      })}
    </>
  );
}
