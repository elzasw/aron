package cz.aron.core.integration;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import cz.aron.apux._2020.Apu;

public class ApuSourceBatchReader implements Closeable {

    static {
        try {
            jaxbContext = JAXBContext.newInstance(Apu.class);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    private static final QName APU = new QName("http://www.aron.cz/apux/2020", "apu");
    private static final QName APUSRC = new QName("http://www.aron.cz/apux/2020", "apusrc");
    private static final QName APUSRC_UUID = new QName("uuid");

    private static XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();

    private static JAXBContext jaxbContext;

    private final InputStream is;

    private final XMLEventReader reader;

    private final String uuid;

    /**
     * 
     * Open xml file. Read apusrc.uuid attribute
     * 
     * @param path
     *            path to apusrc.xml
     * @throws Exception
     *             where fail to open or not find apusrc.uuid
     */
    public ApuSourceBatchReader(Path path) throws Exception {
        InputStream tmpIs = null;
        try {
            tmpIs = new BufferedInputStream(Files.newInputStream(path));
            reader = xmlInputFactory.createXMLEventReader(tmpIs);
            String tmpUuid = null;
            while (reader.hasNext()) {
                XMLEvent nextEvent = reader.peek();
                if (nextEvent.getEventType() == XMLEvent.START_ELEMENT) {
                    StartElement element = nextEvent.asStartElement();
                    if (APUSRC.equals(element.getName())) {
                        Attribute attr = element.getAttributeByName(APUSRC_UUID);
                        if (attr == null) {
                            throw new IOException("Missing apusrc.uuid attribute");
                        }
                        tmpUuid = attr.getValue();
                        if (tmpUuid == null) {
                            throw new IOException("Missing apusrc.uuid attribute value");
                        }
                        break;
                    }
                }
                reader.nextEvent();
            }
            if (tmpUuid == null) {
                throw new IOException("Missing apusrc.uuid attribute");
            }
            is = tmpIs;
            uuid = tmpUuid;
        } catch (Exception e) {
            if (tmpIs != null) {
                try {
                    tmpIs.close();
                } catch (Exception e2) {
                    // ignore
                }
            }
            throw e;
        }
    }

    /**
     * Read apus in batch.
     * 
     * @param consumer
     *            to consume batch of apus
     * @param batchSize
     *            size of batch
     * @throws Exception
     */
    public void process(Consumer<List<Apu>> consumer, int batchSize) throws Exception {
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        var batch = new ArrayList<Apu>(batchSize);
        while (reader.hasNext()) {
            XMLEvent nextEvent = reader.peek();
            if (nextEvent.getEventType() == XMLEvent.START_ELEMENT) {
                StartElement element = nextEvent.asStartElement();
                if (APU.equals(element.getName())) {
                    var jaxbApu = jaxbUnmarshaller.unmarshal(reader, Apu.class);
                    batch.add(jaxbApu.getValue());
                    if (batch.size() == batchSize) {
                        consumer.accept(batch);
                        batch.clear();
                    }
                    continue;
                }
            }
            reader.nextEvent();
        }
        if (batch.size() > 0) {
            consumer.accept(batch);
            batch.clear();
        }
    }

    @Override
    public void close() throws IOException {
        if (is != null) {
            is.close();
        }
    }

    public String getUuid() {
        return uuid;
    }

}
