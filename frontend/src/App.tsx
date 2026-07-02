import { useEffect, useMemo, useState } from "react";
import type { FormEvent } from "react";
import { Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import {
  ArrowLeft,
  ChevronLeft,
  ChevronRight,
  Bell,
  Building2,
  Check,
  CheckCircle2,
  Clock3,
  Database,
  Heart,
  Home,
  LogIn,
  LogOut,
  MapPin,
  RefreshCw,
  Search,
  ShieldCheck,
  SlidersHorizontal,
  Sparkles,
  Train,
  UserRound,
  UsersRound,
  X
} from "lucide-react";
import {
  addFavorite,
  getAdminImports,
  getAdminReviewListings,
  getOlxDebugInfo,
  getAdminSources,
  getAdminUsers,
  getApartments,
  getCurrentUser,
  getFavorites,
  getStats,
  loginUser,
  logoutUser,
  registerUser,
  removeFavorite,
  runAdminSync,
  updateAdminListingStatus,
  updateAdminSource,
  updateUserStatus
} from "./api";
import type { Apartment, CurrencyCode, DataSource, ImportRun, OlxDebugInfo, SearchFilters, UserSummary } from "./types";

const currency = new Intl.NumberFormat("uk-UA");

function formatPrice(value: number, currencyCode: CurrencyCode = "UAH"): string {
  const normalizedCurrency = normalizeCurrency(currencyCode);
  return new Intl.NumberFormat("uk-UA", {
    style: "currency",
    currency: normalizedCurrency,
    maximumFractionDigits: 0
  }).format(value);
}

function normalizeCurrency(currencyCode?: string | null): CurrencyCode {
  const value = String(currencyCode || "").trim().toUpperCase();

  if (value === "USD" || value === "$" || value === "US$") return "USD";
  if (value === "EUR" || value === "€") return "EUR";
  if (value === "UAH" || value === "ГРН" || value === "UAH." || value === "₴") return "UAH";

  return "UAH";
}

function apartmentPreviewImage(apartment: Apartment): string {
  const imageUrl = (apartment.imageUrl || "").trim();
  if (!imageUrl) {
    return "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=1200&q=80";
  }
  if (/^https?:\/\//i.test(imageUrl)) {
    return imageUrl;
  }
  if (imageUrl.startsWith("dom/")) {
    return `https://cdn.riastatic.com/photos/${riaPhotoVariant(imageUrl)}`;
  }
  if (imageUrl.startsWith("/dom/")) {
    return `https://cdn.riastatic.com/photos/${riaPhotoVariant(imageUrl.slice(1))}`;
  }
  return imageUrl;
}

function riaPhotoVariant(path: string): string {
  return path.replace(/(\.[a-z0-9]+)$/i, "f$1");
}

function apartmentGallery(apartment: Apartment): string[] {
  const images = [...(apartment.images || []), apartment.imageUrl]
    .map((image) => apartmentPreviewImage({ ...apartment, imageUrl: image || "" }))
    .filter((image, index, items) => Boolean(image) && items.indexOf(image) === index);

  return images.length
    ? images
    : ["https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=1200&q=80"];
}

function shortText(value?: string, max = 180): string {
  const text = (value || "").trim();
  if (!text) return "Опис відсутній";
  if (text.length <= max) return text;
  return `${text.slice(0, max).trimEnd()}...`;
}

function sourceLabel(source?: string | null): string {
  const normalized = String(source || "").trim().toUpperCase();
  if (normalized === "DIM_RIA" || normalized === "DIM.RIA") return "DIM.RIA";
  if (normalized === "OLX") return "OLX";
  return normalized || "Невідоме джерело";
}

function getErrorMessage(error: unknown): string {
  if (error instanceof Error) {
    if (error.message === "Failed to fetch") {
      return "Не вдалося підключитися до сервера. Перевір, чи запущений Spring Boot і чи дозволений доступ з цієї адреси сайту.";
    }
    return error.message;
  }
  return "Сталася помилка";
}

function ScoreBadge({ label, value, description, tone = "blue" }: { label: string; value: number; description: string; tone?: string }) {
  return (
    <div className={`score-badge ${tone}`} title={`${label}: ${description}`}>
      <span>{label}</span>
      <strong>{value}<small>/100</small></strong>
      <p>{description}</p>
    </div>
  );
}

function StatCard({ icon: Icon, label, value }: { icon: typeof Building2; label: string; value: string | number }) {
  return (
    <article className="stat-card">
      <Icon size={22} />
      <div>
        <strong>{value}</strong>
        <span>{label}</span>
      </div>
    </article>
  );
}

function AuthModal({ initialMode, onClose, onLogin }: { initialMode: "login" | "register"; onClose: () => void; onLogin: (user: UserSummary) => Promise<void> }) {
  const [mode, setMode] = useState<"login" | "register">(initialMode);
  const [form, setForm] = useState({ name: "", email: "", password: "" });
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setLoading(true);
    setMessage("");
    try {
      if (mode === "register") {
        const result = await registerUser(form);
        setMessage(result.message || "Реєстрація пройшла успішно");
        setMode("login");
      } else {
        const result = await loginUser({ email: form.email, password: form.password });
        localStorage.setItem("homesafe_token", result.data.token);
        await onLogin(result.data.user);
        onClose();
      }
    } catch (error) {
      setMessage(getErrorMessage(error));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="modal-backdrop" role="presentation">
      <section className="auth-modal" role="dialog" aria-modal="true" aria-label="Авторизація">
        <button className="modal-close" onClick={onClose} aria-label="Закрити">
          <X size={20} />
        </button>
        <div className="auth-heading">
          <span className="eyebrow">HomeSafe</span>
          <h2>{mode === "login" ? "Вхід до кабінету" : "Створення облікового запису"}</h2>
          <p>
            {mode === "login"
              ? "Увійдіть, щоб користуватися обраним і персональним кабінетом."
              : "Після реєстрації адміністратор має схвалити ваш обліковий запис."}
          </p>
        </div>
        <div className="auth-tabs">
          <button className={mode === "login" ? "active" : ""} onClick={() => setMode("login")}>Вхід</button>
          <button className={mode === "register" ? "active" : ""} onClick={() => setMode("register")}>Реєстрація</button>
        </div>
        <form className="auth-form" onSubmit={handleSubmit}>
          {mode === "register" && (
            <label>
              Ім'я
              <input required minLength={2} value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} placeholder="Р’Р°С€Рµ С–Рј'СЏ" />
            </label>
          )}
          <label>
            Електронна пошта
            <input required type="email" value={form.email} onChange={(event) => setForm({ ...form, email: event.target.value })} placeholder="name@example.com" />
          </label>
          <label>
            Пароль
            <input required type="password" minLength={mode === "register" ? 8 : 1} value={form.password} onChange={(event) => setForm({ ...form, password: event.target.value })} placeholder="Мінімум 8 символів" />
          </label>
          {message && <p className="form-message">{message}</p>}
          <button className="primary-button" type="submit" disabled={loading}>
            <LogIn size={18} />
            {loading ? "Зачекайте..." : mode === "login" ? "Увійти" : "Зареєструватися"}
          </button>
        </form>
      </section>
    </div>
  );
}

function ApartmentCard({ apartment, selected, onSelect, isFavorite, onFavorite }: { apartment: Apartment; selected: boolean; onSelect: (apartment: Apartment) => void; isFavorite: boolean; onFavorite: (apartment: Apartment) => void }) {
  const riskLabel = apartment.fraudRisk === "LOW" ? "Низький ризик" : "Потрібна перевірка";

  return (
    <article className={`apartment-card ${selected ? "selected" : ""}`} onClick={() => onSelect(apartment)}>
      <div className="apartment-image-wrap">
        <img src={apartmentPreviewImage(apartment)} alt={apartment.title} onError={(event) => { event.currentTarget.src = apartmentPreviewImage({ ...apartment, imageUrl: "" }); }} />
        <button
          className={`favorite-button ${isFavorite ? "active" : ""}`}
          aria-label={isFavorite ? "Видалити з обраного" : "Додати в обране"}
          onClick={(event) => {
            event.stopPropagation();
            onFavorite(apartment);
          }}
        >
          <Heart size={18} fill={isFavorite ? "currentColor" : "none"} />
        </button>
        <span className="verified-pill">
          <ShieldCheck size={15} />
          {apartment.isVerified ? "Перевірено" : "На перевірці"}
        </span>
      </div>
      <div className="apartment-content">
        <div className="apartment-heading">
          <div>
            <h3>{apartment.title}</h3>
            <p><MapPin size={15} />{apartment.city}, {apartment.district}</p>
          </div>
          <div className="price">
            <strong>{formatPrice(apartment.price, apartment.currency)}</strong>
            <span>на місяць</span>
          </div>
        </div>
        <div className="meta-row">
          <span>{apartment.rooms} кімн.</span>
          <span>{apartment.area} м²</span>
          <span>{apartment.floor}/{apartment.totalFloors} поверх</span>
          <span>{apartment.ownerType}</span>
        </div>
        <div className="score-grid">
          <ScoreBadge label="Надійність" value={apartment.trustScore} description="перевірка оголошення" tone="green" />
          <ScoreBadge label="Вигідність" value={apartment.valueScore} description="порівняння ціни" tone="blue" />
          <ScoreBadge label="Комфорт" value={apartment.comfortScore} description="умови району" tone="amber" />
        </div>
        <div className="market-row">
          <span>{apartment.savingsPercent}% економії проти ринку</span>
          <span>{riskLabel}</span>
        </div>
      </div>
    </article>
  );
}

function DetailsPanel({ apartment }: { apartment: Apartment | null }) {
  if (!apartment) return null;
  const chartData = apartment.priceHistory?.map((item) => ({
    month: new Date(item.recordedAt).toLocaleDateString("uk-UA", { month: "short" }),
    price: item.price
  })) || [];

  return (
    <aside className="details-panel">
      <div className="panel-header">
        <div>
          <span className="eyebrow">AI рекомендація</span>
          <h2>{apartment.title}</h2>
        </div>
        <Sparkles size={24} />
      </div>
      <div className="recommendation-box">
        <strong>Ця квартира рекомендована, тому що:</strong>
        <ul>
          {apartment.recommendationReasons.map((reason) => (
            <li key={reason}><CheckCircle2 size={16} />{reason}</li>
          ))}
        </ul>
      </div>
      <div className="price-chart">
        <div className="mini-heading">
          <strong>Історія ціни</strong>
          <span>ринкова: {formatPrice(apartment.marketAveragePrice, apartment.currency)}</span>
        </div>
        <ResponsiveContainer width="100%" height={180}>
          <AreaChart data={chartData}>
            <CartesianGrid stroke="#e5ebf4" strokeDasharray="3 3" />
            <XAxis dataKey="month" tickLine={false} axisLine={false} />
            <YAxis tickLine={false} axisLine={false} width={48} />
            <Tooltip formatter={(value) => `${currency.format(Number(value))} грн`} />
            <Area type="monotone" dataKey="price" stroke="#1769e0" strokeWidth={3} fill="#dceaff" />
          </AreaChart>
        </ResponsiveContainer>
      </div>
      <div className="infrastructure-grid">
        <div><span>Школа</span><strong>{apartment.infrastructure?.schoolDistanceMeters ?? "-"} м</strong></div>
        <div><span>Лікарня</span><strong>{apartment.infrastructure?.hospitalDistanceMeters ?? "-"} м</strong></div>
        <div><span>Транспорт</span><strong>{apartment.infrastructure?.transportDistanceMeters ?? "-"} м</strong></div>
        <div><span>Супермаркет</span><strong>{apartment.infrastructure?.supermarketDistanceMeters ?? "-"} м</strong></div>
      </div>
      <div className="map-preview">
        <div className="map-pin"><Home size={22} /></div>
        <span>{apartment.address}</span>
      </div>
    </aside>
  );
}

function ReviewPreviewCard({ apartment, onOpen }: { apartment: Apartment; onOpen: (apartment: Apartment) => void }) {
  return (
    <article className="review-preview-card" onClick={() => onOpen(apartment)}>
      <div className="review-card-image">
        <img src={apartmentPreviewImage(apartment)} alt={apartment.title} onError={(event) => { event.currentTarget.src = apartmentGallery(apartment)[0]; }} />
      </div>
      <div className="review-card-head">
        <div>
          <strong>{apartment.title}</strong>
          <span>{apartment.city}, {apartment.district}</span>
        </div>
        <div className="review-card-badges">
          <span className="source-chip">{sourceLabel(apartment.source)}</span>
          <span className="status-badge pending">Потребує рішення</span>
        </div>
      </div>
      <div className="review-card-meta">
        <span>{formatPrice(apartment.price, apartment.currency)}</span>
        <span>{apartment.rooms} кімн. • {apartment.area} м2 • {apartment.floor}/{apartment.totalFloors} поверх</span>
        <span>Надійність {apartment.trustScore}/100</span>
        <span>Вигідність {apartment.valueScore}/100</span>
      </div>
      <p className="review-card-description">{shortText(apartment.description, 220)}</p>
    </article>
  );
}

function ApartmentDetailPage({
  apartment,
  mode,
  isFavorite,
  onBack,
  onFavorite,
  onReviewStatus
}: {
  apartment: Apartment;
  mode: "catalog" | "review";
  isFavorite: boolean;
  onBack: () => void;
  onFavorite?: (apartment: Apartment) => void;
  onReviewStatus?: (apartmentId: string, status: string) => Promise<void>;
}) {
  const gallery = apartmentGallery(apartment);
  const [activeImage, setActiveImage] = useState(gallery[0]);
  const activeIndex = Math.max(0, gallery.indexOf(activeImage));
  const chartData = apartment.priceHistory?.map((item) => ({
    month: new Date(item.recordedAt).toLocaleDateString("uk-UA", { month: "short" }),
    price: item.price
  })) || [];

  useEffect(() => {
    setActiveImage(gallery[0]);
  }, [apartment.id]);

  function showPrevImage() {
    const nextIndex = activeIndex === 0 ? gallery.length - 1 : activeIndex - 1;
    setActiveImage(gallery[nextIndex]);
  }

  function showNextImage() {
    const nextIndex = activeIndex === gallery.length - 1 ? 0 : activeIndex + 1;
    setActiveImage(gallery[nextIndex]);
  }

  return (
    <main className="detail-page">
      <div className="detail-page-head">
        <button className="secondary-button" onClick={onBack}><ArrowLeft size={18} /> Назад</button>
        <div className="detail-page-actions">
          {mode === "catalog" && onFavorite && (
            <button className="primary-button compact" onClick={() => onFavorite(apartment)}>
              <Heart size={18} fill={isFavorite ? "currentColor" : "none"} />
              {isFavorite ? "В обраному" : "Додати в обране"}
            </button>
          )}
          {mode === "review" && onReviewStatus && (
            <>
              <button className="approve-button action-button" onClick={() => void onReviewStatus(apartment.id, "PUBLISHED")}><Check size={18} /> Опублікувати</button>
              <button className="reject-button action-button" onClick={() => void onReviewStatus(apartment.id, "REJECTED")}><X size={18} /> Відхилити</button>
            </>
          )}
        </div>
      </div>

      <section className="detail-layout">
        <div className="detail-main">
          <div className="detail-hero-image">
            {gallery.length > 1 && (
              <>
                <button className="gallery-arrow left" onClick={showPrevImage} aria-label="Попереднє фото">
                  <ChevronLeft size={20} />
                </button>
                <button className="gallery-arrow right" onClick={showNextImage} aria-label="Наступне фото">
                  <ChevronRight size={20} />
                </button>
              </>
            )}
            <img src={activeImage} alt={apartment.title} />
          </div>
          {gallery.length > 1 && (
            <div className="detail-gallery-strip">
              {gallery.map((image) => (
                <button
                  key={image}
                  className={`detail-thumb ${image === activeImage ? "active" : ""}`}
                  onClick={() => setActiveImage(image)}
                >
                  <img src={image} alt={apartment.title} />
                </button>
              ))}
            </div>
          )}
          <article className="detail-description-card">
            <div className="detail-heading-meta">
              <span className="eyebrow">{mode === "review" ? "Модерація оголошення" : "Повний опис квартири"}</span>
              <span className="source-chip">{sourceLabel(apartment.source)}</span>
            </div>
            <h1>{apartment.title}</h1>
            <p className="detail-location"><MapPin size={16} />{apartment.city}, {apartment.district} • {apartment.address}</p>
            <div className="detail-meta-row">
              <span>{apartment.rooms} кімн.</span>
              <span>{apartment.area} м2</span>
              <span>{apartment.floor}/{apartment.totalFloors} поверх</span>
              <span>{apartment.ownerType}</span>
            </div>
            <div className="score-grid detail-score-grid">
              <ScoreBadge label="Надійність" value={apartment.trustScore} description="перевірка оголошення" tone="green" />
              <ScoreBadge label="Вигідність" value={apartment.valueScore} description="порівняння ціни" tone="blue" />
              <ScoreBadge label="Комфорт" value={apartment.comfortScore} description="умови району" tone="amber" />
            </div>
            <div className="detail-price-line">
              <strong>{formatPrice(apartment.price, apartment.currency)}</strong>
              <span>ринкова ціна: {formatPrice(apartment.marketAveragePrice, apartment.currency)}</span>
            </div>
            <p className="detail-full-description">{apartment.description || "Опис відсутній"}</p>
          </article>
        </div>

        <aside className="detail-sidebar">
          <div className="recommendation-box">
            <strong>Чому система рекомендує цю квартиру:</strong>
            <ul>
              {apartment.recommendationReasons.map((reason) => (
                <li key={reason}><CheckCircle2 size={16} />{reason}</li>
              ))}
            </ul>
          </div>
          <div className="price-chart">
            <div className="mini-heading">
              <strong>Історія ціни</strong>
            </div>
            <ResponsiveContainer width="100%" height={180}>
              <AreaChart data={chartData}>
                <CartesianGrid stroke="#e5ebf4" strokeDasharray="3 3" />
                <XAxis dataKey="month" tickLine={false} axisLine={false} />
                <YAxis tickLine={false} axisLine={false} width={48} />
                <Tooltip formatter={(value) => `${currency.format(Number(value))} грн`} />
                <Area type="monotone" dataKey="price" stroke="#1769e0" strokeWidth={3} fill="#dceaff" />
              </AreaChart>
            </ResponsiveContainer>
          </div>
          <div className="infrastructure-grid detail-infrastructure-grid">
            <div><span>Школа</span><strong>{apartment.infrastructure?.schoolDistanceMeters ?? "-"} м</strong></div>
            <div><span>Лікарня</span><strong>{apartment.infrastructure?.hospitalDistanceMeters ?? "-"} м</strong></div>
            <div><span>Транспорт</span><strong>{apartment.infrastructure?.transportDistanceMeters ?? "-"} м</strong></div>
            <div><span>Супермаркет</span><strong>{apartment.infrastructure?.supermarketDistanceMeters ?? "-"} м</strong></div>
          </div>
        </aside>
      </section>
    </main>
  );
}

function PendingAccount({ user, onLogout }: { user: UserSummary; onLogout: () => Promise<void> }) {
  return (
    <main className="account-page">
      <section className="status-panel">
        <Clock3 size={42} />
        <span className="eyebrow">Обліковий запис створено</span>
        <h1>Вітаємо, {user.name}</h1>
        <p>Ваш профіль очікує схвалення адміністратора. Після схвалення ви зможете додавати квартири в обране та користуватися кабінетом.</p>
        <button className="secondary-button" onClick={onLogout}><LogOut size={18} /> Вийти</button>
      </section>
    </main>
  );
}

function FavoritesPage({ favorites, onSelect, onRemove }: { favorites: Apartment[]; onSelect: (apartment: Apartment) => void; onRemove: (apartment: Apartment) => void }) {
  return (
    <main className="page-section">
      <div className="section-heading">
        <div><span className="eyebrow">Особистий кабінет</span><h1>Обрані квартири</h1></div>
        <div className="trust-summary"><span>Збережено</span><strong>{favorites.length}</strong></div>
      </div>
      {favorites.length === 0 ? (
        <div className="empty-state">
          <Heart size={38} />
          <h2>В обраному поки порожньо</h2>
          <p>Додавайте квартири серцем на сторінці пошуку.</p>
        </div>
      ) : (
        <div className="favorites-grid">
          {favorites.map((apartment) => (
            <ApartmentCard key={apartment.id} apartment={apartment} selected={false} onSelect={onSelect} isFavorite onFavorite={() => onRemove(apartment)} />
          ))}
        </div>
      )}
    </main>
  );
}

function UserAccount({ user, favorites }: { user: UserSummary; favorites: Apartment[] }) {
  return (
    <main className="page-section">
      <div className="profile-header">
        <div className="profile-avatar"><UserRound size={34} /></div>
        <div><span className="eyebrow">Кабінет користувача</span><h1>{user.name}</h1><p>{user.email}</p></div>
        <span className="approved-badge"><Check size={16} /> Схвалено</span>
      </div>
      <div className="dashboard-grid account-stats">
        <article><Heart size={21} /><strong>{favorites.length}</strong><span>обраних квартир</span></article>
        <article><Bell size={21} /><strong>0</strong><span>нових сповіщень</span></article>
        <article><Search size={21} /><strong>0</strong><span>збережених пошуків</span></article>
        <article><ShieldCheck size={21} /><strong>100%</strong><span>профіль підтверджено</span></article>
      </div>
    </main>
  );
}

function AdminConsolePage({
  users,
  sources,
  imports,
  reviewListings,
  tab,
  onTabChange,
  onStatusChange,
  onSourceToggle,
  onRunSync,
  onOlxDebug,
  olxDebug,
  olxDebugBusy,
  adminMessage,
  onReviewStatus,
  onOpenReview
}: {
  users: UserSummary[];
  sources: DataSource[];
  imports: ImportRun[];
  reviewListings: Apartment[];
  tab: "users" | "sources" | "review";
  onTabChange: (tab: "users" | "sources" | "review") => void;
  onStatusChange: (userId: string, status: string) => Promise<void>;
  onSourceToggle: (source: DataSource) => Promise<void>;
  onRunSync: (code: string) => Promise<void>;
  onOlxDebug: () => Promise<void>;
  olxDebug: OlxDebugInfo | null;
  olxDebugBusy: boolean;
  adminMessage: string;
  onReviewStatus: (apartmentId: string, status: string) => Promise<void>;
  onOpenReview: (apartment: Apartment) => void;
}) {
  const pending = users.filter((item) => item.status === "PENDING").length;
  const approved = users.filter((item) => item.status === "APPROVED").length;
  const activeSources = sources.filter((item) => item.enabled).length;
  const latestImportBySource = new Map(imports.map((item) => [item.dataSource?.code || "", item]));

  return (
    <main className="page-section">
      <div className="section-heading">
        <div><span className="eyebrow">Кабінет адміністратора</span><h1>Керування користувачами та імпортом</h1></div>
        <div className="trust-summary"><span>Оголошень на модерації</span><strong>{reviewListings.length}</strong></div>
      </div>
      <div className="admin-summary">
        <StatCard icon={UsersRound} label="всього користувачів" value={users.length} />
        <StatCard icon={Clock3} label="очікують схвалення" value={pending} />
        <StatCard icon={ShieldCheck} label="схвалено" value={approved} />
        <StatCard icon={Database} label="активних джерел" value={activeSources} />
      </div>
      <div className="admin-tabs">
        <button className={tab === "users" ? "active" : ""} onClick={() => onTabChange("users")}>Користувачі</button>
        <button className={tab === "sources" ? "active" : ""} onClick={() => onTabChange("sources")}>Джерела</button>
        <button className={tab === "review" ? "active" : ""} onClick={() => onTabChange("review")}>Модерація</button>
      </div>
      {tab === "users" && (
        <div className="users-table">
          <div className="users-table-head"><span>Користувач</span><span>Дата реєстрації</span><span>Статус</span><span>Дії</span></div>
          {users.map((item) => (
            <div className="user-row" key={item.id}>
              <div><strong>{item.name}</strong><span>{item.email}</span></div>
              <span>{new Date(item.createdAt).toLocaleDateString("uk-UA")}</span>
              <span className={`status-badge ${item.status.toLowerCase()}`}>{item.status === "PENDING" ? "Очікує" : item.status === "APPROVED" ? "Схвалено" : "Відхилено"}</span>
              <div className="row-actions">
                <button className="approve-button" onClick={() => void onStatusChange(item.id, "APPROVED")} aria-label="Схвалити"><Check size={18} /></button>
                <button className="reject-button" onClick={() => void onStatusChange(item.id, "REJECTED")} aria-label="Відхилити"><X size={18} /></button>
              </div>
            </div>
          ))}
        </div>
      )}
      {tab === "sources" && (
        <div className="admin-stack">
          {adminMessage && <div className="admin-notice">{adminMessage}</div>}
          <div className="source-grid">
            {sources.map((source) => (
              <article className="source-card" key={source.code}>
                <div className="source-card-head">
                  <div><strong>{source.name}</strong><span>{source.code}</span></div>
                  <span className={`status-badge ${source.enabled ? "approved" : "pending"}`}>{source.enabled ? "Активне" : "Вимкнене"}</span>
                </div>
                <p>{source.baseUrl}</p>
                <div className="source-meta">
                  <span>{source.code === "OLX" ? "Режим: публічний пошук" : `Ключ: ${source.hasApiKey ? "підключено" : "відсутній"}`}</span>
                  <span>Надійність: {source.reliability}/100</span>
                  <span>Останній статус: {source.lastStatus || "ще не було"}</span>
                </div>
                {source.config?.note && <p className="source-note">{String(source.config.note)}</p>}
                {source.code === "OLX" && latestImportBySource.get("OLX") && (
                  <p className="source-note strong">
                    Останній імпорт OLX: {latestImportBySource.get("OLX")?.publishedCount || 0} опубліковано, {latestImportBySource.get("OLX")?.reviewCount || 0} на модерації, {latestImportBySource.get("OLX")?.receivedCount || 0} отримано з публічного пошуку.
                  </p>
                )}
                {source.code === "OLX" && olxDebug && (
                  <div className="olx-debug-box">
                    <strong>Діагностика OLX</strong>
                    <span>{olxDebug.message}</span>
                    <span>Оголошень у відповіді парсера: {olxDebug.fetchedCount}</span>
                    {olxDebug.fetchedCount === 0 && (
                      <span>Зараз публічний пошук OLX не повернув оголошення за поточним шляхом або містом.</span>
                    )}
                    {olxDebug.advertIds.length > 0 && (
                      <div className="olx-debug-list">
                        {olxDebug.advertIds.map((id, index) => (
                          <span key={`${id}-${index}`}>#{id} — {olxDebug.advertTitles[index] || "без назви"}</span>
                        ))}
                      </div>
                    )}
                  </div>
                )}
                {source.lastError && <p className="source-error">{source.lastError}</p>}
                <div className="source-actions">
                  <button className="secondary-button" onClick={() => void onSourceToggle(source)}>{source.enabled ? "Вимкнути" : "Увімкнути"}</button>
                  {source.code === "OLX" && (
                    <button className="secondary-button" onClick={() => void onOlxDebug()} disabled={olxDebugBusy}>
                      <Search size={16} /> {olxDebugBusy ? "Перевіряємо..." : "Діагностика OLX"}
                    </button>
                  )}
                  <button className="primary-button compact" onClick={() => void onRunSync(source.code)}><RefreshCw size={16} /> Синхронізувати</button>
                </div>
              </article>
            ))}
          </div>
          <div className="users-table import-table">
            <div className="users-table-head"><span>Джерело</span><span>Початок</span><span>Результат</span><span>Підсумок</span></div>
            {imports.map((item) => (
              <div className="user-row" key={item.id}>
                <div><strong>{item.dataSource?.name || item.dataSource?.code}</strong><span>{item.requestedCity || "всі міста"}</span></div>
                <span>{new Date(item.startedAt).toLocaleString("uk-UA")}</span>
                <span className={`status-badge ${String(item.status || "").toLowerCase()}`}>{item.status}</span>
                <div className="import-result"><strong>{item.publishedCount} опубліковано</strong><span>{item.reviewCount} на перевірці, {item.rejectedCount} відхилено</span></div>
              </div>
            ))}
          </div>
        </div>
      )}
      {tab === "review" && (
        <div className="review-grid">
          {reviewListings.length === 0 ? (
            <div className="empty-state">
              <ShieldCheck size={38} />
              <h2>Черга модерації порожня</h2>
              <p>Після імпорту тут з'являться оголошення, які система не змогла схвалити автоматично.</p>
            </div>
          ) : (
            reviewListings.map((item) => (
              <article className="review-card review-preview-card" key={item.id} onClick={() => onOpenReview(item)}>
                <div className="review-card-image">
                  <img src={apartmentPreviewImage(item)} alt={item.title} onError={(event) => { event.currentTarget.src = apartmentPreviewImage({ ...item, imageUrl: "" }); }} />
                </div>
                <div className="review-card-head">
                  <div><strong>{item.title}</strong><span>{item.city}, {item.district}</span></div>
                  <div className="review-card-badges">
                    <span className="source-chip">{sourceLabel(item.source)}</span>
                    <span className="status-badge pending">Потребує рішення</span>
                  </div>
                </div>
                <div className="review-card-meta">
                  <span>{formatPrice(item.price, item.currency)}</span>
                  <span>{item.rooms} кімн. • {item.area} м2 • {item.floor}/{item.totalFloors} поверх</span>
                  <span>Надійність {item.trustScore}/100</span>
                  <span>Вигідність {item.valueScore}/100</span>
                </div>
                <p className="review-card-description">{shortText(item.description, 220)}</p>
                <div className="review-card-actions">
                  <button className="approve-button action-button" onClick={(event) => { event.stopPropagation(); void onReviewStatus(item.id, "PUBLISHED"); }}><Check size={18} /> Опублікувати</button>
                  <button className="reject-button action-button" onClick={(event) => { event.stopPropagation(); void onReviewStatus(item.id, "REJECTED"); }}><X size={18} /> Відхилити</button>
                </div>
              </article>
            ))
          )}
        </div>
      )}
    </main>
  );
}

export default function App() {
  const [filters, setFilters] = useState<SearchFilters>({ city: "", maxPrice: "", rooms: "", currency: "", withoutBroker: "" });
  const [apartments, setApartments] = useState<Apartment[]>([]);
  const [stats, setStats] = useState({ totalListings: 0, verifiedListings: 0, averageSavings: 0, supportedCities: 0 });
  const [selectedApartment, setSelectedApartment] = useState<Apartment | null>(null);
  const [user, setUser] = useState<UserSummary | null>(null);
  const [favorites, setFavorites] = useState<Apartment[]>([]);
  const [adminUsers, setAdminUsers] = useState<UserSummary[]>([]);
  const [adminSources, setAdminSources] = useState<DataSource[]>([]);
  const [adminImports, setAdminImports] = useState<ImportRun[]>([]);
  const [reviewListings, setReviewListings] = useState<Apartment[]>([]);
  const [adminTab, setAdminTab] = useState<"users" | "sources" | "review">("users");
  const [adminMessage, setAdminMessage] = useState("");
  const [olxDebug, setOlxDebug] = useState<OlxDebugInfo | null>(null);
  const [isOlxDebugBusy, setIsOlxDebugBusy] = useState(false);
  const [detailState, setDetailState] = useState<{
    apartment: Apartment;
    mode: "catalog" | "review";
    origin: "search" | "favorites" | "admin";
  } | null>(null);
  const [activeView, setActiveView] = useState<"search" | "favorites" | "account" | "admin">("search");
  const [authMode, setAuthMode] = useState<"login" | "register" | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [catalogMessage, setCatalogMessage] = useState("");

  const isApprovedUser = user?.role === "USER" && user.status === "APPROVED";
  const isAdmin = user?.role === "ADMIN";
  const favoriteIds = useMemo(() => new Set(favorites.map((item) => item.id)), [favorites]);

  useEffect(() => {
    void getStats()
      .then((result) => setStats(result.data))
      .catch(() => setCatalogMessage("Не вдалося завантажити статистику з бекенду."));
    void getApartments({})
      .then((result) => {
        setApartments(result.data);
        setSelectedApartment(result.data[0] || null);
        setCatalogMessage(result.data.length ? "" : "У каталозі ще немає опублікованих квартир. Запусти синхронізацію в адмінці, щоб підтягнути реальні оголошення.");
      })
      .catch(() => setCatalogMessage("РќРµ РІРґР°Р»РѕСЃСЏ Р·Р°РІР°РЅС‚Р°Р¶РёС‚Рё РєРІР°СЂС‚РёСЂРё Р· Р±РµРєРµРЅРґСѓ. РџРµСЂРµРІС–СЂ, С‡Рё Spring-СЃРµСЂРІРµСЂ Р·Р°РїСѓС‰РµРЅРёР№ С– С‡Рё `VITE_API_URL` РІРєР°Р·СѓС” РЅР° РЅСЊРѕРіРѕ."));

    if (localStorage.getItem("homesafe_token")) {
      void getCurrentUser().then((result) => handleAuthenticatedUser(result.data)).catch(() => localStorage.removeItem("homesafe_token"));
    }
  }, []);

  async function loadAdminData() {
    const [usersResult, sourcesResult, importsResult, reviewResult] = await Promise.all([
      getAdminUsers(),
      getAdminSources(),
      getAdminImports(),
      getAdminReviewListings()
    ]);
    setAdminUsers(usersResult.data);
    setAdminSources(sourcesResult.data);
    setAdminImports(importsResult.data);
    setReviewListings(reviewResult.data);
  }

  async function handleAuthenticatedUser(nextUser: UserSummary) {
    setUser(nextUser);
    if (nextUser.status !== "APPROVED") return;
    if (nextUser.role === "ADMIN") {
      await loadAdminData();
      setActiveView("admin");
      return;
    }
    const result = await getFavorites();
    setFavorites(result.data);
  }

  async function handleSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setIsLoading(true);
    try {
      const result = await getApartments(filters);
      setApartments(result.data);
      setSelectedApartment(result.data[0] || null);
      setCatalogMessage(result.data.length ? "" : "За цими фільтрами реальних квартир поки не знайдено.");
    } catch (error) {
      setCatalogMessage(getErrorMessage(error));
    } finally {
      setIsLoading(false);
    }
  }

  async function handleFavorite(apartment: Apartment) {
    if (!user) {
      setAuthMode("login");
      return;
    }
    if (!isApprovedUser) {
      setActiveView("account");
      return;
    }
    if (favoriteIds.has(apartment.id)) {
      await removeFavorite(apartment.id);
      setFavorites((items) => items.filter((item) => item.id !== apartment.id));
    } else {
      await addFavorite(apartment.id);
      setFavorites((items) => [apartment, ...items]);
    }
  }

  async function handleLogout() {
    try {
      await logoutUser();
    } catch {
      // Local session cleanup is enough here.
    }
    localStorage.removeItem("homesafe_token");
    setUser(null);
    setFavorites([]);
    setAdminUsers([]);
    setAdminSources([]);
    setAdminImports([]);
    setReviewListings([]);
    setDetailState(null);
    setActiveView("search");
  }

  async function handleStatusChange(userId: string, status: string) {
    const result = await updateUserStatus(userId, status);
    setAdminUsers((items) => items.map((item) => item.id === userId ? result.data : item));
  }

  async function handleSourceToggle(source: DataSource) {
    const result = await updateAdminSource(source.code, { enabled: !source.enabled });
    setAdminSources((items) => items.map((item) => item.code === source.code ? result.data : item));
  }

  async function handleRunSync(code: string) {
    const source = adminSources.find((item) => item.code === code);
    const payload = { ...(source?.config?.syncDefaults || {}) };
    try {
      setAdminMessage(code === "OLX" ? "Запускаю синхронізацію OLX..." : `Запускаю синхронізацію ${code}...`);
      const result = await runAdminSync(code, payload);
      await loadAdminData();
      if (code === "OLX" && result.data.receivedCount === 0) {
        setAdminMessage("OLX синхронізація завершилась, але оголошення не знайдені. Запускаю діагностику...");
        await handleOlxDebug();
        return;
      }
      setAdminMessage(
        `${code} синхронізація завершена: ${result.data.receivedCount} отримано, ${result.data.reviewCount} на модерації, ${result.data.publishedCount} опубліковано, ${result.data.rejectedCount} відхилено.`
      );
    } catch (error) {
      setAdminMessage(`Помилка синхронізації ${code}: ${getErrorMessage(error)}`);
      if (code === "OLX") {
        setAdminTab("sources");
      }
      setAdminSources((items) => items.map((item) => (
        item.code === code
          ? { ...item, lastStatus: "FAILED", lastError: getErrorMessage(error), enabled: source?.enabled ?? item.enabled }
          : item
      )));
    }
  }

  async function handleOlxDebug() {
    setIsOlxDebugBusy(true);
    try {
      const result = await getOlxDebugInfo();
      setOlxDebug(result.data);
      if (result.data.fetchedCount === 0) {
        setAdminMessage("OLX парсер не знайшов оголошень за поточним шляхом пошуку. Дивись діагностику нижче.");
      } else {
        setAdminMessage(`OLX парсер повернув ${result.data.fetchedCount} оголошень. Вони вже передаються в ту саму модерацію, що й DIM.RIA.`);
      }
    } catch (error) {
      setAdminMessage(`Не вдалося отримати діагностику OLX: ${getErrorMessage(error)}`);
    } finally {
      setIsOlxDebugBusy(false);
    }
  }

  async function handleReviewStatus(apartmentId: string, status: string) {
    await updateAdminListingStatus(apartmentId, status);
    setReviewListings((items) => items.filter((item) => item.id !== apartmentId));
    setDetailState(null);
    if (status === "PUBLISHED") {
      const result = await getApartments(filters);
      setApartments(result.data);
      setCatalogMessage(result.data.length ? "" : "У каталозі ще немає опублікованих квартир.");
      const publishedApartment = result.data.find((item) => item.id === apartmentId);
      setSelectedApartment(publishedApartment || result.data[0] || null);
      setActiveView("search");
      return;
    }
    setActiveView("admin");
  }

  function openApartment(apartment: Apartment) {
    setSelectedApartment(apartment);
    setDetailState({ apartment, mode: "catalog", origin: activeView === "favorites" ? "favorites" : "search" });
  }

  function openReviewApartment(apartment: Apartment) {
    setAdminTab("review");
    setDetailState({ apartment, mode: "review", origin: "admin" });
  }

  function closeDetail() {
    setDetailState(null);
  }

  if (user && user.status !== "APPROVED") {
    return <PendingAccount user={user} onLogout={handleLogout} />;
  }

  return (
    <div className="app-shell">
      <header className="topbar">
        <button className="brand brand-button" onClick={() => setActiveView("search")} aria-label="HomeSafe">
          <span><ShieldCheck size={24} /></span>HomeSafe
        </button>
        <nav className="nav-links" aria-label="Основна навігація">
          <button onClick={() => setActiveView("search")}>Квартири</button>
          {isApprovedUser && <button onClick={() => setActiveView("favorites")}>Обране ({favorites.length})</button>}
          {isApprovedUser && <button onClick={() => setActiveView("account")}>Мій кабінет</button>}
          {isAdmin && <button onClick={() => setActiveView("admin")}>Адміністратор</button>}
        </nav>
        <div className="topbar-actions">
          {!user ? (
            <>
              <button className="text-button" onClick={() => setAuthMode("login")}>Увійти</button>
              <button className="primary-button compact" onClick={() => setAuthMode("register")}>Реєстрація</button>
            </>
          ) : (
            <>
              <span className="user-chip"><UserRound size={17} /> {user.name}</span>
              <button className="icon-button" onClick={() => void handleLogout()} aria-label="Вийти"><LogOut size={19} /></button>
            </>
          )}
        </div>
      </header>
      {activeView === "favorites" && <FavoritesPage favorites={favorites} onSelect={openApartment} onRemove={handleFavorite} />}
      {activeView === "account" && user && <UserAccount user={user} favorites={favorites} />}
      {activeView === "admin" && (
        <AdminConsolePage
          users={adminUsers}
          sources={adminSources}
          imports={adminImports}
          reviewListings={reviewListings}
          tab={adminTab}
          onTabChange={setAdminTab}
          onStatusChange={handleStatusChange}
          onSourceToggle={handleSourceToggle}
          onRunSync={handleRunSync}
          onOlxDebug={handleOlxDebug}
          olxDebug={olxDebug}
          olxDebugBusy={isOlxDebugBusy}
          adminMessage={adminMessage}
          onReviewStatus={handleReviewStatus}
          onOpenReview={openReviewApartment}
        />
      )}
      {activeView === "search" && (
        <main id="top">
          <section className="hero-section">
            <div className="hero-copy">
              <span className="eyebrow">Житло без посередників для ВПО та біженців</span>
              <h1>Знайдіть безпечну квартиру з поясненням, чому вона вигідна</h1>
              <p>HomeSafe перевіряє ризики шахрайства, порівнює ціни та показує найкращі варіанти для реального життя.</p>
            </div>
            <form className="search-panel" onSubmit={handleSearch}>
              <label>Місто<input value={filters.city} onChange={(event) => setFilters({ ...filters, city: event.target.value })} placeholder="Київ, Львів, Дніпро" /></label>
              <label>Бюджет до<input type="number" value={filters.maxPrice} onChange={(event) => setFilters({ ...filters, maxPrice: event.target.value })} placeholder="15000" /></label>
              <label>Кімнат<select value={filters.rooms} onChange={(event) => setFilters({ ...filters, rooms: event.target.value })}><option value="">Будь-яка</option><option value="1">1</option><option value="2">2</option><option value="3">3+</option></select></label>
              <label>Валюта<select value={filters.currency} onChange={(event) => setFilters({ ...filters, currency: event.target.value })}><option value="">Усі валюти</option><option value="UAH">Гривня</option><option value="USD">Долар США</option><option value="EUR">Євро</option></select></label>
              <label>Тип оголошення<select value={filters.withoutBroker} onChange={(event) => setFilters({ ...filters, withoutBroker: event.target.value })}><option value="">Усі оголошення</option><option value="true">Лише без посередників</option><option value="false">Потрібна перевірка власника</option></select></label>
              <button type="submit"><Search size={19} />{isLoading ? "Шукаємо..." : "Знайти житло"}</button>
            </form>
          </section>
          <section className="stats-section">
            <StatCard icon={Building2} label="оголошень у системі" value={currency.format(stats.totalListings)} />
            <StatCard icon={ShieldCheck} label="перевірених квартир" value={currency.format(stats.verifiedListings)} />
            <StatCard icon={SlidersHorizontal} label="середня економія" value={`${stats.averageSavings}%`} />
            <StatCard icon={Train} label="міст підтримується" value={stats.supportedCities} />
          </section>
          <section className="workspace-section" id="apartments">
            <div className="section-heading">
              <div><span className="eyebrow">Рекомендовані варіанти</span><h2>Квартири, відсортовані за довірою та вигідністю</h2></div>
              {!user && <button className="secondary-button" onClick={() => setAuthMode("register")}><UserRound size={18} /> Створити кабінет</button>}
            </div>
            <div className="content-grid">
              <div className="apartment-list">
                {catalogMessage && apartments.length === 0 ? (
                  <div className="empty-state">
                    <Home size={38} />
                    <h2>Реальних квартир поки немає</h2>
                    <p>{catalogMessage}</p>
                  </div>
                ) : (
                  apartments.map((apartment) => (
                    <ApartmentCard
                      key={apartment.id}
                      apartment={apartment}
                      selected={selectedApartment?.id === apartment.id}
                      onSelect={openApartment}
                      isFavorite={favoriteIds.has(apartment.id)}
                      onFavorite={handleFavorite}
                    />
                  ))
                )}
              </div>
              <DetailsPanel apartment={selectedApartment} />
            </div>
          </section>
        </main>
      )}
      {authMode && <AuthModal initialMode={authMode} onClose={() => setAuthMode(null)} onLogin={handleAuthenticatedUser} />}
      {detailState && (
        <div className="detail-overlay">
          <ApartmentDetailPage
            apartment={detailState.apartment}
            mode={detailState.mode}
            isFavorite={favoriteIds.has(detailState.apartment.id)}
            onBack={closeDetail}
            onFavorite={handleFavorite}
            onReviewStatus={handleReviewStatus}
          />
        </div>
      )}
    </div>
  );
}
