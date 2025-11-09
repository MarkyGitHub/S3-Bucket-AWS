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
                    <button class="button" type="submit">Save Changes</button>
                </div>
                <p v-if="message" class="form__message">{{ message }}</p>
            </form>
        </section>
    </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';

const hours = ref(3);
const minutes = ref(0);
const message = ref('');

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

const save = () => {
    message.value = `Synchronization frequency updated to ${summary.value}.`;
    setTimeout(() => {
        message.value = '';
    }, 4000);
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
}

.panel__header {
    margin-bottom: 1.5rem;
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

.form__message {
    margin: 0;
    color: #047857;
    font-weight: 600;
}
</style>
