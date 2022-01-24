package cz.aron.transfagent.par;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.UUID;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import cz.aron.transfagent.config.ConfigPar;
import cz.aron.transfagent.elza.ElzaXmlReader;
import cz.aron.transfagent.repository.EntitySourceRepository;
import cz.aron.transfagent.repository.InstitutionRepository;
import cz.aron.transfagent.service.FileImportService;
import cz.aron.transfagent.service.StorageService;
import cz.aron.transfagent.service.importfromdir.ImportContext;
import cz.aron.transfagent.service.importfromdir.ImportProcessor;
import cz.nacr.nda.par.api._2019.Archiv;
import cz.nacr.nda.par.api._2019.ArchivyPort;
import cz.tacr.elza.schema.v2.AccessPoint;
import cz.tacr.elza.schema.v2.AccessPointEntry;
import cz.tacr.elza.schema.v2.AccessPoints;
import cz.tacr.elza.schema.v2.DescriptionItemBit;
import cz.tacr.elza.schema.v2.DescriptionItemEnum;
import cz.tacr.elza.schema.v2.DescriptionItemString;
import cz.tacr.elza.schema.v2.ElzaDataExchange;
import cz.tacr.elza.schema.v2.Fragment;
import cz.tacr.elza.schema.v2.Fragments;
import cz.tacr.elza.schema.v2.Institution;
import cz.tacr.elza.schema.v2.Institutions;

@Service
@ConditionalOnProperty(value = "par.url")
public class ParImport implements ImportProcessor {
	
	private static final Logger log = LoggerFactory.getLogger(ParImport.class);
	
	private final ConfigPar config;
	
	private final ArchivyPort archivyPort;
	
	private final InstitutionRepository institutionRepository;
	
	private final StorageService storageService;
	
	private final FileImportService importService;
	
	private final EntitySourceRepository entitySourceRepository;
	
	private OffsetDateTime nextRun = null;

	public ParImport(ConfigPar config, ArchivyPort archivyPort, InstitutionRepository institutionRepository,
			StorageService storageService, FileImportService importService,
			EntitySourceRepository entitySourceRepository) {
		this.config = config;
		this.archivyPort = archivyPort;
		this.institutionRepository = institutionRepository;
		this.storageService = storageService;
		this.importService = importService;
		this.entitySourceRepository = entitySourceRepository;
	}
	
	@Override
	public int getPriority() {
		return 10;
	}
	
	@PostConstruct
	void init() {		
		importService.registerImportProcessor(this);
	}

	@Override
	public void importData(ImportContext ic) {

		final OffsetDateTime nowTime = OffsetDateTime.now();
		if (nextRun != null && nextRun.isAfter(OffsetDateTime.now())) {
			// TODO nejake lepsi planovani
			return;
		}

		int numUpdated = 0;
		var institutionsDir = storageService.getInputPath().resolve("institutions");
		var seznamArchivu = archivyPort.vratSeznamArchivu();
		for (var archiv : seznamArchivu.getArchiv()) {
			var inst = institutionRepository.findByCode(archiv.getCisloArchivu());
			if (inst == null) {
				var edx = createElzaDescription(archiv, UUID.randomUUID().toString());
				saveInstitution(archiv, institutionsDir, edx);
				numUpdated++;
			} else {
				var entities = entitySourceRepository.findByApuSourceJoinFetchArchivalEntity(inst.getApuSource());
				if (entities.isEmpty() || entities.size() > 1) {
					log.error("Expecting one archival entity related to institution {}, num entities {}",
							archiv.getCisloArchivu(), entities.size());
					throw new IllegalStateException();
				}
				var uuid = entities.get(0).getArchivalEntity().getUuid().toString();
				var dataDir = storageService.getApuDataDir(inst.getApuSource().getDataDir());
				var edx = createElzaDescription(archiv, uuid.toString());
				var institutionFile = dataDir.resolve("institution-" + archiv.getCisloArchivu() + ".xml");
				if (Files.isRegularFile(institutionFile)) {
					try {
						byte[] orig = Files.readAllBytes(institutionFile);
						byte[] actual = ElzaXmlReader.write(edx);
						if (Arrays.equals(orig, actual)) {
							continue;
						}
					} catch (Exception e) {
						log.warn("Fail to compare institution xmls", e);
					}
				}
				saveInstitution(archiv, institutionsDir, edx);
				numUpdated++;
			}
		}
		log.info("Update archives from par, num updated {}", numUpdated);
		// po uspesne synchronizaci nastavim dalsi cas synchronizace
		nextRun = nowTime.plusSeconds(config.getInterval());
	}
	
	private void saveInstitution(Archiv archiv, Path institutionsDir, ElzaDataExchange edx) {		
		var dir = institutionsDir.resolve("institution-"+archiv.getCisloArchivu());				
		if (!Files.isDirectory(dir)) {
			try {
				Files.createDirectories(dir);
				ElzaXmlReader.write(edx, dir.resolve("institution-" + archiv.getCisloArchivu() + ".xml"));
			} catch (Exception e) {
				log.error("Fail to save institution {}",archiv.getCisloArchivu(),e);
				try {
					FileSystemUtils.deleteRecursively(dir);
				} catch (IOException e1) {
					
				}
			}
		}								
	}
	
	private ElzaDataExchange createElzaDescription(Archiv archiv, String uuid) {		
		var code = archiv.getCisloArchivu();
		var name = archiv.getJmeno();
		var shortName = archiv.getZkraceneJmeno();
		if (StringUtils.isBlank(shortName)) {
			shortName = name;
		}

		var aps = new AccessPoints();		
		var ap = new AccessPoint();
		aps.getAp().add(ap);
		var ape = new AccessPointEntry();
		ape.setId(uuid);
		ape.setUuid(uuid);		
		ape.setT("PUBLIC_ADMINISTRATION");
		ap.setApe(ape);

		Fragments frgs = new Fragments();
		ap.setFrgs(frgs);

		var frg1 = new Fragment();		
		frg1.setT("PT_NAME");
		frg1.getDdOrDoOrDp().add(createDS("NM_MAIN", name));
		frg1.getDdOrDoOrDp().add(createDE("NM_LANG", "LNG_cze"));
		frgs.getFrg().add(frg1);

		var frg2 = new Fragment();
		frg2.setT("PT_NAME");
		frg2.getDdOrDoOrDp().add(createDS("NM_MAIN",shortName));
		frg2.getDdOrDoOrDp().add(createDE("NM_LANG","LNG_cze"));
		frg2.getDdOrDoOrDp().add(createDE("NM_TYPE","NT_ACRONYM"));
		frgs.getFrg().add(frg2);
		
		var frg3 = new Fragment();
		frg3.setT("PT_IDENT");		
		frg3.getDdOrDoOrDp().add(createDE("IDN_TYPE","ARCHNUM"));
		frg3.getDdOrDoOrDp().add(createDS("IDN_VALUE",code));
		frg3.getDdOrDoOrDp().add(createDB("IDN_VERIFIED",true));
		frgs.getFrg().add(frg3);

		var inss = new Institutions();
		var ins = new Institution();
		ins.setC(code);
		ins.setPaid(uuid);
		ins.setT("DEFAULT");
		inss.getInst().add(ins);
		
		var edx = new ElzaDataExchange();
		edx.setAps(aps);
		edx.setInss(inss);		
		return edx;
	}
	
	private DescriptionItemString createDS(String t, String v) {
		var ds = new DescriptionItemString();
		ds.setT(t);
		ds.setV(v);
		return ds;
	}
	
	private DescriptionItemEnum createDE(String t, String s) {
		DescriptionItemEnum de = new DescriptionItemEnum();
		de.setT(t);
		de.setS(s);
		return de;
	}
	
	private DescriptionItemBit createDB(String t, boolean v) {
		DescriptionItemBit db = new DescriptionItemBit();
		db.setT(t);
		db.setV(v);
		return db;
	}

}
