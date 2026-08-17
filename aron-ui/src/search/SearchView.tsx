import { Button, Input, makeStyles, Spinner, Text, Title3, tokens } from "@fluentui/react-components";
import { useQuery } from "@tanstack/react-query";
import { useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { useSearchParams } from "react-router-dom";
import { searchApi } from "../api/client";
import {
  ApuType,
  type DatingFacetResult,
  type EnumFacetResult,
  type FacetDef,
  FacetDisplay,
  FacetResultKind,
  FacetType,
  QueryMode,
  type SearchFilter,
  SortMode,
  TotalRelation,
} from "../api/generated";
import { PRIMARY_DARK, PRIMARY_MAIN } from "../layout/AppHeader";
import FacetPanel from "./FacetPanel";
import { parseFilters, serializeFilters } from "./filters";
import Pagination from "./Pagination";
import ResultList from "./ResultList";

const useStyles = makeStyles({
  root: {
    display: "flex",
    gap: tokens.spacingHorizontalXXL,
    padding: `${tokens.spacingVerticalXL} ${tokens.spacingHorizontalXXL}`,
    alignItems: "flex-start",
    // narrow viewports stack the sidebar above the results
    "@media (max-width: 860px)": {
      flexDirection: "column",
      alignItems: "stretch",
      padding: `${tokens.spacingVerticalL} ${tokens.spacingHorizontalM}`,
    },
  },
  sidebar: {
    width: "320px",
    flexShrink: 0,
    display: "flex",
    flexDirection: "column",
    gap: tokens.spacingVerticalL,
    backgroundColor: tokens.colorNeutralBackground1,
    border: `1px solid ${tokens.colorNeutralStroke2}`,
    borderRadius: tokens.borderRadiusMedium,
    boxShadow: tokens.shadow2,
    padding: tokens.spacingHorizontalL,
    "@media (max-width: 860px)": {
      width: "auto",
    },
  },
  searchRow: {
    display: "flex",
  },
  searchInput: {
    flexGrow: 1,
    borderTopRightRadius: "0",
    borderBottomRightRadius: "0",
  },
  searchButton: {
    backgroundColor: PRIMARY_DARK,
    color: "#ffffff",
    borderTopLeftRadius: "0",
    borderBottomLeftRadius: "0",
    textTransform: "uppercase",
    ":hover": {
      backgroundColor: PRIMARY_MAIN,
      color: "#ffffff",
    },
  },
  main: {
    flexGrow: 1,
    display: "flex",
    flexDirection: "column",
    gap: tokens.spacingVerticalL,
    minWidth: "0",
  },
  status: {
    color: tokens.colorNeutralForeground3,
  },
});

/**
 * The search experience of one portal section (apuType set) or of the general
 * search (/apu, no apuType - no facets by design). All state lives in the URL
 * (q, p, s, f), so result pages are shareable and survive reloads.
 */
export default function SearchView({ apuType, titleKey }: { apuType?: ApuType; titleKey: string }) {
  const styles = useStyles();
  const { t } = useTranslation();
  const [params, setParams] = useSearchParams();

  const query = params.get("q") ?? "";
  const page = Math.max(1, Number(params.get("p")) || 1);
  const size = Math.max(1, Math.min(100, Number(params.get("s")) || 10));
  const filterParam = params.get("f");
  const filters = useMemo(() => parseFilters(filterParam), [filterParam]);
  const [queryInput, setQueryInput] = useState(query);
  useEffect(() => setQueryInput(query), [query]);

  const facetDefs = useQuery({
    queryKey: ["facets", apuType],
    queryFn: () => searchApi.searchGetFacets({ apuType: apuType! }),
    enabled: apuType !== undefined,
    staleTime: Infinity,
  });

  const search = useQuery({
    queryKey: ["apu-search", apuType, query, filterParam, page, size],
    queryFn: () =>
      searchApi.searchSearch({
        apuSearchRequest: {
          apuType,
          query: query || undefined,
          filters,
          from: (page - 1) * size,
          size,
          // stable alphabetical browsing without a query, relevance with one
          sort: query ? SortMode.Relevance : SortMode.Name,
        },
      }),
    placeholderData: (previous) => previous,
  });

  const update = (changes: Record<string, string | null>) => {
    const next = new URLSearchParams(params);
    for (const [key, value] of Object.entries(changes)) {
      if (value === null || value === "") {
        next.delete(key);
      } else {
        next.set(key, value);
      }
    }
    setParams(next);
  };
  const onFilters = (next: SearchFilter[]) =>
    update({ f: serializeFilters(next), p: null });
  const submitQuery = () => update({ q: queryInput.trim() || null, p: null });

  const resultOf = (code: string) => search.data?.facets.find((f) => f.code === code);
  // MULTI_REF_EXT/MULTI_TYPE_REF facets wait for their contract slice; DETAIL facets for the advanced dialog
  const supportedTypes: FacetType[] = [
    FacetType.Enum,
    FacetType.MultiRef,
    FacetType.Fulltext,
    FacetType.Unitdate,
  ];
  // a facet with no values in the current scope is hidden (the old-portal
  // rule); text inputs always show, an actively filtered facet stays visible
  // so its selection can be undone
  const hasData = (def: FacetDef) => {
    if (def.type === FacetType.Fulltext) {
      return true;
    }
    const result = resultOf(def.code);
    if (result === undefined) {
      return false;
    }
    return result.kind === FacetResultKind.Dating
      ? (result as DatingFacetResult).bounds !== undefined
      : ((result as EnumFacetResult).buckets ?? []).length > 0;
  };
  const hasActiveFilter = (code: string) => filters.some((f) => f.facet === code);
  const visibleFacets = (facetDefs.data ?? []).filter(
    (def) =>
      def.display === FacetDisplay.Always &&
      supportedTypes.includes(def.type) &&
      (hasActiveFilter(def.code) || hasData(def)),
  );

  return (
    <div className={styles.root}>
      <div className={styles.sidebar}>
        <div className={styles.searchRow}>
          <Input
            className={styles.searchInput}
            value={queryInput}
            placeholder={t("search.sidebarPlaceholder")}
            onChange={(_, data) => setQueryInput(data.value)}
            onKeyDown={(e) => e.key === "Enter" && submitQuery()}
          />
          <Button className={styles.searchButton} appearance="primary" onClick={submitQuery}>
            {t("search.button")}
          </Button>
        </div>
        {apuType !== undefined &&
          visibleFacets.map((def) => (
            <FacetPanel
              key={def.code}
              def={def}
              filters={filters}
              result={resultOf(def.code)}
              apuType={apuType}
              query={query}
              total={search.data ? Number(search.data.total) : undefined}
              onFilters={onFilters}
            />
          ))}
      </div>
      <div className={styles.main}>
        <Title3>{t(titleKey)}</Title3>
        {search.data && (
          <>
            <Pagination
              page={page}
              size={size}
              total={search.data.total}
              onPage={(p) => update({ p: p > 1 ? String(p) : null })}
              onSize={(s) => update({ s: s !== 10 ? String(s) : null, p: null })}
            />
            <Text size={200} className={styles.status}>
              {t(
                search.data.totalRelation === TotalRelation.Gte
                  ? "search.totalMore"
                  : "search.total",
                { count: search.data.total },
              )}
            </Text>
            {search.data.queryMode === QueryMode.Relaxed && (
              <Text size={200} role="status">
                {t("search.relaxed")}
              </Text>
            )}
            <ResultList items={search.data.items} />
          </>
        )}
        {search.isPending && <Spinner />}
        {search.isError && <Text>{t("search.error")}</Text>}
      </div>
    </div>
  );
}
