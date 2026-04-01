import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import Login from "./pages/Login";
import Register from "./pages/Register";
import Dashboard from "./pages/Dashboard";
import EnterpriseDashboard from "./pages/EnterpriseDashboard";

function RequireAuth({ children }) {
  const token = localStorage.getItem("pm_token");
  return token ? children : <Navigate to="/login" replace />;
}

function RequireEnterprise({ children }) {
  const token = localStorage.getItem("pm_token");
  const plan = localStorage.getItem("pm_plan");
  if (!token) return <Navigate to="/login" replace />;
  if (plan !== "ENTERPRISE") return <Navigate to="/dashboard" replace />;
  return children;
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />

        <Route
          path="/dashboard"
          element={
            <RequireAuth>
              <Dashboard />
            </RequireAuth>
          }
        />

        <Route
          path="/enterprise"
          element={
            <RequireEnterprise>
              <EnterpriseDashboard />
            </RequireEnterprise>
          }
        />

        <Route path="/" element={<Navigate to="/login" replace />} />
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
