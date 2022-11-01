package cz.aron.transfagent.elza;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javax.xml.bind.JAXBException;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.eclipse.jetty.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.apux._2020.Apu;
import cz.aron.apux._2020.ApuType;
import cz.aron.apux._2020.ItemDateRange;
import cz.aron.apux._2020.ItemEnum;
import cz.aron.apux._2020.Part;
import cz.aron.transfagent.elza.convertor.EdxApRefConvertor;
import cz.aron.transfagent.elza.convertor.EdxApRefWithRole;
import cz.aron.transfagent.elza.convertor.EdxEnumConvertor;
import cz.aron.transfagent.elza.convertor.EdxIntConvertor;
import cz.aron.transfagent.elza.convertor.EdxItemConvertor;
import cz.aron.transfagent.elza.convertor.EdxItemCovertContext;
import cz.aron.transfagent.elza.convertor.EdxLinkConvertor;
import cz.aron.transfagent.elza.convertor.EdxNullConvertor;
import cz.aron.transfagent.elza.convertor.EdxStringConvertor;
import cz.aron.transfagent.elza.convertor.EdxStringSpecConvertor;
import cz.aron.transfagent.elza.convertor.EdxStructureConvertor;
import cz.aron.transfagent.elza.convertor.EdxTimeLenghtConvertor;
import cz.aron.transfagent.elza.convertor.EdxUnitDateConvertor;
import cz.aron.transfagent.elza.convertor.EdxUnitDateConvertorEnum;
import cz.aron.transfagent.elza.datace.ItemDateRangeAppender;
import cz.aron.transfagent.peva.ArchiveFundId;
import cz.aron.transfagent.service.DaoFileStore2Service;
import cz.aron.transfagent.service.DaoFileStore2Service.ArchiveFundDao;
import cz.aron.transfagent.service.DaoFileStoreService;
import cz.aron.transfagent.service.LevelEnrichmentService;
import cz.aron.transfagent.transformation.ArchEntityInfo;
import cz.aron.transfagent.transformation.ContextDataProvider;
import cz.aron.transfagent.transformation.CoreTypes;
import cz.aron.transfagent.transformation.PropertiesDataProvider;
import cz.tacr.elza.schema.v2.DescriptionItem;
import cz.tacr.elza.schema.v2.DescriptionItemInteger;
import cz.tacr.elza.schema.v2.DescriptionItemString;
import cz.tacr.elza.schema.v2.DescriptionItemUndefined;
import cz.tacr.elza.schema.v2.DigitalArchivalObject;
import cz.tacr.elza.schema.v2.DigitalArchivalObjects;
import cz.tacr.elza.schema.v2.FundInfo;
import cz.tacr.elza.schema.v2.Level;
import cz.tacr.elza.schema.v2.Levels;
import cz.tacr.elza.schema.v2.Section;
import cz.tacr.elza.schema.v2.Sections;

public class ImportArchDesc implements EdxItemCovertContext {

    private final static Logger log = LoggerFactory.getLogger(ImportArchDesc.class); 

	ElzaXmlReader elzaXmlReader;

	ApuSourceBuilder apusBuilder = new ApuSourceBuilder();

	ContextDataProvider dataProvider;

	Map<Apu, Apu> apuParentMap = new HashMap<>();
	
	Map<String, Apu> apuMap = new HashMap<>();

	final Map<UUID, ArchEntityInfo> apRefs = new HashMap<>();
	final Map<UUID, ArchEntityInfo> apLevelRefs = new HashMap<>();
	
	/**
	 * Mapa (source,daos)
	 */
	private final Map<String,Set<ArchDescDaoRef>> daoRefs = new HashMap<>();
	

	private UUID instApuUuid;
	private UUID fundApuUuid;

	private Part activePart;

	private String institutionCode;

	private String institutionName;
	
	private Integer fundId;

	private Apu activeApu;

    static final String ignoredTypes[] = {"ZP2015_ARRANGEMENT_TYPE", "ZP2015_LEVEL_TYPE",
            "ZP2015_SERIAL_NUMBER", "ZP2015_NAD", "ZP2015_ZNACKA_FONDU",
            "ZP2015_LEVEL_TYPE", "ZP2015_ARRANGER",
            "ZP2015_UNIT_DATE_BULK","ZP2015_FOLDER_TYPE",
             "ZP2015_ITEM_ORDER",
            "ZP2015_UNIT_COUNT_ITEM",
            "ZP2015_INTERNAL_NOTE",
            "ZP2015_AIP_ID",
            "ZP2015_RESTRICTION_ACCESS_SHARED",
            "ZP2015_RESTRICTION_ACCESS_NAME",
            "ZP2015_RESTRICTED_ACCESS_REASON",
            "ZP2015_RESTRICTED_ACCESS_TYPE",
            // geo souradnice - neumime prevest
            ElzaTypes.ZP2015_POSITION,
            "ZP2015_DAO_ID",
            "ZP2015_CONNECTED_RECORD",
            "ZP2015_INVALID_RECORD",
            "ZP2015_ITEM_LINK",
            "ZP2015_TITLE_PUBLIC",                         
    // TODO: k zapracovani:
            "ZP2015_AMOUNT"
    };

    /**
     * Map of item convertors
     */
    Map<String, EdxItemConvertor> stringTypeMap;

    private final ApTypeService apTypeService;
    
    private final DaoFileStoreService daoFileStoreService;
    
    private final DaoFileStore2Service daoFileStoreService2;
    
    private final LevelEnrichmentService levelEnrichmentService;

	public ImportArchDesc(ApTypeService apTypeService, DaoFileStoreService daoFileStoreService,
			DaoFileStore2Service daoFileStoreService2, LevelEnrichmentService levelEnrichmentService) {
		this.apTypeService = apTypeService;
		this.daoFileStoreService = daoFileStoreService;
		this.daoFileStoreService2 = daoFileStoreService2;
		this.levelEnrichmentService = levelEnrichmentService;
	}

    public Set<UUID> getApRefs() {
        return apRefs.keySet();
    }

    public String getInstitutionCode() {
        return institutionCode;
    }
    
    public Integer getFundId() {
    	return fundId;
    }
    
    public Map<String,Set<ArchDescDaoRef>> getDaoRefs() {
    	return daoRefs;
    }

	public static void main(String[] args) {
		Path inputFile = Path.of(args[0]);
		ImportArchDesc iad = new ImportArchDesc(new ApTypeService(), null, null, null);
		try {
			ApuSourceBuilder apusrcBuilder = iad.importArchDesc(inputFile, args[1]);
			Path outputPath = Path.of(args[2]);
			try(OutputStream fos = Files.newOutputStream(outputPath)) {
				apusrcBuilder.build(fos);
			}
	        System.out.println(args[2] + " is done.");
		} catch(Exception e) {
			System.err.println("Failed to process input file: " + inputFile);
			e.printStackTrace();
		}
	}

	public ApuSourceBuilder importArchDesc(Path inputFile, String propFile) throws IOException, JAXBException {
		Validate.isTrue(propFile!=null&&propFile.length()>0);

		PropertiesDataProvider pdp = new PropertiesDataProvider();
		Path propPath = Paths.get(propFile);
		pdp.load(propPath);
		return importArchDesc(inputFile, pdp);
	}

    public ApuSourceBuilder importArchDesc(Path inputFile, final ContextDataProvider cdp) throws IOException, JAXBException {
        this.dataProvider = cdp;

        try(InputStream is = Files.newInputStream(inputFile)) {
            elzaXmlReader = ElzaXmlReader.read(is);
            return importArchDesc();
        }
    }

    private ApuSourceBuilder importArchDesc() {
    	initConvertor();
        Sections sections = elzaXmlReader.getEdx().getFs();
        if(sections==null||sections.getS().size()==0) {
            throw new RuntimeException("Missing section data");
        }
        if(sections.getS().size()>1) {
            throw new RuntimeException("Exports with one section are supported");
        }
        Section sect = sections.getS().get(0);
        if(sect.getLvls()==null) {
            throw new RuntimeException("Missing levels.");
        }

        // read fund info
        FundInfo fi = sect.getFi();
        institutionCode = fi.getIc();
        var institutionInfo = dataProvider.getInstitutionApu(institutionCode);
        instApuUuid = institutionInfo.getUuid();
        institutionName = institutionInfo.getName();
        Validate.notNull(instApuUuid, "Missing institution, code: %s", institutionCode);

		if (fi.getC() != null) {
			fundApuUuid = dataProvider.getFundApu(institutionCode, fi.getC());
		} else if (fi.getNum() != null) {
			fundId = fi.getNum().intValue();
			fundApuUuid = dataProvider.getFundApu(institutionCode,
					ArchiveFundId.createJaFaId(institutionCode, fi.getNum().toString(), null));
		}
		Validate.notNull(fundApuUuid, "Missing fund, code: %s, institution: %s,%s", fi.getC(), fi.getNum(),
				institutionCode);

        Levels lvls = sect.getLvls();
        for(Level lvl: lvls.getLvl()) {
            processLevel(sect, lvl);
        }

        // add dates to siblings
        for (Entry<String, Apu> item : apuMap.entrySet()) {
            var apu = item.getValue();
            var ranges = ApuSourceBuilder.getItemDateRanges(apu, CoreTypes.PT_ARCH_DESC, CoreTypes.UNIT_DATE);
            if (ranges.isEmpty()) {
                copyDateRangeFromParent(apu, CoreTypes.PT_ARCH_DESC, CoreTypes.UNIT_DATE);
            }
        }
        return apusBuilder;
    }

    private void processLevel(Section sect, Level lvl) {
        log.debug("Importing level, id: {}, uuid: {}", lvl.getId(), lvl.getUuid());
        apLevelRefs.clear();
        
        String name = getName(sect, lvl);
        String desc = getDesc(sect, lvl);
        Apu apu = apusBuilder.createApu(name, ApuType.ARCH_DESC);
        apu.setDesc(desc);
        apu.setUuid(lvl.getUuid());

        // set parent
        Apu parentApu = null;
        if(lvl.getPid()!=null) {
            parentApu = apuMap.get(lvl.getPid());
            if(parentApu==null) {
                throw new RuntimeException("Missing parent for level: "+lvl.getPid());
            }
            apu.setPrnt(parentApu.getUuid());
            apuParentMap.put(apu, parentApu);

            // add name from parent if empty
            if(StringUtil.isEmpty(apu.getName())) {
                apu.setName(parentApu.getName());
            }
            if(StringUtil.isEmpty(apu.getDesc())) {
                apu.setDesc(parentApu.getDesc());
            }
        }
        apuMap.put(lvl.getId(), apu);

        activateArchDescPart(apu);                

        // add items
        for(DescriptionItem item: lvl.getDdOrDoOrDp()) {
            addItem(apu, item);
        }
        
        processLevelEnrichment(lvl);

		// daos
		boolean daoExist = false;
		DigitalArchivalObjects daos = lvl.getDaos();
		if (daos != null && daos.getDao().size() > 0) {
			for (DigitalArchivalObject dao : daos.getDao()) {
				var daoHandle = dao.getDoid();
				addDaoRef("dspace", null, daoHandle);
				UUID daoUuid = dataProvider.getDao(daoHandle);
				if (daoUuid != null) {
					ApuSourceBuilder.addDao(activeApu, daoUuid);
					daoExist = true;
				}
			}
		}
		
		daoExist = daoExist || processExternalDaos(lvl);
        
        if(daoExist) {
            ApuSourceBuilder.addEnum(activePart, "DIGITAL", "Ano", false);
        }

        // copy values from parent
        if(parentApu!=null) {
            List<ItemEnum> itemEnums = ApuSourceBuilder.getItemEnums(parentApu, ApuType.ARCH_DESC, CoreTypes.UNIT_TYPE);
            if(CollectionUtils.isNotEmpty(itemEnums)) {
            	ApuSourceBuilder.copyEnums(activePart, itemEnums);
            }
        }

        deactivatePart(apu);

        // add static values
        activatePart(apu, CoreTypes.PT_ARCH_DESC_FUND);
        addEntityClasses(activePart);
        apusBuilder.addApuRef(activePart, "FUND_REF", fundApuUuid);
        apusBuilder.addApuRef(activePart, "FUND_INST_REF", instApuUuid);
        deactivatePart(apu);

        // add date to parent(s)
        var itemDateRanges = ApuSourceBuilder.getItemDateRanges(apu, CoreTypes.PT_ARCH_DESC, CoreTypes.UNIT_DATE);
        for(ItemDateRange item : itemDateRanges) {
            Apu apuParent = parentApu;
            while(apuParent != null) {
                ItemDateRangeAppender dateRangeAppender = new ItemDateRangeAppender(item);
                dateRangeAppender.appendTo(apuParent);
                apuParent = apuParentMap.get(apuParent);
            }
        }
    }
    
	private boolean processExternalDaos(Level lvl) {
		boolean daoExist = false;
		if (daoFileStoreService != null) {
			if (daoFileStoreService.getDaoDir(lvl.getUuid()) != null) {
				addDaoRef(daoFileStoreService.getName(), UUID.fromString(lvl.getUuid()), lvl.getUuid());
				ApuSourceBuilder.addDao(activeApu, UUID.fromString(lvl.getUuid()));
				daoExist = true;
			}
		}
		if (daoFileStoreService2 != null) {
			for (DescriptionItem di : lvl.getDdOrDoOrDp()) {
				if (di instanceof DescriptionItemString) {
					DescriptionItemString ds = (DescriptionItemString) di;
					if ("ZP2015_DAO_ID".equals(ds.getT())) {
						String id = ds.getV();
						try {
							ArchiveFundDao afd = new ArchiveFundDao(institutionCode, fundId, id);
							List<Path> paths = daoFileStoreService2.getDaos(afd);
							if (CollectionUtils.isNotEmpty(paths)) {
								// podivam se jestli uz dao neexistuje abych negeneroval nove uuid a neposlal ho
								// opakovane
								// TODO doresit situaci kdy by neexistujici dao bylo odkazovano z vice urovni (nakesovat nove vytvarena uuid)
								UUID uuid = dataProvider.getDao(afd.toString());
								if (uuid == null) {
									uuid = UUID.randomUUID();
								}
								addDaoRef(daoFileStoreService2.getName(), uuid, afd.toString());
								ApuSourceBuilder.addDao(activeApu, uuid);
								daoExist = true;
							}
						} catch (IOException e) {
							throw new UncheckedIOException(e);
						}
					}
				}
			}
		}
		return daoExist;
	}
	
    private void processLevelEnrichment(Level lvl) {
        if (levelEnrichmentService != null) {
            String url = levelEnrichmentService.getUrlForLevel(lvl.getUuid());
            if (url != null) {
                ApuSourceBuilder.addLink(activePart, "URL", url, levelEnrichmentService.getLabel());
            }
        }
    }
	
	private void addDaoRef(String source, UUID uuid, String handle) {
		Set<ArchDescDaoRef> sourceDaoRefs = daoRefs.get(source);
		if (sourceDaoRefs == null) {
			sourceDaoRefs = new HashSet<>();
			daoRefs.put(source, sourceDaoRefs);
		}
		sourceDaoRefs.add(new ArchDescDaoRef(handle, uuid));
	}

    private void initConvertor() {
	    stringTypeMap = new HashMap<>();
        // TITLE is used as default APU name, it is not converted as separate ABSTRACT
        //stringTypeMap.put("ZP2015_TITLE",new EdxStringConvertor("ABSTRACT"));     
        stringTypeMap.put("ZP2015_TITLE",new EdxNullConvertor());
        stringTypeMap.put("ZP2015_UNIT_TYPE", new EdxEnumConvertor(CoreTypes.UNIT_TYPE, ElzaTypes.unitTypeMap));
        stringTypeMap.put(ElzaTypes.ZP2015_EXTRA_UNITS, new EdxEnumConvertor(CoreTypes.UNIT_TYPE, ElzaTypes.extraUnitTypeMap));
        stringTypeMap.put(ElzaTypes.ZP2015_UNIT_SUBTYPE, new EdxEnumConvertor(CoreTypes.UNIT_TYPE, ElzaTypes.subtypeMap));
        stringTypeMap.put(ElzaTypes.ZP2015_RECORD_TYPE, new EdxEnumConvertor(CoreTypes.RECORD_TYPE, ElzaTypes.recordTypeMap));      
        stringTypeMap.put("ZP2015_UNIT_ID",new EdxStringConvertor("UNIT_ID").addIndexedItem(CoreTypes.UNIT_ID_INDEX));
        stringTypeMap.put(ElzaTypes.ZP2015_STORAGE_ID, new EdxStructureConvertor(CoreTypes.STORAGE_ID, elzaXmlReader.getSoMap()));
        stringTypeMap.put("ZP2015_UNIT_HIST",new EdxStringConvertor("HISTORY"));
        stringTypeMap.put("ZP2015_UNIT_ARR",new EdxStringConvertor("UNIT_ARR"));
        stringTypeMap.put("ZP2015_UNIT_CONTENT",new EdxStringConvertor("UNIT_CONTENT"));
        stringTypeMap.put("ZP2015_UNIT_SOURCE",new EdxStringConvertor("UNIT_SOURCE"));
        stringTypeMap.put("ZP2015_FUTURE_UNITS",new EdxStringConvertor("FUTURE_UNITS"));
        stringTypeMap.put("ZP2015_UNIT_ACCESS",new EdxStringConvertor("UNIT_ACCESS"));
        stringTypeMap.put("ZP2015_UNIT_CURRENT_STATUS",new EdxStringConvertor("UNIT_CURRENT_STATUS"));
        stringTypeMap.put("ZP2015_ARRANGE_RULES",new EdxStringConvertor("ARRANGE_RULES"));
        stringTypeMap.put(ElzaTypes.ZP2015_ORIGINATOR,new EdxApRefConvertor(CoreTypes.ORIGINATOR_REF,this.dataProvider));
        stringTypeMap.put("ZP2015_AP_REF",new EdxApRefConvertor("AP_REF",this.dataProvider));
        stringTypeMap.put("ZP2015_ITEM_TITLE_REF",new EdxApRefConvertor("ITEM_TITLE",this.dataProvider));
        stringTypeMap.put("ZP2015_FORMAL_TITLE",new EdxStringConvertor("FORMAL_TITLE"));
        stringTypeMap.put("ZP2015_SCALE",new EdxStringConvertor("SCALE"));
        stringTypeMap.put(ElzaTypes.ZP2015_LANGUAGE, new EdxEnumConvertor(CoreTypes.LANGUAGE, ElzaTypes.languageTypeMap));
        stringTypeMap.put(ElzaTypes.ZP2015_ORIENTATION, new EdxStringConvertor(CoreTypes.ORIENTATION));
        stringTypeMap.put(ElzaTypes.ZP2015_ITEM_MAT, new EdxStringConvertor(CoreTypes.ITEM_MAT));
        stringTypeMap.put(ElzaTypes.ZP2015_PART, new EdxStringConvertor(CoreTypes.PART));
        stringTypeMap.put("ZP2015_STORAGE_COND",new EdxStringConvertor("STORAGE_COND"));
        stringTypeMap.put("ZP2015_RELATED_UNITS",new EdxStringConvertor("RELATED_UNITS"));
        stringTypeMap.put(ElzaTypes.ZP2015_UNIT_DATE,new EdxUnitDateConvertor(CoreTypes.UNIT_DATE));
        stringTypeMap.put(ElzaTypes.ZP2015_DATE_OTHER,new EdxUnitDateConvertorEnum(ElzaTypes.dateOtherMap, ElzaTypes.dateOtherMapIndex));
        stringTypeMap.put("ZP2015_SIZE",new EdxStringConvertor("SIZE"));
        stringTypeMap.put("ZP2015_ITEM_MAT",new EdxStringConvertor("ITEM_MAT"));
        stringTypeMap.put("ZP2015_INV_CISLO",new EdxStringConvertor("INV_CISLO").addIndexedItem("OTHER_ID_PROC_INDEX"));
        stringTypeMap.put(ElzaTypes.ZP2015_OTHER_ID, new EdxStringSpecConvertor(ElzaTypes.otherIdMap).addIndexedItem(ElzaTypes.otherIdIndexMap));
        stringTypeMap.put("ZP2015_EDITION",new EdxStringConvertor("EDITION"));
        stringTypeMap.put("ZP2015_UNIT_DATE_TEXT",new EdxStringConvertor("UNIT_DATE_TEXT"));
        stringTypeMap.put("ZP2015_EXERQUE",new EdxStringConvertor("EXERQUE"));
        stringTypeMap.put("ZP2015_PAINTING_CHAR",new EdxStringConvertor("PAINTING_CHAR"));
        stringTypeMap.put(ElzaTypes.ZP2015_CORROBORATION, new EdxStringConvertor(CoreTypes.CORROBORATION));
        stringTypeMap.put(ElzaTypes.ZP2015_IMPRINT_COUNT, new EdxStringConvertor(CoreTypes.IMPRINT_COUNT));
        stringTypeMap.put(ElzaTypes.ZP2015_IMPRINT_ORDER, new EdxStringConvertor(CoreTypes.IMPRINT_ORDER));
        stringTypeMap.put(ElzaTypes.ZP2015_LEGEND, new EdxStringConvertor(CoreTypes.LEGEND));
        stringTypeMap.put(ElzaTypes.ZP2015_MOVIE_LENGTH, new EdxTimeLenghtConvertor(CoreTypes.MOVIE_LENGTH));
        stringTypeMap.put(ElzaTypes.ZP2015_RECORD_LENGTH, new EdxTimeLenghtConvertor(CoreTypes.RECORD_LENGTH));
        stringTypeMap.put(ElzaTypes.ZP2015_WRITING, new EdxStringConvertor(CoreTypes.WRITING));
        stringTypeMap.put(ElzaTypes.ZP2015_ITEM_LINK, new EdxLinkConvertor(CoreTypes.ARCH_DESC_REF, CoreTypes.SOURCE_LINK));

        stringTypeMap.put("ZP2015_EXISTING_COPY",new EdxStringConvertor("EXISTING_COPY"));
        stringTypeMap.put("ZP2015_ARRANGEMENT_INFO",new EdxStringConvertor("ARRANGEMENT_INFO"));
        stringTypeMap.put(ElzaTypes.ZP2015_ENTITY_ROLE, new EdxApRefWithRole(CoreTypes.PT_ENTITY_ROLE, this.dataProvider, ElzaTypes.roleSpecMap));
        stringTypeMap.put("ZP2015_UNIT_COUNT",new EdxIntConvertor("UNIT_COUNT"));
        stringTypeMap.put("ZP2015_NOTE",new EdxStringConvertor(CoreTypes.NOTE));
        stringTypeMap.put("ZP2015_DESCRIPTION_DATE",new EdxStringConvertor("DESCRIPTION_DATE"));	    
    }

    private void addItem(Apu apu, DescriptionItem item) {
        if(item instanceof DescriptionItemUndefined ) {
            return;
        }
        // check ignored items
        for(String ignoredType: ignoredTypes) {
            if(ignoredType.equals(item.getT())) {
                return;
            }
        }

        EdxItemConvertor convertor = stringTypeMap.get(item.getT());
        if(convertor!=null) {
            convertor.convert(this, item);
            return;
        }
        throw new RuntimeException("Unsupported item type: " + item.getT());
    }

    private void addEntityClasses(Part part) {
        if(apLevelRefs.isEmpty()) {
            return;
        }
        
        Set<String> rootClasses = new HashSet<>();

        Map<String, String> rtMap = new HashMap<>();
        rtMap.put("PARTY_GROUP", "rejstřík korporací");
        rtMap.put("PERSON", "rejstřík osob, bytostí");
        rtMap.put("DYNASTY", "rejstřík rodů, rodin");
        rtMap.put("EVENT", "rejstřík událostí");
        rtMap.put("ARTWORK", "rejstřík děl");
        rtMap.put("GEO", "rejstřík zeměpisný");
        rtMap.put("TERM", "rejstřík obecných pojmů");

        Map<String, String> rctMap = new HashMap<>();
        rctMap.put("PARTY_GROUP", "REG_GROUP_PARTY_REF");
        rctMap.put("PERSON", "REG_PERSON_REF");
        rctMap.put("DYNASTY", "REG_DYNASTY_REF");
        rctMap.put("EVENT", "REG_EVENT_REF");
        rctMap.put("ARTWORK", "REG_ARTWORK_REF");
        rctMap.put("GEO", "REG_GEO_REF");
        rctMap.put("TERM", "REG_TERM_REF");

        for(var apr: apLevelRefs.values()) {
            var parentCode = apTypeService.getParentCode(apr.getEntityClass());
            Validate.notNull(parentCode, "Failed to get parent code for class: %s", apr.getEntityClass());

            String itemType = rctMap.get(parentCode);
            Validate.notNull(itemType, "Failed to get itemType for parentCode: %s", parentCode);
            // Add APref
            apusBuilder.addApuRef(part, itemType, apr.getUuid(), false);

            rootClasses.add(parentCode);
        }

        // add rootClasses
        for (String rootCls : rootClasses) {
            var name = rtMap.get(rootCls);
            Validate.notNull(name, "Missing mapping for root class: %s", rootCls);
            ApuSourceBuilder.addEnum(part, "REGISTRY_TYPE", name, false);
        }
    }

	private void copyDateRangeFromParent(Apu apu, String partType, String itemType) {
		var apuParent = apuParentMap.get(apu);
		while (apuParent != null) {
			var rangesParent = ApuSourceBuilder.getItemDateRanges(apuParent, partType, CoreTypes.UNIT_DATE);
			if (rangesParent.size() > 0) {
				Part part = ApuSourceBuilder.getFirstPart(apu, partType);
				if (part == null) {
					part = ApuSourceBuilder.addPart(apu, partType);
				}
				ApuSourceBuilder.copyDateRanges(part, rangesParent);
				return;
			} else {
				apuParent = apuParentMap.get(apuParent);
			}
		}
	}

    private void deactivatePart(Apu apu) {
        if(activePart!=null) {
            // delete empty part
            if(activePart.getItms()==null&&activePart.getValue()==null) {
               apu.getPrts().getPart().remove(activePart); 
            }
            activePart = null;
        }
    }

    /**
	 * Set PT_ARCH_DESC as active part
	 */
	void activateArchDescPart(Apu apu) {
	    activatePart(apu, CoreTypes.PT_ARCH_DESC);
	}

    void activatePart(Apu apu, String partName) {
        activeApu = apu;
        activePart = null;
        if(apu.getPrts()!=null) {
            for(Part p : apu.getPrts().getPart()) {
                if(p.getType().equals(partName)) {
                    activePart = p;
                    break;
                }
            }
        }
        if(activePart==null) {
            activePart = ApuSourceBuilder.addPart(apu, partName);
        }
    }

    public static Map<String, Integer> otherIdPriorityMap = new HashMap<>();
    static {
        otherIdPriorityMap.put("ZP2015_OTHERID_SIG_ORIG", 99);
        otherIdPriorityMap.put("ZP2015_OTHERID_SIG", 100);
        otherIdPriorityMap.put("ZP2015_OTHERID_STORAGE_ID", 96);
        otherIdPriorityMap.put("ZP2015_OTHERID_CJ", 95);
        otherIdPriorityMap.put("ZP2015_OTHERID_DOCID", 94);
        otherIdPriorityMap.put("ZP2015_OTHERID_FORMAL_DOCID", 92);
        otherIdPriorityMap.put("ZP2015_OTHERID_ADDID", 98);
        otherIdPriorityMap.put("ZP2015_OTHERID_OLDSIG", 83);
        otherIdPriorityMap.put("ZP2015_OTHERID_OLDSIG2", 93);
        otherIdPriorityMap.put("ZP2015_OTHERID_OLDID", 97);
        otherIdPriorityMap.put("ZP2015_OTHERID_INVALID_UNITID", 81);
        otherIdPriorityMap.put("ZP2015_OTHERID_INVALID_REFNO", 82);
        otherIdPriorityMap.put("ZP2015_OTHERID_PRINTID", 84);
        otherIdPriorityMap.put("ZP2015_OTHERID_PICID", 91);
        otherIdPriorityMap.put("ZP2015_OTHERID_NEGID", 90);
        otherIdPriorityMap.put("ZP2015_OTHERID_CDID", 89);
        otherIdPriorityMap.put("ZP2015_OTHERID_ISBN", 88);
        otherIdPriorityMap.put("ZP2015_OTHERID_ISSN", 87);
        otherIdPriorityMap.put("ZP2015_OTHERID_ISMN", 86);
        otherIdPriorityMap.put("ZP2015_OTHERID_MATRIXID", 85);
    }
    

    private String getName(Section sect, Level lvl) {
        String parentId = lvl.getPid();

        StringBuilder sb = new StringBuilder();
        sb.append(institutionName).append(": ");
        sb.append(sect.getFi().getN());

        if(parentId == null) {
            // Koren AS je bez dalsich identifikatoru
            return sb.toString();
        }

        String refOzn = null;
        String invCislo = null;
        String poradoveCislo = null;
        String otherIdent = null;
        String otherIdentType = null;
        int otherIdentPriority = -1;
        for(DescriptionItem item : lvl.getDdOrDoOrDp()) {
            if(item.getT().equals("ZP2015_UNIT_ID")&&(item instanceof DescriptionItemString)) {
                DescriptionItemString title = (DescriptionItemString)item;
                refOzn = title.getV();
            } else 
            if(item.getT().equals("ZP2015_INV_CISLO")&&(item instanceof DescriptionItemString)) {
                DescriptionItemString title = (DescriptionItemString)item;
                invCislo = title.getV();
            } else 
            if(item.getT().equals("ZP2015_SERIAL_NUMBER")&&(item instanceof DescriptionItemInteger)) {
                DescriptionItemInteger serNum = (DescriptionItemInteger)item;
                poradoveCislo = serNum.getV().toString();
            } else 
            if(item.getT().equals(ElzaTypes.ZP2015_OTHER_ID)&&(item instanceof DescriptionItemString)) {
                DescriptionItemString otherId = (DescriptionItemString) item;
                if(otherIdPriorityMap.get(otherId.getS())>otherIdentPriority) {
                    otherIdentPriority = otherIdPriorityMap.get(otherId.getS());
                    otherIdentType = ElzaTypes.otherIdNameMap.get(otherId.getS());
                    otherIdent = otherId.getV();
                }
            }
        }

        if(StringUtils.isNotEmpty(refOzn)) {
            sb.append(", ").append(refOzn);
        } else if(StringUtils.isNotEmpty(invCislo)) {
            sb.append(", inv.č.: ").append(invCislo);
        } else if(StringUtils.isNotEmpty(poradoveCislo)) {
            sb.append(", poř.č.: ").append(poradoveCislo);
        } else if(StringUtils.isNotEmpty(otherIdent)) {
            if(otherIdentType!=null) {
                sb.append(", ").append(otherIdentType);                
            }
            sb.append(": ").append(otherIdent);
        } else {
            // TODO: stanoveni cesty pomoci hierarchie
            // var parentApu = this.apuMap.get(lvl.getPid());
            // while(parentApu!=null) {
            //    apuPosMap.get()
            // }
            
            // Variantne lze pridat UUID jako ident
            //sb.append(lvl.getUuid().toString());
        }

        return sb.toString();
    }

	private String getDesc(Section sect, Level lvl) {
	    if(lvl.getPid()==null) {
	        // for root no description
	        return null;
	    }
	    
		StringBuilder sb = new StringBuilder();

		for(DescriptionItem item : lvl.getDdOrDoOrDp()) {
			if(item.getT().equals("ZP2015_TITLE")) {
				DescriptionItemString title = (DescriptionItemString)item;
				sb.append(title.getV());
			}
		}

		if(sb.length()==0) {
		    return null;
		}
		return sb.toString();
	}

	@Override
	public ApuSourceBuilder getApusBuilder() {
		return apusBuilder;
	}

	@Override
	public Part getActivePart() {
		return activePart;
	}

	@Override
	public ElzaXmlReader getElzaXmlReader() {
		return elzaXmlReader;
	}

	@Override
	public Apu getActiveApu() {
		return activeApu;
	}

    @Override
    public void addArchEntityRef(ArchEntityInfo aei) {
        apRefs.put(aei.getUuid(), aei);
        apLevelRefs.put(aei.getUuid(), aei);
    }
    
    public static class ArchDescDaoRef {
    	
    	private final String handle;
    	
    	private final UUID uuid;
    	
    	public ArchDescDaoRef(String handle, UUID uuid) {
    		this.handle = handle;
    		this.uuid = uuid;
    	}

		public String getHandle() {
			return handle;
		}

		public UUID getUuid() {
			return uuid;
		}

		@Override
		public int hashCode() {
			return Objects.hash(handle, uuid);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ArchDescDaoRef other = (ArchDescDaoRef) obj;
			return Objects.equals(handle, other.handle) && Objects.equals(uuid, other.uuid);
		}    	
    			
    }
    
}
