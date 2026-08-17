import "@typespec/http";
import "@typespec/openapi";

using TypeSpec.Http;
using TypeSpec.OpenAPI;

/**
 * ARON portal API (v1) - the contract between the ARON backend and its user
 * interface (and possible external consumers).
 *
 * Paths carry the full external prefix `/api/v1`; the application adds no
 * further prefix. Clients resolve the base URL from the page's
 * `window.serverContextPath` (injected by the server into the SPA shell), so the
 * same UI build works at the URL root and under a subpath.
 *
 * The contract deliberately exposes no search-engine internals - search and
 * facet models are typed DTOs, so the indexing backend can evolve or be swapped
 * without breaking clients.
 */
@service(#{ title: "ARON Portal API" })
@info(#{ version: "1.0" })
namespace AronApi;

/** Technical/system endpoints. */
@tag("system")
@route("/api/v1/system")
namespace System {
	/** Basic information about the running application. */
	@get
	@route("/info")
	op getInfo(): SystemInfo;
}

/** Basic information about the running application. */
model SystemInfo {
	/** Application name. */
	name: string;

	/** Application version (build version; "dev" when built without build info). */
	version: string;
}

/**
 * UI configuration and branding. The portal UI is deployment-configured
 * (webResources files on the server); these endpoints expose that configuration
 * as typed data - clients never parse server config files.
 */
@tag("ui")
@route("/api/v1/ui")
namespace Ui {
	/** UI configuration: portal name, available localizations, top-level menu. */
	@get
	@route("/config")
	op getConfig(): UiConfig;

	/** Portal logo image (deployment-supplied; format varies, see Content-Type). */
	@get
	@route("/logo")
	op getLogo(): {
		@header contentType: "image/svg+xml" | "image/png" | "image/jpeg";

		@body body: bytes;
	};
}

/** UI configuration of the deployment. */
model UiConfig {
	/** Portal name displayed in the header and the browser tab. */
	name: string;

	/**
	 * Available localizations (IETF-like codes, e.g. `cs_CZ`). The UI offers a
	 * language switcher only when more than one is listed.
	 */
	localizations: string[];

	/** Top-level menu: which sections this deployment enables, in display order. */
	menuItems: MenuItem[];
}

/** One item of the top-level menu. */
model MenuItem {
	/** Section code; the UI maps it to its route and localized label. */
	code: MenuItemCode;

	/** Accent color of the section (CSS color); the UI supplies a default palette. */
	color?: string;

	/** External URL - the item is a plain link instead of an in-app section (typically HELP or NEWS). */
	url?: string;
}

/** Portal sections available for the top-level menu. */
enum MenuItemCode {
	INSTITUTION,
	FUND,
	FINDING_AID,
	ARCH_DESC,
	ENTITY,
	ORIGINATOR,
	NEWS,
	HELP,
}

/**
 * Search over archival presentation units (APU). Facet semantics live
 * server-side (deployment configuration): clients send facet codes and values,
 * never engine structures, so the search backend stays swappable.
 */
@tag("search")
namespace Search {
	/**
	 * Typed facet definitions of one section, in the deployment-configured
	 * order. Labels are server-localized (deployment configuration is the
	 * label source).
	 */
	@get
	@route("/api/v1/facets")
	op getFacets(@query apuType: ApuType): FacetDef[];

	/**
	 * Searches APUs of one type. Facet bucket counts follow multi-select
	 * semantics: counts of a facet reflect the query and all filters EXCEPT
	 * that facet's own VALUES filter.
	 */
	@post
	@route("/api/v1/apu/search")
	op search(@body request: ApuSearchRequest): ApuSearchResponse;
}

/** Types of archival presentation units. */
enum ApuType {
	ARCH_DESC,
	COLLECTION,
	ENTITY,
	FINDING_AID,
	FUND,
	INSTITUTION,
}

/** Facet kinds (mirrors the searchConfig.yaml model). */
enum FacetType {
	/** Text filter on one item field. */
	FULLTEXT,

	/** Enumerated values with bucket counts; multi-select works as OR. */
	ENUM,

	/** Reference to archival entities; filterable by value (suggest UI arrives later). */
	MULTI_REF,

	/** Dating interval. */
	UNITDATE,

	/** Advanced reference facet (not yet supported by the UI). */
	MULTI_REF_EXT,

	/** Advanced reference facet (not yet supported by the UI). */
	MULTI_TYPE_REF,
}

/** When the facet is offered. */
enum FacetDisplay {
	ALWAYS,
	DETAIL,
}

/** Ordering of enumerated facet buckets. */
enum FacetOrder {
	/** By count, descending. */
	FREQ,

	/** Alphabetically. */
	ASC,
}

/** Definition of one search facet (deployment-configured). */
model FacetDef {
	/** Facet code = source item-type code (tilde form, e.g. `LANG~CODE`); used in filters. */
	code: string;

	type: FacetType;

	/** Display label (server-localized). */
	label: string;

	tooltip?: string;

	description?: string;

	display: FacetDisplay;

	/** Bucket ordering (ENUM facets). */
	orderBy?: FacetOrder;

	/** Buckets shown collapsed (ENUM facets); more after expanding. */
	displayedItems?: int32;

	/** Bucket limit when expanded (ENUM facets); unset = no limit. */
	maxDisplayedItems?: int32;
}

/** Search request over APUs. */
model ApuSearchRequest {
	/** Section restriction; unset = search across all types (general search, no facets). */
	apuType?: ApuType;

	/** Fulltext query over the APU name and description; unset = match all. */
	query?: string;

	/** Active filters; each references a facet of the section by its code. */
	filters?: SearchFilter[];

	/** Zero-based offset of the first returned item. */
	@minValue(0)
	from?: int32 = 0;

	/** Page size. */
	@minValue(1)
	@maxValue(100)
	size?: int32 = 10;

	sort?: SortMode = SortMode.RELEVANCE;
}

/** Named sort modes. */
enum SortMode {
	RELEVANCE,

	/** Czech-alphabetical by name (collation). */
	NAME,
}

/** Filter kinds - the SearchFilter discriminator. */
enum FilterKind {
	VALUES,
	TEXT,
	RANGE,
}

/**
 * One active filter, discriminated by `kind`. TypeSpec requires every subtype
 * to re-declare the discriminator literal; the emitted OpenAPI is post-processed
 * (scripts/strip-discriminator-redeclaration.mjs) into the classic
 * parent-only-property shape that code generators handle (CAM pattern).
 */
@discriminator("kind")
model SearchFilter {
	/**
	 * Filter kind - the discriminator. A named enum so the base and every
	 * subtype share one type.
	 */
	kind: FilterKind;

	/** Code of the facet the filter applies to. */
	facet: string;
}

/** Selected values of an ENUM/MULTI_REF facet; a document matches ANY of them (OR). */
model ValuesFilter extends SearchFilter {
	kind: FilterKind.VALUES;
	values: string[];
}

/** Text filter of a FULLTEXT facet (analyzed match on the facet's field). */
model TextFilter extends SearchFilter {
	kind: FilterKind.TEXT;
	q: string;
}

/**
 * Dating filter of a UNITDATE facet: matches documents whose interval
 * intersects [from, to]. Bounds accept a year (`1190`), a date (`1190-05-01`)
 * or a full ISO date-time; either bound may be omitted.
 */
model RangeFilter extends SearchFilter {
	kind: FilterKind.RANGE;
	from?: string;
	to?: string;
}

/** Search response. */
model ApuSearchResponse {
	/** Total number of matching APUs (independent of paging). */
	total: int64;

	items: ApuSearchItem[];

	/** Bucket counts of the section's ENUM facets (multi-select semantics). */
	facets: FacetResult[];
}

/** One search hit. */
model ApuSearchItem {
	uuid: string;
	name: string;
	description?: string;
	apuType: ApuType;
	containsDigitalObjects: boolean;
}

/** Bucket counts of one facet. */
model FacetResult {
	/** Facet code. */
	code: string;

	buckets: FacetBucket[];
}

/** One value bucket of an enumerated facet. */
model FacetBucket {
	value: string;

	/** Display label when it differs from the value (e.g. resolved references). */
	label?: string;

	count: int64;
}
