import axios, { type AxiosRequestConfig, type AxiosResponse } from "axios";

const baseURL = import.meta.env.VITE_API_BASE_URL ?? "";

export const api = axios.create({
  baseURL,
});

export interface S3ObjectMetadata {
  key: string;
  size: number;
  lastModified: string;
  contentType?: string | null;
}

export interface Customer {
  id: string;
  firstName: string;
  lastName: string;
  country: string;
  [key: string]: unknown;
}

export interface OrderSummary {
  id: string;
  articleNumber: string;
  created?: string | null;
  lastChange?: string | null;
  customer?: Partial<Customer> | null;
  customerId?: string | null;
  kundeid?: string | null;
  [key: string]: unknown;
}

export interface OrderUpdateResponse {
  updatedRows: number;
  appliedTimestamp: string;
}

export interface SyncState {
  tableName: string;
  lastSuccessfulSync: string | null;
}

export interface SyncRunItem {
  tableName: string;
  country: string;
  objectCount: number;
  s3Key: string;
}

export interface SyncRun {
  id: number | null;
  startedAt: string;
  finishedAt: string | null;
  status: string;
  errorMessage: string | null;
  items: SyncRunItem[];
}

export interface SyncScheduleResponse {
  hours: number;
  minutes: number;
  intervalSeconds: number;
  isoDuration: string;
  schedulerEnabled: boolean;
}

export interface SyncScheduleRequest {
  hours: number;
  minutes: number;
}

const endpointBase = {
  s3: "/api/s3",
  customers: "/api/customers",
  orders: "/api/orders",
  sync: "/api/sync",
} as const;

export const endpoints = {
  s3: {
    listFiles: `${endpointBase.s3}/files`,
    file: (key: string) =>
      `${endpointBase.s3}/files?key=${encodeURIComponent(key)}`,
  },
  customers: endpointBase.customers,
  orders: {
    list: endpointBase.orders,
    touchLastChange: `${endpointBase.orders}/lastchange/touch`,
  },
  sync: {
    run: `${endpointBase.sync}/run`,
    runs: `${endpointBase.sync}/runs`,
    state: `${endpointBase.sync}/state`,
  },
  syncSchedule: `${endpointBase.sync}/schedule`,
} as const;

export const s3Service = {
  listFiles(): Promise<AxiosResponse<S3ObjectMetadata[]>> {
    return api.get(endpoints.s3.listFiles);
  },
  downloadFile(
    key: string,
    config?: AxiosRequestConfig
  ): Promise<AxiosResponse<Blob>> {
    return api.get(endpoints.s3.file(key), {
      responseType: "blob",
      ...config,
    });
  },
};

export const customerService = {
  listCustomers(): Promise<AxiosResponse<Customer[]>> {
    return api.get(endpoints.customers);
  },
};

export const orderService = {
  listOrders(): Promise<AxiosResponse<OrderSummary[]>> {
    return api.get(endpoints.orders.list);
  },
  touchLastChange(): Promise<AxiosResponse<OrderUpdateResponse>> {
    return api.post(endpoints.orders.touchLastChange);
  },
};

export const syncService = {
  triggerRun(): Promise<AxiosResponse<SyncRun>> {
    return api.post(endpoints.sync.run);
  },
  listRuns(limit = 10): Promise<AxiosResponse<SyncRun[]>> {
    return api.get(endpoints.sync.runs, {
      params: { limit },
    });
  },
  listStates(): Promise<AxiosResponse<SyncState[]>> {
    return api.get(endpoints.sync.state);
  },
};

export const syncScheduleService = {
  getSchedule(): Promise<AxiosResponse<SyncScheduleResponse>> {
    return api.get(endpoints.syncSchedule);
  },
  updateSchedule(
    request: SyncScheduleRequest
  ): Promise<AxiosResponse<SyncScheduleResponse>> {
    return api.put(endpoints.syncSchedule, request);
  },
};
