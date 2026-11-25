import type { OrderedRecord } from "../api/generated";
import { serverContextPath } from "../serverContext";
import type { AccessType } from "./basket";

/**
 * Client of the deployment's order service. Not part of the generated client:
 * the service is a separate application with a contract of its own, published
 * beside the portal at `/api/orders` by the deployment's reverse proxy, and
 * the portal's backend takes no part in sending an order. The shapes below
 * are that contract's (`NepripojenaObjednavka`), property names included.
 */
export interface OrderRequest {
  name: string;
  email: string;
  phoneNumber?: string;
  /** The first entry is the subject of the mail the order becomes. */
  subjects: string[];
  /** The page the order was sent from. */
  returnUrl: string;
  /** Code of the archive the order is addressed to. */
  archiv: string;
  /** Code of the reading room, where the items name one. */
  badatelna?: string;
  /** Free text for the archive. */
  popis?: string;
  polozky: { archivalie: OrderedRecord; zpusobZpristupneni: AccessType }[];
}

/** A refused or failed order. */
export class OrderError extends Error {
  /** HTTP status, when the service answered. */
  readonly status?: number;

  /**
   * The service's own explanation for the reader (`zprava`), when it gave one
   * - a rate limit says so here. Its other failures carry none.
   */
  readonly serverMessage?: string;

  constructor(message: string, status?: number, serverMessage?: string) {
    super(message);
    this.name = "OrderError";
    this.status = status;
    this.serverMessage = serverMessage;
  }
}

const ORDERS_URL = `${serverContextPath}/api/orders/orders`;

/** Sends one order; resolves to the order number the service assigned. */
export async function submitOrder(order: OrderRequest): Promise<string> {
  let response: Response;
  try {
    response = await fetch(ORDERS_URL, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(order),
    });
  } catch (error) {
    // no answer at all (offline, the service not published): still an OrderError,
    // so the form has one kind of failure to show
    throw new OrderError(`POST ${ORDERS_URL}: ${error instanceof Error ? error.message : String(error)}`);
  }
  if (!response.ok) {
    let serverMessage: string | undefined;
    try {
      const body = (await response.json()) as { zprava?: unknown } | null;
      if (typeof body?.zprava === "string" && body.zprava.trim() !== "") {
        serverMessage = body.zprava;
      }
    } catch {
      // no JSON body - the status alone is what is known
    }
    throw new OrderError(
      `POST ${ORDERS_URL}: HTTP ${response.status}${serverMessage ? ` - ${serverMessage}` : ""}`,
      response.status,
      serverMessage,
    );
  }
  // the number arrives as a JSON string or as plain text, depending on the service's version
  return (await response.text()).trim().replace(/^"(.*)"$/, "$1");
}
