import type {
  ApiResponse,
  Apartment,
  DataSource,
  ImportRun,
  LoginPayload,
  OlxDebugInfo,
  PlatformStats,
  RegisterPayload,
  SearchFilters,
  UserSummary
} from "./types";

const API_URL =
  import.meta.env.VITE_API_URL ||
  (window.location.hostname.includes("ngrok-free.")
    ? "http://127.0.0.1:8080/api"
    : `${window.location.protocol}//${window.location.hostname}:8080/api`);

async function request<T>(path: string, options: RequestInit = {}, authTokenOverride?: string | null): Promise<ApiResponse<T>> {
  const token = authTokenOverride ?? localStorage.getItem("homesafe_token");
  const response = await fetch(`${API_URL}${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers
    }
  });

  if (response.status === 204) {
    return { data: undefined as T };
  }

  const body = (await response.json()) as ApiResponse<T> & { message?: string };
  if (!response.ok) {
    throw new Error(body.message || "Помилка запиту");
  }
  return body;
}

export async function getApartments(filters: SearchFilters | Record<string, string | number | boolean> = {}): Promise<ApiResponse<Apartment[]>> {
  const params = new URLSearchParams();

  Object.entries(filters).forEach(([key, value]) => {
    if (value !== "" && value !== undefined && value !== null) {
      params.set(key, String(value));
    }
  });

  const response = await fetch(`${API_URL}/apartments?${params.toString()}`);
  if (!response.ok) throw new Error("Could not load apartments");
  return response.json() as Promise<ApiResponse<Apartment[]>>;
}

export async function getStats(): Promise<ApiResponse<PlatformStats>> {
  const response = await fetch(`${API_URL}/stats`);
  if (!response.ok) throw new Error("Could not load platform statistics");
  return response.json() as Promise<ApiResponse<PlatformStats>>;
}

export function registerUser(data: RegisterPayload): Promise<ApiResponse<never>> {
  return request("/auth/register", {
    method: "POST",
    body: JSON.stringify(data)
  });
}

export function loginUser(data: LoginPayload): Promise<ApiResponse<{ token: string; user: UserSummary }>> {
  return request("/auth/login", {
    method: "POST",
    body: JSON.stringify(data)
  });
}

export function getCurrentUser(): Promise<ApiResponse<UserSummary>> {
  return request("/auth/me");
}

export function logoutUser(): Promise<ApiResponse<never>> {
  return request("/auth/logout", { method: "POST" });
}

export function getFavorites(): Promise<ApiResponse<Apartment[]>> {
  return request("/favorites");
}

export function addFavorite(apartmentId: string): Promise<ApiResponse<never>> {
  return request(`/favorites/${apartmentId}`, { method: "POST" });
}

export function removeFavorite(apartmentId: string): Promise<ApiResponse<never>> {
  return request(`/favorites/${apartmentId}`, { method: "DELETE" });
}

export function getAdminUsers(): Promise<ApiResponse<UserSummary[]>> {
  return request("/admin/users");
}

export function updateUserStatus(userId: string, status: string): Promise<ApiResponse<UserSummary>> {
  return request(`/admin/users/${userId}/status`, {
    method: "PATCH",
    body: JSON.stringify({ status })
  });
}

export function getAdminSources(): Promise<ApiResponse<DataSource[]>> {
  return request("/admin/sources");
}

export function updateAdminSource(code: string, data: Record<string, unknown>): Promise<ApiResponse<DataSource>> {
  return request(`/admin/sources/${code}`, {
    method: "PATCH",
    body: JSON.stringify(data)
  });
}

export function runAdminSync(code: string, data: Record<string, unknown>): Promise<ApiResponse<ImportRun>> {
  return request(`/admin/sources/${code}/sync`, {
    method: "POST",
    body: JSON.stringify(data)
  });
}

export function getOlxAuthUrl(redirectUri: string, state?: string): Promise<ApiResponse<{ url: string }>> {
  const params = new URLSearchParams({ redirectUri });
  if (state) {
    params.set("state", state);
  }
  return request(`/admin/olx/auth-url?${params.toString()}`);
}

export function exchangeOlxCode(data: {
  code: string;
  redirectUri: string;
  clientId?: string;
  clientSecret?: string;
}, authTokenOverride?: string | null): Promise<ApiResponse<{
  accessToken: string;
  refreshToken: string;
  expiresIn?: number | null;
  scope?: string | null;
  tokenType?: string | null;
}>> {
  return request("/admin/olx/exchange-code", {
    method: "POST",
    body: JSON.stringify(data)
  }, authTokenOverride);
}

export function getOlxDebugInfo(): Promise<ApiResponse<OlxDebugInfo>> {
  return request("/admin/olx/debug");
}

export function getAdminImports(code?: string): Promise<ApiResponse<ImportRun[]>> {
  const suffix = code ? `?code=${encodeURIComponent(code)}` : "";
  return request(`/admin/imports${suffix}`);
}

export function getAdminReviewListings(): Promise<ApiResponse<Apartment[]>> {
  return request("/admin/listings/review");
}

export function updateAdminListingStatus(apartmentId: string, status: string): Promise<ApiResponse<Apartment>> {
  return request(`/admin/listings/${apartmentId}/status`, {
    method: "PATCH",
    body: JSON.stringify({ status })
  });
}
