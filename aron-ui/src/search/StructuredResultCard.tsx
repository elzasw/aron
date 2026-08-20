import { makeStyles, tokens } from "@fluentui/react-components";
import { Fragment } from "react";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import type { ResultField, ResultValue, StructuredResult } from "../api/generated";
import { PRIMARY_DARK } from "../layout/palette";
import type { ResultLayoutLookup } from "./useResultLayout";

type Styles = ReturnType<typeof useStyles>;

const useStyles = makeStyles({
  // the record icon sits on a tile in the header's color, full card height; the
  // minimum width keeps the text of neighbouring cards aligned when the
  // deployment gives its icons different sizes
  icon: {
    flexShrink: 0,
    display: "flex",
    alignItems: "flex-start",
    justifyContent: "center",
    minWidth: "56px",
    backgroundColor: PRIMARY_DARK,
    padding: `${tokens.spacingVerticalM} ${tokens.spacingHorizontalM}`,
  },
  body: {
    display: "flex",
    flexDirection: "column",
    gap: tokens.spacingVerticalXXS,
    padding: `${tokens.spacingVerticalM} ${tokens.spacingHorizontalL}`,
    flexGrow: 1,
    minWidth: "0",
  },
  heading: {
    margin: "0",
    fontSize: tokens.fontSizeBase400,
    fontWeight: tokens.fontWeightSemibold,
  },
  headingLink: {
    color: tokens.colorBrandForegroundLink,
    textDecorationLine: "none",
    ":hover": {
      textDecorationLine: "underline",
    },
  },
  row: {
    display: "flex",
    flexWrap: "wrap",
    alignItems: "center",
    columnGap: tokens.spacingHorizontalXXS,
    fontSize: tokens.fontSizeBase300,
    color: tokens.colorNeutralForeground2,
  },
  field: {
    display: "inline-flex",
    alignItems: "center",
    columnGap: tokens.spacingHorizontalXXS,
  },
  fieldIcon: {
    height: "1.2em",
    width: "auto",
  },
  // deployment text that belongs to the values, e.g. "Inv. č.: "
  prefix: {
    whiteSpace: "pre",
  },
  separator: {
    whiteSpace: "pre",
    color: tokens.colorNeutralForeground4,
  },
  valueLink: {
    color: "currentcolor",
    textDecorationLine: "underline",
    ":hover": {
      color: tokens.colorBrandForegroundLink,
    },
  },
  thumbnail: {
    flexShrink: 0,
    display: "flex",
    alignItems: "flex-start",
    padding: tokens.spacingVerticalM,
  },
  thumbnailImage: {
    maxHeight: "120px",
    maxWidth: "192px",
    borderRadius: tokens.borderRadiusMedium,
  },
  /** Text carried for assistive technology only (the field's own name). */
  srOnly: {
    position: "absolute",
    width: "1px",
    height: "1px",
    overflow: "hidden",
    clip: "rect(0 0 0 0)",
    clipPath: "inset(50%)",
    whiteSpace: "nowrap",
  },
});

/**
 * One search hit that the source system delivered as a structured presentation:
 * rows of coded fields, laid out by the deployment's configuration.
 *
 * The card is deliberately **not** one big link. The old portal wrapped the
 * whole card in a link and nested the referenced values' links inside it - which
 * is invalid markup and unusable by keyboard. Here the heading is the link into
 * the record, referenced values are their own links, and the thumbnail is a
 * third, separately named one.
 */
export default function StructuredResultCard({
  uuid,
  name,
  structured,
  layout,
}: {
  uuid: string;
  name: string;
  structured: StructuredResult;
  layout: ResultLayoutLookup;
}) {
  const styles = useStyles();
  const { t } = useTranslation();

  const icon = layout.iconOf(structured.code);
  // the heading is the field the deployment marked, else the first field of the
  // first row (the source systems put the record's name there); its text becomes
  // the link into the record
  const headingCode =
    layout.headingCode ?? structured.rows[0]?.fields[0]?.code;
  const headingField = structured.rows
    .flatMap((row) => row.fields)
    .find((field) => field.code === headingCode);
  const headingText = headingField ? plainText(headingField) : "";

  return (
    <>
      {icon && (
        <div className={styles.icon}>
          {/* the heading names the card; a record-shape icon adds no information a
              reader could act on */}
          <img src={icon.url} alt="" width={icon.size} />
        </div>
      )}
      <div className={styles.body}>
        <h3 className={styles.heading}>
          <Link to={`/apu/${uuid}`} className={styles.headingLink}>
            {headingText || name}
          </Link>
        </h3>
        {structured.rows.map((row, rowIndex) => {
          // the heading is rendered above; its row keeps the remaining fields
          const fields = row.fields.filter((field) => field !== headingField);
          if (fields.length === 0) {
            return null;
          }
          return (
            <div key={rowIndex} className={styles.row}>
              {fields.map((field, fieldIndex) => (
                <Fragment key={`${field.code}-${fieldIndex}`}>
                  {fieldIndex > 0 && (
                    <span className={styles.separator} aria-hidden="true">
                      {layout.fieldSeparator}
                    </span>
                  )}
                  <Field field={field} layout={layout} styles={styles} />
                </Fragment>
              ))}
            </div>
          );
        })}
      </div>
      {structured.thumbnailUrl && (
        <div className={styles.thumbnail}>
          {/* the link carries the name, so the image adds no duplicate text; the
              target is the source system's own when it gave one, else the record */}
          {structured.thumbnailLinkUrl ? (
            <a
              href={structured.thumbnailLinkUrl}
              aria-label={t("search.thumbnailLink", { name: headingText || name })}
            >
              <img src={structured.thumbnailUrl} alt="" className={styles.thumbnailImage} />
            </a>
          ) : (
            <Link
              to={`/apu/${uuid}`}
              aria-label={t("search.thumbnailLink", { name: headingText || name })}
            >
              <img src={structured.thumbnailUrl} alt="" className={styles.thumbnailImage} />
            </Link>
          )}
        </div>
      )}
    </>
  );
}

/** One coded field: its icon, the deployment's prefix, then the values. */
function Field({
  field,
  layout,
  styles,
}: {
  field: ResultField;
  layout: ResultLayoutLookup;
  styles: Styles;
}) {
  const style = layout.styleOf(field.code);
  const separator = style?.valueSeparator ?? " ";
  return (
    <span
      className={styles.field}
      style={{
        color: style?.color,
        fontSize: style?.scale ? `${style.scale}em` : undefined,
        fontWeight: style?.bold ? tokens.fontWeightSemibold : undefined,
      }}
    >
      {style?.iconUrl && <img src={style.iconUrl} alt="" className={styles.fieldIcon} />}
      {style?.prefix ? (
        <span className={styles.prefix}>{style.prefix}</span>
      ) : (
        // a field code means nothing on its own, so a reader who cannot see the
        // layout gets the field's configured name instead
        style?.label && <span className={styles.srOnly}>{style.label}: </span>
      )}
      {field.values.map((value, index) => (
        <Fragment key={index}>
          {index > 0 && (
            <span className={styles.separator} aria-hidden="true">
              {separator}
            </span>
          )}
          <Value value={value} styles={styles} />
        </Fragment>
      ))}
    </span>
  );
}

function Value({ value, styles }: { value: ResultValue; styles: Styles }) {
  if (value.refUuid) {
    return (
      <Link to={`/apu/${value.refUuid}`} className={styles.valueLink}>
        {value.text}
      </Link>
    );
  }
  return <span>{value.text}</span>;
}

/** The field's values as one string - what the heading link reads. */
function plainText(field: ResultField): string {
  return field.values.map((value) => value.text).join(" ").trim();
}
