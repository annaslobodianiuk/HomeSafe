export type FraudRisk = "LOW" | "MEDIUM" | "HIGH";
export type UserRole = "USER" | "ADMIN";
export type UserStatus = "PENDING" | "APPROVED" | "REJECTED";
export type CatalogStatus = "PUBLISHED" | "REJECTED" | "REVIEW";
export type CurrencyCode = "UAH" | "USD" | "EUR";

export interface PriceHistoryPoint {
  recordedAt: string;
  price: number;
}

export interface Infrastructure {
  schoolDistanceMeters: number;
  hospitalDistanceMeters: number;
  transportDistanceMeters: number;
  supermarketDistanceMeters: number;
  kindergartenDistanceMeters: number;
}

export interface Apartment {
  id: string;
  title: string;
  city: string;
  district: string;
  address: string;
  price: number;
  marketAveragePrice: number;
  rooms: number;
  area: number;
  floor: number;
  totalFloors: number;
  imageUrl: string;
  images: string[];
  description?: string;
  trustScore: number;
  valueScore: number;
  comfortScore: number;
  fraudRisk: FraudRisk;
  isVerified: boolean;
  isWithoutBroker: boolean;
  savingsPercent: number;
  ownerType: string;
  documents: string;
  source?: string;
  currency: CurrencyCode;
  recommendationReasons: string[];
  riskFactors: string[];
  infrastructure?: Infrastructure;
  priceHistory?: PriceHistoryPoint[];
}

export interface PlatformStats {
  totalListings: number;
  verifiedListings: number;
  averageSavings: number;
  supportedCities: number;
}

export interface UserSummary {
  id: string;
  name: string;
  email: string;
  role: UserRole;
  status: UserStatus;
  createdAt: string;
}

export interface RegisterPayload {
  name: string;
  email: string;
  password: string;
}

export interface LoginPayload {
  email: string;
  password: string;
}

export interface SourceConfig {
  syncDefaults?: Record<string, string | number | boolean>;
  note?: string;
  [key: string]: unknown;
}

export interface DataSource {
  id: string;
  code: string;
  name: string;
  baseUrl: string;
  apiKeyEnv: string;
  enabled: boolean;
  reliability: number;
  config?: SourceConfig;
  lastSyncAt?: string | null;
  lastStatus?: string | null;
  lastError?: string | null;
  hasApiKey: boolean;
}

export interface ImportRun {
  id: string;
  status: string;
  requestedCity?: string | null;
  receivedCount: number;
  normalizedCount: number;
  publishedCount: number;
  reviewCount: number;
  rejectedCount: number;
  duplicateCount: number;
  errorMessage?: string | null;
  startedAt: string;
  finishedAt?: string | null;
  dataSource?: { code: string; name: string };
}

export interface OlxDebugInfo {
  accountName?: string | null;
  accountEmail?: string | null;
  fetchedCount: number;
  advertIds: string[];
  advertTitles: string[];
  message: string;
}

export interface ApiResponse<T> {
  data: T;
  message?: string;
}

export interface SearchFilters {
  city: string;
  maxPrice: string;
  rooms: string;
  currency: string;
  withoutBroker: string;
}
