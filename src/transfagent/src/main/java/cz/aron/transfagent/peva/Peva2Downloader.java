package cz.aron.transfagent.peva;

import java.time.OffsetDateTime;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.lang3.StringUtils;
import org.springframework.transaction.support.TransactionTemplate;

import cz.aron.peva2.wsdl.PEvA;
import cz.aron.transfagent.config.ConfigPeva2;
import cz.aron.transfagent.domain.Property;
import cz.aron.transfagent.repository.PropertyRepository;
import cz.aron.transfagent.service.StorageService;
import cz.aron.transfagent.service.importfromdir.ImportContext;

public abstract class Peva2Downloader {

	private final String updateAfterPropertyName;

	private final String searchAfterPropertyName;

	protected final PEvA peva2;

	protected final PropertyRepository propertyRepository;

	protected final ConfigPeva2 config;

	protected final TransactionTemplate tt;

	protected final StorageService storageService;

	public Peva2Downloader(String agendaName, PEvA peva2, PropertyRepository propertyRepository, ConfigPeva2 config,
			TransactionTemplate tt, StorageService storageService) {
		this.peva2 = peva2;
		this.propertyRepository = propertyRepository;
		this.config = config;
		this.tt = tt;
		this.storageService = storageService;
		updateAfterPropertyName = "PEVA2_" + agendaName + "_UPDATE_AFTER";
		searchAfterPropertyName = "PEVA2_" + agendaName + "_SEARCH_AFTER";
	}

	public void importDataInternal(ImportContext ic, Peva2CodeListProvider codeListProvider) {

		var updateAfterProp = propertyRepository.findByName(updateAfterPropertyName);
		var searchAfterProp = propertyRepository.findByName(searchAfterPropertyName);

		final OffsetDateTime nowTime = OffsetDateTime.now();

		XMLGregorianCalendar od = null;
		if (updateAfterProp != null && StringUtils.isNotBlank(updateAfterProp.getValue())) {
			try {
				od = DatatypeFactory.newInstance().newXMLGregorianCalendar(updateAfterProp.getValue());
				// chyba v PEvA2, pokud je zadana casova zona, tak je updatedAfter ignorovano
				od.setTimezone(DatatypeConstants.FIELD_UNDEFINED);
			} catch (DatatypeConfigurationException e) {
				throw new IllegalStateException();
			}
		}

		String searchAfter = searchAfterProp != null && StringUtils.isNotBlank(searchAfterProp.getValue())
				? searchAfterProp.getValue()
				: null;

		if (synchronizeAgenda(od, 0L, searchAfter, codeListProvider) > 0) {
			// TODO reportovat realnou hodnotu
			ic.addProcessed();
		}

		tt.execute(t -> {
			var sa = propertyRepository.findByName(searchAfterPropertyName);
			if (sa != null) {
				sa.setValue("");
				propertyRepository.save(sa);
			}
			var ua = propertyRepository.findByName(updateAfterPropertyName);
			if (ua != null) {
				ua.setValue(nowTime.toString());
			} else {
				ua = new Property();
				ua.setName(updateAfterPropertyName);
				ua.setValue(nowTime.toString());
			}
			propertyRepository.save(ua);
			return null;
		});

	}

	protected void storeSearchAfter(String searchAfter) {
		String searchAfterFinal = searchAfter;
		tt.execute(t -> {
			var sa = propertyRepository.findByName(searchAfterPropertyName);
			if (sa != null) {
				sa.setValue(searchAfterFinal != null ? searchAfterFinal : "");
			} else {
				sa = new Property();
				sa.setName(searchAfterPropertyName);
				sa.setValue(searchAfterFinal);
			}
			propertyRepository.save(sa);
			return null;
		});
	}

	protected abstract int synchronizeAgenda(XMLGregorianCalendar updateAfter, long eventId, String searchAfterInitial,
			Peva2CodeListProvider codeListProvider);

}
