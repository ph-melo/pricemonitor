import { useState, useEffect, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import {
  fetchEnterpriseProducts,
  addEnterpriseProduct,
  deleteEnterpriseProduct,
  checkEnterpriseProductNow,
  fetchViolations,
  markViolationsAsSeen,
} from "../services/api";
import "./EnterpriseDashboard.css";

export default function EnterpriseDashboard() {
  const navigate = useNavigate();
  const userName = localStorage.getItem("pm_userName") || "Usuário";

  const [products, setProducts] = useState([]);
  const [violations, setViolations] = useState([]);
  const [unseenCount, setUnseenCount] = useState(0);
  const [activeTab, setActiveTab] = useState("products"); // "products" | "violations"

  const [ean, setEan] = useState("");
  const [productName, setProductName] = useState("");
  const [mapPrice, setMapPrice] = useState("");
  const [tolerancePercent, setTolerancePercent] = useState("0");

  const [loading, setLoading] = useState(false);
  const [checking, setChecking] = useState(null);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const loadData = useCallback(async () => {
    try {
      const [prods, viols] = await Promise.all([
        fetchEnterpriseProducts(),
        fetchViolations(),
      ]);
      setProducts(prods || []);
      setViolations(viols || []);
      setUnseenCount((viols || []).filter((v) => !v.seen).length);
    } catch {
      setError("Erro ao carregar dados.");
    }
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData]);

  function logout() {
    localStorage.clear();
    navigate("/login");
  }

  async function handleAdd(e) {
    e.preventDefault();
    setError("");
    setSuccess("");
    setLoading(true);
    try {
      await addEnterpriseProduct({
        ean: ean.trim(),
        productName: productName.trim(),
        mapPrice: parseFloat(mapPrice),
        tolerancePercent: parseFloat(tolerancePercent || "0"),
      });
      setEan("");
      setProductName("");
      setMapPrice("");
      setTolerancePercent("0");
      setSuccess("Produto cadastrado com sucesso!");
      await loadData();
    } catch (err) {
      setError(err.message || "Erro ao cadastrar produto.");
    } finally {
      setLoading(false);
    }
  }

  async function handleDelete(productId) {
    if (!confirm("Remover este produto e todos seus alertas?")) return;
    try {
      await deleteEnterpriseProduct(productId);
      await loadData();
    } catch (err) {
      setError(err.message || "Erro ao remover produto.");
    }
  }

  async function handleCheck(productId) {
    setChecking(productId);
    setError("");
    setSuccess("");
    try {
      const res = await checkEnterpriseProductNow(productId);
      setSuccess(res.message || "Verificação concluída.");
      await loadData();
    } catch (err) {
      setError(err.message || "Erro ao verificar produto.");
    } finally {
      setChecking(null);
    }
  }

  async function handleMarkSeen() {
    await markViolationsAsSeen();
    await loadData();
  }

  function formatPrice(value) {
    if (value == null) return "-";
    return new Intl.NumberFormat("pt-BR", {
      style: "currency",
      currency: "BRL",
    }).format(value);
  }

  function formatDate(dateStr) {
    if (!dateStr) return "-";
    return new Date(dateStr).toLocaleString("pt-BR");
  }

  return (
    <div className="enterprise-page">
      {/* Header */}
      <header className="enterprise-header">
        <div className="enterprise-header-left">
          <h1 className="enterprise-title">PriceMonitor</h1>
          <span className="enterprise-badge">ENTERPRISE</span>
        </div>
        <div className="enterprise-header-right">
          <span className="enterprise-username">Olá, {userName}</span>
          <button className="enterprise-logout-btn" onClick={logout}>
            Sair
          </button>
        </div>
      </header>

      {/* Tabs */}
      <nav className="enterprise-tabs">
        <button
          className={`enterprise-tab ${activeTab === "products" ? "active" : ""}`}
          onClick={() => setActiveTab("products")}
        >
          Produtos Monitorados ({products.length})
        </button>
        <button
          className={`enterprise-tab ${activeTab === "violations" ? "active" : ""}`}
          onClick={() => { setActiveTab("violations"); handleMarkSeen(); }}
        >
          Alertas de Violação
          {unseenCount > 0 && (
            <span className="enterprise-badge-count">{unseenCount}</span>
          )}
        </button>
      </nav>

      <main className="enterprise-main">
        {/* Mensagens */}
        {error && <div className="enterprise-error">{error}</div>}
        {success && <div className="enterprise-success">{success}</div>}

        {/* Tab: Produtos */}
        {activeTab === "products" && (
          <div className="enterprise-section">
            {/* Formulário de cadastro */}
            <div className="enterprise-card">
              <h2 className="enterprise-card-title">Cadastrar Produto por EAN</h2>
              <form className="enterprise-form" onSubmit={handleAdd}>
                <div className="enterprise-form-row">
                  <div className="enterprise-form-group">
                    <label>Código EAN</label>
                    <input
                      type="text"
                      placeholder="Ex: 7891234567890"
                      value={ean}
                      onChange={(e) => setEan(e.target.value)}
                      required
                    />
                  </div>
                  <div className="enterprise-form-group">
                    <label>Nome do Produto</label>
                    <input
                      type="text"
                      placeholder="Ex: Tênis Nike Air Max 42"
                      value={productName}
                      onChange={(e) => setProductName(e.target.value)}
                      required
                    />
                  </div>
                </div>
                <div className="enterprise-form-row">
                  <div className="enterprise-form-group">
                    <label>Preço MAP (R$)</label>
                    <input
                      type="number"
                      step="0.01"
                      min="0.01"
                      placeholder="Ex: 599.90"
                      value={mapPrice}
                      onChange={(e) => setMapPrice(e.target.value)}
                      required
                    />
                  </div>
                  <div className="enterprise-form-group">
                    <label>Tolerância (%)</label>
                    <input
                      type="number"
                      step="0.1"
                      min="0"
                      max="100"
                      placeholder="Ex: 5 = alertar se 5% abaixo"
                      value={tolerancePercent}
                      onChange={(e) => setTolerancePercent(e.target.value)}
                    />
                  </div>
                </div>
                <button
                  className="enterprise-add-btn"
                  type="submit"
                  disabled={loading}
                >
                  {loading ? "Cadastrando..." : "+ Adicionar Produto"}
                </button>
              </form>
            </div>

            {/* Lista de produtos */}
            <div className="enterprise-card">
              <h2 className="enterprise-card-title">Produtos Cadastrados</h2>
              {products.length === 0 ? (
                <p className="enterprise-empty">
                  Nenhum produto cadastrado ainda.
                </p>
              ) : (
                <div className="enterprise-table-wrapper">
                  <table className="enterprise-table">
                    <thead>
                      <tr>
                        <th>EAN</th>
                        <th>Produto</th>
                        <th>Preço MAP</th>
                        <th>Tolerância</th>
                        <th>Última Verificação</th>
                        <th>Ações</th>
                      </tr>
                    </thead>
                    <tbody>
                      {products.map((p) => (
                        <tr key={p.id}>
                          <td><code>{p.ean}</code></td>
                          <td>{p.productName}</td>
                          <td>{formatPrice(p.mapPrice)}</td>
                          <td>{p.tolerancePercent}%</td>
                          <td>{formatDate(p.lastCheckAt)}</td>
                          <td className="enterprise-actions">
                            <button
                              className="enterprise-check-btn"
                              onClick={() => handleCheck(p.id)}
                              disabled={checking === p.id}
                              title="Verificar agora"
                            >
                              {checking === p.id ? "⏳" : "🔍"}
                            </button>
                            <button
                              className="enterprise-delete-btn"
                              onClick={() => handleDelete(p.id)}
                              title="Remover"
                            >
                              ✕
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          </div>
        )}

        {/* Tab: Violações */}
        {activeTab === "violations" && (
          <div className="enterprise-card">
            <h2 className="enterprise-card-title">
              Alertas de Violação de MAP
              {violations.length > 0 && (
                <span className="enterprise-violation-count">
                  {violations.length} registro(s)
                </span>
              )}
            </h2>
            {violations.length === 0 ? (
              <p className="enterprise-empty">
                ✅ Nenhuma violação de MAP detectada.
              </p>
            ) : (
              <div className="enterprise-table-wrapper">
                <table className="enterprise-table">
                  <thead>
                    <tr>
                      <th>Produto</th>
                      <th>EAN</th>
                      <th>Vendedor</th>
                      <th>Preço Anunciado</th>
                      <th>Preço MAP</th>
                      <th>% Abaixo</th>
                      <th>Detectado em</th>
                      <th>Link</th>
                    </tr>
                  </thead>
                  <tbody>
                    {violations.map((v) => (
                      <tr
                        key={v.id}
                        className={!v.seen ? "enterprise-row-unseen" : ""}
                      >
                        <td>{v.productName}</td>
                        <td><code>{v.ean}</code></td>
                        <td>{v.sellerName || "-"}</td>
                        <td className="enterprise-price-violation">
                          {formatPrice(v.listedPrice)}
                        </td>
                        <td>{formatPrice(v.mapPrice)}</td>
                        <td>
                          <span className="enterprise-percent-badge">
                            ↓ {v.percentBelow}%
                          </span>
                        </td>
                        <td>{formatDate(v.detectedAt)}</td>
                        <td>
                          {v.listingUrl ? (
                            <a
                              href={v.listingUrl}
                              target="_blank"
                              rel="noopener noreferrer"
                              className="enterprise-link"
                            >
                              Ver anúncio
                            </a>
                          ) : "-"}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        )}
      </main>
    </div>
  );
}
