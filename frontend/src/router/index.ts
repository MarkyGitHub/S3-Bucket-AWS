import {
  createRouter,
  createWebHistory,
  type RouteRecordRaw,
} from "vue-router";

import AwsConnectView from "../views/AwsConnectView.vue";
import DataView from "../views/DataView.vue";
import SettingsView from "../views/SettingsView.vue";
import SynchronizeView from "../views/SynchronizeView.vue";

const routes: RouteRecordRaw[] = [
  {
    path: "/",
    redirect: { name: "sync-overview" },
  },
  {
    path: "/sync",
    name: "sync-overview",
    component: SynchronizeView,
    meta: {
      endpoints: [
        "GET /api/sync/state",
        "GET /api/sync/runs",
        "POST /api/sync/run",
      ],
    },
  },
  {
    path: "/data",
    name: "data",
    component: DataView,
    meta: {
      endpoints: ["GET /api/customers", "GET /api/orders"],
    },
  },
  {
    path: "/settings",
    name: "settings",
    component: SettingsView,
    meta: {
      endpoints: [
        "GET /api/sync/schedule",
        "PUT /api/sync/schedule",
        "POST /api/orders/lastchange/touch",
      ],
    },
  },
  {
    path: "/s3",
    name: "s3-browser",
    component: AwsConnectView,
    meta: {
      endpoints: ["GET /api/s3/files", "GET /api/s3/files/{key}"],
    },
  },
  {
    path: "/aws",
    redirect: { name: "s3-browser" },
  },
];

export const router = createRouter({
  history: createWebHistory(),
  routes,
});
