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

/**
 * Presentation language of the server-rendered display text in a response -
 * item and part labels, facet labels, formatted datings. An IETF language tag
 * (`cs`, `en-GB`); an `Accept-Language`-style list with quality values is
 * accepted too.
 *
 * Deliberately an explicit parameter rather than the `Accept-Language` header:
 * the language is then part of the URL, so caches and shared links carry it.
 * Absent or unmatched, the deployment's first configured localization applies
 * (`UiConfig.localizations`).
 *
 * This is the *reader's* language. It never affects which records match or the
 * order they come in - alphabetical ordering follows the language of the
 * described material, a deployment setting.
 */
model LangParam {
	@query lang?: string;
}

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

	/**
	 * Application version (build version; "dev" when built without build info).
	 *
	 * Absent unless the deployment exposes it (`system.expose-version`). Which
	 * version is running answers a support question and helps whoever debugs an
	 * installation, and it tells a reader of a public portal nothing they can
	 * act on - so a deployment that does not want it disclosed does not send it,
	 * rather than sending it and asking the client not to show it.
	 */
	version?: string;
}

/**
 * UI configuration and branding. The portal UI is deployment-configured
 * (webResources files on the server); these endpoints expose that configuration
 * as typed data - clients never parse server config files.
 */
@tag("ui")
@route("/api/v1/ui")
namespace Ui {
	/**
	 * UI configuration: portal name, available localizations, top-level menu and
	 * footer links. Footer link labels are server-rendered in the requested
	 * language.
	 */
	@get
	@route("/config")
	op getConfig(...LangParam): UiConfig;

	/** Portal logo image (deployment-supplied; format varies, see Content-Type). */
	@get
	@route("/logo")
	op getLogo(): {
		@header contentType: "image/svg+xml" | "image/png" | "image/jpeg";

		@body body: bytes;
	};

	/**
	 * One image the deployment supplies by name - a record or field icon of the
	 * structured results, a picture of a home-page tile, a mark beside a link, a
	 * thumbnail delivered as a file name. Clients never build these URLs
	 * themselves: whatever carries an image carries a ready-to-use one.
	 */
	@get
	@route("/images/{name}")
	op getImage(
		/** File name within the deployment's image directory; no path segments. */
		@path name: string,
	): {
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

	/**
	 * Links the deployment publishes in the portal footer, in display order -
	 * legally required ones (the accessibility statement of a public-sector
	 * body), operator information, contacts, anything else the deployment adds.
	 *
	 * Empty is a valid configuration: a portal embedded in a customer's own web
	 * presentation lets the host render its footer, and the obligations then
	 * belong to the host page.
	 */
	footerLinks: FooterLink[];

	/**
	 * What the deployment publishes on its own home page: groups of curated
	 * entry points. Absent when it configures none - the home page is then the
	 * search box alone.
	 */
	homePage?: HomePage;

	/**
	 * Citation forms this deployment offers, in display order, and the record
	 * types each of them covers - the record page offers a citation only where
	 * a form covers the record's type, so a reader is never offered one that
	 * cannot be produced.
	 *
	 * A list because archival practice knows several citation norms: adding a
	 * second one is deployment configuration, not a contract change. Empty when
	 * the deployment configures none, which turns the feature off.
	 */
	citations: CitationForm[];
}

/**
 * Roles a footer link can have. A well-known role lets the UI label the link
 * itself (in the reader's language) and lets a deployment be checked for the
 * links it is legally required to publish; anything else is a plain labelled
 * link with no code.
 */
enum FooterLinkCode {
	/** Accessibility statement - public-sector deployments must publish one. */
	ACCESSIBILITY,

	/** Personal-data / privacy information. */
	PRIVACY,

	/** Terms of use. */
	TERMS,

	/** Contact of the operator. */
	CONTACT,
}

/** One footer link of the deployment. */
model FooterLink {
	/** Well-known role, when the link has one. */
	code?: FooterLinkCode;

	/**
	 * Display label in the requested language. Absent only for a well-known
	 * `code`, which the UI labels itself; a link without a code always carries
	 * one.
	 */
	label?: string;

	url: string;

	/**
	 * Ready-to-use URL of a small mark to show beside the label, for links a
	 * reader recognizes by their sign rather than by their name. Deployment-
	 * supplied and server-built, like every other configured image, so this
	 * contract names no third party: the marks a portal shows are its operator's
	 * own affair, they change without warning (Twitter became X), and a closed
	 * list of them here would turn every rename into a breaking change and every
	 * addition into a release. Swapping the file is enough.
	 *
	 * Always **same-origin**: the deployment configures a file of its own image
	 * directory, never a URL, so no page needs a third-party host - which keeps
	 * a strict `img-src` workable and keeps the reader's browser from announcing
	 * the visit to whoever hosts the mark.
	 *
	 * The `label` stays the link's accessible name, so the mark never carries
	 * meaning on its own.
	 */
	imageUrl?: string;
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
 * One citation form the deployment offers. What a citation *says* - which
 * identifiers appear, in which order, with which abbreviations - is agreed
 * between the deployment and its archivists per citation norm, so this contract
 * carries only a form's identity, its name and where it applies.
 */
model CitationForm {
	/** Stable code of the form as configured (e.g. `DEFAULT`, `CSN_ISO_690`). */
	code: string;

	/** Server-localized display name of the form. */
	label: string;

	/**
	 * Record types the form covers. A record of any other type has no citation
	 * of this form.
	 */
	apuTypes: ApuType[];
}

/**
 * The deployment's home page: groups of curated entry points - the "prepared
 * queries" a reader is offered before typing anything, and links to related
 * institutions. Which record types exist is not configured here; the menu
 * decides that (`MenuItem`).
 */
model HomePage {
	groups: TileGroup[];

	/**
	 * The deployment's own footer band at the foot of the home page: who runs
	 * the portal, what it presents, how to reach them. Deliberately part of the
	 * home page rather than of the application frame - the frame is one viewport
	 * tall and every row of it is taken from the routed content, so a band on
	 * every page would cost the record detail its pane height for good. The
	 * frame keeps the slim `footerLinks` row, which is where a deployment's
	 * legally required links stay visible everywhere.
	 */
	footer?: HomeFooter;
}

/** The home page's footer band: columns, in display order. */
model HomeFooter {
	columns: FooterColumn[];
}

/**
 * One column of the band. Its prose comes first, then its links - a footer
 * column reads that way round, so the order needs no configuration.
 */
model FooterColumn {
	/** Column heading in the reader's language, when it has one. */
	heading?: string;

	paragraphs: FooterParagraph[];

	links: FooterLink[];
}

/**
 * One paragraph of prose. A list of runs rather than a string, because these
 * sentences carry links inside them ("a web application of the *Moravian
 * Provincial Archives* presenting ..."): the deployment writes the sentence
 * with named placeholders and the server resolves them, so the portal renders
 * real anchors it controls and never configured markup.
 */
model FooterParagraph {
	runs: TextRun[];
}

/**
 * One stretch of a paragraph: plain text, or - when `url` is set - a link.
 * Deliberately not a discriminated union: with one optional field and no
 * ambiguity, a union would buy three models and a type predicate for nothing.
 */
model TextRun {
	text: string;

	url?: string;
}

/** One labelled group of tiles, rendered in one style. */
model TileGroup {
	/**
	 * Group heading in the reader's language. Required: a group of links is a
	 * navigable region and needs a name of its own.
	 */
	label: string;

	style: TileStyle;

	tiles: HomeTile[];
}

/** How a group lays its tiles out. */
enum TileStyle {
	/** Compact rows - suits a long list of links, with a small image per row. */
	LIST,

	/**
	 * Picture tiles on a responsive grid. A tile with no image keeps the
	 * deployment's primary colour, so an imageless grid is still coherent.
	 */
	GRID,
}

/** Where a tile leads - the discriminator of {@link HomeTile}. */
enum TileKind {
	/** Out of the portal. */
	LINK,

	/** Into one section's search. */
	SEARCH,
}

/**
 * One entry point on the home page, discriminated by `kind` - the same
 * `ItemData` pattern as `SearchFilter` and `DetailItem`.
 */
@discriminator("kind")
model HomeTile {
	kind: TileKind;

	/** Tile label in the reader's language. */
	label: string;

	/** One line below the label - what the tile covers. Always visible. */
	note?: string;

	/**
	 * Ready-to-use URL of the tile's image, built by the server from the
	 * deployment's image directory - clients never assemble one, which is what
	 * keeps a single artifact serving the URL root and any subpath. Always
	 * same-origin: configuration names a file, never a URL.
	 */
	imageUrl?: string;

	/**
	 * CSS background position of the image, for a photo whose subject is not in
	 * the middle. A percentage or a keyword; absent = centred.
	 */
	imagePositionX?: string;

	imagePositionY?: string;

	/** Grid cells the tile spans (GRID only), 1-2; absent = one cell. */
	columnSpan?: int32;

	rowSpan?: int32;
}

/** A tile leading out of the portal - another archive, a related project. */
model LinkTile extends HomeTile {
	@invisible(Lifecycle)
	kind: TileKind.LINK;

	/** Absolute `http`/`https`/`mailto` URL. */
	url: string;
}

/**
 * A tile leading into one section's search with the deployment's own filters
 * already applied. The filters are this contract's own `SearchFilter`s, so the
 * search page receives them as ordinary constraints the reader can see in the
 * sidebar and undo - a tile is a starting point, not a locked scope.
 *
 * `apuType` is required: a tile's filters are validated against the section's
 * configured facets at startup, so a tile can never lead into a search the
 * server then rejects. The general search's built-in facets are deliberately
 * not offered here - a tile names a destination, and "everything" is not one.
 */
model SearchTile extends HomeTile {
	@invisible(Lifecycle)
	kind: TileKind.SEARCH;

	apuType: ApuType;

	query?: string;

	filters: SearchFilter[];
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
	 *
	 * Without `apuType` these are the general search's **built-in** facets
	 * (reserved codes, see `SearchFilter.facet`): they carry no deployment
	 * configuration, because a search spanning every record type cannot be
	 * scoped by a section's item types. Their labels are server-localized all
	 * the same, so a client needs no vocabulary of its own.
	 */
	@get
	@route("/api/v1/facets")
	op getFacets(@query apuType?: ApuType, ...LangParam): FacetDef[];

	/**
	 * Searches APUs of one type. Facet results follow multi-select semantics:
	 * bucket counts of a facet reflect the query and all filters EXCEPT that
	 * facet's own VALUES filter, and dating bounds of a UNITDATE facet ignore
	 * that facet's own RANGE filter - so an active selection can always be
	 * widened.
	 */
	@post
	@route("/api/v1/apu/search")
	op search(@body request: ApuSearchRequest): ApuSearchResponse;

	/**
	 * Options of one enumerated facet (ENUM/REF) for its picker - the
	 * type-ahead companion of `search`. The same multi-select semantics apply:
	 * the request's query and filters constrain the options EXCEPT the facet's
	 * own VALUES filter. `q` narrows options by their display label (label of
	 * a reference, the value itself of an ENUM): diacritics-insensitive, all
	 * words must match with the last one as a prefix. Facets of other kinds
	 * (FULLTEXT, UNITDATE) have no options - 400.
	 */
	@post
	@route("/api/v1/facets/{code}/options")
	op getFacetOptions(@path code: string, @body request: FacetOptionsRequest): FacetOptionsResponse;

	/**
	 * Deployment layout of structured search results: how the record and field
	 * codes carried by `StructuredResult` are presented. Language-dependent
	 * (prefixes and labels are deployment text).
	 *
	 * Empty lists are a valid answer - a deployment that configures no layout
	 * still gets structured results, rendered with the client's own defaults.
	 */
	@get
	@route("/api/v1/search/result-layout")
	op getResultLayout(...LangParam): ResultLayout;
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

	/**
	 * Relation to another APU: the facet's values are APU uuids, and a document
	 * matches when one of its reference items points at one of them. Filtered by
	 * `ValuesFilter` on the facet's own field, or by `RelatedFilter`, which adds
	 * the direction and the built-in all-reference-types scope.
	 *
	 * One type on purpose: the old portal split this three ways, of which one
	 * was never implemented and the other two differed only by narrowing on the
	 * relation type - which belongs on the filter, not in a facet type.
	 */
	REF,

	/** Dating interval. */
	UNITDATE,
}

/** When the facet is offered. */
enum FacetDisplay {
	/** Offered in the section's search sidebar from the start. */
	ALWAYS,

	/**
	 * Not offered by default - reached through the advanced-search dialog, or
	 * set for the reader by an action elsewhere in the portal (the record
	 * page's "find related"). A facet carrying an active filter is shown
	 * regardless, so the reader can always see the constraint and undo it.
	 */
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

	/**
	 * Offered only while EVERY one of these conditions holds; absent = offered
	 * whenever its `display` says so.
	 *
	 * A deployment writes such a facet for a narrowing that only makes sense
	 * further in: an archival description offers the register facet of the
	 * register kind the reader picked, not all seven at once. The client
	 * evaluates it, because whether a condition holds depends on the filters the
	 * client holds - the definitions of a section are the same for everyone and
	 * stay cacheable.
	 *
	 * A filter on a facet whose conditions stop holding is **dropped**: the
	 * reader removed the selection the constraint depended on, and a constraint
	 * with no ground left is not one they could act on. The server keeps
	 * accepting such a filter, so a shared link stays answerable while the
	 * client settles it.
	 */
	offeredWhen?: FacetValueCondition[];

	/**
	 * Per-option explanations of an enumerated facet - what "technical drawing"
	 * covers, where the value alone does not say. Server-localized like every
	 * other display text here.
	 *
	 * Not on the buckets, where they would seem to belong: bucket counts come
	 * from `search`, which takes no language. A client matches an entry to an
	 * option the same way the conditions do, by value or by displayed label.
	 */
	optionTooltips?: FacetOptionTooltip[];
}

/**
 * "That facet has this value selected" - one condition of `FacetDef.offeredWhen`.
 *
 * The named facet's own selection is a set, so the condition holds when ANY of
 * its selected values matches (the reader may be looking at several kinds of
 * register at once). `value` is matched against a selected value or against the
 * label that value is displayed under, because a deployment naming a reference
 * facet's option thinks of its name, not of its uuid.
 */
model FacetValueCondition {
	/** Code of the facet whose selection is examined. */
	facet: string;

	/** The value that facet must have selected. */
	value: string;
}

/** One option's explanation (see `FacetDef.optionTooltips`). */
model FacetOptionTooltip {
	/** The option this belongs to - its value, or the label it is shown under. */
	value: string;

	tooltip: string;
}

/** Search request over APUs. */
model ApuSearchRequest {
	/**
	 * Section restriction; unset = search across all types (the general
	 * search), whose facets are the built-in ones - `getFacets` without an
	 * `apuType` lists them.
	 */
	apuType?: ApuType;

	/** Fulltext query over the APU name and description; unset = match all. */
	query?: string;

	/** Active filters; each references a facet of the section by its code. */
	filters?: SearchFilter[];

	/**
	 * Zero-based offset of the first returned item. `from + size` must stay
	 * within the server's page window (default 10 000); beyond it the request
	 * is rejected (400).
	 */
	@minValue(0)
	from?: int32 = 0;

	/** Page size. */
	@minValue(1)
	@maxValue(100)
	size?: int32 = 10;

	sort?: SortMode = SortMode.AUTO;

	/**
	 * Requested accuracy of `total`: matching APUs are counted exactly up to
	 * this value (see `totalRelation`). Unset = the server default (10 000);
	 * the server clamps the value to its configured maximum.
	 */
	@minValue(1)
	totalUpTo?: int32;
}

/**
 * Named sort modes. Every mode tie-breaks deterministically (name, then uuid),
 * so paging is stable. Records without the sorted value (no name, no dating)
 * always sort last.
 */
enum SortMode {
	/** The server decides: RELEVANCE when a query is present, NAME otherwise. */
	AUTO,

	/** Best match first; ties in Czech-alphabetical order. */
	RELEVANCE,

	/** Czech-alphabetical by name (collation: č after c, ch after h). */
	NAME,

	/** Czech-alphabetical by name, reversed. */
	NAME_DESC,

	/** By the earliest dating of the record; undated last. */
	DATE_ASC,

	/** By the latest dating of the record; undated last. */
	DATE_DESC,
}

/** How the fulltext query was matched (see `ApuSearchResponse.queryMode`). */
enum QueryMode {
	/** Every query word matched. */
	STRICT,

	/** Automatic any-word retry after zero strict hits. */
	RELAXED,
}

/** Accuracy of a search response's `total`. */
enum TotalRelation {
	/** `total` is the exact number of matching APUs. */
	EQ,

	/** At least `total` APUs match (counting stopped at the accuracy limit). */
	GTE,
}

/** Filter kinds - the SearchFilter discriminator. */
enum FilterKind {
	VALUES,
	TEXT,
	RANGE,
	RELATED,
}

/**
 * One active filter, discriminated by `kind` - the Elza/CAM `ItemData` pattern
 * this contract uses throughout (see also `FacetResult` and `DetailItem`): the
 * subtype narrows the discriminator to one enum member and marks it
 * `@invisible(Lifecycle)`, which keeps it out of the child schema. The emitted
 * OpenAPI is then the parent-only-property shape every generator handles;
 * redeclaring the literal in the child breaks them.
 */
@discriminator("kind")
model SearchFilter {
	/**
	 * Filter kind - the discriminator. A named enum so the base and every
	 * subtype share one type.
	 */
	kind: FilterKind;

	/**
	 * Code of the facet the filter applies to - a deployment-configured facet of
	 * the section, or one of the reserved built-in codes the general search
	 * offers (`~DATE`, `~TYPE`, `~RELATED`). A leading tilde cannot occur in an
	 * item-type code, so a reserved code can never collide with a configured
	 * facet.
	 */
	facet: string;
}

/** Selected values of an ENUM/REF facet; a document matches ANY of them (OR). */
model ValuesFilter extends SearchFilter {
	@invisible(Lifecycle)
	kind: FilterKind.VALUES;
	values: string[];
}

/** Text filter of a FULLTEXT facet (analyzed match on the facet's field). */
model TextFilter extends SearchFilter {
	@invisible(Lifecycle)
	kind: FilterKind.TEXT;
	q: string;
}

/**
 * Dating filter of a UNITDATE facet: matches documents whose interval
 * intersects [from, to]. Bounds accept a year (`1190`), a date (`1190-05-01`)
 * or a full ISO date-time; either bound may be omitted.
 */
model RangeFilter extends SearchFilter {
	@invisible(Lifecycle)
	kind: FilterKind.RANGE;
	from?: string;
	to?: string;

	/**
	 * Whether records carrying no dating in this facet's field match as well.
	 * Off by default, so the filter means what it says - a record with no date
	 * is not in 1805-1852. It exists because in a result set mixing dated and
	 * undated records (the general search: access points and archives are often
	 * undated) excluding them silently hides records the reader's query did
	 * match, for a reason the reader cannot see. `DatingFacetResult.undatedCount`
	 * is what makes that number visible, and this flag is how the reader acts
	 * on it.
	 */
	includeUndated?: boolean = false;
}

/** Which end of the relation the APUs named by a `RelatedFilter` sit on. */
enum RelationDirection {
	/** The document references one of the named APUs. */
	INCOMING,

	/**
	 * The document is one of the records a named APU visibly references - the
	 * links its own page shows. Reference items the source system marked
	 * index-only (APUX `visible="false"`) are deliberately left out: they exist
	 * to make the record carrying them findable, not to lead the reader on to
	 * records that page never showed.
	 */
	OUTGOING,

	/** Either of the above. */
	BOTH,
}

/**
 * Relation filter of a reference facet: matches documents related to ANY of the
 * named APUs (OR) through one of the relation types the facet spans. Two such
 * filters on the same facet therefore AND - "related to Germany and to
 * Postbox" is two conditions, not one condition with two values.
 *
 * The reserved facet code `~RELATED` is the built-in facet spanning EVERY
 * reference item type; a leading tilde cannot occur in an item-type code, so
 * the reserved code can never collide with a deployment's own facet. That is
 * the facet a record page's "find related" action filters on, and the one case
 * that asks for `BOTH` directions.
 */
model RelatedFilter extends SearchFilter {
	@invisible(Lifecycle)
	kind: FilterKind.RELATED;

	/** UUIDs of the APUs the relation leads to; a document matches ANY of them (OR). */
	apus: string[];

	direction?: RelationDirection = RelationDirection.INCOMING;
}

/** Search response. */
model ApuSearchResponse {
	/**
	 * Total number of matching APUs (independent of paging), counted exactly
	 * up to the effective accuracy limit — `totalRelation` says whether it is
	 * exact.
	 */
	total: int64;

	/** EQ = `total` is exact; GTE = at least `total` APUs match. */
	totalRelation: TotalRelation;

	/**
	 * STRICT = every query word had to match. RELAXED = the strict query had no
	 * hits and the server automatically retried with any-word matching - the UI
	 * labels such results.
	 */
	queryMode: QueryMode;

	items: ApuSearchItem[];

	/**
	 * Per-facet results of the section (multi-select semantics), each shaped by
	 * its facet kind - see the `FacetResult` specializations. A facet's full
	 * option list (beyond the bucket limit) comes from `getFacetOptions`.
	 */
	facets: FacetResult[];

	/**
	 * Matching APUs per type, count descending. The query and all filters
	 * apply; the request's own `apuType` restriction does NOT - a user browsing
	 * one section still sees the other sections' counts and can switch there.
	 */
	typeCounts: TypeCount[];
}

/** Number of matching APUs of one type. */
model TypeCount {
	apuType: ApuType;

	count: int64;
}

/** One search hit. */
model ApuSearchItem {
	uuid: string;
	name: string;
	description?: string;
	apuType: ApuType;
	containsDigitalObjects: boolean;

	/**
	 * Structured presentation of this record, when the source system delivered
	 * one. Present per record: a list can mix records that have one with records
	 * that do not, and a client without it renders `name` and `description` as
	 * usual - there is no deployment flag to turn this on.
	 *
	 * The record and field codes are a deployment vocabulary (the source system
	 * chose them); `getResultLayout` says how to present them.
	 */
	structured?: StructuredResult;
}

/**
 * Pre-rendered structured presentation of one record: rows of coded fields, as
 * delivered by the source system in the APUX exchange format. The portal stores
 * and lays it out, it does not compose it.
 */
model StructuredResult {
	/** Record-shape code (an archival level in practice); keys the layout's icon. */
	code: string;

	/** Rows in display order. */
	rows: ResultRow[];

	/**
	 * Thumbnail image, ready to use: an external absolute URL when the source
	 * delivered one, otherwise this deployment's own `/ui/images` URL.
	 */
	thumbnailUrl?: string;

	/** Where the thumbnail leads; unset = this record's own detail page. */
	thumbnailLinkUrl?: string;
}

/** One row of a structured result: the fields shown on one line. */
model ResultRow {
	fields: ResultField[];
}

/** One field of a row. */
model ResultField {
	/** Field code; keys the layout's styling, prefix and label. */
	code: string;

	values: ResultValue[];
}

/** One value of a field; with `refUuid` it is a link to another record. */
model ResultValue {
	text: string;

	/** Referenced APU - the value renders as a link to its detail. */
	refUuid?: string;
}

/**
 * How one deployment presents its structured search results. Fetched once per
 * language and cached by clients; the codes it keys on come from the data, so a
 * code with no entry here simply renders plain.
 */
model ResultLayout {
	/** Styling per field code, in no particular order. */
	fields: ResultFieldStyle[];

	/** Icon per `StructuredResult.code`. */
	icons: ResultIcon[];

	/** Separator between fields of one row; unset = the client default (" | "). */
	fieldSeparator?: string;
}

/** Presentation of one field code. */
model ResultFieldStyle {
	code: string;

	/**
	 * This field is the record's heading - its text becomes the link to the
	 * record's detail. At most one field of a record should be marked; with none,
	 * clients fall back to the record's `name`.
	 */
	heading?: boolean;

	/** Localized text shown before the values ("Inv. č.: "). */
	prefix?: string;

	/**
	 * Localized name of the field for readers who get no visual context (screen
	 * readers, tooltips). Defaults to `prefix` when that is set.
	 */
	label?: string;

	/** Separator between the field's own values; unset = a space. */
	valueSeparator?: string;

	/** CSS color of the text; unset = the client's own body color. */
	color?: string;

	/** Font-size multiplier, 1 = base text size. */
	scale?: float32;

	bold?: boolean;

	/** Icon shown before the field, ready to use (see `getImage`). */
	iconUrl?: string;
}

/** Icon of one record-shape code. */
model ResultIcon {
	/** The `StructuredResult.code` this icon belongs to. */
	code: string;

	/** Ready-to-use URL of the deployment image (see `getImage`). */
	url: string;

	/** Rendered width in CSS pixels; unset = the client default. */
	size?: int32;
}

/** Facet result kinds - the FacetResult discriminator (follows the facet's type). */
enum FacetResultKind {
	/** Value buckets of an ENUM facet. */
	ENUM,

	/** Reference buckets of a REF facet. */
	REF,

	/** Dating bounds of a UNITDATE facet. */
	DATING,
}

/**
 * Result of one facet, discriminated by `kind` - each facet kind carries only
 * the fields it really has (same TypeSpec pattern as `SearchFilter`).
 */
@discriminator("kind")
model FacetResult {
	/** Result kind - the discriminator; follows the facet's type. */
	kind: FacetResultKind;

	/** Facet code. */
	code: string;
}

/** Value buckets of an ENUM facet (`value` doubles as the display label). */
model EnumFacetResult extends FacetResult {
	@invisible(Lifecycle)
	kind: FacetResultKind.ENUM;
	buckets: FacetBucket[];
}

/** Reference buckets of a REF facet: `value` = APU uuid, `label` = display name. */
model RefFacetResult extends FacetResult {
	@invisible(Lifecycle)
	kind: FacetResultKind.REF;
	buckets: FacetBucket[];
}

/** Dating bounds of a UNITDATE facet. */
model DatingFacetResult extends FacetResult {
	@invisible(Lifecycle)
	kind: FacetResultKind.DATING;

	/** Absent when no matching document carries a dating. */
	bounds?: DatingBounds;

	/**
	 * How many matching records carry no dating in this facet's field - the ones
	 * a dating filter leaves out unless `RangeFilter.includeUndated` is set.
	 * Counted like `bounds`, with the facet's own RANGE filter excluded, so the
	 * number does not change when the reader moves the slider.
	 *
	 * Zero is the normal answer wherever every record is dated (an archival
	 * description, a fond), which is why a client should offer the
	 * include-undated choice only when this count is greater than zero:
	 * whether the choice is meaningful is a property of the data, not of the
	 * deployment's configuration.
	 */
	undatedCount?: int64;
}

/** One value bucket of an enumerated facet. */
model FacetBucket {
	/** Filter value to send back in a VALUES filter (for references the APU uuid). */
	value: string;

	/** Display label when it differs from the value (e.g. resolved references). */
	label?: string;

	count: int64;
}

/**
 * Year bounds of the matching documents' dating - the range of a UNITDATE
 * facet's slider. Computed with the facet's own RANGE filter excluded
 * (multi-select), so the user can widen a selected range.
 */
model DatingBounds {
	minYear: int32;
	maxYear: int32;
}

/** Options request of one facet - carries the active search state. */
model FacetOptionsRequest {
	/** Section whose facet configuration applies (facets are section-scoped). */
	apuType: ApuType;

	/** Fulltext query of the current search; unset = match all. */
	query?: string;

	/** Active filters of the current search; the facet's own VALUES filter is ignored. */
	filters?: SearchFilter[];

	/** Type-ahead text matched against option labels; unset = all options (up to `size`). */
	q?: string;

	/** Maximum number of returned options. */
	@minValue(1)
	@maxValue(1000)
	size?: int32 = 100;
}

/** Facet options, ordered per the facet configuration (default: count descending). */
model FacetOptionsResponse {
	options: FacetBucket[];
}

/**
 * APU detail. The response is a render model: parts and items arrive
 * display-ready — server-ordered by the deployment's display configuration,
 * invisible items filtered out, labels server-localized, references resolved —
 * so clients never download the server's types configuration. Items keep their
 * item-type codes, letting a client still special-case known types (e.g. the
 * fund's link to its archival-description tree root, `ARCHDESC~ROOT~REF`).
 */
@tag("apu")
namespace Apu {
	/**
	 * Detail of one APU. Supports conditional requests: responses carry
	 * `ETag`/`Last-Modified`, and a request with `If-None-Match` /
	 * `If-Modified-Since` is answered with 304 when unchanged. The `ETag`
	 * covers the presentation language as well, so switching `lang` never
	 * yields a 304 with the previous language's body.
	 */
	@get
	@route("/api/v1/apu/{uuid}")
	op getDetail(
		@path uuid: string,

		...LangParam,

		@header("If-None-Match") ifNoneMatch?: string,

		@header("If-Modified-Since") ifModifiedSince?: string,
	): {
		@statusCode statusCode: 200;

		@header("ETag") eTag?: string;

		@header("Last-Modified") lastModified?: string;

		@body body: ApuDetail;
	} | {
		/** Not modified - the client's cached representation is still valid. */
		@statusCode statusCode: 304;
	};

	/**
	 * One window of the archival-description tree around an APU - the source of
	 * the incremental tree navigation. `BEFORE`/`AFTER` return up to a
	 * server-configured window of siblings preceding/following the APU in tree
	 * order; `UNDER` returns the first window of its children. Conditional
	 * requests behave as in `getDetail`.
	 */
	@get
	@route("/api/v1/apu/{uuid}/tree")
	op getTreeNodes(
		@path uuid: string,

		@query direction: TreeDirection,

		@header("If-None-Match") ifNoneMatch?: string,

		@header("If-Modified-Since") ifModifiedSince?: string,
	): {
		@statusCode statusCode: 200;

		@header("ETag") eTag?: string;

		@header("Last-Modified") lastModified?: string;

		@body body: TreeNode[];
	} | {
		/** Not modified - the client's cached representation is still valid. */
		@statusCode statusCode: 304;
	};

	/**
	 * Citations of one APU - one entry per configured citation form covering the
	 * record's type, in configured order (`UiConfig.citations` says which those
	 * are, so a client knows before asking whether to offer the action).
	 *
	 * The text is rendered server-side because an archival citation is archival
	 * grammar, not string formatting: it is composed from the record, the fund
	 * it belongs to and that fund's institution, its wording is agreed with the
	 * archives, and it may draw on description items no detail response carries
	 * (a source system can prepare a value for citation without displaying it).
	 * A client copies it verbatim; it never re-composes or reformats one.
	 *
	 * Responses carry no `ETag`: the text follows deployment configuration
	 * rather than the record alone, and a citation norm may date the access.
	 *
	 * A form that cannot render this record - an archival description whose
	 * fund is missing, say - is left out rather than reported, since a reader
	 * can do nothing about it. When no form renders, the response is a 422 and
	 * which piece was missing is logged on the server: that is a data error,
	 * diagnosis for whoever looks into the data, while the reader gets the UI's
	 * own localized message.
	 */
	@get
	@route("/api/v1/apu/{uuid}/citations")
	op getCitations(@path uuid: string, ...LangParam): Citation[];
}

/**
 * Content of digital-object and attachment files. Which URL to call never
 * comes from here alone: every file in a detail response carries its
 * ready-to-use `url`/`dziUrl` (which may point at an external image server) -
 * these endpoints are where portal-served ones lead.
 */
@tag("dao")
@route("/api/v1/daofile")
namespace DaoFile {
	/**
	 * Content of one file, streamed with its real MIME type. Serves stored
	 * files and referenced files the deployment allows (`files.referenced-dirs`);
	 * externally hosted files are never proxied - their `url` points at the
	 * external server directly. Supports conditional and range requests.
	 */
	@get
	@route("/{id}")
	op getContent(
		/** The file's `FileInfo.id`. */
		@path id: string,

		/** True = download disposition (attachment); default is inline. */
		@query download?: boolean,

		@header("If-None-Match") ifNoneMatch?: string,

		@header("If-Modified-Since") ifModifiedSince?: string,
	): {
		@statusCode statusCode: 200;

		@header("ETag") eTag?: string;

		@header("Last-Modified") lastModified?: string;

		/** Placeholder - the response carries the file's real MIME type. */
		@header contentType: "application/octet-stream";

		@body body: bytes;
	} | {
		/** Not modified - the client's cached representation is still valid. */
		@statusCode statusCode: 304;
	};

	/**
	 * Deep Zoom descriptor of a TILE file. Tile images resolve relative to
	 * this URL (`image_files/...`), which is how deep-zoom viewers fetch them.
	 */
	@get
	@route("/{id}/tiles/image.dzi")
	op getDziDescriptor(
		/** The TILE file's `FileInfo.id`. */
		@path id: string,
	): {
		@header contentType: "application/xml";

		@body body: bytes;
	};

	/** One tile image of a TILE file's Deep Zoom pyramid. */
	@get
	@route("/{id}/tiles/image_files/{level}/{tile}")
	op getTileImage(
		/** The TILE file's `FileInfo.id`. */
		@path id: string,

		/** Pyramid level. */
		@path level: int32,

		/** Tile file name: `{column}_{row}.{format}`. */
		@path tile: string,
	): {
		@header contentType: "image/jpeg" | "image/png";

		@body body: bytes;
	};
}

/** Directions of the incremental tree navigation. */
enum TreeDirection {
	/** Siblings preceding the APU, in tree order. */
	BEFORE,

	/** Siblings following the APU, in tree order. */
	AFTER,

	/** Children of the APU (first window). */
	UNDER,
}

/** One node of the archival-description tree. */
model TreeNode {
	uuid: string;

	name: string;

	/** Tree nodes display `description` when present, `name` otherwise. */
	description?: string;

	/** Depth within the tree (relative; larger = deeper). */
	depth: int32;

	/** 1-based position among the node's siblings. */
	pos: int32;

	/** Number of children (0 = leaf). */
	childCount: int32;
}

/** Display-ready detail of one APU (the render model). */
model ApuDetail {
	uuid: string;

	/** Display name. */
	name: string;

	description?: string;

	apuType: ApuType;

	/** Public permalink of this APU, when assigned. */
	permalink?: string;

	/** Number of child APUs in the archival-description tree (0 = leaf). */
	childCount: int32;

	/**
	 * The APU's path in the archival-description tree, root first, INCLUDING
	 * the APU itself as the last element - a single element for root or
	 * standalone APUs. Drives both the breadcrumb navigation (all but the last
	 * element) and the seed of the incremental tree (`getTreeNodes`).
	 */
	treePath: TreeNode[];

	/** Display-ready parts in display order; parts with no visible items are omitted. */
	parts: DetailPart[];

	/** Attachments, with ready-to-use file URLs. */
	attachments: AttachmentInfo[];

	/** Digital objects, with ready-to-use file/tile URLs. */
	digitalObjects: DigitalObjectInfo[];
}

/** Reference to another APU, resolved with its display data. */
model ApuLink {
	uuid: string;

	name: string;

	description?: string;
}

/** One display-ready part of the detail. */
model DetailPart {
	/** Part-type code (display-model code, tilde form). */
	code: string;

	/** Server-localized display label. */
	label: string;

	viewType: PartViewType;

	/** Textual value of the whole part, when the source supplied one. */
	value?: string;

	/** Display-ready items in display order (invisible items already filtered out). */
	items: DetailItem[];
}

/** How a part is displayed (the display model's viewType). */
enum PartViewType {
	/** Items are always visible. */
	STANDALONE,

	/**
	 * Collapsed by default to a one-line summary of its item values (references
	 * excluded); expandable to the full item rows.
	 */
	GROUPED,
}

/**
 * One display-ready item of a part, discriminated by `kind`: each kind is its
 * own model carrying exactly the fields that kind has, so a client never has to
 * ask which optional fields happen to be filled in (the Elza/CAM `ItemData`
 * pattern).
 */
@discriminator("kind")
model DetailItem {
	/** Item-type code (display-model code, tilde form) - lets clients special-case known types. */
	code: string;

	/** Server-localized display label. */
	label: string;

	kind: DetailItemKind;
}

/** Plain text: strings, enumerations and numbers. */
model TextItem extends DetailItem {
	@invisible(Lifecycle)
	kind: DetailItemKind.TEXT;

	/** Display value. */
	value: string;
}

/**
 * A dating, rendered server-side and carried machine-readably as well.
 *
 * The display string stays the server's job - rendering a dating means the
 * archival grammar (granularity, estimates, interval collapsing) and not just
 * date formatting, so clients would have to reimplement it. The bounds are here
 * for what the string cannot do: semantic markup (`<time datetime>`), and
 * consumers that process datings rather than show them.
 */
model UnitDateItem extends DetailItem {
	@invisible(Lifecycle)
	kind: DetailItemKind.UNITDATE;

	/** The dating as displayed, in the reader's language. */
	value: string;

	/** Lower bound as an ISO local date-time (`1850-01-01T00:00:00`). */
	from?: string;

	/** Upper bound, same form. */
	to?: string;

	/** Granularity the lower bound was recorded in. */
	fromPrecision?: DatingPrecision;

	/** Granularity the upper bound was recorded in. */
	toPrecision?: DatingPrecision;

	/** The lower bound is an estimate (the display string brackets it). */
	fromEstimated?: boolean;

	/** The upper bound is an estimate. */
	toEstimated?: boolean;
}

/** External link. */
model LinkItem extends DetailItem {
	@invisible(Lifecycle)
	kind: DetailItemKind.LINK;

	/** Link target. */
	href: string;

	/** Text to show for the link; the target itself when the source gave no caption. */
	caption: string;
}

/** Reference to another APU. */
model RefItem extends DetailItem {
	@invisible(Lifecycle)
	kind: DetailItemKind.REF;

	/** The referenced APU, resolved to uuid + display name. */
	ref: ApuLink;
}

/** Structured value (aggregated items), carried as a raw JSON string. */
model JsonItem extends DetailItem {
	@invisible(Lifecycle)
	kind: DetailItemKind.JSON;

	/** The raw JSON string. */
	json: string;
}

/** Granularity a dating bound was recorded in. */
enum DatingPrecision {
	CENTURY,
	YEAR,
	MONTH,
	DAY,
	TIME,
}

/** Item kinds - each one is a model of its own (see {@link DetailItem}). */
enum DetailItemKind {
	/** Plain text: strings, enumerations, numbers ({@link TextItem}). */
	TEXT,

	/** A dating ({@link UnitDateItem}). */
	UNITDATE,

	/** External link ({@link LinkItem}). */
	LINK,

	/** Reference to another APU ({@link RefItem}). */
	REF,

	/** Structured value ({@link JsonItem}). */
	JSON,
}


/** Attachment of an APU. */
model AttachmentInfo {
	/** Display name. */
	name: string;

	/** The attachment's file. */
	file?: FileInfo;
}

/** Digital object (digitized attachment) of an APU. */
model DigitalObjectInfo {
	uuid: string;

	name?: string;

	permalink?: string;

	/**
	 * License code of the object, as delivered by the source system. Codes are
	 * the source system's vocabulary; presenting them is deployment
	 * configuration (a later slice).
	 */
	license?: string;

	/** Files of the object in display order. */
	files: FileInfo[];
}

/** One file of a digital object or attachment. */
model FileInfo {
	/** File id - the key of the `DaoFile` endpoints. */
	id: string;

	/** Role of the file within its digital object. */
	fileType?: FileType;

	name?: string;

	/** MIME type. */
	contentType?: string;

	/** Size in bytes. */
	size?: int64;

	/** The file a viewer should open first. */
	selected?: boolean;

	/**
	 * 1-based page position within the digital object. Files of different
	 * roles sharing a position are renditions of the same page; clients match
	 * them by this value, never by list order.
	 */
	position?: int32;

	/**
	 * Ready-to-use content URL - portal-served or external, the server
	 * decides; clients never assemble file URLs. Absent when the content is
	 * not servable (e.g. a tile pyramid, which is reached via `dziUrl`, or a
	 * referenced file outside the deployment's allowed directories).
	 */
	url?: string;

	/**
	 * Deep Zoom descriptor URL of a TILE file - portal-served or an external
	 * image server's, the server decides. Tile images resolve relative to it.
	 */
	dziUrl?: string;
}

/** Roles of digital-object files. */
enum FileType {
	/** Published (downloadable) rendition. */
	PUBLISHED,

	/** Preview thumbnail. */
	THUMBNAIL,

	/** Deep-zoom tile source. */
	TILE,
}

/** One rendered citation of a record, in one citation form. */
model Citation {
	/** Code of the form this text follows (`CitationForm.code`). */
	code: string;

	/** Server-localized display name of the form. */
	label: string;

	/**
	 * The citation itself, ready to be copied verbatim into a bibliography.
	 * Plain text, in the deployment's own citation language - the wording is
	 * the archives' agreement, not product UI, so it does not follow `lang`
	 * unless the configured form itself distinguishes languages.
	 */
	text: string;
}
