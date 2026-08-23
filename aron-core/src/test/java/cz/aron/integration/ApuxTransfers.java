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

}
