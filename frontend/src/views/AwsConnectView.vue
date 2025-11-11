<template>
  <div class="view">
    <section class="panel">
      <header class="panel__header">
        <div>
          <h2>AWS Connect</h2>
          <p>Browse objects stored in the configured S3 bucket.</p>
        </div>
        <button class="button" :disabled="isLoading" @click="loadFiles">
          <span v-if="!isLoading">Refresh</span>
          <span v-else>Loading…</span>
        </button>
      </header>

      <div v-if="isLoading" class="status">Loading S3 objects…</div>
      <p v-else-if="error" class="error">{{ error }}</p>
      <div v-else-if="!files.length" class="status">No objects found in the bucket.</div>
      <div v-else class="sections">
        <section class="subsection">
          <header class="subsection__header">
            <h3>Kunden</h3>
            <span class="badge">{{ customerFiles.length }}</span>
          </header>
          <p v-if="!customerFiles.length" class="status">Keine Kunden-Dateien gefunden.</p>
          <table v-else class="table">
            <thead>
              <tr>
                <th>Key</th>
                <th>Size</th>
                <th>Last Modified</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="file in customerFiles" :key="file.key">
                <td class="key">{{ file.key }}</td>
                <td>{{ formatSize(file.size) }}</td>
                <td>{{ formatTimestamp(file.lastModified) }}</td>
                <td class="actions">
                  <button class="button button--ghost" @click="downloadFile(file.key)">Download</button>
                </td>
              </tr>
            </tbody>
          </table>
        </section>

        <section class="subsection">
          <header class="subsection__header">
            <h3>Aufträge</h3>
            <span class="badge">{{ orderFiles.length }}</span>
          </header>
          <p v-if="!orderFiles.length" class="status">Keine Aufträge gefunden.</p>
          <table v-else class="table">
            <thead>
              <tr>
                <th>Key</th>
                <th>Size</th>
                <th>Last Modified</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="file in orderFiles" :key="file.key">
                <td class="key">{{ file.key }}</td>
                <td>{{ formatSize(file.size) }}</td>
                <td>{{ formatTimestamp(file.lastModified) }}</td>
                <td class="actions">
                  <button class="button button--ghost" @click="downloadFile(file.key)">Download</button>
                </td>
              </tr>
            </tbody>
          </table>
        </section>

        <section v-if="otherFiles.length" class="subsection">
          <header class="subsection__header">
            <h3>Weitere Dateien</h3>
            <span class="badge">{{ otherFiles.length }}</span>
          </header>
          <table class="table">
            <thead>
              <tr>
                <th>Key</th>
                <th>Size</th>
                <th>Last Modified</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="file in otherFiles" :key="file.key">
                <td class="key">{{ file.key }}</td>
                <td>{{ formatSize(file.size) }}</td>
                <td>{{ formatTimestamp(file.lastModified) }}</td>
                <td class="actions">
                  <button class="button button--ghost" @click="downloadFile(file.key)">Download</button>
                </td>
              </tr>
            </tbody>
          </table>
        </section>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import dayjs from 'dayjs';
import { computed, onMounted, ref } from 'vue';

import { s3Service, type S3ObjectMetadata } from '../services/api';

const files = ref<S3ObjectMetadata[]>([]);
const isLoading = ref(false);
const error = ref('');

const orderFiles = computed(() =>
  files.value.filter((file) => file.key.startsWith('auftraege/'))
);
const customerFiles = computed(() =>
  files.value.filter((file) => file.key.startsWith('kunde/'))
);
const otherFiles = computed(() =>
  files.value.filter(
    (file) => !file.key.startsWith('auftraege/') && !file.key.startsWith('kunde/')
  )
);

const loadFiles = async () => {
  try {
    isLoading.value = true;
    error.value = '';
    const response = await s3Service.listFiles();
    files.value = response.data;
  } catch (err) {
    error.value = getErrorMessage(err);
  } finally {
    isLoading.value = false;
  }
};

const downloadFile = async (key: string) => {
  try {
    error.value = '';
    const response = await s3Service.downloadFile(key);

    const blob = new Blob([response.data], { type: response.headers['content-type'] ?? 'text/csv;charset=utf-8;' });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    const fileName = key.split('/').pop() ?? 'download.csv';

    link.href = url;
    link.setAttribute('download', fileName);
    document.body.appendChild(link);
    link.click();

    link.remove();
    window.URL.revokeObjectURL(url);
  } catch (err) {
    console.error(err);
    error.value = getErrorMessage(err);
  }
};

const formatTimestamp = (value: string) => {
  return dayjs(value).format('YYYY-MM-DD HH:mm:ss');
};

const formatSize = (size: number) => {
  if (size < 1024) {
    return `${size} B`;
  }
  if (size < 1024 * 1024) {
    return `${(size / 1024).toFixed(1)} KB`;
  }
  if (size < 1024 * 1024 * 1024) {
    return `${(size / 1024 / 1024).toFixed(1)} MB`;
  }
  return `${(size / 1024 / 1024 / 1024).toFixed(1)} GB`;
};

const getErrorMessage = (err: unknown) => {
  if (typeof err === 'string') return err;
  if (err && typeof err === 'object' && 'response' in err) {
    const rsp = err as { response?: { data?: { message?: string }; statusText?: string } };
    return rsp.response?.data?.message ?? rsp.response?.statusText ?? 'Request failed';
  }
  return 'Unexpected error occurred';
};

onMounted(loadFiles);
</script>

<style scoped>
.view {
  display: grid;
  gap: 1.5rem;
}

.panel {
  background: white;
  padding: 1.5rem;
  border-radius: 12px;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.08);
  display: grid;
  gap: 1.25rem;
}

.panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

.status {
  color: #475569;
}

.error {
  color: #b91c1c;
  font-weight: 600;
}

.sections {
  display: grid;
  gap: 2rem;
}

.subsection {
  display: grid;
  gap: 1rem;
}

.subsection__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
}

.badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0.2rem 0.75rem;
  border-radius: 999px;
  background: #eff6ff;
  color: #1d4ed8;
  font-weight: 600;
  font-size: 0.85rem;
}

.table {
  width: 100%;
  border-collapse: collapse;
}

.table th,
.table td {
  text-align: left;
  padding: 0.75rem;
  border-bottom: 1px solid #e5e7eb;
}

.table th {
  font-weight: 600;
}

.key {
  font-family: 'Fira Code', 'Courier New', Courier, monospace;
  font-size: 0.95rem;
}

.actions {
  text-align: right;
}

.button {
  border: none;
  background: #2563eb;
  color: white;
  padding: 0.6rem 1.1rem;
  border-radius: 8px;
  font-weight: 600;
}

.button--ghost {
  background: transparent;
  border: 1px solid #cbd5f5;
  color: #1f2937;
}

.button[disabled] {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
