import { Route, Routes } from "react-router-dom";
import { MenuItemCode } from "./api/generated";
import AppLayout from "./layout/AppLayout";
import ApuPage from "./pages/ApuPage";
import HomePage from "./pages/HomePage";
import NotFoundPage from "./pages/NotFoundPage";
import SearchPage from "./pages/SearchPage";
import SectionPage from "./pages/SectionPage";

// Top-level route families must stay in sync with the server-side enumeration
// in IndexController (a new family needs a mapping there too). Section routes
// always exist; the menu configuration only controls their visibility.
export default function App() {
  return (
    <Routes>
      <Route element={<AppLayout />}>
        <Route index element={<HomePage />} />
        <Route path="apu" element={<SearchPage />} />
        <Route path="apu/:uuid" element={<ApuPage />} />
        <Route path="institution" element={<SectionPage code={MenuItemCode.Institution} />} />
        <Route path="fund" element={<SectionPage code={MenuItemCode.Fund} />} />
        <Route path="finding-aid" element={<SectionPage code={MenuItemCode.FindingAid} />} />
        <Route path="arch-desc" element={<SectionPage code={MenuItemCode.ArchDesc} />} />
        <Route path="entity" element={<SectionPage code={MenuItemCode.Entity} />} />
        <Route path="originator" element={<SectionPage code={MenuItemCode.Originator} />} />
        <Route path="news" element={<SectionPage code={MenuItemCode.News} />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  );
}
