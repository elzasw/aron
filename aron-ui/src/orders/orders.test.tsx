import { screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { Route, Routes } from "react-router-dom";
import { afterAll, beforeAll, beforeEach, describe, expect, it, vi } from "vitest";
import { ApuType, type ApuDetail, type OrderItem, type UiConfig } from "../api/generated";
import AppHeader from "../layout/AppHeader";
import ApuPage from "../pages/ApuPage";
import { expectNoA11yViolations } from "../test/a11y";
import { giveElementsABox } from "../test/layout";
import { renderWithProviders } from "../test/render";
import { setViewport } from "../test/viewport";
import { type BasketItem, groupsOf, isBasketItem } from "./basket";
import { OrderError, type OrderRequest } from "./orderApi";

const RECORD: ApuDetail = {
  uuid: "kronika",
  name: "Kronika obce Testov",
  apuType: ApuType.ArchDesc,
  childCount: 0,
  treePath: [],
  parts: [],
  attachments: [],
  digitalObjects: [],
  orderable: true,
};

/** The same record as the server describes one it does not let be ordered. */
const NOT_ORDERABLE: ApuDetail = { ...RECORD, uuid: "zlomek", name: "Zlomek spisu", orderable: undefined };

const ORDER_ITEM: OrderItem = {
  archivalie: {
    cisloNad: 4711,
    nazevArchivnihoSouboru: "Farní úřad Testov",
    archivniIdentifikator: "inv. č. 3",
    popis: "Kronika obce Testov",
    uplnyPopis: "Farní úřad Testov / Kronika obce Testov",
  },
  institution: { name: "SOkA Testov", code: "226101010" },
};

function basketItem(uuid: string, popis: string, overrides: Partial<BasketItem> = {}): BasketItem {
  return {
    ...ORDER_ITEM,
    archivalie: { ...ORDER_ITEM.archivalie, popis, url: `http://localhost/apu/${uuid}` },
    uuid,
    accessType: "NAHLIZENI",
    ...overrides,
  };
}

let config: UiConfig;

const records: Record<string, ApuDetail> = { kronika: RECORD, zlomek: NOT_ORDERABLE };

/** Replaced per test: the server describes the record, or refuses to. */
let orderItem: () => Promise<OrderItem>;

/** Replaced per test: the order service accepts the order, or refuses it. */
let submit: (order: OrderRequest) => Promise<string>;

vi.mock("../api/client", () => ({
  logoUrl: "/api/v1/ui/logo",
  apuApi: {
    apuGetDetail: vi.fn((request: { uuid: string }) => Promise.resolve(records[request.uuid])),
    apuGetTreeNodes: vi.fn(() => Promise.resolve([])),
    apuGetOrderItem: vi.fn(() => orderItem()),
  },
  uiApi: { uiGetConfig: vi.fn(() => Promise.resolve(config)) },
}));

vi.mock("./orderApi", async (original) => ({
  ...(await original<typeof import("./orderApi")>()),
  submitOrder: vi.fn((order: OrderRequest) => submit(order)),
}));

const STORAGE_KEY = "aron.orderItems";

function stored(): BasketItem[] {
  return JSON.parse(window.localStorage.getItem(STORAGE_KEY) ?? "[]") as BasketItem[];
}

function renderPortal(path = "/apu/kronika") {
  return renderWithProviders(
    <>
      <AppHeader />
      <main>
        <Routes>
          <Route path="apu/:uuid" element={<ApuPage />} />
        </Routes>
      </main>
    </>,
    { initialEntries: [path] },
  );
}

describe("orders", () => {
  // the basket and the form are dialogs opened from the header: without a box
  // nothing in them is focusable and they stay hidden from role queries
  let takeBoxAway: () => void;
  beforeAll(() => {
    takeBoxAway = giveElementsABox();
  });
  afterAll(() => takeBoxAway());

  beforeEach(() => {
    window.localStorage.clear();
    setViewport(1280, 900);
    config = {
      name: "Test portal",
      localizations: ["en"],
      menuItems: [],
      footerLinks: [],
      citations: [],
      orders: true,
    };
    orderItem = () => Promise.resolve(ORDER_ITEM);
    submit = vi.fn(() => Promise.resolve("2026/123"));
  });

  it("offers the order only on a record the server marked orderable", async () => {
    const orderable = renderPortal("/apu/kronika");
    expect(
      await screen.findByRole("button", { name: `Order ${RECORD.name}` }),
    ).toHaveTextContent("Order");
    orderable.unmount();

    renderPortal("/apu/zlomek");
    await screen.findByRole("heading", { level: 1, name: NOT_ORDERABLE.name });
    expect(screen.queryByRole("button", { name: /^Order / })).not.toBeInTheDocument();
  });

  it("offers nothing where the deployment takes no orders, a filled basket included", async () => {
    config = { ...config, orders: undefined };
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify([basketItem("kronika", "Kronika")]));

    renderPortal();
    await screen.findByRole("heading", { level: 1, name: RECORD.name });

    expect(screen.queryByRole("button", { name: /^Order / })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /Order basket/ })).not.toBeInTheDocument();
  });

  it("puts the record into the basket with its public address and takes it out again", async () => {
    renderPortal();
    // an empty basket is not shown at all
    await screen.findByRole("heading", { level: 1, name: RECORD.name });
    expect(screen.queryByRole("button", { name: /Order basket/ })).not.toBeInTheDocument();

    await userEvent.click(await screen.findByRole("button", { name: `Order ${RECORD.name}` }));

    const remove = await screen.findByRole("button", {
      name: `Remove from order: ${RECORD.name}`,
    });
    expect(remove).toHaveTextContent("Remove from order");
    expect(screen.getByRole("button", { name: "Order basket: 1 record" })).toHaveTextContent("1");
    // the change is announced, not only shown as a new button label
    expect(screen.getByText("Added to the order basket.")).toHaveAttribute("role", "status");
    // the basket outlives the page, and the item carries the record's address
    expect(stored()).toEqual([
      {
        ...ORDER_ITEM,
        archivalie: { ...ORDER_ITEM.archivalie, url: `${window.location.origin}/apu/kronika` },
        uuid: "kronika",
        accessType: "NAHLIZENI",
      },
    ]);

    await userEvent.click(remove);
    expect(await screen.findByRole("button", { name: `Order ${RECORD.name}` })).toBeVisible();
    expect(screen.queryByRole("button", { name: /Order basket/ })).not.toBeInTheDocument();
    expect(window.localStorage.getItem(STORAGE_KEY)).toBeNull();
  });

  it("says so when the server cannot describe the record, and puts nothing into the basket", async () => {
    // a 422: a record without a fund, say - nothing half-described is stored
    orderItem = () => Promise.reject(new Error("Unprocessable Entity"));
    renderPortal();

    await userEvent.click(await screen.findByRole("button", { name: `Order ${RECORD.name}` }));

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "This record could not be added to the order.",
    );
    expect(screen.queryByRole("button", { name: /Order basket/ })).not.toBeInTheDocument();
    expect(stored()).toEqual([]);
  });

  it("shows the basket per archive and reading room and lets the reader change it", async () => {
    window.localStorage.setItem(
      STORAGE_KEY,
      JSON.stringify([
        basketItem("a", "Kronika"),
        basketItem("b", "Matrika", { badatelna: { name: "Badatelna Sever", code: "S" } }),
        basketItem("c", "Urbář"),
      ]),
    );
    const { baseElement } = renderPortal();

    await userEvent.click(await screen.findByRole("button", { name: "Order basket: 3 records" }));
    const basket = await screen.findByRole("dialog", { name: "Order basket" });

    // one block per order: the archive, and the archive's reading room apart
    const archive = within(basket).getByRole("region", { name: "SOkA Testov" });
    expect(within(archive).getAllByRole("listitem")).toHaveLength(2);
    expect(within(archive).getByRole("link", { name: "Kronika" })).toHaveAttribute("href", "/apu/a");
    const readingRoom = within(basket).getByRole("region", {
      name: "SOkA Testov — Badatelna Sever",
    });
    expect(within(readingRoom).getAllByRole("listitem")).toHaveLength(1);
    await expectNoA11yViolations(baseElement as HTMLElement, ["region"]);

    // every item offers all three forms of access, consultation by default
    const access = within(archive).getByRole("combobox", { name: "Form of access to Kronika" });
    expect(access).toHaveValue("NAHLIZENI");
    expect(within(access).getAllByRole("option")).toHaveLength(3);
    await userEvent.selectOptions(access, "Reproduction and replica");
    expect(stored().find((item) => item.uuid === "a")?.accessType).toBe("REPLIKA");

    await userEvent.click(
      within(archive).getByRole("button", { name: "Remove from order: Urbář" }),
    );
    expect(stored().map((item) => item.uuid)).toEqual(["a", "b"]);
    expect(within(basket).getByRole("status")).toHaveTextContent(
      "Urbář was removed from the basket.",
    );

    // a whole group goes only after the reader confirmed it
    await userEvent.click(
      within(readingRoom).getByRole("button", {
        name: "Remove all records of SOkA Testov — Badatelna Sever",
      }),
    );
    const question = await screen.findByRole("alertdialog", { name: "Confirmation" });
    expect(question).toHaveTextContent(
      "Remove all records of SOkA Testov — Badatelna Sever from the basket?",
    );
    await userEvent.click(within(question).getByRole("button", { name: "Cancel" }));
    expect(stored()).toHaveLength(2);
    // the focus returns to the control the reader used, inside the basket
    await waitFor(() =>
      expect(
        within(readingRoom).getByRole("button", {
          name: "Remove all records of SOkA Testov — Badatelna Sever",
        }),
      ).toHaveFocus(),
    );

    await userEvent.click(
      within(readingRoom).getByRole("button", {
        name: "Remove all records of SOkA Testov — Badatelna Sever",
      }),
    );
    await userEvent.click(
      within(await screen.findByRole("alertdialog")).getByRole("button", { name: "Confirm" }),
    );
    await waitFor(() => expect(stored().map((item) => item.uuid)).toEqual(["a"]));
  });

  it("sends one group's order to the order service and takes it out of the basket", async () => {
    window.localStorage.setItem(
      STORAGE_KEY,
      JSON.stringify([
        basketItem("a", "Kronika", { accessType: "REPLIKA" }),
        basketItem("b", "Matrika", {
          institution: { name: "SOkA Jinde", code: "999" },
        }),
      ]),
    );
    const { baseElement } = renderPortal();

    await userEvent.click(await screen.findByRole("button", { name: "Order basket: 2 records" }));
    await userEvent.click(await screen.findByRole("button", { name: "Order from SOkA Testov" }));
    const form = await screen.findByRole("dialog", { name: "Reservation of archival material" });
    await expectNoA11yViolations(baseElement as HTMLElement, ["region"]);

    // the reader checks what they order before sending it
    expect(within(form).getByRole("textbox", { name: "Basket contents – SOkA Testov" })).toHaveValue(
      "--- 1 ---\nNAD number: 4711 | inv. č. 3\nFarní úřad Testov / Kronika obce Testov\n" +
        "Direct link: http://localhost/apu/a",
    );

    // mistakes are said once the reader tried to send, and the first one takes the focus
    expect(within(form).queryByText("This field is required.")).not.toBeInTheDocument();
    await userEvent.click(within(form).getByRole("button", { name: "Send" }));
    const name = within(form).getByRole("textbox", { name: /Your name and surname/ });
    expect(name).toBeInvalid();
    expect(name).toHaveFocus();
    expect(within(form).getAllByText("This field is required.")).toHaveLength(2);
    expect(submit).not.toHaveBeenCalled();

    await userEvent.type(name, "Jana Nováková");
    const email = within(form).getByRole("textbox", { name: /Your e-mail/ });
    await userEvent.type(email, "jana");
    await userEvent.click(within(form).getByRole("button", { name: "Send" }));
    expect(within(form).getByText("The e-mail address is not valid.")).toBeVisible();
    expect(email).toHaveFocus();

    await userEvent.type(email, "@example.org");
    await userEvent.type(within(form).getByRole("textbox", { name: "Reserve for (date)" }), "3. 12.");
    await userEvent.type(
      within(form).getByRole("textbox", { name: "Message for SOkA Testov" }),
      "Přijdu dopoledne.",
    );
    await userEvent.click(within(form).getByRole("button", { name: "Send" }));

    // sending is confirmed: an order reaches people
    const question = await screen.findByRole("alertdialog", { name: "Confirmation" });
    expect(question).toHaveTextContent("Send the order?");
    expect(submit).not.toHaveBeenCalled();
    await userEvent.click(within(question).getByRole("button", { name: "Confirm" }));

    await waitFor(() => expect(submit).toHaveBeenCalledTimes(1));
    expect(submit).toHaveBeenCalledWith({
      name: "Jana Nováková",
      email: "jana@example.org",
      phoneNumber: undefined,
      subjects: ["Preliminary reservation of archival material"],
      returnUrl: window.location.href,
      archiv: "226101010",
      badatelna: undefined,
      // the wished date is introduced in Czech: the archive's staff read it
      popis: "Na kdy rezervovat: 3. 12.\nPřijdu dopoledne.",
      polozky: [
        {
          archivalie: {
            ...ORDER_ITEM.archivalie,
            popis: "Kronika",
            url: "http://localhost/apu/a",
          },
          zpusobZpristupneni: "REPLIKA",
        },
      ],
    });

    // the sent records leave the basket, the other archive's stay for their own order
    const basket = await screen.findByRole("dialog", { name: "Order basket" });
    expect(within(basket).getByRole("status")).toHaveTextContent(
      "The order for SOkA Testov was sent.",
    );
    expect(stored().map((item) => item.uuid)).toEqual(["b"]);
    // the button that opened the form went with its group; the focus must not
    // fall out of the basket, which would hide it from assistive technology
    expect(within(basket).getByRole("button", { name: "Close" })).toHaveFocus();
    // the reader did not ask for the contact to be remembered
    expect(window.localStorage.getItem("aron.orderContact")).toBeNull();
  });

  it("remembers the contact when asked to, and an emptied basket still reports the sent order", async () => {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify([basketItem("a", "Kronika")]));
    window.localStorage.setItem(
      "aron.orderContact",
      JSON.stringify({ name: "Jan Novák", email: "jan@example.org", phoneNumber: "123" }),
    );
    renderPortal();

    await userEvent.click(await screen.findByRole("button", { name: "Order basket: 1 record" }));
    await userEvent.click(await screen.findByRole("button", { name: "Order from SOkA Testov" }));
    const form = await screen.findByRole("dialog", { name: "Reservation of archival material" });

    expect(within(form).getByRole("textbox", { name: /Your name and surname/ })).toHaveValue(
      "Jan Novák",
    );
    expect(within(form).getByRole("checkbox", { name: "Remember these details" })).toBeChecked();
    await userEvent.click(within(form).getByRole("button", { name: "Send" }));
    await userEvent.click(
      within(await screen.findByRole("alertdialog")).getByRole("button", { name: "Confirm" }),
    );

    await waitFor(() =>
      expect(submit).toHaveBeenCalledWith(
        expect.objectContaining({ name: "Jan Novák", phoneNumber: "123", popis: undefined }),
      ),
    );
    const basket = await screen.findByRole("dialog", { name: "Order basket" });
    expect(within(basket).getByRole("status")).toHaveTextContent(
      "The order for SOkA Testov was sent.",
    );
    expect(basket).toHaveTextContent("The basket is empty.");
    expect(JSON.parse(window.localStorage.getItem("aron.orderContact") ?? "null")).toEqual({
      name: "Jan Novák",
      email: "jan@example.org",
      phoneNumber: "123",
    });

    // closing the emptied basket takes its button out of the header
    await userEvent.click(within(basket).getByRole("button", { name: "Close" }));
    await waitFor(() =>
      expect(screen.queryByRole("button", { name: /Order basket/ })).not.toBeInTheDocument(),
    );
  });

  it("says why an order was refused, in the service's words where it has any, and keeps the basket", async () => {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify([basketItem("a", "Kronika")]));
    window.localStorage.setItem(
      "aron.orderContact",
      JSON.stringify({ name: "Jan Novák", email: "jan@example.org", phoneNumber: "" }),
    );
    submit = vi.fn(() =>
      Promise.reject(new OrderError("HTTP 429", 429, "Byl překročen povolený počet požadavků.")),
    );
    renderPortal();

    await userEvent.click(await screen.findByRole("button", { name: "Order basket: 1 record" }));
    await userEvent.click(await screen.findByRole("button", { name: "Order from SOkA Testov" }));
    const form = await screen.findByRole("dialog", { name: "Reservation of archival material" });
    await userEvent.click(within(form).getByRole("button", { name: "Send" }));
    await userEvent.click(
      within(await screen.findByRole("alertdialog")).getByRole("button", { name: "Confirm" }),
    );

    expect(await within(form).findByRole("alert")).toHaveTextContent(
      "Byl překročen povolený počet požadavků.",
    );
    expect(stored()).toHaveLength(1);

    // a failure that explains nothing (Spring's default body, no network) is still said
    submit = vi.fn(() => Promise.reject(new OrderError("HTTP 500", 500)));
    await userEvent.click(within(form).getByRole("button", { name: "Send" }));
    await userEvent.click(
      within(await screen.findByRole("alertdialog")).getByRole("button", { name: "Confirm" }),
    );
    await waitFor(() =>
      expect(within(form).getByRole("alert")).toHaveTextContent(
        "The order could not be sent. Please try again later.",
      ),
    );
  });

  it("empties the whole basket only after a confirmation", async () => {
    window.localStorage.setItem(
      STORAGE_KEY,
      JSON.stringify([basketItem("a", "Kronika"), basketItem("b", "Matrika")]),
    );
    renderPortal();

    await userEvent.click(await screen.findByRole("button", { name: "Order basket: 2 records" }));
    const basket = await screen.findByRole("dialog", { name: "Order basket" });
    await userEvent.click(within(basket).getByRole("button", { name: "Empty the basket" }));
    const question = await screen.findByRole("alertdialog", { name: "Confirmation" });
    expect(question).toHaveTextContent("Empty the whole basket?");
    await userEvent.click(within(question).getByRole("button", { name: "Confirm" }));

    await waitFor(() => expect(window.localStorage.getItem(STORAGE_KEY)).toBeNull());
    expect(await screen.findByRole("dialog", { name: "Order basket" })).toHaveTextContent(
      "The basket is empty.",
    );
  });
});

describe("basket", () => {
  it("groups by archive and reading room, in the order of the first item", () => {
    const groups = groupsOf([
      basketItem("a", "A"),
      basketItem("b", "B", { badatelna: { name: "Sever", code: "S" } }),
      basketItem("c", "C"),
    ]);

    expect(groups.map((group) => [group.key, group.items.map((item) => item.uuid)])).toEqual([
      ["226101010", ["a", "c"]],
      ["226101010_S", ["b"]],
    ]);
  });

  it("accepts only items this build can work with", () => {
    // storage outlives a release and can be edited: the old portal's entry
    // without an institution must not break the header
    expect(isBasketItem(basketItem("a", "A"))).toBe(true);
    expect(isBasketItem({ ...basketItem("a", "A"), institution: undefined })).toBe(false);
    expect(isBasketItem({ ...basketItem("a", "A"), accessType: "JINAK" })).toBe(false);
    expect(isBasketItem({ error: "Fund not exist", apuId: "a" })).toBe(false);
    expect(isBasketItem(null)).toBe(false);
  });
});
