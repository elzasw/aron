import cz.aron.mapper.ApuSerializer
import java.time.LocalDateTime

/*
 * What an order says about a record: the fund it belongs to (NAD number and
 * name), its identifiers, where it is stored, and the institution the order is
 * addressed to. The reading room identifies the ordered material by these, so
 * a prefix or the choice of identifiers is not changed without it.
 *
 * The portal supplies the bindings `id` (uuid of the record), `apuRepository`,
 * `objectMapper` and `lang`, and expects JSON carrying either `archivalie` +
 * `institution` (+ optional `badatelna`) or a diagnostic `error`. The property
 * names are those of the order service, which receives the item unchanged.
 * `archivalie.url` is left empty: the portal's UI fills in the address the
 * record is published at.
 *
 * A record without a fund, a fund without a NAD number and an institution
 * without a code cannot be ordered - the order service routes by the code and
 * the reading room finds the material by the number - so each is reported
 * rather than passed on half-described.
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
 * Every value of one item type, in the order the source system delivered them.
 * Items marked invisible are read as well: invisible means "not displayed on
 * the record's page", and the reading room needs the identifier all the same.
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

/** One segment per value, each carrying its own prefix ("sign. A, sign. B"). */
def addItems(sj, prefix, part, type) {
    getPartItemValues(part, type).each { sj.add(prefix + it) }
}

def error(message) {
    objectMapper.writeValueAsString([error: message])
}

/** "1850 - 1900" from the record's dating, or null when it carries none. */
def dating(archDescPart) {
    def json = getPartItemValue(archDescPart, "UNIT~DATE")
    if (json == null) return null
    def date = objectMapper.readValue(json, Map)
    if (date.from == null || date.to == null) return null
    LocalDateTime.parse(date.from).getYear() + " - " + LocalDateTime.parse(date.to).getYear()
}

def generateOrderItem(entity) {
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
    def fundInfoPart = getPart(fundParts, "PT~FUND~INFO")

    // "12" or "12/3": the fund's number and, after the slash, its sub-number
    def nadText = getPartItemValue(fundInfoPart, "CISLO~NAD")
    if (nadText == null) return error("Fund has no NAD number")
    def nad
    try {
        nad = nadText.split('/')*.trim()*.toInteger()
    } catch (NumberFormatException e) {
        return error("Fund NAD number is not a number: " + nadText)
    }

    // the record names its institution; a fund that carries only its own stands in
    def institutionUuid = getPartItemValue(fundPart, "FUND~INST~REF")
            ?: getPartItemValue(fundInfoPart, "INST~REF")
    if (institutionUuid == null) return error("Institution uuid not exist")
    def institution = apuRepository.findByUuid(UUID.fromString(institutionUuid))
    if (institution == null) return error("Institution not exist " + institutionUuid)
    def institutionCode = getPartItemValue(getPart(loadParts(institution), "PT~INST~INFO"), "INST~CODE")
    if (institutionCode == null) return error("Institution has no code " + institutionUuid)

    def archivniIdentifikator = new StringJoiner(", ")
    addItems(archivniIdentifikator, "ref. ozn. ", archDescPart, "UNIT~ID")
    addItems(archivniIdentifikator, "inv. č. ", archDescPart, "INV~CISLO")
    // poř. č.: the source systems deliver no item type for it yet

    def jineOznaceni = new StringJoiner(", ")
    addItems(jineOznaceni, "čj. ", archDescPart, "OTHERID~CJ")
    addItems(jineOznaceni, "sp. zn. ", archDescPart, "OTHERID~OLDSIG2")
    addItems(jineOznaceni, "sign. ", archDescPart, "OTHERID~SIG")
    addItems(jineOznaceni, "sign. pův. ", archDescPart, "OTHERID~SIG~ORIG")
    addItems(jineOznaceni, "ukl. j. ", archDescPart, "STORAGE~ID")
    addItems(jineOznaceni, "ukl. znak ", archDescPart, "OTHERID~STORAGE~ID")
    addItems(jineOznaceni, "zn. sp. ", archDescPart, "OTHERID~DOCID")
    addItems(jineOznaceni, "č. vl. ", archDescPart, "OTHERID~FORMAL~DOCID")
    addItems(jineOznaceni, "přír. č. ", archDescPart, "OTHERID~ADDID")
    addItems(jineOznaceni, "nakl. č. ", archDescPart, "OTHERID~PICID")
    addItems(jineOznaceni, "č. neg. ", archDescPart, "OTHERID~NEGID")
    addItems(jineOznaceni, "číslo produkce CD ", archDescPart, "OTHERID~CDID")
    addItems(jineOznaceni, "kód ISBN ", archDescPart, "OTHERID~ISBN")
    addItems(jineOznaceni, "kód ISSN ", archDescPart, "OTHERID~ISSN")
    addItems(jineOznaceni, "kód ISMN ", archDescPart, "OTHERID~ISMN")
    addItems(jineOznaceni, "matriční číslo ", archDescPart, "OTHERID~MATRIXID")

    // ApuEntity.parent is a lazy association and the script runs outside the
    // persistence session, so the ancestors come from the dedicated recursive
    // query instead - nearest first, hence the reversal to "root / ... / record"
    def names = [entity.name]
    apuRepository.findAncestors(entity.id).each { names.add(it.name()) }

    def data = [
        archivalie: [
            cisloNad: nad[0],
            dilciList: nad.size() > 1 ? nad[1] : null,
            nazevArchivnihoSouboru: getPartItemValue(getPart(fundParts, "PT~TITLE"), "TITLE") ?: fund.name,
            ukladaciJednotka: getPartItemValue(archDescPart, "STORAGE"),
            aipId: null,
            archivniIdentifikator: archivniIdentifikator.toString() ?: null,
            jineOznaceni: jineOznaceni.toString() ?: null,
            popis: entity.name,
            formNazev: null,
            uplnyPopis: names.reverse().join(" / "),
            url: null,
            datace: dating(archDescPart),
        ],
        institution: [
            name: institution.name,
            code: institutionCode,
        ],
    ]

    // An archive with several reading rooms names the one the record is ordered
    // into here; the portal then keeps a separate order per reading room:
    //
    //   if (nad[0] == 1234) data.badatelna = [name: "Badatelna Testov", code: "TESTOV"]

    objectMapper.writeValueAsString(data)
}

def entity = apuRepository.findByUuid(id)
if (entity == null) {
    return error("Entity not exist")
}

switch (entity.type?.name()) {
case "ARCH_DESC":
    return generateOrderItem(entity)
default:
    return error("Unsupported entity type " + entity.type)
}
