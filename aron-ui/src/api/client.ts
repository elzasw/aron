import { ApuApi, Configuration, SearchApi, SystemApi, UiApi } from "./generated";
import { serverContextPath } from "../serverContext";

// Contract paths carry the full /api/v1 prefix, so the client's base path is
// just the deployment prefix (empty at the URL root; the Vite dev server
// proxies /api to the backend).
const configuration = new Configuration({ basePath: serverContextPath });

export const systemApi = new SystemApi(configuration);
export const uiApi = new UiApi(configuration);
export const searchApi = new SearchApi(configuration);
export const apuApi = new ApuApi(configuration);

/** URL of the deployment-supplied logo (plain <img> source, no client needed). */
export const logoUrl = `${serverContextPath}/api/v1/ui/logo`;
