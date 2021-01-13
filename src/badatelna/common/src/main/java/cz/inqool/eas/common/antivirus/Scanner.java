package cz.inqool.eas.common.antivirus;

import cz.inqool.eas.common.antivirus.clamav.ClamAV;
import cz.inqool.eas.common.antivirus.scan.Scan;
import cz.inqool.eas.common.antivirus.scan.ScanResult;
import cz.inqool.eas.common.antivirus.scan.ScanStore;
import cz.inqool.eas.common.storage.file.File;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import javax.transaction.Transactional;
import java.io.InputStream;

@Slf4j
public class Scanner {
    private ClamAV clamAV;

    private ScanStore store;

    @Transactional
    public ScanResult scanFile(File file, InputStream dataStream) {
        log.debug("Scanning {}.", file);
        ScanResult result;

        try {
            boolean virusFound = clamAV.scanFile(dataStream);
            result = virusFound ? ScanResult.VIRUS_FOUND : ScanResult.OK;
        } catch (Exception e) {
            log.warn("File can not be scanned.", e);
            result = ScanResult.ERROR;
        }

        Scan scan = new Scan();
        scan.setContent(file);
        scan.setResult(result);
        store.create(scan);

        log.debug("\t Scan result: {}", result);

        return result;
    }

    @Autowired
    public void setClamAV(ClamAV clamAV) {
        this.clamAV = clamAV;
    }

    @Autowired
    public void setStore(ScanStore store) {
        this.store = store;
    }
}
