import cz.aron.mapper.ApuSerializer

/*
 * Default Czech citation of a record: the institution, the fund the record
 * belongs to, every identifier its description carries, and the unit's own
 * title. The wording - which identifiers appear, in which order, with which
 * abbreviations - is the archivists' agreement, so a prefix or the order of the
 * segments is not changed without them.
 *
 * The portal supplies the bindings `id` (uuid of the record), `apuRepository`,
 * `objectMapper` and `lang`, and expects JSON carrying either `citation` or a
 * diagnostic `error`. Records with no fund cannot be cited: an archival
 * description always belongs to one, so a missing fund is a data error and is
 * reported rather than silently left out of the citation.
 */

// Parts are Kryo-serialized into apu.data as the REST model
// (cz.aron.api.rest.model.ApuPart). Deserialize once per record and flatten the
// childParts tree, so a lookup by part type finds nested parts as well.
def loadParts(entity) {
    def flat = []
    def collector
    collector = { parts ->
        parts.each { part ->
            flat << part
            if (part.childParts) collector.call(part.childParts)
        }
    }
    collector.call(ApuSerializer.deserialize(entity.data))
    flat
}

/** The first part of the given type, or null. */
def getPart(parts, type) {
    parts.find { it.type == type }
}

/*
 * Every value of one item type, in the order the source system delivered them -
 * a description may carry the same identifier twice, and dropping the second
 * would cite the record by half its signature.
 *
 * Items marked invisible are read as well: invisible means "not displayed on the
 * record's page", and a source system may prepare a value for the citation
 * without showing it. Only item types named here are ever read, so nothing
 * reaches a citation by accident.
 */
def getPartItemValues(part, type) {
    if (part == null || part.items == null) return []
    part.items.findAll { it.type == type && it.value != null && !it.value.trim().isEmpty() }
              .collect { it.value }
}

def getPartItemValue(part, type) {
    def values = getPartItemValues(part, type)
    values.isEmpty() ? null : values[0]
}

def addStr(sj, prefix, text) {
    if (text == null || text.trim().isEmpty()) return
    sj.add((prefix == null ? "" : prefix) + text)
}

def addText(sj, text) {
    addStr(sj, null, text)
}

/** One segment per value, each carrying its own prefix ("sign. A, sign. B"). */
def addItems(sj, prefix, part, type) {
    getPartItemValues(part, type).each { addStr(sj, prefix, it) }
}

/*
 * "<institution>, <fund> (<codePrefix><number>)" - the head both citations
 * start with. The number is left out when the fund carries none, and the fund's
 * own name stands in when its title part is missing; neither is a reason to
 * refuse a citation.
 */
def addFundHead(sj, institution, fund, fundParts, codePrefix) {
    addText(sj, institution.name)
    def fundName = getPartItemValue(getPart(fundParts, "PT~TITLE"), "TITLE") ?: fund.name
    def fundCode = getPartItemValue(getPart(fundParts, "PT~FUND~INFO"), "CISLO~NAD")
    if (fundName) {
        addText(sj, fundCode ? fundName + " (" + codePrefix + fundCode + ")" : fundName)
    }
}

def error(message) {
    objectMapper.writeValueAsString([error: message])
}

def citation(sj) {
    objectMapper.writeValueAsString([citation: sj.toString()])
}

def generateArchDescCitation(entity) {
    def parts = loadParts(entity)
    def archDescPart = getPart(parts, "PT~ARCH~DESC")
    if (archDescPart == null) return error("Archdesc part not exist")
    def fundPart = getPart(parts, "PT~ARCH~DESC~FUND")
    if (fundPart == null) return error("Fund part not exist")
    def fundUuid = getPartItemValue(fundPart, "FUND~REF")
    if (fundUuid == null) return error("Fund uuid not exist")
    def fund = apuRepository.findByUuid(UUID.fromString(fundUuid))
    if (fund == null) return error("Fund not exist")
    def fundParts = loadParts(fund)
    // the record names its institution; a fund that carries only its own stands in
    def institutionUuid = getPartItemValue(fundPart, "FUND~INST~REF")
            ?: getPartItemValue(getPart(fundParts, "PT~FUND~INFO"), "INST~REF")
    if (institutionUuid == null) return error("Institution uuid not exist")
    def institution = apuRepository.findByUuid(UUID.fromString(institutionUuid))
    if (institution == null) return error("Institution not exist " + institutionUuid)

    def sj = new StringJoiner(", ")
    addFundHead(sj, institution, fund, fundParts, "NAD ")
    addItems(sj, "ref. ozn. ", archDescPart, "UNIT~ID")
    addItems(sj, "inv. č. ", archDescPart, "INV~CISLO")
    // poř. č.: the source systems deliver no item type for it yet
    addItems(sj, "čj. ", archDescPart, "OTHERID~CJ")
    addItems(sj, "sp. zn. ", archDescPart, "OTHERID~OLDSIG2")
    addItems(sj, "sign. ", archDescPart, "OTHERID~SIG")
    addItems(sj, "sign. pův. ", archDescPart, "OTHERID~SIG~ORIG")
    addItems(sj, "ukl. j. ", archDescPart, "STORAGE~ID")
    addItems(sj, "ukl. znak ", archDescPart, "OTHERID~STORAGE~ID")
    addItems(sj, "zn. sp. ", archDescPart, "OTHERID~DOCID")
    addItems(sj, "č. vl. ", archDescPart, "OTHERID~FORMAL~DOCID")
    addItems(sj, "přír. č. ", archDescPart, "OTHERID~ADDID")
    addItems(sj, "nakl. č. ", archDescPart, "OTHERID~PICID")
    addItems(sj, "č. neg. ", archDescPart, "OTHERID~NEGID")
    addItems(sj, "číslo produkce CD ", archDescPart, "OTHERID~CDID")
    addItems(sj, "kód ISBN ", archDescPart, "OTHERID~ISBN")
    addItems(sj, "kód ISSN ", archDescPart, "OTHERID~ISSN")
    addItems(sj, "kód ISMN ", archDescPart, "OTHERID~ISMN")
    addItems(sj, "matriční číslo ", archDescPart, "OTHERID~MATRIXID")
    // the unit's title: tree nodes show the description and fall back to the name
    addText(sj, entity.description ?: entity.name)
    citation(sj)
}

def generateFundCitation(entity) {
    def parts = loadParts(entity)
    def institutionUuid = getPartItemValue(getPart(parts, "PT~FUND~INFO"), "INST~REF")
    if (institutionUuid == null) return error("Institution uuid not exist")
    def institution = apuRepository.findByUuid(UUID.fromString(institutionUuid))
    if (institution == null) return error("Institution not exist " + institutionUuid)

    def sj = new StringJoiner(", ")
    // a fund cites its number without the "NAD" prefix - deliberate, as agreed
    addFundHead(sj, institution, entity, parts, "")
    citation(sj)
}

/*
 * Finding aids are not offered by the shipped configuration (citation.yaml lists
 * ARCH_DESC and FUND); the form is kept so that enabling it is one line there
 * once the archivists approve its wording.
 */
def generateFindingAidCitation(entity) {
    def parts = loadParts(entity)
    def faInfoPart = getPart(parts, "PT~FINDINGAID~INFO")
    def institutionUuid = getPartItemValue(faInfoPart, "FUND~INST~REF")
            ?: getPartItemValue(getPart(parts, "PT~ARCH~DESC~FUND"), "FUND~INST~REF")
    if (institutionUuid == null) return error("Institution uuid not exist")
    def institution = apuRepository.findByUuid(UUID.fromString(institutionUuid))
    if (institution == null) return error("Institution not exist " + institutionUuid)

    def sj = new StringJoiner(", ")
    addText(sj, institution.name)
    def faName = getPartItemValue(getPart(parts, "PT~TITLE"), "TITLE") ?: entity.name
    def faCode = getPartItemValue(faInfoPart, "FINDINGAID~ID")
    if (faName) {
        addText(sj, faCode ? faName + " (" + faCode + ")" : faName)
    }
    addItems(sj, null, faInfoPart, "FINDINGAID~TYPE")
    citation(sj)
}

def entity = apuRepository.findByUuid(id)
if (entity == null) {
    return error("Entity not exist")
}

switch (entity.type?.name()) {
case "ARCH_DESC":
    return generateArchDescCitation(entity)
case "FUND":
    return generateFundCitation(entity)
case "FINDING_AID":
    return generateFindingAidCitation(entity)
default:
    return error("Unsupported entity type " + entity.type)
}
