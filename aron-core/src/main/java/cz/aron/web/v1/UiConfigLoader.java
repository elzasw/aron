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
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import cz.aron.api.v1.model.HomePage;
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
 * ({@code code} + optional {@code color}/{@code url} per item), the optional
 * {@code primaryColor} pair, the optional {@code footer.links} list and the
 * optional {@code homepage} section ({@link HomePageConfig}). Without a
 * {@code menu} key the deployment gets the default portal menu: FUND,
 * ARCH_DESC, ENTITY, plus HELP when {@code help-url} is configured.
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
 * The optional primary colour is the deployment's own instead of the portal
 * default, as the original portal's {@code primaryColor} was:
 *
 * <pre>
 * primaryColor:
 *   dark: hsl(272, 14%, 21%)
 *   main: hsl(272, 14%, 31%)
 * </pre>
 *
 * It reaches the UI through the SPA shell rather than this endpoint
 * ({@code cz.aron.web.IndexController}), so the very first paint already has
 * the deployment's colour. Both shades are required together and must be plain
 * CSS colours - a value that is not one fails the startup, because it would
 * otherwise end up in the page's stylesheet unnoticed.
 * <p>
 * Configuration is parsed once at startup; {@link #getConfig(Locale)} renders it
 * for one reader's language (labels only - the rest is language-independent).
 */
@Component
public class UiConfigLoader {

	private static final Logger log = LoggerFactory.getLogger(UiConfigLoader.class);

	private final String pageTemplateFile;

	private final String helpUrl;

	private final FacetScope facetScope;

	private final ResultImages images;

	private String name;

	private List<LocalizedItem> nameTranslations;

	private List<String> localizations;

	private List<MenuItem> menuItems;

	private List<FooterLinkConfig> footerLinks;

	private PrimaryColor primaryColor;

	private HomePageConfig homePage;

	/**
	 * Primary colour of one deployment: the header's shade and the lighter one of
	 * active menu items and record-icon tiles (the original portal's primary.dark
	 * and primary.main).
	 */
	public record PrimaryColor(String dark, String main) {
	}

	/**
	 * Colour syntax accepted in configuration: a CSS name, a hex code or a
	 * functional notation. A whitelist rather than escaping - the value is written
	 * into the page's stylesheet, where a stray semicolon or closing brace would
	 * mean something.
	 */
	private static final Pattern COLOR = Pattern
			.compile("[a-zA-Z]{3,20}|#[0-9a-fA-F]{3,8}|(?:rgb|rgba|hsl|hsla)\\([a-zA-Z0-9.%,/ +-]{3,60}\\)");

	public UiConfigLoader(@Value("${webResources.pageTemplate}") String pageTemplateFile,
			@Value("${help-url:}") String helpUrl, FacetScope facetScope, ResultImages images) {
		this.pageTemplateFile = pageTemplateFile;
		this.helpUrl = helpUrl;
		this.facetScope = facetScope;
		this.images = images;
	}

	@PostConstruct
	void load() {
		Map<String, Object> pageTemplate = readPageTemplate();
		name = pageTemplate.get("name") instanceof String s && !s.isBlank() ? s : "Archives online";
		nameTranslations = readNameTranslations();
		localizations = readLocalizations(pageTemplate);
		menuItems = readMenu(pageTemplate);
		primaryColor = readPrimaryColor(pageTemplate);
		// an image name is checked against the directory here, at startup: a link
		// or tile pointing at a missing file would otherwise show a broken picture
		Predicate<String> imageIsServable = name -> images.resolve(name) != null;
		footerLinks = pageTemplate.get("footer") instanceof Map<?, ?> footer
				? FooterLinkConfig.parse(footer.get("links"), "footer.links", imageIsServable)
				: List.of();
		homePage = HomePageConfig.parse(pageTemplate.get("homepage"), facetScope, imageIsServable);
	}

	/** Configured presentation languages - needed before a locale can be resolved. */
	public List<String> getLocalizations() {
		return localizations;
	}

	/**
	 * The deployment's own primary colour, or {@code null} when it keeps the
	 * portal default. Served in the SPA shell, not in {@link #getConfig(Locale)}.
	 */
	public PrimaryColor getPrimaryColor() {
		return primaryColor;
	}

	/** Typed configuration for one reader's language. */
	public UiConfig getConfig(Locale locale) {
		var links = footerLinks.stream().map(link -> link.render(locale, images::url)).toList();
		var config = new UiConfig(LocalizedText.pick(nameTranslations, name, locale), localizations, menuItems, links);
		if (homePage != null) {
			// image URLs carry this request's context path, so they are built here
			// rather than at startup (see ResultImages)
			config.setHomePage(homePage.render(locale, images::url));
		}
		return config;
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

	/**
	 * Presentation languages of the deployment; the first is its default. English
	 * is the source language, so an unconfigured deployment gets English - a
	 * Czech one declares {@code localizations: [cs_CZ, en]} and is Czech by
	 * default for every reader who has not asked for something else.
	 */
	private static List<String> readLocalizations(Map<String, Object> pageTemplate) {
		if (pageTemplate.get("localizations") instanceof List<?> values && !values.isEmpty()) {
			return values.stream().map(String::valueOf).toList();
		}
		return List.of("en");
	}

	/**
	 * Both shades are read together: a deployment that sets one and not the other
	 * would get its own header above portal-coloured tiles, which is a mistake
	 * rather than a configuration.
	 */
	private static PrimaryColor readPrimaryColor(Map<String, Object> pageTemplate) {
		if (!(pageTemplate.get("primaryColor") instanceof Map<?, ?> colors)) {
			return null;
		}
		return new PrimaryColor(readColor(colors, "dark"), readColor(colors, "main"));
	}

	private static String readColor(Map<?, ?> colors, String shade) {
		if (!(colors.get(shade) instanceof String value) || value.isBlank()) {
			throw new IllegalStateException("pageTemplate primaryColor: '" + shade + "' is missing");
		}
		String color = value.trim();
		if (!COLOR.matcher(color).matches()) {
			throw new IllegalStateException(
					"pageTemplate primaryColor." + shade + ": '" + color + "' is not a CSS color");
		}
		return color;
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
