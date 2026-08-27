import {
  Spinner,
  Text,
  Title2,
  makeStyles,
  tokens,
} from "@fluentui/react-components";
import { useTranslation } from "react-i18next";
import { Link, useParams, useSearchParams } from "react-router-dom";
import { ResponseError } from "../api/generated";
import { useApuDetail } from "../apu/useApuDetail";
import DaoViewer from "../dao/DaoViewer";
import { SHORT, media } from "../layout/breakpoints";

const useStyles = makeStyles({
  // fills the frame the ApuPage way: the page contributes no height, the
  // document never scrolls, canvas and rail scroll (or zoom) on their own.
  // A phone held upright keeps that (its frame is tall); held sideways, the
  // frame's leftover would be a strip, so the page flows instead and the
  // viewer takes one whole viewport: scrolling the header away shows the scan
  // on every row there is
  layout: {
    position: "absolute",
    inset: 0,
    display: "flex",
    flexDirection: "column",
    gap: tokens.spacingVerticalXS,
    padding: `${tokens.spacingVerticalM} ${tokens.spacingHorizontalXXL}`,
    "@media (max-width: 860px)": {
      padding: `${tokens.spacingVerticalS} ${tokens.spacingHorizontalM}`,
    },
    [media(SHORT)]: {
      position: "static",
    },
  },
  header: {
    display: "flex",
    flexWrap: "wrap",
    alignItems: "baseline",
    columnGap: tokens.spacingHorizontalL,
    rowGap: tokens.spacingVerticalXXS,
  },
  link: {
    color: tokens.colorBrandForegroundLink,
    textDecorationLine: "none",
    ":hover": { textDecorationLine: "underline" },
  },
  viewer: {
    flexGrow: 1,
    minHeight: 0,
    [media(SHORT)]: {
      height: "100vh",
      flexGrow: 0,
    },
  },
});

/**
 * The fullscreen digital-object viewer: a routed page (deep-linkable,
 * back-button-closable) filling the frame - the record page embeds the same
 * {@link DaoViewer} beside the description. The current page travels as the
 * `file` search parameter - the pathname stays stable, so AppLayout's
 * route-change focus does not steal focus on every page turn, and Back returns
 * to the record, not through the pages (`replace`).
 */
export default function DaoViewerPage() {
  const styles = useStyles();
  const { t } = useTranslation();
  const { uuid, daoUuid } = useParams<{ uuid: string; daoUuid: string }>();
  const [searchParams, setSearchParams] = useSearchParams();
  const detail = useApuDetail(uuid);

  if (detail.isPending) {
    return <Spinner className={styles.layout} />;
  }
  if (detail.isError) {
    const notFound = detail.error instanceof ResponseError && detail.error.response.status === 404;
    return (
      <div className={styles.layout}>
        <Text>{t(notFound ? "apu.notFound" : "apu.error")}</Text>
      </div>
    );
  }

  const dao = detail.data.digitalObjects.find((candidate) => candidate.uuid === daoUuid);
  if (dao === undefined || dao.files.length === 0) {
    return (
      <div className={styles.layout}>
        <Text>{t("dao.notFound")}</Text>
        <Link to={`/apu/${uuid}`} className={styles.link}>
          {t("dao.backToRecord")}
        </Link>
      </div>
    );
  }

  return (
    <div className={styles.layout}>
      <header className={styles.header}>
        <Title2 as="h1">{dao.name ?? detail.data.name}</Title2>
        <Link to={`/apu/${uuid}`} className={styles.link}>
          {t("dao.backToRecord")}
        </Link>
      </header>
      <div className={styles.viewer}>
        <DaoViewer
          apuUuid={detail.data.uuid}
          dao={dao}
          globalKeyboard
          initialFileId={searchParams.get("file")}
          onPageChange={(fileId) => {
            if (fileId !== undefined) {
              setSearchParams({ file: fileId }, { replace: true });
            }
          }}
        />
      </div>
    </div>
  );
}
