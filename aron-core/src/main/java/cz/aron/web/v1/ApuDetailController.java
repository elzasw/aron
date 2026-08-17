package cz.aron.web.v1;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import cz.aron.api.v1.ApuApi;
import cz.aron.api.v1.model.ApuDetail;
import cz.aron.api.v1.model.ApuLink;
import cz.aron.api.v1.model.ApuType;
import cz.aron.api.v1.model.AttachmentInfo;
import cz.aron.api.v1.model.DigitalObjectInfo;
import cz.aron.api.v1.model.FileInfo;
import cz.aron.api.v1.model.FileType;
import cz.aron.commons.HttpUtils;
import cz.aron.domain.ApuAttachment;
import cz.aron.domain.ApuEntity;
import cz.aron.domain.DigitalObject;
import cz.aron.domain.DigitalObjectFile;
import cz.aron.mapper.ApuSerializer;
import cz.aron.repository.ApuEntityRepository;
import cz.aron.service.ApuService;

/**
 * Implements the /api/v1 APU detail: the render model of D-9 - display-ready
 * parts assembled server-side ({@link ApuDetailBuilder}), breadcrumbs from the
 * ancestor chain, attachment/digital-object metadata (binaries arrive with the
 * tiles slice). Conditional requests follow the old API's mechanism (the tree
 * UX depends on client caching).
 */
@RestController
public class ApuDetailController implements ApuApi {

	private static final long CACHE_MAX_AGE_SECONDS = 1800;

	private final ApuEntityRepository apuEntityRepository;

	private final ApuService apuService;

	private final ApuDetailBuilder detailBuilder;

	public ApuDetailController(ApuEntityRepository apuEntityRepository, ApuService apuService,
			ApuDetailBuilder detailBuilder) {
		this.apuEntityRepository = apuEntityRepository;
		this.apuService = apuService;
		this.detailBuilder = detailBuilder;
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ApuDetail> apuGetDetail(String uuid, String ifNoneMatch, String ifModifiedSince) {
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

		var expireStatus = HttpUtils.computeExpired(publishedOf(apu), ifNoneMatch, ifModifiedSince);
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
				apu.getChildCnt(), breadcrumbs(apu), detailBuilder.buildParts(parts, refLabels),
				attachments(apu), digitalObjects(apu));
		detail.setDescription(apu.getDescription());
		detail.setPermalink(apu.getPermalink());

		return ResponseEntity.ok()
				.cacheControl(cacheControl)
				.eTag(expireStatus.eTag())
				.lastModified(expireStatus.lastModified())
				.body(detail);
	}

	/** Ancestor chain root-first (the repository returns it nearest-first). */
	private List<ApuLink> breadcrumbs(ApuEntity apu) {
		if (apu.getParent() == null) {
			return List.of();
		}
		var ancestors = new ArrayList<>(apuEntityRepository.findAncestors(apu.getId()));
		Collections.reverse(ancestors);
		return ancestors.stream().map(a -> {
			var link = new ApuLink(a.uuid().toString(), a.name());
			link.setDescription(a.description());
			return link;
		}).toList();
	}

	private static List<AttachmentInfo> attachments(ApuEntity apu) {
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

	private static List<DigitalObjectInfo> digitalObjects(ApuEntity apu) {
		return apu.getDigitalObjects().stream()
				.sorted(Comparator.comparingInt(DigitalObject::getOrder))
				.map(digitalObject -> {
					var files = digitalObject.getFiles().stream()
							.sorted(Comparator.comparingInt(DigitalObjectFile::getOrder))
							.map(ApuDetailController::fileInfo)
							.toList();
					var info = new DigitalObjectInfo(digitalObject.getUuid().toString(), files);
					info.setName(digitalObject.getName());
					info.setPermalink(digitalObject.getPermalink());
					return info;
				}).toList();
	}

	private static FileInfo fileInfo(DigitalObjectFile file) {
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
