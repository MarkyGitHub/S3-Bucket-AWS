import { createRouter, createWebHistory } from "vue-router";

import DataView from "../views/DataView.vue";
import SettingsView from "../views/SettingsView.vue";
import SynchronizeView from "../views/SynchronizeView.vue";

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: "/",
      name: "synchronize",
      component: SynchronizeView,
    },
    {
      path: "/data",
      name: "data",
      component: DataView,
    },
    {
      path: "/settings",
      name: "settings",
      component: SettingsView,
    },
  ],
});
