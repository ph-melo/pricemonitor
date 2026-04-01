import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { login } from "../services/api";
import ThemeToggle from "../components/ThemeToggle";
import { useTheme } from "../theme";
import "./Login.css";

export default function Login() {
  const navigate = useNavigate();
  const { theme, toggleTheme } = useTheme();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  async function onSubmit(e) {
    e.preventDefault();
    setError("");
    setLoading(true);

    try {
      const data = await login(email, password);

      localStorage.setItem("pm_token", data.token);
      localStorage.setItem("pm_userId", String(data.userId));
      localStorage.setItem("pm_userEmail", data.email || "");
      localStorage.setItem("pm_userName", data.name || "");
      localStorage.setItem("pm_plan", data.plan || "FREE");
      localStorage.setItem("pm_maxProducts", String(data.maxProducts || 3));

      // Redireciona para o dashboard correto conforme o plano
      if (data.plan === "ENTERPRISE") {
        navigate("/enterprise");
      } else {
        navigate("/dashboard");
      }
    } catch (err) {
      setError("Email ou senha inválidos");
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="login-page">
      <section className="login-card">
        <div className="login-card-topbar">
          <h1 className="login-title">PriceMonitor</h1>
          <ThemeToggle theme={theme} onToggle={toggleTheme} />
        </div>
        <p className="login-subtitle">Acesse sua conta para continuar</p>

        <form className="login-form" onSubmit={onSubmit}>
          <label className="login-label" htmlFor="email">
            Email
          </label>
          <input
            id="email"
            className="login-input"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            type="email"
            placeholder="seu@email.com"
            autoComplete="email"
            required
          />

          <label className="login-label" htmlFor="password">
            Senha
          </label>
          <input
            id="password"
            className="login-input"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            type="password"
            placeholder="********"
            autoComplete="current-password"
            required
          />

          {error && <div className="login-error">{error}</div>}

          <button className="login-button" disabled={loading}>
            {loading ? "Entrando..." : "Entrar"}
          </button>
        </form>

        <p className="login-footer">
          Ainda não tem uma conta?{" "}
          <Link className="login-link" to="/register">
            Cadastrar-se
          </Link>
        </p>
      </section>
    </main>
  );
}
