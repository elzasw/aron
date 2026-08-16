import "@typespec/http";
import "@typespec/openapi";

using TypeSpec.Http;
using TypeSpec.OpenAPI;

/**
 * ARON portal API (v1) - the contract between the ARON backend and its user
 * interface (and possible external consumers).
 *
 * Paths carry the full external prefix `/api/v1`; the application adds no
 * further prefix. Clients resolve the base URL from the page's
 * `window.serverContextPath` (injected by the server into the SPA shell), so the
 * same UI build works at the URL root and under a subpath.
 *
 * The contract deliberately exposes no search-engine internals - search and
 * facet models are typed DTOs, so the indexing backend can evolve or be swapped
 * without breaking clients.
 */
@service(#{ title: "ARON Portal API" })
@info(#{ version: "1.0" })
namespace AronApi;

/** Technical/system endpoints. */
@tag("system")
@route("/api/v1/system")
namespace System {
	/** Basic information about the running application. */
	@get
	@route("/info")
	op getInfo(): SystemInfo;
}

/** Basic information about the running application. */
model SystemInfo {
	/** Application name. */
	name: string;

	/** Application version (build version; "dev" when built without build info). */
	version: string;
}

/**
 * UI configuration and branding. The portal UI is deployment-configured
 * (webResources files on the server); these endpoints expose that configuration
 * as typed data - clients never parse server config files.
 */
@tag("ui")
@route("/api/v1/ui")
namespace Ui {
	/** UI configuration: portal name, available localizations, top-level menu. */
	@get
	@route("/config")
	op getConfig(): UiConfig;

	/** Portal logo image (deployment-supplied; format varies, see Content-Type). */
	@get
	@route("/logo")
	op getLogo(): {
		@header contentType: "image/svg+xml" | "image/png" | "image/jpeg";

		@body body: bytes;
	};
}

/** UI configuration of the deployment. */
model UiConfig {
	/** Portal name displayed in the header and the browser tab. */
	name: string;

	/**
	 * Available localizations (IETF-like codes, e.g. `cs_CZ`). The UI offers a
	 * language switcher only when more than one is listed.
	 */
	localizations: string[];

	/** Top-level menu: which sections this deployment enables, in display order. */
	menuItems: MenuItem[];
}

/** One item of the top-level menu. */
model MenuItem {
	/** Section code; the UI maps it to its route and localized label. */
	code: MenuItemCode;

	/** Accent color of the section (CSS color); the UI supplies a default palette. */
	color?: string;

	/** External URL - the item is a plain link instead of an in-app section (typically HELP or NEWS). */
	url?: string;
}

/** Portal sections available for the top-level menu. */
enum MenuItemCode {
	INSTITUTION,
	FUND,
	FINDING_AID,
	ARCH_DESC,
	ENTITY,
	ORIGINATOR,
	NEWS,
	HELP,
}
