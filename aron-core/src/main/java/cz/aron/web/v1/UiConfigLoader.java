package cz.aron.web.v1;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import cz.aron.api.v1.model.FooterLink;
import cz.aron.api.v1.model.FooterLinkCode;
import cz.aron.commons.LocalizationFile;
import cz.aron.api.v1.model.MenuItem;
import cz.aron.api.v1.model.MenuItemCode;
import cz.aron.api.v1.model.UiConfig;
import cz.aron.domain.types.LocalizedText;
import cz.aron.domain.types.dto.LocalizedItem;
import jakarta.annotation.PostConstruct;

/**
 * Builds the typed {@link UiConfig} of the /api/v1/ui/config endpoint from the
 * deployment's pageTemplate.yaml ({@code webResources.pageTemplate}). Recognized
 * keys: {@code name}, {@code localizations}, the optional {@code menu} list
 * ({@code code} + optional {@code color}/{@code url} per item) and the optional
 * {@code footer.links} list. Without a {@code menu} key the deployment gets the
 * default portal menu: FUND, ARCH_DESC, ENTITY, plus HELP when {@code help-url}
 * is configured.
 * <p>
 * A HELP item without an explicit url falls back to {@code help-url}; when
 * neither is set the item is dropped (with a warning). An unknown menu code
 * fails the startup - configuration errors must surface, not hide.
 * <p>
 * Footer links carry the deployment's own published pages - the accessibility
 * statement a public-sector body must publish (see doc/accessibility.md),
 * privacy information, contacts. Each entry needs a {@code url} plus either a
 * well-known {@code code} (the UI labels those itself) or a {@code label}; a
 * label may be one string or a per-language mapping:
 *
 * <pre>
 * footer:
 *   links:
 *     - code: ACCESSIBILITY
 *       url: https://archiv.example/pristupnost
 *     - url: https://archiv.example/kontakt
 *       label:
 *         cs: Kontakt
 *         en: Contact
 * </pre>
 *
 * Configuration is parsed once at startup; {@link #getConfig(Locale)} renders it
 * for one reader's language (labels only - the rest is language-independent).
 */
@Component
public class UiConfigLoader {

	private static final Logger log = LoggerFactory.getLogger(UiConfigLoader.class);

	private final String pageTemplateFile;

	private final String helpUrl;

	private String name;

	private List<LocalizedItem> nameTranslations;

	private List<String> localizations;

	private List<MenuItem> menuItems;

	private List<FooterLinkConfig> footerLinks;

	/** Parsed footer link: the localized labels are picked per request. */
	private record FooterLinkConfig(FooterLinkCode code, String label, List<LocalizedItem> translations, String url) {
	}

	public UiConfigLoader(@Value("${webResources.pageTemplate}") String pageTemplateFile,
			@Value("${help-url:}") String helpUrl) {
		this.pageTemplateFile = pageTemplateFile;
		this.helpUrl = helpUrl;
	}

	@PostConstruct
	void load() {
		Map<String, Object> pageTemplate = readPageTemplate();
		name = pageTemplate.get("name") instanceof String s && !s.isBlank() ? s : "Archiv online";
		nameTranslations = readNameTranslations();
		localizations = readLocalizations(pageTemplate);
		menuItems = readMenu(pageTemplate);
		footerLinks = readFooterLinks(pageTemplate);
	}

	/** Configured presentation languages - needed before a locale can be resolved. */
	public List<String> getLocalizations() {
		return localizations;
	}

	/** Typed configuration for one reader's language. */
	public UiConfig getConfig(Locale locale) {
		var links = footerLinks.stream()
				.map(link -> {
					var footerLink = new FooterLink(link.url());
					if (link.code() != null) {
						footerLink.code(link.code());
					}
					String label = LocalizedText.pick(link.translations(), link.label(), locale);
					if (label != null && !label.isBlank()) {
						footerLink.label(label);
					}
					return footerLink;
				})
				.toList();
		return new UiConfig(LocalizedText.pick(nameTranslations, name, locale), localizations, menuItems, links);
	}

	/**
	 * Portal name per language, from pageTemplate_localization.yaml. Written as
	 * {@code name: {en: ...}} - a single field, so no code keys are involved.
	 */
	private List<LocalizedItem> readNameTranslations() {
		var translations = new ArrayList<LocalizedItem>();
		if (LocalizationFile.besides(pageTemplateFile).get("name") instanceof Map<?, ?> byLanguage) {
			byLanguage.forEach((language, text) -> {
				if (text instanceof String value && !value.isBlank()) {
					translations.add(new LocalizedItem(String.valueOf(language), value));
				}
			});
		}
		return translations;
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

	private static List<FooterLinkConfig> readFooterLinks(Map<String, Object> pageTemplate) {
		if (!(pageTemplate.get("footer") instanceof Map<?, ?> footer)
				|| !(footer.get("links") instanceof List<?> entries)) {
			return List.of();
		}
		var links = new ArrayList<FooterLinkConfig>();
		for (Object entry : entries) {
			if (!(entry instanceof Map<?, ?> map)) {
				throw new IllegalStateException("pageTemplate footer.links: each item must be a mapping");
			}
			String url = map.get("url") instanceof String value && !value.isBlank() ? value : null;
			if (url == null) {
				throw new IllegalStateException("pageTemplate footer.links: an item has no 'url'");
			}
			FooterLinkCode code = map.get("code") != null ? parseFooterCode(String.valueOf(map.get("code"))) : null;
			String label = map.get("label") instanceof String value && !value.isBlank() ? value : null;
			List<LocalizedItem> translations = readLabelTranslations(map.get("label"));
			if (code == null && label == null && translations.isEmpty()) {
				throw new IllegalStateException(
						"pageTemplate footer.links: item '" + url + "' needs a known 'code' or a 'label'");
			}
			links.add(new FooterLinkConfig(code, label, translations, url));
		}
		return links;
	}

	/** A label is either one string (source language) or a language → text mapping. */
	private static List<LocalizedItem> readLabelTranslations(Object label) {
		if (!(label instanceof Map<?, ?> byLanguage)) {
			return List.of();
		}
		var translations = new ArrayList<LocalizedItem>();
		byLanguage.forEach((language, text) -> {
			if (text != null && !String.valueOf(text).isBlank()) {
				translations.add(new LocalizedItem(String.valueOf(language), String.valueOf(text)));
			}
		});
		return translations;
	}

	private static MenuItemCode parseCode(String value) {
		try {
			return MenuItemCode.fromValue(value);
		} catch (IllegalArgumentException e) {
			throw new IllegalStateException("pageTemplate menu: unknown menu item code '" + value + "'", e);
		}
	}

	private static FooterLinkCode parseFooterCode(String value) {
		try {
			return FooterLinkCode.fromValue(value);
		} catch (IllegalArgumentException e) {
			throw new IllegalStateException("pageTemplate footer.links: unknown link code '" + value + "'", e);
		}
	}

}
