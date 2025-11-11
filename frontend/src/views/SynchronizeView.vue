<template>
    <div class="view">
        <section class="panel">
            <header class="panel__header">
                <div>
                    <h2>Sync Overview</h2>
                    <p>Last known sync timestamp per table.</p>
                </div>
                <div class="actions">
                    <button class="button" :disabled="isTriggering" @click="triggerSync">
                        <span v-if="!isTriggering">Run Sync Now</span>
                        <span v-else>Running…</span>
                    </button>
                    <button class="button button--ghost" :disabled="isLoading" @click="refresh">
                        Refresh
                    </button>
                </div>
            </header>
            <table class="table" v-if="states.length">
                <thead>
                    <tr>
                        <th>Table</th>
                        <th>Last Successful Sync</th>
                    </tr>
                </thead>
                <tbody>
                    <tr v-for="state in states" :key="state.tableName">
                        <td>{{ state.tableName }}</td>
                        <td>{{ formatDate(state.lastSuccessfulSync) }}</td>
                    </tr>
                </tbody>
            </table>
            <p v-else class="empty">No sync has run yet.</p>
        </section>

        <section class="panel">
            <header class="panel__header">
                <div>
                    <h2>Recent Sync Runs</h2>
                    <p>Includes uploaded CSVs grouped by country.</p>
                </div>
            </header>
            <ul class="run-list" v-if="runs.length">
                <li v-for="run in runs" :key="run.id ?? run.startedAt" class="run-card">
                    <div class="run-card__header">
                        <div>
                            <p class="run-card__title">Run #{{ run.id ?? '—' }}</p>
                            <p class="run-card__timestamp">Started {{ formatDate(run.startedAt) }}</p>
                        </div>
                        <span class="badge" :class="statusClass(run.status)">{{ run.status }}</span>
                    </div>
                    <p v-if="run.finishedAt" class="run-card__timestamp">Finished {{ formatDate(run.finishedAt) }}</p>
                    <p v-if="run.errorMessage" class="run-card__error">{{ run.errorMessage }}</p>
                    <div class="run-card__items" v-if="run.items.length">
                        <div v-for="item in run.items" :key="item.s3Key" class="run-card__item">
                            <div>
                                <p class="run-card__item-title">{{ item.tableName }} · {{ item.country }}</p>
                                <p class="run-card__item-subtitle">{{ item.objectCount }} records</p>
                            </div>
                            <code class="run-card__item-key">{{ item.s3Key }}</code>
                        </div>
                    </div>
                </li>
            </ul>
            <p v-else class="empty">No runs found.</p>
        </section>

        <p v-if="error" class="error">{{ error }}</p>
    </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import dayjs from 'dayjs';
import { syncService, type SyncRun, type SyncState } from '../services/api';

const states = ref<SyncState[]>([]);
const runs = ref<SyncRun[]>([]);
const isLoading = ref(false);
const isTriggering = ref(false);
const error = ref('');

const refresh = async () => {
    try {
        isLoading.value = true;
        error.value = '';
        const [stateResponse, runResponse] = await Promise.all([
            syncService.listStates(),
            syncService.listRuns(10)
        ]);
        states.value = stateResponse.data ?? [];
        runs.value = runResponse.data ?? [];
    } catch (err) {
        error.value = getErrorMessage(err);
    } finally {
        isLoading.value = false;
    }
};

const triggerSync = async () => {
    try {
        isTriggering.value = true;
        error.value = '';
        await syncService.triggerRun();
        await refresh();
    } catch (err) {
        error.value = getErrorMessage(err);
    } finally {
        isTriggering.value = false;
    }
};

const formatDate = (value: string | null | undefined) => {
    if (!value) return '—';
    return dayjs(value).format('YYYY-MM-DD HH:mm:ss');
};

const statusClass = (status: string) => {
    switch (status) {
        case 'SUCCESS':
            return 'badge--success';
        case 'FAILED':
            return 'badge--error';
        default:
            return 'badge--info';
    }
};

const getErrorMessage = (err: unknown) => {
    if (typeof err === 'string') return err;
    if (err && typeof err === 'object' && 'response' in err) {
        const rsp = err as { response?: { data?: { message?: string }; statusText?: string } };
        return rsp.response?.data?.message ?? rsp.response?.statusText ?? 'Request failed';
    }
    return 'Unexpected error occurred';
};

onMounted(refresh);
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
}

.panel__header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: 1rem;
    margin-bottom: 1rem;
}

.actions {
    display: flex;
    gap: 0.75rem;
}

.button {
    border: none;
    background: #2563eb;
    color: white;
    padding: 0.6rem 1.2rem;
    border-radius: 8px;
    font-weight: 600;
}

.button[disabled] {
    opacity: 0.6;
    cursor: not-allowed;
}

.button--ghost {
    background: transparent;
    border: 1px solid #cbd5f5;
    color: #1f2937;
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

.empty {
    color: #64748b;
}

.run-list {
    list-style: none;
    padding: 0;
    margin: 0;
    display: grid;
    gap: 1rem;
}

.run-card {
    border: 1px solid #e2e8f0;
    border-radius: 12px;
    padding: 1.25rem;
    background: #f8fafc;
}

.run-card__header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 0.75rem;
}

.run-card__title {
    font-weight: 600;
    margin: 0;
}

.run-card__timestamp {
    margin: 0;
    color: #64748b;
    font-size: 0.9rem;
}

.run-card__error {
    color: #dc2626;
    margin: 0.5rem 0;
}

.run-card__items {
    display: grid;
    gap: 0.75rem;
}

.run-card__item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    background: white;
    border: 1px solid #dbeafe;
    padding: 0.75rem;
    border-radius: 8px;
}

.run-card__item-title {
    margin: 0;
    font-weight: 600;
}

.run-card__item-subtitle {
    margin: 0;
    font-size: 0.9rem;
    color: #64748b;
}

.run-card__item-key {
    background: #0f172a;
    color: #e2e8f0;
    padding: 0.25rem 0.5rem;
    border-radius: 4px;
    font-size: 0.75rem;
}

.badge {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    padding: 0.3rem 0.75rem;
    border-radius: 999px;
    font-size: 0.85rem;
    font-weight: 600;
}

.badge--success {
    background: #ecfdf5;
    color: #047857;
}

.badge--error {
    background: #fef2f2;
    color: #b91c1c;
}

.badge--info {
    background: #eff6ff;
    color: #1d4ed8;
}

.error {
    color: #b91c1c;
    font-weight: 600;
}

@media (max-width: 768px) {
    .run-card__item {
        flex-direction: column;
        align-items: flex-start;
        gap: 0.5rem;
    }

    .run-card__timestamp {
        white-space: normal;
    }
}
</style>
