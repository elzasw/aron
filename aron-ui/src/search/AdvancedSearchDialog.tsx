import {
  Button,
  Dialog,
  DialogActions,
  DialogBody,
  DialogContent,
  DialogSurface,
  DialogTitle,
  makeStyles,
  Text,
  tokens,
} from "@fluentui/react-components";
import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { searchApi } from "../api/client";
import { type ApuType, type FacetDef, type SearchFilter, TotalRelation } from "../api/generated";
import FacetPanel from "./FacetPanel";
import {
  bucketLabel,
  dropInapplicable,
  facetHasData,
  facetIsApplicable,
  SUPPORTED_FACET_TYPES,
} from "./filters";

const useStyles = makeStyles({
  surface: {
    maxWidth: "720px",
    width: "calc(100% - 2 * var(--spacingHorizontalL))",
  },
  content: {
    display: "flex",
    flexDirection: "column",
    gap: tokens.spacingVerticalL,
    paddingTop: tokens.spacingVerticalS,
  },
  intro: {
    color: tokens.colorNeutralForeground3,
  },
  status: {
    color: tokens.colorNeutralForeground3,
  },
});

export interface Props {
  /** Every facet definition of the search, offered or not. */
  defs: FacetDef[];
  /** The filters currently applied; the dialog starts from them and never changes them until applied. */
  filters: SearchFilter[];
  apuType?: ApuType;
  /** Fulltext query of the current search (kept, so the counts describe the reader's actual result). */
  query: string;
  onApply: (filters: SearchFilter[]) => void;
  onClose: () => void;
}

/**
 * The advanced search: every filter a section has, the ones the sidebar hides
 * (`display: DETAIL`) included, each with the deployment's own description of
 * what it filters. The reader works on a draft - the search page behind the
 * dialog does not move while conditions are being combined - and applies it in
 * one step, so a half-built combination is never a URL somebody can copy. Cancel
 * throws the draft away.
 *
 * The draft is nevertheless searched as it changes: the counts on its options
 * and the number on the apply button answer the only question a reader has while
 * combining conditions - is anything left? - which the old portal's dialog also
 * did. The same response feeds the widgets their buckets and bounds, exactly as
 * the page's own response feeds the sidebar.
 */
export default function AdvancedSearchDialog({
  defs,
  filters,
  apuType,
  query,
  onApply,
  onClose,
}: Props) {
  const styles = useStyles();
  const { t } = useTranslation();
  const [draft, setDraft] = useState<SearchFilter[]>(filters);

  // one hit is the smallest page the contract allows; the facets and the total
  // are what this asks for
  const preview = useQuery({
    queryKey: ["apu-search-preview", apuType, query, JSON.stringify(draft)],
    queryFn: () =>
      searchApi.searchSearch({
        apuSearchRequest: { apuType, query: query || undefined, filters: draft, size: 1 },
      }),
    placeholderData: (previous) => previous,
  });

  const resultOf = (code: string) => preview.data?.facets.find((f) => f.code === code);
  const labelOf = (facet: string, value: string) => bucketLabel(resultOf(facet), value);

  // a facet that waited for a selection the reader just undid loses its own
  // constraint here too - the sidebar's rule, applied to the draft. Derived
  // rather than stored, and only once the response that carries the labels a
  // condition may match on is in: what the widgets show and what Apply writes
  // is this pruned list, never the raw one
  const effective = preview.data ? dropInapplicable(defs, draft, labelOf) : draft;
  const hasDraftFilter = (code: string) => effective.some((f) => f.facet === code);

  // every facet of the search, in the server's order: offered by default or
  // reachable only here, as long as it has something to offer in the draft's
  // scope or already carries a draft constraint
  const facets = defs.filter(
    (def) =>
      SUPPORTED_FACET_TYPES.includes(def.type) &&
      facetIsApplicable(def, effective, labelOf) &&
      (hasDraftFilter(def.code) || facetHasData(def, resultOf(def.code))),
  );

  const total = preview.data ? Number(preview.data.total) : undefined;
  const countKey =
    preview.data?.totalRelation === TotalRelation.Gte ? "search.totalMore" : "search.total";
  const statusText = preview.isError
    ? t("search.error")
    : preview.isPending
      ? t("search.loading")
      : total !== undefined
        ? t(countKey, { count: total })
        : "";

  return (
    <Dialog
      open
      onOpenChange={(_event, data) => {
        if (!data.open) {
          onClose();
        }
      }}
    >
      <DialogSurface className={styles.surface}>
        <DialogBody>
          <DialogTitle>{t("search.advanced.title")}</DialogTitle>
          <DialogContent className={styles.content}>
            <Text size={200} className={styles.intro}>
              {t("search.advanced.intro")}
            </Text>
            {facets.map((def) => (
              <FacetPanel
                key={def.code}
                def={def}
                filters={effective}
                result={resultOf(def.code)}
                apuType={apuType}
                query={query}
                total={total}
                onFilters={setDraft}
                withDescription
              />
            ))}
            {/* the draft's outcome, announced as it changes: the reader is
                combining conditions and needs to hear when nothing is left */}
            <div role="status" aria-live="polite" className={styles.status}>
              <Text size={200}>{statusText}</Text>
            </div>
          </DialogContent>
          <DialogActions>
            <Button appearance="secondary" onClick={onClose}>
              {t("search.advanced.cancel")}
            </Button>
            <Button appearance="primary" onClick={() => onApply(effective)}>
              {total !== undefined
                ? t(
                    preview.data?.totalRelation === TotalRelation.Gte
                      ? "search.advanced.applyMore"
                      : "search.advanced.apply",
                    { count: total },
                  )
                : t("search.advanced.applyPending")}
            </Button>
          </DialogActions>
        </DialogBody>
      </DialogSurface>
    </Dialog>
  );
}
