package cz.aron.web.v1;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import cz.aron.api.v1.SearchApi;
import cz.aron.api.v1.model.ApuSearchItem;
import cz.aron.api.v1.model.ApuSearchRequest;
import cz.aron.api.v1.model.ApuSearchResponse;
import cz.aron.api.v1.model.ApuType;
import cz.aron.api.v1.model.FacetBucket;
import cz.aron.api.v1.model.FacetDef;
import cz.aron.api.v1.model.FacetDisplay;
import cz.aron.api.v1.model.FacetOrder;
import cz.aron.api.v1.model.FacetResult;
import cz.aron.api.v1.model.RangeFilter;
import cz.aron.api.v1.model.SearchFilter;
import cz.aron.api.v1.model.SortMode;
import cz.aron.api.v1.model.TextFilter;
import cz.aron.api.v1.model.ValuesFilter;
import cz.aron.domain.facets.FacetsLoader;
import cz.aron.domain.facets.dto.DisplayType;
import cz.aron.domain.facets.dto.FacetConfigDto;
import cz.aron.domain.types.TypesHolder;
import cz.aron.search.ApuSearchQuery;
import cz.aron.search.ApuSearchResult;
import cz.aron.search.FieldFilter;
import cz.aron.search.IndexingService;
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

	private final FacetsLoader facetsLoader;

	private final TypesHolder typesHolder;

	private final IndexingService indexingService;

	private List<FacetConfigDto> facets;

	public SearchController(FacetsLoader facetsLoader, TypesHolder typesHolder, IndexingService indexingService) {
		this.facetsLoader = facetsLoader;
		this.typesHolder = typesHolder;
		this.indexingService = indexingService;
	}

	@PostConstruct
	void load() {
		try {
			facets = facetsLoader.loadFacets();
		} catch (IOException e) {
			throw new UncheckedIOException("Fail to load facet configuration", e);
		}
	}

	@Override
	public ResponseEntity<List<FacetDef>> searchGetFacets(ApuType apuType) {
		return ResponseEntity.ok(facetsFor(apuType).stream().map(this::toFacetDef).toList());
	}

	@Override
	public ResponseEntity<ApuSearchResponse> searchSearch(ApuSearchRequest request) {
		List<FacetConfigDto> sectionFacets = request.getApuType() != null
				? facetsFor(request.getApuType())
				: List.of();
		Map<String, FacetConfigDto> byCode = sectionFacets.stream()
				.collect(Collectors.toMap(FacetConfigDto::getSource, Function.identity(), (a, b) -> a));

		var filters = new ArrayList<FieldFilter>();
		for (SearchFilter filter : request.getFilters() != null ? request.getFilters() : List.<SearchFilter>of()) {
			filters.add(toFieldFilter(filter, byCode));
		}
		// buckets are counted for every ENUM facet of the section
		Set<String> bucketFields = sectionFacets.stream()
				.filter(f -> f.getType() == cz.aron.domain.facets.dto.FacetType.ENUM)
				.map(FacetConfigDto::getSource)
				.collect(Collectors.toCollection(LinkedHashSet::new));

		int from = request.getFrom() != null ? request.getFrom() : 0;
		int size = request.getSize() != null ? request.getSize() : 10;
		var sort = request.getSort() == SortMode.NAME
				? ApuSearchQuery.SortMode.NAME
				: ApuSearchQuery.SortMode.RELEVANCE;
		String fulltext = request.getQuery() != null && !request.getQuery().isBlank() ? request.getQuery() : null;
		String apuType = request.getApuType() != null ? request.getApuType().getValue() : null;

		ApuSearchResult result = indexingService
				.search(new ApuSearchQuery(apuType, fulltext, filters, bucketFields, from, size, sort));

		var items = result.hits().stream()
				.map(h -> {
					var item = new ApuSearchItem(h.uuid(), h.name(), ApuType.fromValue(h.type()),
							h.containsDigitalObjects());
					item.setDescription(h.description());
					return item;
				})
				.toList();
		var facetResults = new ArrayList<FacetResult>();
		for (FacetConfigDto facet : sectionFacets) {
			if (facet.getType() != cz.aron.domain.facets.dto.FacetType.ENUM) {
				continue;
			}
			var buckets = result.buckets().getOrDefault(facet.getSource(), List.of());
			facetResults.add(new FacetResult(facet.getSource(), orderBuckets(buckets, facet).stream()
					.map(b -> new FacetBucket(b.value(), b.count()))
					.toList()));
		}
		return ResponseEntity.ok(new ApuSearchResponse((long) result.total(), items, facetResults));
	}

	private FieldFilter toFieldFilter(SearchFilter filter, Map<String, FacetConfigDto> byCode) {
		FacetConfigDto facet = byCode.get(filter.getFacet());
		if (facet == null) {
			throw badRequest("Unknown facet '" + filter.getFacet() + "' for the requested apuType.");
		}
		if (filter instanceof ValuesFilter values) {
			if (facet.getType() != cz.aron.domain.facets.dto.FacetType.ENUM
					&& facet.getType() != cz.aron.domain.facets.dto.FacetType.MULTI_REF
					&& facet.getType() != cz.aron.domain.facets.dto.FacetType.MULTI_REF_EXT
					&& facet.getType() != cz.aron.domain.facets.dto.FacetType.MULTI_TYPE_REF) {
				throw badRequest("Facet '" + filter.getFacet() + "' does not accept a VALUES filter.");
			}
			if (values.getValues() == null || values.getValues().isEmpty()) {
				throw badRequest("VALUES filter of facet '" + filter.getFacet() + "' has no values.");
			}
			return new FieldFilter.Values(facet.getSource(), values.getValues());
		}
		if (filter instanceof TextFilter text) {
			if (facet.getType() != cz.aron.domain.facets.dto.FacetType.FULLTEXT
					&& facet.getType() != cz.aron.domain.facets.dto.FacetType.FULLTEXTF) {
				throw badRequest("Facet '" + filter.getFacet() + "' does not accept a TEXT filter.");
			}
			if (text.getQ() == null || text.getQ().isBlank()) {
				throw badRequest("TEXT filter of facet '" + filter.getFacet() + "' has no query.");
			}
			return new FieldFilter.Text(facet.getSource(), text.getQ());
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
			return new FieldFilter.Range(facet.getSource(), fromBound, toBound);
		}
		throw badRequest("Unsupported filter kind.");
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

	private List<FacetConfigDto> facetsFor(ApuType apuType) {
		return facets.stream().filter(f -> appliesTo(f, apuType)).toList();
	}

	/** A facet without a when-condition applies to every APU type. */
	private static boolean appliesTo(FacetConfigDto facet, ApuType apuType) {
		if (facet.getWhen() == null) {
			return true;
		}
		if (facet.getWhen() instanceof Map<?, ?> when) {
			Object condition = when.get("apuType");
			return condition == null || Objects.equals(String.valueOf(condition), apuType.getValue());
		}
		return true;
	}

	private FacetDef toFacetDef(FacetConfigDto facet) {
		var def = new FacetDef(facet.getSource(), toFacetType(facet.getType()), label(facet),
				facet.getDisplay() == DisplayType.DETAIL ? FacetDisplay.DETAIL : FacetDisplay.ALWAYS);
		def.setTooltip(facet.getTooltip());
		def.setDescription(facet.getDescription());
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

	/** Facet label: explicit title, otherwise the (localized) item-type name, otherwise the code. */
	private String label(FacetConfigDto facet) {
		if (facet.getTitle() != null && !facet.getTitle().isBlank()) {
			return facet.getTitle();
		}
		var itemType = typesHolder.getItemTypeForCode(facet.getSource());
		if (itemType != null && itemType.getName() != null) {
			return itemType.getName();
		}
		return facet.getSource();
	}

	private static cz.aron.api.v1.model.FacetType toFacetType(cz.aron.domain.facets.dto.FacetType type) {
		// FULLTEXTF is a server-side variant; for clients both are a text filter
		return switch (type) {
			case FULLTEXT, FULLTEXTF -> cz.aron.api.v1.model.FacetType.FULLTEXT;
			case ENUM -> cz.aron.api.v1.model.FacetType.ENUM;
			case MULTI_REF -> cz.aron.api.v1.model.FacetType.MULTI_REF;
			case UNITDATE -> cz.aron.api.v1.model.FacetType.UNITDATE;
			case MULTI_REF_EXT -> cz.aron.api.v1.model.FacetType.MULTI_REF_EXT;
			case MULTI_TYPE_REF -> cz.aron.api.v1.model.FacetType.MULTI_TYPE_REF;
		};
	}

	/** ASC = alphabetical, otherwise (FREQ, the default) by count with a stable alphabetical tiebreak. */
	private static List<ApuSearchResult.Bucket> orderBuckets(List<ApuSearchResult.Bucket> buckets,
			FacetConfigDto facet) {
		Comparator<ApuSearchResult.Bucket> comparator = "ASC".equalsIgnoreCase(facet.getOrderBy())
				? Comparator.comparing(ApuSearchResult.Bucket::value)
				: Comparator.comparingLong(ApuSearchResult.Bucket::count).reversed()
						.thenComparing(ApuSearchResult.Bucket::value);
		return buckets.stream().sorted(comparator).toList();
	}

	private static ResponseStatusException badRequest(String message) {
		return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
	}

}
