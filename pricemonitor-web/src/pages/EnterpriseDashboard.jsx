import { useState, useEffect, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import {
  fetchEnterpriseProducts,
  addEnterpriseProduct,
  deleteEnterpriseProduct,
  checkEnterpriseProductNow,
  fetchListingsByProduct,
  fetchViolations,
  markViolationsAsSeen,
} from "../services/api";
import "./EnterpriseDashboard.css";

export default function EnterpriseDashboard() {
  const navigate = useNavigate();
  const userName = localStorage.getItem("pm_userName") || "Usuário";

  const [activePage, setActivePage] = useState("produtos"); // produtos | anuncios | violacoes
  const [products, setProducts] = useState([]);
  const [selectedProduct, setSelectedProduct] = useState(null);
  const [listings, setListings] = useState([]);
  const [violations, setViolations] = useState([]);
  const [unseenCount, setUnseenCount] = useState(0);

  // Filtros
  const [filterMarca, setFilterMarca] = useState("");
  const [filterOnlyViolations, setFilterOnlyViolations] = useState(false);

  // Form
  const [ean, setEan] = useState("");
  const [productName, setProductName] = useState("");
  const [marca, setMarca] = useState("");
  const [mapPrice, setMapPrice] = useState("");
  const [tolerancePercent, setTolerancePercent] = useState("0");

  const [loading, setLoading] = useState(false);
  const [checking, setChecking] = useState(null);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const loadProducts = useCallback(async () => {
    try {
      const prods = await fetchEnterpriseProducts();
      setProducts(prods || []);
    } catch { setError("Erro ao carregar produtos."); }
  }, []);

  const loadViolations = useCallback(async () => {
    try {
      const viols = await fetchViolations();
      setViolations(viols || []);
      setUnseenCount((viols || []).filter((v) => !v.seen).length);
    } catch { }
  }, []);

  useEffect(() => {
    loadProducts();
    loadViolations();
  }, [loadProducts, loadViolations]);

  function logout() {
    localStorage.clear();
    navigate("/login");
  }

  // ─── Produto: adicionar ───────────────────────────────────────────────────
  async function handleAdd(e) {
    e.preventDefault();
    setError(""); setSuccess(""); setLoading(true);
    try {
      await addEnterpriseProduct({
        ean: ean.trim(),
        productName: productName.trim(),
        marca: marca.trim(),
        mapPrice: parseFloat(mapPrice),
        tolerancePercent: parseFloat(tolerancePercent || "0"),
      });
      setEan(""); setProductName(""); setMarca("");
      setMapPrice(""); setTolerancePercent("0");
      setSuccess("Produto cadastrado!");
      await loadProducts();
    } catch (err) {
      setError(err.message || "Erro ao cadastrar produto.");
    } finally { setLoading(false); }
  }

  // ─── Produto: deletar ─────────────────────────────────────────────────────
  async function handleDelete(productId) {
    if (!confirm("Remover este produto e todos seus anúncios?")) return;
    try {
      await deleteEnterpriseProduct(productId);
      if (selectedProduct?.id === productId) { setSelectedProduct(null); setListings([]); }
      await loadProducts();
    } catch (err) { setError(err.message || "Erro ao remover."); }
  }

  // ─── Produto: verificar agora ─────────────────────────────────────────────
  async function handleCheck(product) {
    setChecking(product.id); setError(""); setSuccess("");
    try {
      const res = await checkEnterpriseProductNow(product.id);
      setSuccess(res.message);
      await loadProducts();
      await loadViolations();
      if (selectedProduct?.id === product.id) await loadListings(product);
    } catch (err) { setError(err.message || "Erro ao verificar."); }
    finally { setChecking(null); }
  }

  // ─── Anúncios de um produto ───────────────────────────────────────────────
  async function loadListings(product) {
    try {
      const data = await fetchListingsByProduct(product.id);
      setListings(data || []);
      setSelectedProduct(product);
      setActivePage("anuncios");
    } catch { setError("Erro ao carregar anúncios."); }
  }

  // ─── Violações: marcar vistas ─────────────────────────────────────────────
  async function handleMarkSeen() {
    await markViolationsAsSeen();
    await loadViolations();
  }

  // ─── Helpers ──────────────────────────────────────────────────────────────
  function fmt(value) {
    if (value == null) return "-";
    return new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }).format(value);
  }

  function fmtDate(d) {
    if (!d) return "-";
    return new Date(d).toLocaleString("pt-BR");
  }

  const marcas = [...new Set(products.map((p) => p.marca).filter(Boolean))];

  const filteredProducts = products.filter((p) => {
    if (filterMarca && p.marca !== filterMarca) return false;
    if (filterOnlyViolations && p.totalViolations === 0) return false;
    return true;
  });

  const totalAtivos = products.filter((p) => p.active).length;
  const totalViolados = products.filter((p) => p.totalViolations > 0).length;

  return (
    <div className="ep-page">
      {/* Header */}
      <header className="ep-header">
        <div className="ep-header-left">
          <div className="ep-logo">
            <span className="ep-logo-icon">📊</span>
            <span className="ep-logo-name">PriceMonitor</span>
          </div>
          <nav className="ep-nav">
            <button className={`ep-nav-btn ${activePage === "produtos" ? "active" : ""}`}
              onClick={() => setActivePage("produtos")}>Produtos</button>
            <button className={`ep-nav-btn ${activePage === "anuncios" ? "active" : ""}`}
              onClick={() => setActivePage("anuncios")} disabled={!selectedProduct}>
              {selectedProduct ? `Anúncios · ${selectedProduct.productName}` : "Anúncios"}
            </button>
            <button className={`ep-nav-btn ${activePage === "violacoes" ? "active" : ""}`}
              onClick={() => { setActivePage("violacoes"); handleMarkSeen(); }}>
              Violações
              {unseenCount > 0 && <span className="ep-badge-red">{unseenCount}</span>}
            </button>
          </nav>
        </div>
        <div className="ep-header-right">
          <span className="ep-username">{userName}</span>
          <button className="ep-logout" onClick={logout}>Sair</button>
        </div>
      </header>

      <main className="ep-main">
        {error && <div className="ep-alert ep-alert-error">{error}</div>}
        {success && <div className="ep-alert ep-alert-success">{success}</div>}

        {/* ── Página: Produtos ── */}
        {activePage === "produtos" && (
          <div>
            {/* Título + Badges */}
            <div className="ep-page-header">
              <div>
                <h2 className="ep-page-title">
                  Produtos
                  <span className="ep-chip ep-chip-blue">{totalAtivos} Ativos</span>
                  {totalViolados > 0 && (
                    <span className="ep-chip ep-chip-red">{totalViolados} Violados</span>
                  )}
                </h2>
                <p className="ep-page-sub">Gerencie os produtos do seu catálogo</p>
              </div>
            </div>

            {/* Filtros */}
            <div className="ep-filters">
              <select className="ep-select" value={filterMarca}
                onChange={(e) => setFilterMarca(e.target.value)}>
                <option value="">Filtrar marcas...</option>
                {marcas.map((m) => <option key={m} value={m}>{m}</option>)}
              </select>
              <label className="ep-filter-check">
                <input type="checkbox" checked={filterOnlyViolations}
                  onChange={(e) => setFilterOnlyViolations(e.target.checked)} />
                Apenas violados
              </label>
              {(filterMarca || filterOnlyViolations) && (
                <button className="ep-clear-filter"
                  onClick={() => { setFilterMarca(""); setFilterOnlyViolations(false); }}>
                  ✕ Limpar filtro
                </button>
              )}
            </div>

            {/* Formulário */}
            <div className="ep-form-card">
              <form className="ep-form" onSubmit={handleAdd}>
                <div className="ep-form-grid">
                  <div className="ep-form-group">
                    <label>EAN *</label>
                    <input type="text" placeholder="1234567890123"
                      value={ean} onChange={(e) => setEan(e.target.value)} required />
                  </div>
                  <div className="ep-form-group">
                    <label>Nome do Produto *</label>
                    <input type="text" placeholder="Digite o nome do produto"
                      value={productName} onChange={(e) => setProductName(e.target.value)} required />
                  </div>
                  <div className="ep-form-group">
                    <label>Preço Mínimo (R$) *</label>
                    <input type="number" step="0.01" min="0.01" placeholder="0,00"
                      value={mapPrice} onChange={(e) => setMapPrice(e.target.value)} required />
                  </div>
                  <div className="ep-form-group">
                    <label>Tolerância (%)</label>
                    <input type="number" step="0.1" min="0" max="100" placeholder="0,00"
                      value={tolerancePercent} onChange={(e) => setTolerancePercent(e.target.value)} />
                  </div>
                  <div className="ep-form-group">
                    <label>Marca *</label>
                    <input type="text" placeholder="Ex: Nike"
                      value={marca} onChange={(e) => setMarca(e.target.value)} />
                  </div>
                </div>
                <button className="ep-add-btn" type="submit" disabled={loading}>
                  {loading ? "Cadastrando..." : "Adicionar +"}
                </button>
              </form>
            </div>

            {/* Tabela de produtos */}
            <div className="ep-card">
              <h3 className="ep-card-title">Lista de Produtos ({filteredProducts.length})</h3>
              {filteredProducts.length === 0 ? (
                <p className="ep-empty">Nenhum produto encontrado.</p>
              ) : (
                <div className="ep-table-wrap">
                  <table className="ep-table">
                    <thead>
                      <tr>
                        <th>Nome</th>
                        <th>Marca</th>
                        <th>Preço Mínimo</th>
                        <th>Anúncios</th>
                        <th>Violações</th>
                        <th>Criado em</th>
                        <th>Ativo</th>
                        <th>Ações</th>
                      </tr>
                    </thead>
                    <tbody>
                      {filteredProducts.map((p) => (
                        <tr key={p.id} className={p.totalViolations > 0 ? "ep-row-violation" : ""}>
                          <td>
                            <div className="ep-product-name">{p.productName}</div>
                            <div className="ep-product-ean">{p.ean}</div>
                          </td>
                          <td>{p.marca
                            ? <span className="ep-marca-chip">{p.marca}</span>
                            : "-"}</td>
                          <td>{fmt(p.mapPrice)}</td>
                          <td>
                            <span className="ep-chip ep-chip-blue">{p.totalListings}</span>
                          </td>
                          <td>
                            {p.totalViolations > 0
                              ? <span className="ep-chip ep-chip-red">{p.totalViolations}</span>
                              : <span className="ep-chip ep-chip-green">0</span>}
                          </td>
                          <td>{fmtDate(p.createdAt)}</td>
                          <td>
                            <span className={`ep-status ${p.active ? "ep-status-on" : "ep-status-off"}`}>
                              {p.active ? "●" : "○"}
                            </span>
                          </td>
                          <td className="ep-actions">
                            <button className="ep-btn-icon ep-btn-view"
                              onClick={() => loadListings(p)} title="Ver anúncios">👁</button>
                            <button className="ep-btn-icon ep-btn-check"
                              onClick={() => handleCheck(p)}
                              disabled={checking === p.id} title="Verificar agora">
                              {checking === p.id ? "⏳" : "🔍"}
                            </button>
                            <button className="ep-btn-icon ep-btn-del"
                              onClick={() => handleDelete(p.id)} title="Remover">✕</button>
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

        {/* ── Página: Anúncios ── */}
        {activePage === "anuncios" && selectedProduct && (
          <div>
            <div className="ep-page-header">
              <div>
                <button className="ep-back-btn" onClick={() => setActivePage("produtos")}>
                  ← Voltar
                </button>
                <h2 className="ep-page-title">
                  {selectedProduct.productName}
                  <span className="ep-chip ep-chip-blue">{listings.length} Anúncios</span>
                  {listings.filter((l) => l.violation).length > 0 && (
                    <span className="ep-chip ep-chip-red">
                      {listings.filter((l) => l.violation).length} Violações
                    </span>
                  )}
                </h2>
                <p className="ep-page-sub">EAN: {selectedProduct.ean} · MAP: {fmt(selectedProduct.mapPrice)}</p>
              </div>
              <button className="ep-btn-check-all"
                onClick={() => handleCheck(selectedProduct)}
                disabled={checking === selectedProduct.id}>
                {checking === selectedProduct.id ? "Verificando..." : "🔍 Verificar agora"}
              </button>
            </div>

            <div className="ep-card">
              {listings.length === 0 ? (
                <p className="ep-empty">Nenhum anúncio encontrado. Clique em "Verificar agora".</p>
              ) : (
                <div className="ep-table-wrap">
                  <table className="ep-table">
                    <thead>
                      <tr>
                        <th>Título</th>
                        <th>Vendedor</th>
                        <th>Preço Anunciado</th>
                        <th>Preço MAP</th>
                        <th>% Abaixo</th>
                        <th>Status</th>
                        <th>Link</th>
                      </tr>
                    </thead>
                    <tbody>
                      {listings.map((l) => (
                        <tr key={l.id} className={l.violation ? "ep-row-violation" : ""}>
                          <td className="ep-listing-title">{l.listingTitle || "-"}</td>
                          <td>{l.sellerName || "-"}</td>
                          <td className={l.violation ? "ep-price-red" : "ep-price-ok"}>
                            {fmt(l.listedPrice)}
                          </td>
                          <td>{fmt(l.mapPrice)}</td>
                          <td>
                            {l.percentBelow != null
                              ? <span className={`ep-percent ${l.violation ? "ep-percent-red" : "ep-percent-ok"}`}>
                                  ↓ {l.percentBelow}%
                                </span>
                              : <span className="ep-percent ep-percent-ok">↑ Acima do MAP</span>}
                          </td>
                          <td>
                            {l.violation
                              ? <span className="ep-chip ep-chip-red">Violado</span>
                              : <span className="ep-chip ep-chip-green">OK</span>}
                          </td>
                          <td>
                            {l.listingUrl
                              ? <a href={l.listingUrl} target="_blank" rel="noopener noreferrer"
                                  className="ep-link">Ver anúncio →</a>
                              : "-"}
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

        {/* ── Página: Violações ── */}
        {activePage === "violacoes" && (
          <div>
            <div className="ep-page-header">
              <div>
                <h2 className="ep-page-title">
                  Violações de MAP
                  {violations.length > 0 && (
                    <span className="ep-chip ep-chip-red">{violations.length}</span>
                  )}
                </h2>
                <p className="ep-page-sub">Anúncios abaixo do preço mínimo configurado</p>
              </div>
            </div>
            <div className="ep-card">
              {violations.length === 0 ? (
                <p className="ep-empty">✅ Nenhuma violação detectada.</p>
              ) : (
                <div className="ep-table-wrap">
                  <table className="ep-table">
                    <thead>
                      <tr>
                        <th>Produto</th>
                        <th>Marca</th>
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
                        <tr key={v.id} className={!v.seen ? "ep-row-unseen" : ""}>
                          <td>{v.productName}</td>
                          <td>{v.marca ? <span className="ep-marca-chip">{v.marca}</span> : "-"}</td>
                          <td><code>{v.ean}</code></td>
                          <td>{v.sellerName || "-"}</td>
                          <td className="ep-price-red">{fmt(v.listedPrice)}</td>
                          <td>{fmt(v.mapPrice)}</td>
                          <td><span className="ep-percent ep-percent-red">↓ {v.percentBelow}%</span></td>
                          <td>{fmtDate(v.detectedAt)}</td>
                          <td>
                            {v.listingUrl
                              ? <a href={v.listingUrl} target="_blank" rel="noopener noreferrer"
                                  className="ep-link">Ver →</a>
                              : "-"}
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
      </main>
    </div>
  );
}
