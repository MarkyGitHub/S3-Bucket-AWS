<template>
  <div class="app">
    <aside class="sidebar">
      <h1 class="sidebar__title">S3 Sync Dashboard</h1>
      <p class="sidebar__subtitle">Monitor customer and order exports to S3</p>
      <nav class="menu">
        <RouterLink v-for="item in menuItems" :key="item.to" :to="item.to" class="menu__link">
          {{ item.label }}
        </RouterLink>
      </nav>
    </aside>
    <main class="content">
      <RouterView />
    </main>
  </div>
</template>

<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router';

const menuItems = [
  { to: '/', label: 'Synchronize' },
  { to: '/data', label: 'Data View' },
  { to: '/settings', label: 'Settings' }
];
</script>

<style scoped>
.app {
  display: grid;
  grid-template-columns: 260px 1fr;
  min-height: 100vh;
  background: #f5f7fa;
}

.sidebar {
  padding: 2rem 1.5rem;
  background: #0f172a;
  color: #e2e8f0;
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.sidebar__title {
  margin: 0;
  font-size: 1.6rem;
}

.sidebar__subtitle {
  margin: 0;
  color: #94a3b8;
  font-size: 0.95rem;
}

.menu {
  display: grid;
  gap: 0.75rem;
}

.menu__link {
  display: block;
  padding: 0.75rem 1rem;
  border-radius: 10px;
  color: inherit;
  text-decoration: none;
  font-weight: 600;
  transition: background 0.2s ease, color 0.2s ease;
}

.menu__link:hover {
  background: rgba(148, 163, 184, 0.2);
}

.menu__link.router-link-active {
  background: #2563eb;
  color: #f8fafc;
}

.content {
  padding: 2.5rem 2rem 3rem;
  max-width: 960px;
  width: 100%;
  margin: 0 auto;
}

@media (max-width: 900px) {
  .app {
    grid-template-columns: 1fr;
  }

  .sidebar {
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
    gap: 1rem;
    padding: 1.5rem;
  }

  .menu {
    grid-auto-flow: column;
    gap: 0.5rem;
  }

  .menu__link {
    padding: 0.6rem 0.9rem;
  }
}

@media (max-width: 640px) {
  .sidebar {
    flex-direction: column;
    align-items: flex-start;
  }

  .menu {
    width: 100%;
  }

  .menu__link {
    width: 100%;
  }
}
</style>
