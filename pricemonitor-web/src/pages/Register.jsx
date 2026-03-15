import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { register } from "../services/api";
import "./Login.css"; // reutiliza o mesmo estilo

export default function Register() {
  const navigate = useNavigate();

  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  async function onSubmit(e) {
    e.preventDefault();
    setError("");

    if (password !== confirm) {
      setError("As senhas não coincidem");
      return;
    }

    if (password.length < 6) {
      setError("A senha deve ter pelo menos 6 caracteres");
      return;
    }

    setLoading(true);

    try {
      const data = await register(name, email, password);

      localStorage.setItem("pm_token", data.token);
      localStorage.setItem("pm_userId", String(data.userId));
      localStorage.setItem("pm_userEmail", data.email || "");
      localStorage.setItem("pm_userName", data.name || "");
      localStorage.setItem("pm_plan", data.plan || "FREE");
      localStorage.setItem("pm_maxProducts", String(data.maxProducts || 3));

      navigate("/dashboard");
    } catch (err) {
      setError(err.message || "Erro ao criar conta");
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="login-page">
      <section className="login-card">
        <h1 className="login-title">PriceMonitor</h1>
        <p className="login-subtitle">Crie sua conta gratuita</p>

        <form className="login-form" onSubmit={onSubmit}>
          <label className="login-label" htmlFor="name">
            Nome
          </label>
          <input
            id="name"
            className="login-input"
            value={name}
            onChange={(e) => setName(e.target.value)}
            type="text"
            placeholder="Seu nome"
            autoComplete="name"
            required
          />

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
            placeholder="Mínimo 6 caracteres"
            autoComplete="new-password"
            required
          />

          <label className="login-label" htmlFor="confirm">
            Confirmar senha
          </label>
          <input
            id="confirm"
            className="login-input"
            value={confirm}
            onChange={(e) => setConfirm(e.target.value)}
            type="password"
            placeholder="Repita a senha"
            autoComplete="new-password"
            required
          />

          {error && <div className="login-error">{error}</div>}

          <button className="login-button" disabled={loading}>
            {loading ? "Criando conta..." : "Criar conta"}
          </button>
        </form>

        <p className="login-footer">
          Já tem conta?{" "}
          <Link className="login-link" to="/login">
            Entrar
          </Link>
        </p>
      </section>
    </main>
  );
}
