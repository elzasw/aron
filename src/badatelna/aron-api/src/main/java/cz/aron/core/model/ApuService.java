package cz.aron.core.model;

import cz.inqool.eas.common.domain.DomainService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author Lukas Jane (inQool) 03.11.2020.
 */
@Service
public class ApuService extends DomainService<
        ApuEntity,
        ApuEntity,
        ApuEntity,
        ApuEntity,
        ApuEntity,
        ApuRepository
        > {

    public List<IdLabelDto> mapNames(List<String> ids) {
        return repository.mapNames(ids);
    }
}