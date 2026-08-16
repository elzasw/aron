package cz.aron.web.v1;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import cz.aron.api.v1.model.MenuItem;
import cz.aron.api.v1.model.MenuItemCode;
import cz.aron.api.v1.model.UiConfig;
import jakarta.annotation.PostConstruct;

/**
 * Builds the typed {@link UiConfig} of the /api/v1/ui/config endpoint from the
 * deployment's pageTemplate.yaml ({@code webResources.pageTemplate}). Recognized
 * keys: {@code name}, {@code localizations}, and the optional {@code menu} list
 * ({@code code} + optional {@code color}/{@code url} per item). Without a
 * {@code menu} key the deployment gets the default portal menu: FUND, ARCH_DESC,
 * ENTITY, plus HELP when {@code help-url} is configured.
 * <p>
 * A HELP item without an explicit url falls back to {@code help-url}; when
 * neither is set the item is dropped (with a warning). An unknown menu code
 * fails the startup - configuration errors must surface, not hide.
 */
@Component
public class UiConfigLoader {

	private static final Logger log = LoggerFactory.getLogger(UiConfigLoader.class);

	private final String pageTemplateFile;

	private final String helpUrl;

	private UiConfig config;

	public UiConfigLoader(@Value("${webResources.pageTemplate}") String pageTemplateFile,
			@Value("${help-url:}") String helpUrl) {
		this.pageTemplateFile = pageTemplateFile;
		this.helpUrl = helpUrl;
	}

	@PostConstruct
	void load() {
		Map<String, Object> pageTemplate = readPageTemplate();
		String name = pageTemplate.get("name") instanceof String s && !s.isBlank() ? s : "Archiv online";
		List<String> localizations = readLocalizations(pageTemplate);
		List<MenuItem> menuItems = readMenu(pageTemplate);
		config = new UiConfig(name, localizations, menuItems);
	}

	public UiConfig getConfig() {
		return config;
	}

	private Map<String, Object> readPageTemplate() {
		try (InputStream in = Files.newInputStream(Paths.get(pageTemplateFile))) {
			Map<String, Object> parsed = new Yaml().load(in);
			return parsed != null ? parsed : Map.of();
		} catch (IOException e) {
			throw new UncheckedIOException("Fail to read page template " + pageTemplateFile, e);
		}
	}

	private static List<String> readLocalizations(Map<String, Object> pageTemplate) {
		if (pageTemplate.get("localizations") instanceof List<?> values && !values.isEmpty()) {
			return values.stream().map(String::valueOf).toList();
		}
		return List.of("cs_CZ");
	}

	private List<MenuItem> readMenu(Map<String, Object> pageTemplate) {
		if (!(pageTemplate.get("menu") instanceof List<?> entries)) {
			return defaultMenu();
		}
		var items = new ArrayList<MenuItem>();
		for (Object entry : entries) {
			if (!(entry instanceof Map<?, ?> map)) {
				throw new IllegalStateException("pageTemplate menu: each item must be a mapping with a 'code' key");
			}
			MenuItemCode code = parseCode(String.valueOf(map.get("code")));
			var item = new MenuItem(code);
			if (map.get("color") instanceof String color && !color.isBlank()) {
				item.color(color);
			}
			if (map.get("url") instanceof String url && !url.isBlank()) {
				item.url(url);
			}
			addWithHelpFallback(items, item);
		}
		return items;
	}

	private List<MenuItem> defaultMenu() {
		var items = new ArrayList<MenuItem>();
		items.add(new MenuItem(MenuItemCode.FUND));
		items.add(new MenuItem(MenuItemCode.ARCH_DESC));
		items.add(new MenuItem(MenuItemCode.ENTITY));
		addWithHelpFallback(items, new MenuItem(MenuItemCode.HELP));
		return items;
	}

	/** HELP is an external link by nature - without a url it cannot be offered. */
	private void addWithHelpFallback(List<MenuItem> items, MenuItem item) {
		if (item.getCode() == MenuItemCode.HELP && item.getUrl() == null) {
			if (helpUrl == null || helpUrl.isBlank()) {
				log.warn("Menu item HELP has no url and help-url is not configured - dropping the item.");
				return;
			}
			item.url(helpUrl);
		}
		items.add(item);
	}

	private static MenuItemCode parseCode(String value) {
		try {
			return MenuItemCode.fromValue(value);
		} catch (IllegalArgumentException e) {
			throw new IllegalStateException("pageTemplate menu: unknown menu item code '" + value + "'", e);
		}
	}

}
