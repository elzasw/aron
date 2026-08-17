import { Route, Routes } from "react-router-dom";
import { ApuType, MenuItemCode } from "./api/generated";
import AppLayout from "./layout/AppLayout";
import ApuPage from "./pages/ApuPage";
import HomePage from "./pages/HomePage";
import NotFoundPage from "./pages/NotFoundPage";
import SectionPage from "./pages/SectionPage";
import SearchView from "./search/SearchView";

// Top-level route families must stay in sync with the server-side enumeration
// in IndexController (a new family needs a mapping there too). Section routes
// always exist; the menu configuration only controls their visibility.
export default function App() {
  return (
    <Routes>
      <Route element={<AppLayout />}>
        <Route index element={<HomePage />} />
        <Route path="apu" element={<SearchView titleKey="nav.search" />} />
        <Route path="apu/:uuid" element={<ApuPage />} />
        <Route
          path="institution"
          element={<SearchView apuType={ApuType.Institution} titleKey="sections.INSTITUTION" />}
        />
        <Route path="fund" element={<SearchView apuType={ApuType.Fund} titleKey="sections.FUND" />} />
        <Route
          path="finding-aid"
          element={<SearchView apuType={ApuType.FindingAid} titleKey="sections.FINDING_AID" />}
        />
        <Route
          path="arch-desc"
          element={<SearchView apuType={ApuType.ArchDesc} titleKey="sections.ARCH_DESC" />}
        />
        <Route path="entity" element={<SearchView apuType={ApuType.Entity} titleKey="sections.ENTITY" />} />
        {/* originator is an ENTITY search preset (arrives with the suggest slice); news is content */}
        <Route path="originator" element={<SectionPage code={MenuItemCode.Originator} />} />
        <Route path="news" element={<SectionPage code={MenuItemCode.News} />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  );
}
