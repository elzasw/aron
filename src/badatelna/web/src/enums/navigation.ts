import { ModulePath } from "./module";
import { Evidence } from "../components/evidence";
import { Help, Main, News } from "../modules";
import { IconType } from "../components/icon";

export const appHeaderItems = [
  {
    path: ModulePath.ARCHIVAL_FILES,
    label: "Archivní soubory",
    Component: Evidence,
  },
  {
    path: ModulePath.ARCHIVAL_AIDS,
    label: "Archivní pomůcky",
    Component: Evidence,
  },
  {
    path: ModulePath.ARCHIVAL_MATERIAL,
    label: "Archiválie",
    Component: Evidence,
  },
  {
    path: ModulePath.ACCESS_POINTS,
    label: "Přístupové body",
    Component: Evidence,
  },
  { path: ModulePath.NEWS, label: "Aktuality", Component: News },
  { path: ModulePath.HELP, label: "Nápověda", Component: Help },
];

export const navigationItems = [
  {
    exact: true,
    path: ModulePath.MAIN,
    Component: Main,
  },
  ...appHeaderItems,
];

export const favouriteQueries = [
  { icon: IconType.USERS_SOLID, label: "Matriky" },
  { icon: IconType.GRADUATION_CAP_SOLID, label: "Spisy žáků/studentů" },
  { icon: IconType.BOOK_SOLID, label: "Kroniky úřední" },
  { icon: IconType.USERS_SOLID, label: "Spisy evidence obyvatelstva" },
  { icon: IconType.BOOK_SOLID, label: "Kroniky neúřední" },
  { icon: IconType.ID_BADGE_SOLID, label: "Úřední kihy/evidence obyvatelstva" },
  { icon: IconType.BUILDING_SOLID, label: "Stavební spisy" },
  { icon: IconType.ID_BADGE_SOLID, label: "Kartotéky evidence obyvatelstva" },
  { icon: IconType.GRADUATION_CAP_SOLID, label: "Třídní výkazy" },
  { icon: IconType.USERS_SOLID, label: "Sčítání lidu" },
  { icon: IconType.MAP_SOLID, label: "Technické výkresy staveb" },
  { icon: IconType.FOLDER_OPEN_SOLID, label: "Spis evidující nemovistosti" },
];