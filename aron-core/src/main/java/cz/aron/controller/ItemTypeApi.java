package cz.aron.controller;

import java.util.Collection;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cz.aron.domain.types.TypesHolder;
import cz.aron.domain.types.dto.ItemType;

@RestController
@RequestMapping("/apuPartItemType")
public class ItemTypeApi {
	
    private final TypesHolder typesHolder;        

    public ItemTypeApi(TypesHolder typesHolder) {
		this.typesHolder = typesHolder;
	}

	@GetMapping
    public Collection<ItemType> list() {
        return typesHolder.getAllItemTypes();
    }
}
