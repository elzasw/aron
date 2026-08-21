import { Button, Input, makeStyles, Select, Spinner, Text, Title3, tokens } from "@fluentui/react-components";
import { useQuery } from "@tanstack/react-query";
import { useApiLanguage } from "../i18n/useApiLanguage";
import { useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useSearchParams } from "react-router-dom";
import { searchApi } from "../api/client";
import {
  ApuType,
  type DatingFacetResult,
  type EnumFacetResult,
  type FacetDef,
  FacetResultKind,
  FacetType,
  type MenuItemCode,
  QueryMode,
  type SearchFilter,
  SortMode,
  TotalRelation,
} from "../api/generated";
import { PRIMARY_DARK, PRIMARY_MAIN } from "../layout/AppHeader";
import { SECTIONS } from "../sections";
import FacetPanel from "./FacetPanel";
import { facetIsOffered, parseFilters, serializeFilters } from "./filters";
import Pagination from "./Pagination";
import RelatedChips from "./RelatedChips";
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
  sortRow: {
    display: "flex",
    alignItems: "center",
    gap: tokens.spacingHorizontalS,
    fontSize: tokens.fontSizeBase200,
    color: tokens.colorNeutralForeground3,
  },
  // per-type counts of the general search: one chip per record type
  typeChips: {
    display: "flex",
    flexWrap: "wrap",
    gap: tokens.spacingHorizontalS,
  },
  chip: {
    padding: `${tokens.spacingVerticalXXS} ${tokens.spacingHorizontalM}`,
    borderRadius: tokens.borderRadiusCircular,
    backgroundColor: tokens.colorNeutralBackground3,
    color: tokens.colorBrandForegroundLink,
    fontSize: tokens.fontSizeBase200,
    textDecorationLine: "none",
    ":hover": {
      backgroundColor: tokens.colorNeutralBackground3Hover,
      textDecorationLine: "underline",
    },
  },
});

/**
 * URL `sort` values. The empty string sends no `sort` parameter, i.e. the
 * contract's AUTO: relevance with a query, name without one. Both are what
 * RELEVANCE itself yields - with nothing scored, its name tie-break decides -
 * so the two share this single entry, labelled as relevance.
 */
const SORT_OPTIONS = ["", "NAME", "NAME_DESC", "DATE_ASC", "DATE_DESC"] as const;

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
  // unset = AUTO: the server picks relevance with a query, name without
  const sortParam = params.get("sort") ?? "";
  const lang = useApiLanguage();
  const [queryInput, setQueryInput] = useState(query);
  // the box follows the query in the URL (back/forward, shared link); adjusting
  // state during render costs one pass instead of an effect's cascade
  const [lastQuery, setLastQuery] = useState(query);
  if (query !== lastQuery) {
    setLastQuery(query);
    setQueryInput(query);
  }

  const facetDefs = useQuery({
    queryKey: ["facets", apuType, lang],
    queryFn: () => searchApi.searchGetFacets({ apuType: apuType!, lang }),
    enabled: apuType !== undefined,
    staleTime: Infinity,
  });

  const search = useQuery({
    queryKey: ["apu-search", apuType, query, filterParam, page, size, sortParam],
    queryFn: () =>
      searchApi.searchSearch({
        apuSearchRequest: {
          apuType,
          query: query || undefined,
          filters,
          from: (page - 1) * size,
          size,
          sort: sortParam ? (sortParam as SortMode) : undefined,
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
  // every facet type in the contract has a widget below; the list is the seam for
  // the next one - an unsupported type is hidden rather than rendered empty
  const supportedTypes: FacetType[] = [
    FacetType.Enum,
    FacetType.Ref,
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

  // what the live region below reports: the failure, the wait, or the outcome
  const statusText = search.isError
    ? t("search.error")
    : search.isPending
      ? t("search.loading")
      : search.data
        ? t(
            search.data.totalRelation === TotalRelation.Gte ? "search.totalMore" : "search.total",
            { count: search.data.total },
          )
        : "";
  // switching section keeps what the reader has narrowed to - the query and every
  // filter, the relation constraint included (it is not bound to one section)
  const sectionParams = (() => {
    const next = new URLSearchParams();
    if (query) {
      next.set("q", query);
    }
    if (filterParam) {
      next.set("f", filterParam);
    }
    const serialized = next.toString();
    return serialized ? `?${serialized}` : "";
  })();

  // offered by default, or - the DETAIL case - because it carries a constraint
  // the reader has to be able to see and undo
  const visibleFacets = (facetDefs.data ?? []).filter(
    (def) =>
      facetIsOffered(def.display, hasActiveFilter(def.code)) &&
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
            aria-label={t("search.sidebarPlaceholder")}
            placeholder={t("search.sidebarPlaceholder")}
            onChange={(_, data) => setQueryInput(data.value)}
            onKeyDown={(e) => e.key === "Enter" && submitQuery()}
          />
          <Button className={styles.searchButton} appearance="primary" onClick={submitQuery}>
            {t("search.button")}
          </Button>
        </div>
        <RelatedChips filters={filters} onFilters={onFilters} />
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
        <Title3 as="h1">{t(titleKey)}</Title3>
        {search.data && (
          <>
            {apuType === undefined && search.data.typeCounts.length > 0 && (
              <nav aria-label={t("search.typeCountsLabel")} className={styles.typeChips}>
                {search.data.typeCounts.map((typeCount) => {
                  const section = SECTIONS[typeCount.apuType as unknown as MenuItemCode];
                  const label = `${t(`sections.${typeCount.apuType}`)} (${typeCount.count})`;
                  return section?.route ? (
                    <Link
                      key={typeCount.apuType}
                      to={`${section.route}${sectionParams}`}
                      className={styles.chip}
                    >
                      {label}
                    </Link>
                  ) : (
                    <span key={typeCount.apuType} className={styles.chip}>
                      {label}
                    </span>
                  );
                })}
              </nav>
            )}
            <Pagination
              page={page}
              size={size}
              total={search.data.total}
              onPage={(p) => update({ p: p > 1 ? String(p) : null })}
              onSize={(s) => update({ s: s !== 10 ? String(s) : null, p: null })}
              controls={
                <div className={styles.sortRow}>
                  <label htmlFor="search-sort">{t("search.sortLabel")}</label>
                  <Select
                    id="search-sort"
                    value={sortParam}
                    onChange={(_, data) => update({ sort: data.value || null, p: null })}
                  >
                    {SORT_OPTIONS.map((option) => (
                      <option key={option} value={option}>
                        {t(`search.sort.${option || "RELEVANCE"}`)}
                      </option>
                    ))}
                  </Select>
                </div>
              }
            />
          </>
        )}
        {/* One live region, always mounted: filters and paging apply without a
            navigation, so the outcome (count, loading, failure) has to be
            announced rather than only redrawn - WCAG 4.1.3. */}
        <div role="status" aria-live="polite" className={styles.status}>
          <Text size={200}>{statusText}</Text>
          {search.data?.queryMode === QueryMode.Relaxed && (
            <div>
              <Text size={200}>{t("search.relaxed")}</Text>
            </div>
          )}
        </div>
        {search.data && <ResultList items={search.data.items} />}
        {search.isPending && <Spinner />}
      </div>
    </div>
  );
}
