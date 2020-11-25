package cz.aron.core.model;

import cz.aron.core.model.types.TypesHolder;
import cz.aron.core.model.types.dto.ItemType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;
import java.util.Collection;

/**
 * @author Lukas Jane (inQool) 03.11.2020.
 */
@RestController
@RequestMapping("/apuPartItemType")
public class ItemTypeApi {
    @Inject private TypesHolder typesHolder;

    @GetMapping
    public Collection<ItemType> list() {
        return typesHolder.getAllItemTypes();
    }
}
