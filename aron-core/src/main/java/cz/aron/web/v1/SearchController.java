package cz.aron.web.v1;

import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import cz.aron.api.v1.SearchApi;
import cz.aron.api.v1.model.ApuSearchItem;
import cz.aron.api.v1.model.ApuSearchRequest;
import cz.aron.api.v1.model.ApuSearchResponse;
import cz.aron.api.v1.model.ApuType;
import cz.aron.api.v1.model.DatingBounds;
import cz.aron.api.v1.model.DatingFacetResult;
import cz.aron.api.v1.model.EnumFacetResult;
import cz.aron.api.v1.model.FacetBucket;
import cz.aron.api.v1.model.FacetDef;
import cz.aron.api.v1.model.FacetDisplay;
import cz.aron.api.v1.model.FacetOptionsRequest;
import cz.aron.api.v1.model.FacetOptionsResponse;
import cz.aron.api.v1.model.FacetOrder;
import cz.aron.api.v1.model.FacetResult;
import cz.aron.api.v1.model.FacetResultKind;
import cz.aron.api.v1.model.QueryMode;
import cz.aron.api.v1.model.RefFacetResult;
import cz.aron.api.v1.model.RangeFilter;
import cz.aron.api.v1.model.RelatedFilter;
import cz.aron.api.v1.model.RelationDirection;
import cz.aron.api.v1.model.ResultLayout;
import cz.aron.api.v1.model.SearchFilter;
import cz.aron.api.v1.model.SortMode;
import cz.aron.api.v1.model.TextFilter;
import cz.aron.api.v1.model.TotalRelation;
import cz.aron.api.v1.model.TypeCount;
import cz.aron.api.v1.model.ValuesFilter;
import cz.aron.domain.DataType;
import cz.aron.domain.facets.dto.DisplayType;
import cz.aron.domain.facets.dto.FacetConfigDto;
import cz.aron.domain.types.LocalizedText;
import cz.aron.domain.types.TypesHolder;
import cz.aron.domain.types.dto.ItemType;
import cz.aron.mapper.ApuSerializer;
import cz.aron.repository.ApuEntityRepository;
import cz.aron.search.ApuSearchQuery;
import cz.aron.search.ApuSearchResult;
import cz.aron.search.FieldFilter;
import cz.aron.search.IndexingService;
import cz.aron.search.relevance.RelevancePlan;
import cz.aron.search.relevance.RelevanceService;
import cz.aron.service.ApuService;
import jakarta.annotation.PostConstruct;

/**
 * Implements the /api/v1 search endpoints. Facet semantics live server-side:
 * the deployment's searchConfig.yaml binds facets to APU types and index fields
 * (item-type codes), this controller validates client filters against it and
 * translates them to the engine-neutral search port. Clients never send engine
 * structures.
 */
@RestController
public class SearchController implements SearchApi {

	/** Upper bound of buckets requested from the engine per facet (= the contract's max options size). */
	private static final int BUCKET_LIMIT = 1000;

	private final FacetScope facetScope;

	private final BuiltInFacets builtInFacets;

	private final TypesHolder typesHolder;

	private final PresentationLocales presentationLocales;

	private final IndexingService indexingService;

	private final RelevanceService relevanceService;

	private final ApuService apuService;

	private final ApuEntityRepository apuEntityRepository;

	private final ResultLayoutLoader resultLayoutLoader;

	private final DeploymentImages deploymentImages;

	/**
	 * Whether hits carry their stored structured presentation. {@code AUTO} (the
	 * default) attaches it to every hit that has one - availability is data, not
	 * configuration; {@code OFF} serves plain hits and skips the lookup.
	 */
	private final boolean structuredResults;

	/** Page window cap: {@code from + size} must stay within (protects deep paging). */
	private final int maxWindow;

	/** Default accuracy of totals - exact up to this count, "more than N" above it. */
	private final int totalUpToDefault;

	/** Upper clamp for the request's own {@code totalUpTo} override. */
	private final int totalUpToMax;

	/** Every indexed reference item field - the built-in relation facet's scope. */
	private List<String> referenceFields;

	public SearchController(FacetScope facetScope, BuiltInFacets builtInFacets, TypesHolder typesHolder,
			IndexingService indexingService,
			RelevanceService relevanceService, PresentationLocales presentationLocales, ApuService apuService,
			ApuEntityRepository apuEntityRepository, ResultLayoutLoader resultLayoutLoader, DeploymentImages deploymentImages,
			@Value("${search.structured-results:AUTO}") String structuredResults,
			@Value("${search.max-window:10000}") int maxWindow,
			@Value("${search.track-total-hits-up-to:10000}") int totalUpToDefault,
			@Value("${search.track-total-hits-max:100000}") int totalUpToMax) {
		this.facetScope = facetScope;
		this.builtInFacets = builtInFacets;
		this.typesHolder = typesHolder;
		this.presentationLocales = presentationLocales;
		this.indexingService = indexingService;
		this.relevanceService = relevanceService;
		this.apuService = apuService;
		this.apuEntityRepository = apuEntityRepository;
		this.resultLayoutLoader = resultLayoutLoader;
		this.deploymentImages = deploymentImages;
		this.structuredResults = parseStructuredResults(structuredResults);
		this.maxWindow = maxWindow;
		this.totalUpToDefault = totalUpToDefault;
		this.totalUpToMax = totalUpToMax;
	}

	private static boolean parseStructuredResults(String value) {
		return switch (value == null ? "AUTO" : value.trim().toUpperCase(Locale.ROOT)) {
			case "AUTO" -> true;
			case "OFF" -> false;
			default -> throw new IllegalStateException(
					"search.structured-results must be AUTO or OFF, not '" + value + "'");
		};
	}

	@PostConstruct
	void load() {
		// unindexed reference items carry no field to match on, so they would only
		// add dead clauses to every relation query
		referenceFields = typesHolder.getAllItemTypes().stream()
				.filter(itemType -> DataType.APU_REF.equals(itemType.getType()) && itemType.isIndexed())
				.map(ItemType::getCode)
				.toList();
	}

	@Override
	public ResponseEntity<List<FacetDef>> searchGetFacets(ApuType apuType, String lang) {
		Locale locale = presentationLocales.resolve(lang);
		return ResponseEntity.ok(facetsInScope(apuType).stream().map(facet -> toFacetDef(facet, locale)).toList());
	}

	/**
	 * The facets a request may filter on: the section's configured ones, or - for
	 * the general search, which spans every record type and so has no section
	 * configuration to apply - the built-in ones. One list either way, so
	 * validation, aggregation and result mapping need no second code path.
	 */
	private List<FacetConfigDto> facetsInScope(ApuType apuType) {
		return apuType != null ? facetScope.facetsFor(apuType) : builtInFacets.facets();
	}

	@Override
	public ResponseEntity<ApuSearchResponse> searchSearch(ApuSearchRequest request) {
		List<FacetConfigDto> sectionFacets = facetsInScope(request.getApuType());
		Map<String, FacetConfigDto> byCode = sectionFacets.stream()
				.collect(Collectors.toMap(FacetConfigDto::getSource, Function.identity(), (a, b) -> a));

		var filters = new ArrayList<FieldFilter>();
		for (SearchFilter filter : request.getFilters() != null ? request.getFilters() : List.<SearchFilter>of()) {
			filters.add(toFieldFilter(filter, byCode));
		}
		// buckets for every enumerable facet of the section (reference-valued
		// facets enumerate their composite ~ID~LABEL field, see BucketRequest),
		// dating bounds for every UNITDATE facet
		var bucketRequests = new ArrayList<ApuSearchQuery.BucketRequest>();
		var boundsFields = new LinkedHashSet<String>();
		for (FacetConfigDto facet : sectionFacets) {
			switch (facet.getType()) {
				case ENUM, MULTI_REF -> {
					// the built-in relation facet is the one reference facet with no
					// buckets: its options are records found by name, not a union of
					// every reference field's terms
					if (builtInFacets.isEnumerable(facet.getSource())) {
						bucketRequests.add(new ApuSearchQuery.BucketRequest(
								bucketFieldOf(facet), indexFieldOf(facet), BUCKET_LIMIT));
					}
				}
				case UNITDATE -> boundsFields.add(indexFieldOf(facet));
				default -> { /* FULLTEXT and the not-yet-served reference variants have no facet result */ }
			}
		}

		int from = request.getFrom() != null ? request.getFrom() : 0;
		int size = request.getSize() != null ? request.getSize() : 10;
		if (from + size > maxWindow) {
			throw badRequest("from + size must not exceed " + maxWindow + ".");
		}
		RelevancePlan plan = relevanceService.plan(blankToNull(request.getQuery()));
		var sort = resolveSort(request.getSort(), plan);
		String apuType = request.getApuType() != null ? request.getApuType().getValue() : null;

		QueryMode queryMode = QueryMode.STRICT;
		ApuSearchResult result = indexingService.search(new ApuSearchQuery(apuType, plan, filters,
				bucketRequests, boundsFields, from, size, sort, effectiveTotalUpTo(request.getTotalUpTo()), true));
		// zero strict hits - one automatic any-word retry, visibly labeled (B7);
		// facet filters and the section restriction are never relaxed
		if (result.total() == 0 && plan != null && plan.relaxable()
				&& relevanceService.config().relaxOnNoHits()) {
			result = indexingService.search(new ApuSearchQuery(apuType, plan.relaxed(), filters,
					bucketRequests, boundsFields, from, size, sort, effectiveTotalUpTo(request.getTotalUpTo()),
					true));
			if (result.total() > 0) {
				queryMode = QueryMode.RELAXED;
			}
		}

		var items = result.hits().stream()
				.map(h -> {
					var item = new ApuSearchItem(h.uuid(), h.name(), ApuType.fromValue(h.type()),
							h.containsDigitalObjects());
					item.setDescription(h.description());
					return item;
				})
				.toList();
		attachStructuredResults(items);
		var facetResults = new ArrayList<FacetResult>();
		for (FacetConfigDto facet : sectionFacets) {
			switch (facet.getType()) {
				// the built-in type facet's buckets carry the ApuType member itself:
				// this response has no language (search takes no lang - a hit's
				// display text comes from the index), and a contract enum is the one
				// value a client can label on its own
				case ENUM -> facetResults.add(new EnumFacetResult(
						orderFacetBuckets(toFacetBuckets(result.buckets().get(bucketFieldOf(facet)),
								referenceValued(facet)), facet),
						FacetResultKind.ENUM, facet.getSource()));
				case MULTI_REF -> {
					if (builtInFacets.isEnumerable(facet.getSource())) {
						facetResults.add(new RefFacetResult(
								orderFacetBuckets(toFacetBuckets(result.buckets().get(bucketFieldOf(facet)), true),
										facet),
								FacetResultKind.REF, facet.getSource()));
					} else {
						// no buckets to send, but the facet is still in the response so a
						// client renders it (and any active constraint on it)
						facetResults.add(new RefFacetResult(List.of(), FacetResultKind.REF, facet.getSource()));
					}
				}
				case UNITDATE -> {
					var facetResult = new DatingFacetResult(FacetResultKind.DATING, facet.getSource());
					var bounds = result.bounds().get(indexFieldOf(facet));
					if (bounds != null) {
						facetResult.setBounds(new DatingBounds(yearOf(bounds.minMillis()), yearOf(bounds.maxMillis())));
						facetResult.setUndatedCount(bounds.undatedCount());
					}
					facetResults.add(facetResult);
				}
				default -> { /* no facet result */ }
			}
		}
		var typeCounts = result.typeCounts().stream()
				.map(bucket -> new TypeCount(ApuType.fromValue(bucket.value()), bucket.count()))
				.toList();
		return ResponseEntity.ok(new ApuSearchResponse(result.total(),
				result.totalRelation() == ApuSearchResult.TotalRelation.EQ
						? TotalRelation.EQ
						: TotalRelation.GTE,
				queryMode, items, facetResults, typeCounts));
	}

	/**
	 * Attaches the stored structured presentation to every hit of the page that
	 * has one - one lookup by the page's uuids, the same the old API's
	 * /apu/listresults does. A hit without a stored result keeps no
	 * {@code structured} field at all: clients then render name and description,
	 * which is why the feature needs no deployment flag.
	 */
	private void attachStructuredResults(List<ApuSearchItem> items) {
		if (!structuredResults || items.isEmpty()) {
			return;
		}
		var uuids = items.stream().map(item -> java.util.UUID.fromString(item.getUuid())).toList();
		var stored = apuService.findAllResultsByUuidIn(uuids);
		for (ApuSearchItem item : items) {
			item.setStructured(
					StructuredResultMapper.toApi(stored.get(item.getUuid()), deploymentImages::thumbnailUrl));
		}
	}

	@Override
	public ResponseEntity<ResultLayout> searchGetResultLayout(String lang) {
		return ResponseEntity.ok(resultLayoutLoader.getLayout(presentationLocales.resolve(lang)));
	}

	/** The request's own accuracy override, clamped by the server maximum; unset = the server default. */
	private int effectiveTotalUpTo(Integer requested) {
		return requested != null ? Math.min(requested, totalUpToMax) : totalUpToDefault;
	}

	/** AUTO resolves server-side: relevance with a query, name otherwise (§4.4). */
	private static ApuSearchQuery.SortMode resolveSort(SortMode requested, RelevancePlan plan) {
		SortMode mode = requested != null ? requested : SortMode.AUTO;
		return switch (mode) {
			case AUTO -> plan != null ? ApuSearchQuery.SortMode.RELEVANCE : ApuSearchQuery.SortMode.NAME;
			case RELEVANCE -> ApuSearchQuery.SortMode.RELEVANCE;
			case NAME -> ApuSearchQuery.SortMode.NAME;
			case NAME_DESC -> ApuSearchQuery.SortMode.NAME_DESC;
			case DATE_ASC -> ApuSearchQuery.SortMode.DATE_ASC;
			case DATE_DESC -> ApuSearchQuery.SortMode.DATE_DESC;
		};
	}

	/**
	 * Options of one enumerable facet (type-ahead): the same multi-select search
	 * as {@code searchSearch} restricted to the facet's buckets, narrowed by the
	 * option label. For reference facets a document-level filter on the analyzed
	 * label companion narrows the candidates; the term-level label match below
	 * removes the remaining false positives of multi-reference documents.
	 */
	@Override
	public ResponseEntity<FacetOptionsResponse> searchGetFacetOptions(String code, FacetOptionsRequest request) {
		List<FacetConfigDto> sectionFacets = facetScope.facetsFor(request.getApuType());
		Map<String, FacetConfigDto> byCode = sectionFacets.stream()
				.collect(Collectors.toMap(FacetConfigDto::getSource, Function.identity(), (a, b) -> a));
		FacetConfigDto facet = byCode.get(code);
		if (facet == null) {
			throw badRequest("Unknown facet '" + code + "' for the requested apuType.");
		}
		if (facet.getType() != cz.aron.domain.facets.dto.FacetType.MULTI_REF
				&& facet.getType() != cz.aron.domain.facets.dto.FacetType.ENUM) {
			throw badRequest("Facet '" + code + "' has no options.");
		}
		boolean reference = referenceValued(facet);

		var filters = new ArrayList<FieldFilter>();
		for (SearchFilter filter : request.getFilters() != null ? request.getFilters() : List.<SearchFilter>of()) {
			filters.add(toFieldFilter(filter, byCode));
		}
		String q = blankToNull(request.getQ());
		if (reference && q != null) {
			filters.add(new FieldFilter.Text(facet.getSource() + "~LABEL", q));
		}

		String bucketField = bucketFieldOf(facet);
		ApuSearchResult result = indexingService.search(new ApuSearchQuery(
				request.getApuType().getValue(), relevanceService.plan(blankToNull(request.getQuery())), filters,
				List.of(new ApuSearchQuery.BucketRequest(bucketField, facet.getSource(), BUCKET_LIMIT)),
				Set.of(), 0, 0, ApuSearchQuery.SortMode.RELEVANCE));

		var options = toFacetBuckets(result.buckets().get(bucketField), reference).stream()
				.filter(b -> q == null || labelMatches(b.getLabel() != null ? b.getLabel() : b.getValue(), q))
				.collect(Collectors.toCollection(ArrayList::new));
		var ordered = orderFacetBuckets(options, facet);
		int size = request.getSize() != null ? request.getSize() : 100;
		return ResponseEntity
				.ok(new FacetOptionsResponse(ordered.size() > size ? ordered.subList(0, size) : ordered));
	}

	private FieldFilter toFieldFilter(SearchFilter filter, Map<String, FacetConfigDto> byCode) {
		if (filter instanceof RelatedFilter related) {
			return toRelatedFilter(related, byCode);
		}
		FacetConfigDto facet = byCode.get(filter.getFacet());
		if (facet == null) {
			throw badRequest("Unknown facet '" + filter.getFacet() + "' for the requested apuType.");
		}
		if (filter instanceof ValuesFilter values) {
			if (facet.getType() != cz.aron.domain.facets.dto.FacetType.ENUM
					&& facet.getType() != cz.aron.domain.facets.dto.FacetType.MULTI_REF) {
				throw badRequest("Facet '" + filter.getFacet() + "' does not accept a VALUES filter.");
			}
			if (values.getValues() == null || values.getValues().isEmpty()) {
				throw badRequest("VALUES filter of facet '" + filter.getFacet() + "' has no values.");
			}
			return new FieldFilter.Values(indexFieldOf(facet), values.getValues());
		}
		if (filter instanceof TextFilter text) {
			if (facet.getType() != cz.aron.domain.facets.dto.FacetType.FULLTEXT
					&& facet.getType() != cz.aron.domain.facets.dto.FacetType.FULLTEXTF) {
				throw badRequest("Facet '" + filter.getFacet() + "' does not accept a TEXT filter.");
			}
			if (text.getQ() == null || text.getQ().isBlank()) {
				throw badRequest("TEXT filter of facet '" + filter.getFacet() + "' has no query.");
			}
			return new FieldFilter.Text(indexFieldOf(facet), text.getQ());
		}
		if (filter instanceof RangeFilter range) {
			if (facet.getType() != cz.aron.domain.facets.dto.FacetType.UNITDATE) {
				throw badRequest("Facet '" + filter.getFacet() + "' does not accept a RANGE filter.");
			}
			LocalDateTime fromBound = parseBound(range.getFrom(), false, filter.getFacet());
			LocalDateTime toBound = parseBound(range.getTo(), true, filter.getFacet());
			if (fromBound == null && toBound == null) {
				throw badRequest("RANGE filter of facet '" + filter.getFacet() + "' has no bounds.");
			}
			return new FieldFilter.Range(indexFieldOf(facet), fromBound, toBound,
					Boolean.TRUE.equals(range.getIncludeUndated()));
		}
		throw badRequest("Unsupported filter kind.");
	}

	/**
	 * Resolves a relation filter into the port's field-level form. The facet
	 * decides the scope - one reference field for a configured REF facet, every
	 * one of them for the built-in {@link BuiltInFacets#RELATED_FACET} - and the
	 * direction decides which ends of the relation count.
	 *
	 * <p>OUTGOING is expanded here, above the port: the named APUs' own
	 * reference items are read and the index-only ones (APUX {@code
	 * visible="false"}) dropped, so this half follows exactly the links a
	 * reader saw on that record's page. Engines stay free of the display model.
	 */
	private FieldFilter toRelatedFilter(RelatedFilter related, Map<String, FacetConfigDto> byCode) {
		if (related.getApus() == null || related.getApus().isEmpty()) {
			throw badRequest("RELATED filter of facet '" + related.getFacet() + "' names no APU.");
		}
		List<UUID> apus = related.getApus().stream().map(value -> parseUuid(value, related.getFacet())).toList();
		List<String> scope;
		// the built-in facet spans every reference type, so it restricts nothing when
		// following a record's own references outwards (null = any item type); a
		// configured facet follows only its own field
		Set<String> outgoingScope = null;
		if (BuiltInFacets.RELATED_FACET.equals(related.getFacet())) {
			scope = referenceFields;
		} else {
			FacetConfigDto facet = byCode.get(related.getFacet());
			if (facet == null) {
				throw badRequest("Unknown facet '" + related.getFacet() + "' for the requested apuType.");
			}
			if (facet.getType() != cz.aron.domain.facets.dto.FacetType.MULTI_REF) {
				throw badRequest("Facet '" + related.getFacet() + "' does not accept a RELATED filter.");
			}
			scope = List.of(facet.getSource());
			outgoingScope = Set.of(facet.getSource());
		}
		RelationDirection direction = related.getDirection() != null
				? related.getDirection()
				: RelationDirection.INCOMING;
		List<String> targets = direction == RelationDirection.OUTGOING
				? List.of()
				: apus.stream().map(UUID::toString).toList();
		List<String> uuids = direction == RelationDirection.INCOMING
				? List.of()
				: visibleReferencesOf(apus, outgoingScope);
		return new FieldFilter.Related(scope, targets, uuids);
	}

	/**
	 * The APUs the given records visibly reference - {@code scope} restricts the
	 * reference item types followed, {@code null} follows every one of them
	 * (indexability is irrelevant here: the match is on the target's own uuid).
	 * An unknown uuid contributes nothing rather than failing the request - a
	 * relation to a record that is not published is simply no relation.
	 */
	private List<String> visibleReferencesOf(List<UUID> apus, Set<String> scope) {
		var targets = new LinkedHashSet<String>();
		for (UUID uuid : apus) {
			var apu = apuEntityRepository.findByUuid(uuid);
			if (apu == null) {
				continue;
			}
			for (var part : ApuSerializer.deserialize(apu.getData())) {
				if (part.getItems() == null) {
					continue;
				}
				for (var item : part.getItems()) {
					if (Boolean.FALSE.equals(item.getVisible())
							|| (scope != null && !scope.contains(item.getType()))) {
						continue;
					}
					ItemType itemType = typesHolder.getItemTypeForCode(item.getType());
					if (itemType != null && DataType.APU_REF.equals(itemType.getType())) {
						targets.add(item.getValue());
					}
				}
			}
		}
		return List.copyOf(targets);
	}

	private static UUID parseUuid(String value, String facet) {
		try {
			return UUID.fromString(value);
		} catch (IllegalArgumentException e) {
			throw badRequest("RELATED filter of facet '" + facet + "' has a malformed uuid '" + value + "'.");
		}
	}

	/** Accepts a year (1190), a date (1190-05-01) or a full ISO date-time; expands to the interval edge. */
	private static LocalDateTime parseBound(String value, boolean upper, String facet) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			if (value.matches("\\d{1,4}")) {
				Year year = Year.parse(String.format("%04d", Integer.parseInt(value)));
				return upper ? year.atMonth(12).atEndOfMonth().atTime(23, 59, 59) : year.atDay(1).atStartOfDay();
			}
			if (value.matches("\\d{4}-\\d{2}-\\d{2}")) {
				LocalDate date = LocalDate.parse(value);
				return upper ? date.atTime(23, 59, 59) : date.atStartOfDay();
			}
			return LocalDateTime.parse(value);
		} catch (DateTimeParseException | NumberFormatException e) {
			throw badRequest("Invalid date bound '" + value + "' of facet '" + facet + "'.");
		}
	}

	private FacetDef toFacetDef(FacetConfigDto facet, Locale locale) {
		var def = new FacetDef(facet.getSource(), toFacetType(facet.getType()), label(facet, locale),
				facet.getDisplay() == DisplayType.DETAIL ? FacetDisplay.DETAIL : FacetDisplay.ALWAYS);
		def.setTooltip(LocalizedText.pick(facet.getTooltipTranslations(), facet.getTooltip(), locale));
		String description = builtInFacets.description(facet.getSource(), locale);
		def.setDescription(description != null
				? description
				: LocalizedText.pick(facet.getDescriptionTranslations(), facet.getDescription(), locale));
		if (facet.getOrderBy() != null) {
			def.setOrderBy("ASC".equalsIgnoreCase(facet.getOrderBy()) ? FacetOrder.ASC : FacetOrder.FREQ);
		}
		if (facet.getDisplayedItems() > 0) {
			def.setDisplayedItems(facet.getDisplayedItems());
		}
		if (facet.getMaxDisplayedItems() > 0) {
			def.setMaxDisplayedItems(facet.getMaxDisplayedItems());
		}
		return def;
	}

	/**
	 * Facet label: a built-in facet's own text, otherwise an explicit title, the
	 * (localized) item-type name, or - failing all of those - the code.
	 */
	private String label(FacetConfigDto facet, Locale locale) {
		String builtIn = builtInFacets.label(facet.getSource(), locale);
		if (builtIn != null) {
			return builtIn;
		}
		if (facet.getTitle() != null && !facet.getTitle().isBlank()) {
			return LocalizedText.pick(facet.getTitleTranslations(), facet.getTitle(), locale);
		}
		var itemType = typesHolder.getItemTypeForCode(facet.getSource());
		if (itemType != null && itemType.getName() != null) {
			return LocalizedText.pick(itemType.getLang(), itemType.getName(), locale);
		}
		return facet.getSource();
	}

	private static cz.aron.api.v1.model.FacetType toFacetType(cz.aron.domain.facets.dto.FacetType type) {
		// FULLTEXTF is a server-side variant; for clients both are a text filter
		return switch (type) {
			case FULLTEXT, FULLTEXTF -> cz.aron.api.v1.model.FacetType.FULLTEXT;
			case ENUM -> cz.aron.api.v1.model.FacetType.ENUM;
			case MULTI_REF -> cz.aron.api.v1.model.FacetType.REF;
			case UNITDATE -> cz.aron.api.v1.model.FacetType.UNITDATE;
			// filtered out by facetsFor, so it never reaches a client
			case MULTI_REF_EXT -> throw new IllegalStateException("MULTI_REF_EXT is not served by /api/v1.");
		};
	}

	/**
	 * A facet whose values are APU references: MULTI_REF facets by definition,
	 * and ENUM facets whose source item type is APU_REF (deployments configure
	 * e.g. an institution facet this way). Such facets enumerate the composite
	 * {@code ~ID~LABEL} field so their buckets carry display labels, not uuids.
	 */
	private boolean referenceValued(FacetConfigDto facet) {
		if (facet.getType() == cz.aron.domain.facets.dto.FacetType.MULTI_REF) {
			return true;
		}
		if (facet.getType() == cz.aron.domain.facets.dto.FacetType.ENUM) {
			var itemType = typesHolder.getItemTypeForCode(facet.getSource());
			return itemType != null && DataType.APU_REF.equals(itemType.getType());
		}
		return false;
	}

	/**
	 * The index field behind a facet code: its item-type code for a configured
	 * facet, the built-in's own field for a reserved one (see
	 * {@link BuiltInFacets}).
	 */
	private String indexFieldOf(FacetConfigDto facet) {
		return builtInFacets.indexField(facet.getSource());
	}

	private String bucketFieldOf(FacetConfigDto facet) {
		String field = indexFieldOf(facet);
		return referenceValued(facet) ? field + "~ID~LABEL" : field;
	}

	/**
	 * Port buckets to contract buckets; reference buckets split their composite
	 * {@code uuid|label} value (see the ~ID~LABEL layout of ApuDocumentBuilder).
	 */
	private static List<FacetBucket> toFacetBuckets(List<ApuSearchResult.Bucket> buckets, boolean reference) {
		if (buckets == null) {
			return List.of();
		}
		return buckets.stream().map(b -> {
			if (reference) {
				int separator = b.value().indexOf('|');
				if (separator > 0) {
					var facetBucket = new FacetBucket(b.value().substring(0, separator), b.count());
					facetBucket.setLabel(b.value().substring(separator + 1));
					return facetBucket;
				}
			}
			return new FacetBucket(b.value(), b.count());
		}).collect(Collectors.toCollection(ArrayList::new));
	}

	/** ASC = alphabetical by display label, otherwise (FREQ, the default) by count with a label tiebreak. */
	private static List<FacetBucket> orderFacetBuckets(List<FacetBucket> buckets, FacetConfigDto facet) {
		Function<FacetBucket, String> label = b -> b.getLabel() != null ? b.getLabel() : b.getValue();
		Comparator<FacetBucket> comparator = "ASC".equalsIgnoreCase(facet.getOrderBy())
				? Comparator.comparing(label)
				: Comparator.comparingLong(FacetBucket::getCount).reversed().thenComparing(label);
		return buckets.stream().sorted(comparator).toList();
	}

	/**
	 * Label match of the type-ahead: all folded words of {@code q} must match
	 * words of the label, the last one as a prefix (the engines' analyzed-match
	 * semantics, applied engine-neutrally on the option labels).
	 */
	static boolean labelMatches(String label, String q) {
		// both sides split like the index analyzers tokenize (non-alphanumerics)
		String[] words = fold(label).split("[^\\p{L}\\p{N}]+");
		String[] tokens = fold(q).split("[^\\p{L}\\p{N}]+");
		for (int i = 0; i < tokens.length; i++) {
			String token = tokens[i];
			if (token.isEmpty()) {
				continue;
			}
			boolean last = i == tokens.length - 1;
			boolean found = false;
			for (String word : words) {
				if (last ? word.startsWith(token) : word.equals(token)) {
					found = true;
					break;
				}
			}
			if (!found) {
				return false;
			}
		}
		return true;
	}

	/** Folds to the comparison form of the index analyzers: lowercase, diacritics stripped. */
	private static String fold(String text) {
		return Normalizer.normalize(text, Normalizer.Form.NFD)
				.replaceAll("\\p{M}+", "")
				.toLowerCase(Locale.ROOT);
	}

	private static int yearOf(long epochMillis) {
		return Instant.ofEpochMilli(epochMillis).atOffset(ZoneOffset.UTC).getYear();
	}

	private static String blankToNull(String value) {
		return value != null && !value.isBlank() ? value : null;
	}

	private static ResponseStatusException badRequest(String message) {
		return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
	}

}
