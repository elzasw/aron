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
