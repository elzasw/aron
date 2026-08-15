import { Configuration, SystemApi } from "./generated";
import { serverContextPath } from "../serverContext";

// Contract paths carry the full /api/v1 prefix, so the client's base path is
// just the deployment prefix (empty at the URL root; the Vite dev server
// proxies /api to the backend).
const configuration = new Configuration({ basePath: serverContextPath });

export const systemApi = new SystemApi(configuration);
