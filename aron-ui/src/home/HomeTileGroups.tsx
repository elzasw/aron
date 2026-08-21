import { makeStyles, mergeClasses, tokens } from "@fluentui/react-components";
import { Link } from "react-router-dom";
import { type HomeTile, type TileGroup, TileStyle } from "../api/generated";
import { PRIMARY_DARK, PRIMARY_MAIN } from "../layout/palette";
import { isLinkTile, tileUrl } from "./tiles";

const useStyles = makeStyles({
  groups: {
    display: "flex",
    flexDirection: "column",
    gap: tokens.spacingVerticalXXL,
    width: "100%",
  },
  heading: {
    margin: `0 0 ${tokens.spacingVerticalM} 0`,
    fontSize: tokens.fontSizeBase500,
    fontWeight: tokens.fontWeightSemibold,
    color: PRIMARY_MAIN,
  },
  list: {
    listStyleType: "none",
    margin: "0",
    padding: "0",
  },
  // LIST: two columns of rows on a wide viewport, one on a narrow one - the
  // original portal's proportions for a long list of prepared queries
  listRows: {
    display: "grid",
    gridTemplateColumns: "repeat(auto-fill, minmax(min(20rem, 100%), 1fr))",
    columnGap: tokens.spacingHorizontalXXL,
    rowGap: tokens.spacingVerticalS,
  },
  // GRID: picture tiles. The column count follows the viewport, so a tile
  // spanning two columns stays within the grid at every width.
  grid: {
    display: "grid",
    gridAutoFlow: "dense",
    gridTemplateColumns: "repeat(2, 1fr)",
    gridAutoRows: "minmax(8rem, auto)",
    gap: tokens.spacingHorizontalM,
    "@media (min-width: 640px)": {
      gridTemplateColumns: "repeat(4, 1fr)",
    },
    "@media (min-width: 1024px)": {
      gridTemplateColumns: "repeat(6, 1fr)",
    },
  },
  // A tile is one link and contains no other interactive element, so the whole
  // surface may be the target - unlike a result card, whose own links would end
  // up nested inside it. A big target is easier to hit (WCAG 2.5.5).
  rowLink: {
    display: "flex",
    alignItems: "baseline",
    gap: tokens.spacingHorizontalS,
    padding: `${tokens.spacingVerticalXS} 0`,
    color: tokens.colorNeutralForeground1,
    textDecorationLine: "none",
    ":hover .aron-tile-label": {
      textDecorationLine: "underline",
    },
  },
  rowMark: {
    height: "1.25em",
    width: "auto",
    alignSelf: "center",
    flexShrink: 0,
  },
  rowText: {
    display: "flex",
    flexDirection: "column",
    minWidth: "0",
  },
  rowLabel: {
    color: tokens.colorBrandForegroundLink,
  },
  note: {
    fontSize: tokens.fontSizeBase200,
    color: tokens.colorNeutralForeground3,
  },
  gridLink: {
    position: "relative",
    display: "flex",
    alignItems: "flex-start",
    // the <li> is the grid item; the link fills it, so the whole cell is the target
    width: "100%",
    height: "100%",
    overflow: "hidden",
    borderRadius: tokens.borderRadiusMedium,
    // an imageless tile keeps the deployment's primary colour, so a group with
    // pictures for only some of its tiles still reads as one set
    backgroundColor: PRIMARY_MAIN,
    boxShadow: tokens.shadow4,
    textDecorationLine: "none",
    ":hover .aron-tile-picture": {
      transform: "scale(1.05)",
    },
  },
  // the picture sits behind the label rather than in an <img>: it is decoration,
  // and the label is the accessible name
  picture: {
    position: "absolute",
    inset: "0",
    backgroundSize: "cover",
    backgroundPosition: "50% 50%",
    transitionProperty: "transform",
    transitionDuration: tokens.durationSlower,
    transitionTimingFunction: tokens.curveDecelerateMid,
  },
  gridText: {
    position: "relative",
    display: "flex",
    flexDirection: "column",
    maxWidth: "100%",
  },
  // the label rides on a solid band of the darker shade: white text directly on
  // a photograph has no guaranteed contrast, on this it has the header's
  gridLabel: {
    backgroundColor: PRIMARY_DARK,
    color: "#ffffff",
    padding: `${tokens.spacingVerticalXS} ${tokens.spacingHorizontalM}`,
    fontSize: tokens.fontSizeBase400,
    fontWeight: tokens.fontWeightSemibold,
  },
  gridNote: {
    backgroundColor: PRIMARY_DARK,
    color: "#ffffff",
    padding: `${tokens.spacingVerticalXXS} ${tokens.spacingHorizontalM}`,
    fontSize: tokens.fontSizeBase200,
    fontStyle: "italic",
    alignSelf: "flex-start",
  },
});

type Styles = ReturnType<typeof useStyles>;

/** One tile's destination: an external site, or a search inside the portal. */
function TileLink({
  tile,
  className,
  children,
}: {
  tile: HomeTile;
  className: string;
  children: React.ReactNode;
}) {
  const url = tileUrl(tile);
  if (url === undefined) {
    return null;
  }
  // an external destination is a plain anchor; an in-app one goes through the
  // router, so the portal is not reloaded
  return isLinkTile(tile) ? (
    <a className={className} href={url}>
      {children}
    </a>
  ) : (
    <Link className={className} to={url}>
      {children}
    </Link>
  );
}

function ListTile({ tile, styles }: { tile: HomeTile; styles: Styles }) {
  return (
    <li>
      <TileLink tile={tile} className={styles.rowLink}>
        {tile.imageUrl && <img className={styles.rowMark} src={tile.imageUrl} alt="" />}
        <span className={styles.rowText}>
          <span className={mergeClasses(styles.rowLabel, "aron-tile-label")}>{tile.label}</span>
          {tile.note && <span className={styles.note}>{tile.note}</span>}
        </span>
      </TileLink>
    </li>
  );
}

function GridTile({ tile, styles }: { tile: HomeTile; styles: Styles }) {
  return (
    <li
      style={{
        gridColumn: tile.columnSpan ? `span ${tile.columnSpan}` : undefined,
        gridRow: tile.rowSpan ? `span ${tile.rowSpan}` : undefined,
      }}
    >
      <TileLink tile={tile} className={styles.gridLink}>
        {tile.imageUrl && (
          <span
            className={mergeClasses(styles.picture, "aron-tile-picture")}
            style={{
              backgroundImage: `url('${encodeURI(tile.imageUrl)}')`,
              backgroundPositionX: tile.imagePositionX,
              backgroundPositionY: tile.imagePositionY,
            }}
          />
        )}
        <span className={styles.gridText}>
          <span className={styles.gridLabel}>{tile.label}</span>
          {tile.note && <span className={styles.gridNote}>{tile.note}</span>}
        </span>
      </TileLink>
    </li>
  );
}

/**
 * The deployment's curated entry points: groups of tiles, each group a named
 * region so a reader can be told what a set of links is for.
 *
 * A group is a list of links and nothing more - which is why the tile itself is
 * the link (see `rowLink`), and why a picture is a background rather than an
 * `<img>`: the label carries the meaning, the picture only makes the set
 * recognizable.
 */
export default function HomeTileGroups({ groups }: { groups: TileGroup[] }) {
  const styles = useStyles();

  return (
    <div className={styles.groups}>
      {groups.map((group, index) => {
        const headingId = `home-group-${index}`;
        const grid = group.style === TileStyle.Grid;
        return (
          <section key={headingId} aria-labelledby={headingId}>
            <h2 className={styles.heading} id={headingId}>
              {group.label}
            </h2>
            <ul className={mergeClasses(styles.list, grid ? styles.grid : styles.listRows)}>
              {group.tiles.map((tile, tileIndex) =>
                grid ? (
                  <GridTile key={tileIndex} tile={tile} styles={styles} />
                ) : (
                  <ListTile key={tileIndex} tile={tile} styles={styles} />
                ),
              )}
            </ul>
          </section>
        );
      })}
    </div>
  );
}
