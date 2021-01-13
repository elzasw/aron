package cz.aron.core.model;

import cz.aron.core.model.types.TypesHolder;
import cz.aron.core.model.types.dto.MetadataType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;
import java.util.Collection;

/**
 * @author Lukas Jane (inQool) 03.11.2020.
 */
@RestController
@RequestMapping("/metadataType")
public class MetadataTypeApi {
    @Inject private TypesHolder typesHolder;

    @GetMapping
    public Collection<MetadataType> list() {
        return typesHolder.getAllMetadataTypes();
    }
}
