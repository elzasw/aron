package cz.aron.integration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import cz.aron.ft.handling.TransferType;

/**
 * Minimal APUSRC transfers for the tests that drive the import mechanism - the
 * input-directory scan and the SOAP management interface both need one, and a
 * transfer written twice from two files is a fixture that can disagree with
 * itself about what an import even is.
 *
 * <p>One APU of a type with no required parts, so the XML says only what the
 * test is about: which source it belongs to, which APU it carries, and its name.
 */
public final class ApuxTransfers {

	private ApuxTransfers() {
	}

	/**
	 * Writes a one-APU transfer into {@code folder} (created if absent). Writing
	 * the same {@code apuSourceUuid} again is a re-import of that source, not a
	 * second one - which is what makes a changed name observable.
	 */
	public static Path write(Path folder, UUID apuSourceUuid, UUID apuUuid, String name) throws IOException {
		Files.createDirectories(folder);
		var xml = """
				<?xml version="1.0"?>
				<apusrc xmlns="http://www.aron.cz/apux/2020" uuid="%s">
				 <apus>
				  <apu type="Institution" uuid="%s">
				   <name>%s</name>
				  </apu>
				 </apus>
				</apusrc>
				""".formatted(apuSourceUuid, apuUuid, name);
		// the import mechanism picks the transfer's descriptor by this prefix
		Files.writeString(folder.resolve("apusrc-transfer.xml"), xml, StandardCharsets.UTF_8);
		return folder;
	}

	/** Writes such a transfer and imports it through the internal import mechanism. */
	public static void writeAndImport(ImportDataProcessingService importService, Path folder, UUID apuSourceUuid,
			UUID apuUuid, String name) throws IOException {
		importService.processData(write(folder, apuSourceUuid, apuUuid, name), TransferType.APUSRC);
	}

	/**
	 * Writes a one-APU transfer that also announces a digital object (the
	 * {@code <daos>} uuid creates the placeholder a later DAO transfer fills) and
	 * carries one attachment with its file content.
	 */
	public static Path writeWithDao(Path folder, UUID apuSourceUuid, UUID apuUuid, String name, UUID daoUuid,
			String attachmentName, UUID attachmentFileUuid, byte[] attachmentContent, String attachmentMimeType)
			throws IOException {
		Files.createDirectories(folder);
		var xml = """
				<?xml version="1.0"?>
				<apusrc xmlns="http://www.aron.cz/apux/2020" uuid="%s">
				 <apus>
				  <apu type="Institution" uuid="%s">
				   <name>%s</name>
				   <attchs>
				    <name>%s</name>
				    <file pos="1" uuid="%s">
				     <mtdt><itms><code>mimeType</code><value>%s</value></itms></mtdt>
				    </file>
				   </attchs>
				   <daos><uuid>%s</uuid></daos>
				  </apu>
				 </apus>
				</apusrc>
				""".formatted(apuSourceUuid, apuUuid, name, attachmentName, attachmentFileUuid, attachmentMimeType,
				daoUuid);
		Files.writeString(folder.resolve("apusrc-transfer.xml"), xml, StandardCharsets.UTF_8);
		Path filesDir = Files.createDirectories(folder.resolve("files"));
		Files.write(filesDir.resolve("file-" + attachmentFileUuid), attachmentContent);
		return folder;
	}

	/**
	 * A DAO transfer under construction: the {@code dao-<uuid>.xml} descriptor
	 * plus the {@code files/file-<uuid>} payloads of its non-reference files.
	 * Bundle types are the APUX vocabulary ({@code Published}, {@code Thumbnail},
	 * {@code HighResView}); files keep the order they were added in.
	 */
	public static final class DaoTransfer {

		private static final class FileSpec {
			final String bundleType;
			final UUID uuid;
			final int pos;
			final byte[] content;
			final String path;
			final String mimeType;
			final String name;
			final boolean selected;
			String permalink;

			FileSpec(String bundleType, UUID uuid, int pos, byte[] content, String path, String mimeType,
					String name, boolean selected) {
				this.bundleType = bundleType;
				this.uuid = uuid;
				this.pos = pos;
				this.content = content;
				this.path = path;
				this.mimeType = mimeType;
				this.name = name;
				this.selected = selected;
			}
		}

		private final UUID daoUuid;

		private final String name;

		private final String license;

		private final java.util.List<FileSpec> files = new java.util.ArrayList<>();

		public DaoTransfer(UUID daoUuid, String name, String license) {
			this.daoUuid = daoUuid;
			this.name = name;
			this.license = license;
		}

		/** A transferred file: its bytes travel as {@code files/file-<uuid>}. */
		public DaoTransfer addFile(String bundleType, UUID uuid, int pos, byte[] content, String mimeType,
				String name, boolean selected) {
			files.add(new FileSpec(bundleType, uuid, pos, content, null, mimeType, name, selected));
			return this;
		}

		/** A reference: no bytes, only the {@code path} metadata (a local path or an external URL). */
		public DaoTransfer addReference(String bundleType, UUID uuid, int pos, String path, String mimeType,
				String name, boolean selected) {
			files.add(new FileSpec(bundleType, uuid, pos, null, path, mimeType, name, selected));
			return this;
		}

		/** Permalink of the most recently added file. */
		public DaoTransfer withPermalink(String permalink) {
			files.get(files.size() - 1).permalink = permalink;
			return this;
		}

		public Path write(Path folder) throws IOException {
			Files.createDirectories(folder);
			var xml = new StringBuilder();
			xml.append("<?xml version=\"1.0\"?>\n");
			xml.append("<dao xmlns=\"http://www.aron.cz/apux/2020\" uuid=\"").append(daoUuid).append("\">\n");
			if (name != null) {
				xml.append(" <name>").append(name).append("</name>\n");
			}
			// bundles in the order their first file was added; schema wants bndl before license
			var bundleTypes = files.stream().map(file -> file.bundleType).distinct().toList();
			for (String bundleType : bundleTypes) {
				xml.append(" <bndl type=\"").append(bundleType).append("\">\n");
				for (FileSpec file : files) {
					if (!file.bundleType.equals(bundleType)) {
						continue;
					}
					xml.append("  <file pos=\"").append(file.pos).append("\" uuid=\"").append(file.uuid)
							.append("\">\n");
					if (file.permalink != null) {
						xml.append("   <prmLnk>").append(file.permalink).append("</prmLnk>\n");
					}
					xml.append("   <mtdt>\n");
					if (file.path != null) {
						xml.append("    <itms><code>reference</code><value>1</value></itms>\n");
						xml.append("    <itms><code>path</code><value>").append(file.path)
								.append("</value></itms>\n");
					}
					if (file.mimeType != null) {
						xml.append("    <itms><code>mimeType</code><value>").append(file.mimeType)
								.append("</value></itms>\n");
					}
					if (file.name != null) {
						xml.append("    <itms><code>name</code><value>").append(file.name)
								.append("</value></itms>\n");
					}
					if (file.selected) {
						xml.append("    <itms><code>selected</code><value>1</value></itms>\n");
					}
					xml.append("   </mtdt>\n  </file>\n");
				}
				xml.append(" </bndl>\n");
			}
			if (license != null) {
				xml.append(" <license>").append(license).append("</license>\n");
			}
			xml.append("</dao>\n");
			Files.writeString(folder.resolve("dao-" + daoUuid + ".xml"), xml.toString(), StandardCharsets.UTF_8);
			for (FileSpec file : files) {
				if (file.content != null) {
					Path filesDir = Files.createDirectories(folder.resolve("files"));
					Files.write(filesDir.resolve("file-" + file.uuid), file.content);
				}
			}
			return folder;
		}

	}

	/**
	 * A transfer of several APUs with the shapes the one-APU writers cannot express:
	 * a parent ({@code <prnt>}) and reference items ({@code <ref>}), so the tests of
	 * the import's relation handling can say who points at whom. Item types are the
	 * test display model's ({@code REL_ENTITY}, {@code FUND_REF}, ...); a reference may
	 * name a record that does not exist - that is data, and exactly what some of
	 * those tests are about.
	 */
	public static final class Transfer {

		private record ApuSpec(UUID uuid, String type, String name, UUID parent, java.util.List<String[]> refs) {
		}

		private final UUID apuSourceUuid;

		private final java.util.List<ApuSpec> apus = new java.util.ArrayList<>();

		public Transfer(UUID apuSourceUuid) {
			this.apuSourceUuid = apuSourceUuid;
		}

		/** Adds an APU; {@code parent} may be null. Its references follow through {@link #ref}. */
		public Transfer apu(UUID uuid, String type, String name, UUID parent) {
			apus.add(new ApuSpec(uuid, type, name, parent, new java.util.ArrayList<>()));
			return this;
		}

		/** A reference item of the most recently added APU. */
		public Transfer ref(String itemType, String targetUuid) {
			apus.get(apus.size() - 1).refs().add(new String[] { itemType, targetUuid });
			return this;
		}

		public Path write(Path folder) throws IOException {
			Files.createDirectories(folder);
			var xml = new StringBuilder();
			xml.append("<?xml version=\"1.0\"?>\n");
			xml.append("<apusrc xmlns=\"http://www.aron.cz/apux/2020\" uuid=\"").append(apuSourceUuid)
					.append("\">\n <apus>\n");
			for (var apu : apus) {
				xml.append("  <apu type=\"").append(apu.type()).append("\" uuid=\"").append(apu.uuid()).append("\">\n");
				xml.append("   <name>").append(apu.name()).append("</name>\n");
				if (apu.parent() != null) {
					xml.append("   <prnt>").append(apu.parent()).append("</prnt>\n");
				}
				if (!apu.refs().isEmpty()) {
					xml.append("   <prts>\n    <part type=\"PT_BODY\">\n     <itms>\n");
					for (var ref : apu.refs()) {
						xml.append("      <ref type=\"").append(ref[0]).append("\">").append(ref[1]).append("</ref>\n");
					}
					xml.append("     </itms>\n    </part>\n   </prts>\n");
				}
				xml.append("  </apu>\n");
			}
			xml.append(" </apus>\n</apusrc>\n");
			Files.writeString(folder.resolve("apusrc-transfer.xml"), xml.toString(), StandardCharsets.UTF_8);
			return folder;
		}

		public void importInto(ImportDataProcessingService importService, Path folder) throws IOException {
			importService.processData(write(folder), TransferType.APUSRC);
		}

	}

}
