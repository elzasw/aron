import { Route, Routes } from "react-router-dom";
import AppLayout from "./layout/AppLayout";
import ApuPage from "./pages/ApuPage";
import HomePage from "./pages/HomePage";
import NotFoundPage from "./pages/NotFoundPage";

// Top-level route families must stay in sync with the server-side enumeration
// in IndexController (a new family needs a mapping there too).
export default function App() {
  return (
    <Routes>
      <Route element={<AppLayout />}>
        <Route index element={<HomePage />} />
        <Route path="apu/:uuid" element={<ApuPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  );
}
