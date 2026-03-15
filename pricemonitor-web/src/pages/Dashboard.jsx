import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  fetchProducts,
  addProduct,
  deleteProduct,
  checkProductNow,
} from "../services/api";
import "./Dashboard.css";

export default function Dashboard() {
  const navigate = useNavigate();

  const [productUrl, setProductUrl]   = useState("");
  const [products, setProducts]       = useState([]);
  const [loadingList, setLoadingList] = useState(false);
  const [saving, setSaving]           = useState(false);
  const [checkingId, setCheckingId]   = useState(null);
  const [error, setError]             = useState("");

  const userName    = localStorage.getItem("pm_userName") || "";
  const plan        = localStorage.getItem("pm_plan") || "FREE";
  const maxProducts = parseInt(localStorage.getItem("pm_maxProducts") || "3", 10);

  const limitReached = products.length >= maxProducts;

  useEffect(() => {
    if (!localStorage.getItem("pm_token")) {
      navigate("/login", { replace: true });
    }
  }, [navigate]);

  async function loadProducts() {
    setLoadingList(true);
    setError("");
    try {
      const data = await fetchProducts();
      setProducts(Array.isArray(data) ? data : []);
    } catch (e) {
      if (e.status === 401 || e.status === 403) { handleLogout(); return; }
      setError(e.message || "Erro ao carregar produtos");
    } finally {
      setLoadingList(false);
    }
  }

  useEffect(() => { loadProducts(); }, []); // eslint-disable-line

  async function handleAdd(e) {
    e.preventDefault();
    setError("");
    const url = productUrl.trim();
    if (!url) return;

    setSaving(true);
    try {
      const created = await addProduct(url);
      setProducts((prev) => [created, ...prev]);
      setProductUrl("");
    } catch (e) {
      setError(e.message || "Erro ao cadastrar produto");
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete(productId) {
    setError("");
    try {
      await deleteProduct(productId);
      setProducts((prev) => prev.filter((p) => p.id !== productId));
    } catch (e) {
      setError(e.message || "Erro ao remover produto");
    }
  }

  async function handleCheckNow(productId) {
    setError("");
    setCheckingId(productId);
    try {
      const result = await checkProductNow(productId);
      if (result.status === "OK" && result.product) {
        setProducts((prev) =>
          prev.map((p) => (p.id === productId ? result.product : p))
        );
      } else {
        setError(result.message || "Não foi possível verificar o produto agora");
      }
    } catch (e) {
      setError(e.message || "Erro ao verificar produto");
    } finally {
      setCheckingId(null);
    }
  }

  function handleLogout() {
    localStorage.clear();
    navigate("/login");
  }

  return (
    <main className="dashboard-page">
      <section className="dashboard-card">

        <header className="dashboard-header">
          <div>
            <h1 className="dashboard-title">Painel</h1>
            <p className="dashboard-subtitle">
              {userName ? `Olá, ${userName}! ` : ""}Gerencie os produtos monitorados
            </p>
            <p>O monitoramento de preços ocorre a cada 30 minutos.</p>
          </div>
          <div className="dashboard-header-actions">
            <button
              className="dashboard-button dashboard-button-ghost"
              type="button"
              onClick={loadProducts}
              disabled={loadingList}
            >
              {loadingList ? "Recarregando..." : "Recarregar"}
            </button>
            <button
              className="dashboard-button dashboard-button-secondary"
              type="button"
              onClick={handleLogout}
            >
              Sair
            </button>
          </div>
        </header>

        {/* ── Contador de limite ── */}
        <div className={`plan-bar ${limitReached ? "plan-bar--full" : ""}`}>
          <span className="plan-badge">Plano {plan}</span>
          <span className="plan-counter">
            {products.length} / {maxProducts} produtos monitorados
          </span>
          {limitReached && (
            <span className="plan-limit-msg">
              Limite atingido — remova um produto para adicionar outro
            </span>
          )}
        </div>

        {/* ── Formulário de adição ── */}
        {limitReached ? (
          <div className="dashboard-limit-banner">
            🔒 Você atingiu o limite de <strong>{maxProducts} produtos</strong> do plano{" "}
            <strong>{plan}</strong>. Remova um produto monitorado para adicionar outro.
          </div>
        ) : (
          <form className="dashboard-form" onSubmit={handleAdd}>
            <input
              className="dashboard-input"
              value={productUrl}
              onChange={(e) => setProductUrl(e.target.value)}
              placeholder="Cole o link do produto no Mercado Livre"
              required
            />
            <button className="dashboard-button" type="submit" disabled={saving}>
              {saving ? "Salvando..." : "Adicionar"}
            </button>
          </form>
        )}

        {error && <div className="dashboard-error" role="alert">{error}</div>}

        {/* ── Lista de produtos ── */}
        <section className="dashboard-list">
          <h2 className="dashboard-list-title">Itens monitorados</h2>

          {loadingList ? (
            <div className="dashboard-empty">Carregando...</div>
          ) : products.length === 0 ? (
            <div className="dashboard-empty">
              Nenhum item ainda. Cole um link do Mercado Livre acima para começar.
            </div>
          ) : (
            <div className="dashboard-table-wrap">
              <div className="dashboard-table dashboard-table-head">
                <div>Produto</div>
                <div>Preço</div>
                <div>Status</div>
                <div>Link</div>
                <div>Ação</div>
              </div>

              {products.map((p) => {
                const statusUi      = getStatusUI(p.lastStatus, p.lastError);
                const isCheckingThis = checkingId === p.id;

                return (
                  <div key={p.id} className="dashboard-table dashboard-table-row">
                    <div>
                      <div className="dashboard-product-title">{p.title || "Sem título"}</div>
                      {p.lastCheckAt && (
                        <div className="dashboard-meta">
                          Última verificação: {formatDateTime(p.lastCheckAt)}
                        </div>
                      )}
                    </div>

                    <div>{p.lastPrice != null ? formatPrice(p.lastPrice, p.currency) : "—"}</div>

                    <div>
                      <span className={`dashboard-status dashboard-status-${statusUi.tone}`}>
                        {statusUi.label}
                      </span>
                      {statusUi.details && (
                        <div className="dashboard-meta">{statusUi.details}</div>
                      )}
                    </div>

                    <div className="dashboard-link-cell">
                      <a className="dashboard-link" href={p.productUrl} target="_blank" rel="noreferrer">
                        abrir
                      </a>
                    </div>

                    <div style={{ display: "flex", gap: 8, justifyContent: "flex-end" }}>
                      <button
                        className="dashboard-icon-button"
                        type="button"
                        onClick={() => handleCheckNow(p.id)}
                        title="Verificar agora"
                        disabled={isCheckingThis || saving}
                      >
                        {isCheckingThis ? "…" : "↻"}
                      </button>
                      <button
                        className="dashboard-icon-button"
                        type="button"
                        onClick={() => handleDelete(p.id)}
                        title="Remover produto"
                        disabled={isCheckingThis}
                      >
                        ✕
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </section>
      </section>
    </main>
  );
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

function getStatusUI(lastStatus, lastError) {
  switch ((lastStatus || "").toUpperCase()) {
    case "ERROR":    return { label: "Erro", details: lastError ? truncate(lastError, 140) : "Falha ao consultar preço", tone: "error" };
    case "CHANGED":  return { label: "Preço mudou", details: null, tone: "warn" };
    case "NO_CHANGE":return { label: "Sem mudança", details: null, tone: "neutral" };
    case "INITIAL":  return { label: "Inicial", details: "Primeira coleta", tone: "neutral" };
    case "PAUSED":   return { label: "Pausado", details: null, tone: "neutral" };
    default:         return { label: "—", details: null, tone: "neutral" };
  }
}

function truncate(text, max) {
  return text && text.length > max ? text.slice(0, max - 1) + "…" : text || "";
}

function formatPrice(value, currency) {
  const n = Number(value);
  if (Number.isNaN(n)) return String(value);
  return new Intl.NumberFormat("pt-BR", { style: "currency", currency: currency || "BRL" }).format(n);
}

function formatDateTime(value) {
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return String(value);
  return new Intl.DateTimeFormat("pt-BR", { dateStyle: "short", timeStyle: "short" }).format(d);
}
