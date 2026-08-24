package cz.aron.web.v1;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import cz.aron.api.rest.model.ApuEntityTreeViewDto;
import cz.aron.api.v1.ApuApi;
import cz.aron.api.v1.model.ApuDetail;
import cz.aron.api.v1.model.ApuType;
import cz.aron.api.v1.model.AttachmentInfo;
import cz.aron.api.v1.model.Citation;
import cz.aron.api.v1.model.DigitalObjectInfo;
import cz.aron.api.v1.model.FileInfo;
import cz.aron.api.v1.model.FileType;
import cz.aron.api.v1.model.TreeDirection;
import cz.aron.api.v1.model.TreeNode;
import cz.aron.commons.HttpUtils;
import cz.aron.domain.ApuAttachment;
import cz.aron.domain.ApuEntity;
import cz.aron.domain.DigitalObject;
import cz.aron.domain.DigitalObjectFile;
import cz.aron.mapper.ApuSerializer;
import cz.aron.repository.ApuEntityRepository;
import cz.aron.service.ApuService;
import cz.aron.service.CitationService;

/**
 * Implements the /api/v1 APU detail: the render model of D-9 - display-ready
 * parts assembled server-side ({@link ApuDetailBuilder}), breadcrumbs from the
 * ancestor chain, attachments and digital objects with ready-to-use file URLs
 * ({@link DaoFileUrls}). Conditional requests follow the old API's mechanism
 * (the tree UX depends on client caching).
 */
@RestController
public class ApuDetailController implements ApuApi {

	private static final long CACHE_MAX_AGE_SECONDS = 1800;

	private final ApuEntityRepository apuEntityRepository;

	private final ApuService apuService;

	private final ApuDetailBuilder detailBuilder;

	private final PresentationLocales presentationLocales;

	private final DaoFileUrls daoFileUrls;

	private final DaoFooterConfig daoFooterConfig;

	private final DeploymentImages images;

	private final CitationService citationService;

	public ApuDetailController(ApuEntityRepository apuEntityRepository, ApuService apuService,
			ApuDetailBuilder detailBuilder, PresentationLocales presentationLocales, DaoFileUrls daoFileUrls,
			CitationService citationService, DaoFooterConfig daoFooterConfig, DeploymentImages images) {
		this.presentationLocales = presentationLocales;
		this.apuEntityRepository = apuEntityRepository;
		this.apuService = apuService;
		this.detailBuilder = detailBuilder;
		this.daoFileUrls = daoFileUrls;
		this.citationService = citationService;
		this.daoFooterConfig = daoFooterConfig;
		this.images = images;
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ApuDetail> apuGetDetail(String uuid, String lang, String ifNoneMatch, String ifModifiedSince) {
		ApuEntity apu = require(uuid);

		Locale locale = presentationLocales.resolve(lang);
		// the rendered labels and datings depend on the language, so it discriminates the ETag
		var expireStatus = HttpUtils.computeExpired(publishedOf(apu), ifNoneMatch, ifModifiedSince,
				locale.toLanguageTag());
		var cacheControl = CacheControl.maxAge(CACHE_MAX_AGE_SECONDS, TimeUnit.SECONDS).cachePrivate()
				.mustRevalidate();
		if (!expireStatus.expired()) {
			return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
					.cacheControl(cacheControl)
					.eTag(expireStatus.eTag())
					.lastModified(expireStatus.lastModified())
					.build();
		}

		var parts = ApuSerializer.deserialize(apu.getData());
		var refLabels = apuService.resolveApuRefLabels(List.of(apu));

		var detail = new ApuDetail(uuid, apu.getName(), ApuType.fromValue(apu.getType().toString()),
				apu.getChildCnt(), treePath(apu), detailBuilder.buildParts(parts, refLabels, locale),
				attachments(apu), digitalObjects(apu, locale));
		detail.setDescription(apu.getDescription());
		detail.setPermalink(apu.getPermalink());

		return ResponseEntity.ok()
				.cacheControl(cacheControl)
				.eTag(expireStatus.eTag())
				.lastModified(expireStatus.lastModified())
				.body(detail);
	}

	/**
	 * Citations of one record, rendered by the deployment's citation scripts
	 * ({@link CitationService}). Not cached: the text follows the deployment's
	 * citation configuration rather than the record alone.
	 * <p>
	 * A record no form can cite answers 422; which piece of the description was
	 * missing is logged by the service. The reader was never offered a citation
	 * here ({@code UiConfig.citations} says where one exists), so that is
	 * diagnosis for whoever looks into the data - the reason travels with the
	 * exception and reaches a client only where a deployment turns Spring's
	 * error message on.
	 */
	@Override
	public ResponseEntity<List<Citation>> apuGetCitations(String uuid, String lang) {
		ApuEntity apu = require(uuid);
		var citations = citationService.citations(apu, presentationLocales.resolve(lang));
		if (citations.items().isEmpty()) {
			throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, citations.diagnostic() != null
					? "The record cannot be cited: " + citations.diagnostic()
					: "No citation form covers " + apu.getType() + ".");
		}
		return ResponseEntity.ok()
				.cacheControl(CacheControl.noStore())
				.body(citations.items().stream()
						.map(item -> new Citation(item.code(), item.label(), item.text()))
						.toList());
	}

	@Override
	public ResponseEntity<List<TreeNode>> apuGetTreeNodes(String uuid, TreeDirection direction, String ifNoneMatch,
			String ifModifiedSince) {
		UUID apuUuid;
		try {
			apuUuid = UUID.fromString(uuid);
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No such APU.");
		}
		// the service owns the window queries and the conditional-request logic
		ApuService.Result<List<ApuEntityTreeViewDto>> result = switch (direction) {
			case BEFORE -> apuService.getEntitiesBefore(apuUuid, ifNoneMatch, ifModifiedSince);
			case AFTER -> apuService.getEntitiesAfter(apuUuid, ifNoneMatch, ifModifiedSince);
			case UNDER -> apuService.getEntitiesUnder(apuUuid, ifNoneMatch, ifModifiedSince);
		};
		var cacheControl = CacheControl.maxAge(CACHE_MAX_AGE_SECONDS, TimeUnit.SECONDS).cachePrivate()
				.mustRevalidate();
		if (result instanceof ApuService.NotModified<List<ApuEntityTreeViewDto>> notModified) {
			return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
					.cacheControl(cacheControl)
					.eTag(notModified.eTag())
					.lastModified(notModified.lastModified())
					.build();
		}
		var data = (ApuService.Data<List<ApuEntityTreeViewDto>>) result;
		return ResponseEntity.ok()
				.cacheControl(cacheControl)
				.eTag(data.eTag())
				.lastModified(data.lastModified())
				.body(data.value().stream().map(ApuDetailController::treeNode).toList());
	}

	/** The record, or 404 - an unparsable uuid names no record either. */
	private ApuEntity require(String uuid) {
		UUID apuUuid;
		try {
			apuUuid = UUID.fromString(uuid);
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No such APU.");
		}
		ApuEntity apu = apuEntityRepository.findByUuid(apuUuid);
		if (apu == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No such APU.");
		}
		return apu;
	}

	private static TreeNode treeNode(ApuEntityTreeViewDto dto) {
		var node = new TreeNode(dto.getId(), dto.getName(), dto.getDepth(), dto.getPos(), dto.getChildCnt());
		if (dto.getDescription() != null && !dto.getDescription().isBlank()) {
			node.setDescription(dto.getDescription());
		}
		return node;
	}

	/** The APU's tree path root-first, itself last (ancestors come nearest-first from the repository). */
	private List<TreeNode> treePath(ApuEntity apu) {
		var path = new ArrayList<TreeNode>();
		if (apu.getParent() != null) {
			var ancestors = new ArrayList<>(apuEntityRepository.findAncestors(apu.getId()));
			Collections.reverse(ancestors);
			for (var ancestor : ancestors) {
				var node = new TreeNode(ancestor.uuid().toString(), ancestor.name(), ancestor.depth(),
						ancestor.pos(), ancestor.childCnt());
				if (ancestor.description() != null && !ancestor.description().isBlank()) {
					node.setDescription(ancestor.description());
				}
				path.add(node);
			}
		}
		var self = new TreeNode(apu.getUuid().toString(), apu.getName(), apu.getDepth(), apu.getPos(),
				apu.getChildCnt());
		if (apu.getDescription() != null && !apu.getDescription().isBlank()) {
			self.setDescription(apu.getDescription());
		}
		path.add(self);
		return path;
	}

	private List<AttachmentInfo> attachments(ApuEntity apu) {
		return apu.getAttachments().stream()
				.sorted(Comparator.comparingInt(ApuAttachment::getOrder))
				.map(attachment -> {
					var info = new AttachmentInfo(attachment.getName());
					if (attachment.getFile() != null) {
						info.setFile(fileInfo(attachment.getFile()));
					}
					return info;
				}).toList();
	}

	private List<DigitalObjectInfo> digitalObjects(ApuEntity apu, Locale locale) {
		return apu.getDigitalObjects().stream()
				.sorted(Comparator.comparingInt(DigitalObject::getOrder))
				.map(digitalObject -> {
					var files = digitalObject.getFiles().stream()
							.sorted(Comparator.comparingInt(DigitalObjectFile::getOrder))
							.map(this::fileInfo)
							.toList();
					var info = new DigitalObjectInfo(digitalObject.getUuid().toString(), files);
					info.setName(digitalObject.getName());
					info.setPermalink(digitalObject.getPermalink());
					info.setLicense(digitalObject.getLicense());
					// the deployment's attribution, resolved here so the client never matches codes
					info.setFooter(daoFooterConfig.footerFor(digitalObject.getLicense(), locale, images::url));
					return info;
				}).toList();
	}

	private FileInfo fileInfo(DigitalObjectFile file) {
		var info = new FileInfo(file.getUuid().toString());
		if (file.getType() != null) {
			info.setFileType(FileType.fromValue(file.getType().toString()));
		}
		info.setName(file.getName());
		info.setContentType(file.getContentType());
		info.setSize(file.getSize());
		if (file.isSelected()) {
			info.setSelected(Boolean.TRUE);
		}
		if (file.getOrder() > 0) {
			info.setPosition(file.getOrder());
		}
		info.setUrl(daoFileUrls.contentUrl(file));
		info.setDziUrl(daoFileUrls.dziUrl(file));
		return info;
	}

	/**
	 * The moment the APU was last published - the later of its source's publish
	 * time and, when present, the time a DAO was connected (the old API's rule).
	 */
	private static LocalDateTime publishedOf(ApuEntity apu) {
		var published = apu.getSource().getPublished();
		if (apu.getDaoPublished() != null && published.isBefore(apu.getDaoPublished())) {
			published = apu.getDaoPublished();
		}
		return published;
	}

}
