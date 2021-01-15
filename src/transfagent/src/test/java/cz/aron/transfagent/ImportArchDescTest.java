package cz.aron.transfagent;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.apux._2020.Apu;
import cz.aron.apux._2020.ItemDateRange;
import cz.aron.transfagent.elza.ImportArchDesc;
import cz.aron.transfagent.elza.datace.LocalDateTimeRange;
import cz.aron.transfagent.transformation.CoreTypes;

public class ImportArchDescTest {

    private final static String ARCHDESC_DIR = "src/test/resources/files/archdesc/archdesc-testdate";

    private final String ARCHDESC_FILE = "archdesc-CR2303.xml";

    private final String PROPERTIES_FILE = "archdesc.properties"; 

    private final static String APUSRC_XML = "apusrc.xml";

    @Test
    public void testImportArchDesc() throws IOException, JAXBException {
        Path inputFile = Path.of(ARCHDESC_DIR, ARCHDESC_FILE);

        ImportArchDesc iad = new ImportArchDesc();
        ApuSourceBuilder builder = iad.importArchDesc(inputFile, ARCHDESC_DIR + "/" + PROPERTIES_FILE);

        Path outputPath = Path.of(ARCHDESC_DIR, APUSRC_XML);
        try(OutputStream fos = Files.newOutputStream(outputPath)) {
            builder.build(fos);
        }

        // testing root: Test datace
        Apu apu = getApuByName(builder, "Test datace");
        List<ItemDateRange> ranges = builder.getItemDateRanges(apu, CoreTypes.PT_ARCH_DESC);
        assertTrue(ranges.size() == 1);
        
        LocalDateTimeRange idra = new LocalDateTimeRange(ranges.get(0));
        assertTrue(idra.getFrom().getYear() == 1810);
        assertTrue(idra.getTo().getYear() == 1860);

        // testing s1
        apu = getApuByName(builder, "s1");
        ranges = builder.getItemDateRanges(apu, CoreTypes.PT_ARCH_DESC);
        assertTrue(ranges.size() == 2);

        idra = new LocalDateTimeRange(ranges.get(0));
        assertTrue(idra.getFrom().getYear() == 1820);
        assertTrue(idra.getTo().getYear() == 1830);

        idra = new LocalDateTimeRange(ranges.get(1));
        assertTrue(idra.getFrom().getYear() == 1840);
        assertTrue(idra.getTo().getYear() == 1855);

        // testing s2
        apu = getApuByName(builder, "s2");
        ranges = builder.getItemDateRanges(apu, CoreTypes.PT_ARCH_DESC);
        assertTrue(ranges.size() == 1);

        idra = new LocalDateTimeRange(ranges.get(0));
        assertTrue(idra.getFrom().getYear() == 1810);
        assertTrue(idra.getTo().getYear() == 1860);
    }

    @AfterAll
    public static void deleteApusrcXml() throws IOException {
        Files.delete(Path.of(ARCHDESC_DIR, APUSRC_XML));
    }

    private Apu getApuByName(ApuSourceBuilder builder, String apuName) {
        for(Apu apu : builder.getApusrc().getApus().getApu()) {
            if(apu.getName().equals(apuName)) {
                return apu;
            }
        }
        return null;
    }
}
