<template>
    <div class="view">
        <section class="panel">
            <header class="panel__header">
                <div>
                    <h2>Synchronization Settings</h2>
                    <p>Configure how often the synchronization should run automatically.</p>
                </div>
            </header>
            <form class="form" @submit.prevent="save">
                <div class="form__group">
                    <label class="form__label" for="hours">Hours</label>
                    <input id="hours" v-model.number="hours" class="form__input" type="number" min="0" step="1"
                        required />
                </div>
                <div class="form__group">
                    <label class="form__label" for="minutes">Minutes</label>
                    <input id="minutes" v-model.number="minutes" class="form__input" type="number" min="0" max="59"
                        step="1" required />
                </div>
                <p class="form__hint">
                    Current frequency: <strong>{{ summary }}</strong>
                </p>
                <div class="form__actions">
                    <button class="button" :disabled="isSaving" type="submit">
                        <span v-if="!isSaving">Save Changes</span>
                        <span v-else>Saving…</span>
                    </button>
                </div>
                <p v-if="message" class="form__message">{{ message }}</p>
                <p v-if="saveError" class="form__message form__message--error">{{ saveError }}</p>
                <p v-if="loadError" class="form__message form__message--error">{{ loadError }}</p>
            </form>
        </section>

        <section class="panel">
            <header class="panel__header">
                <div>
                    <h2>Run Update</h2>
                    <p>Run the update routine to refresh the latest order changes.</p>
                </div>
                <button class="button" :disabled="isTouching" @click="touchLastChange">
                    <span v-if="!isTouching">Run Update Script</span>
                    <span v-else>Running…</span>
                </button>
            </header>
            <div class="panel__body">
                <p v-if="statusStore.lastUpdate" class="touch__summary">
                    Updated <strong>{{ statusStore.lastUpdate.updatedRows }}</strong> rows at
                    <strong>{{ formatTimestamp(statusStore.lastUpdate.appliedTimestamp) }}</strong>.
                </p>
                <p v-if="touchError" class="touch__error">{{ touchError }}</p>
            </div>
        </section>
    </div>
</template>

<script setup lang="ts">
import dayjs from 'dayjs';
import { computed, onMounted, ref, watch } from 'vue';

import {
    orderService,
    syncScheduleService,
    type OrderUpdateResponse,
    type SyncScheduleRequest,
    type SyncScheduleResponse
} from '../services/api';
import { setLastUpdateStatus, useStatusStore } from '../stores/statusStore';

const statusStore = useStatusStore();
const hours = ref(3);
const minutes = ref(0);
const schedulerEnabled = ref(true);
const message = ref('');
const saveError = ref('');
const loadError = ref('');
const isSaving = ref(false);

const isTouching = ref(false);
const touchSummary = ref<OrderUpdateResponse | null>(null);
const touchError = ref('');

watch(hours, (value) => {
    if (value < 0 || Number.isNaN(value)) {
        hours.value = 0;
    } else {
        hours.value = Math.trunc(value);
    }
});

watch(minutes, (value) => {
    if (value < 0 || Number.isNaN(value)) {
        minutes.value = 0;
    } else if (value > 59) {
        minutes.value = 59;
    } else {
        minutes.value = Math.trunc(value);
    }
});

const summary = computed(() => {
    if (!schedulerEnabled.value) {
        return 'disabled';
    }

    const parts: string[] = [];
    if (hours.value > 0) {
        parts.push(`${hours.value} ${hours.value === 1 ? 'hour' : 'hours'}`);
    }
    if (minutes.value > 0) {
        parts.push(`${minutes.value} ${minutes.value === 1 ? 'minute' : 'minutes'}`);
    }
    if (!parts.length) {
        return 'disabled';
    }
    return parts.join(' and ');
});

const formatTimestamp = (value: string) => {
    return dayjs(value).format('YYYY-MM-DD HH:mm:ss');
};

const getErrorMessage = (err: unknown) => {
    if (typeof err === 'string') return err;
    if (err && typeof err === 'object' && 'response' in err) {
        const rsp = err as { response?: { data?: { message?: string }; statusText?: string } };
        return rsp.response?.data?.message ?? rsp.response?.statusText ?? 'Request failed';
    }
    return 'Unexpected error occurred';
};

const loadSchedule = async () => {
    try {
        const response = await syncScheduleService.getSchedule();
        const data = response.data;
        schedulerEnabled.value = data.schedulerEnabled;
        hours.value = data.hours;
        minutes.value = data.minutes;
        loadError.value = '';
    } catch (err) {
        loadError.value = getErrorMessage(err);
    }
};

onMounted(() => {
    void loadSchedule();
});

const save = async () => {
    saveError.value = '';
    message.value = '';

    if (hours.value === 0 && minutes.value === 0) {
        saveError.value = 'Interval must be greater than zero.';
        return;
    }

    try {
        isSaving.value = true;
        const payload: SyncScheduleRequest = {
            hours: hours.value,
            minutes: minutes.value
        };
        const response = await syncScheduleService.updateSchedule(payload);
        const data = response.data;
        schedulerEnabled.value = data.schedulerEnabled;
        hours.value = data.hours;
        minutes.value = data.minutes;
        message.value = `Synchronization frequency updated to ${summary.value}.`;
        setTimeout(() => {
            message.value = '';
        }, 4000);
    } catch (err) {
        saveError.value = getErrorMessage(err);
    } finally {
        isSaving.value = false;
    }
};

const touchLastChange = async () => {
    try {
        isTouching.value = true;
        touchError.value = '';
        const response = await orderService.touchLastChange();
        touchSummary.value = response.data;
        setLastUpdateStatus({
            updatedRows: response.data.updatedRows,
            appliedTimestamp: response.data.appliedTimestamp
        });
    } catch (err) {
        touchSummary.value = null;
        touchError.value = getErrorMessage(err);
        setLastUpdateStatus(null);
    } finally {
        isTouching.value = false;
    }
};
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

.panel__body {
    display: grid;
    gap: 0.75rem;
    color: #475569;
}

.form {
    display: grid;
    gap: 1.25rem;
}

.form__group {
    display: grid;
    gap: 0.5rem;
}

.form__label {
    font-weight: 600;
    color: #1f2937;
}

.form__input {
    padding: 0.65rem 0.75rem;
    border: 1px solid #cbd5f5;
    border-radius: 8px;
    font-size: 1rem;
    color: #1f2937;
    background: #f8fafc;
}

.form__input:focus {
    outline: none;
    border-color: #2563eb;
    box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.15);
}

.form__hint {
    margin: 0;
    color: #475569;
}

.form__actions {
    display: flex;
    justify-content: flex-end;
}

.button {
    border: none;
    background: #2563eb;
    color: white;
    padding: 0.6rem 1.4rem;
    border-radius: 8px;
    font-weight: 600;
}

.button[disabled] {
    opacity: 0.6;
    cursor: not-allowed;
}

.form__message {
    margin: 0;
    color: #047857;
    font-weight: 600;
}

.form__message--error {
    color: #b91c1c;
}

.touch__summary {
    margin: 0;
    color: #047857;
    font-weight: 600;
}

.touch__error {
    margin: 0;
    color: #b91c1c;
    font-weight: 600;
}
</style>
