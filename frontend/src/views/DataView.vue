<template>
    <div class="view">
        <section class="panel">
            <header class="panel__header">
                <div>
                    <h2>Customers and Orders</h2>
                    <p>Orders per customer, grouped by country. Most recent changes first.</p>
                </div>
                <button class="button button--ghost" :disabled="isLoading" @click="refresh">
                    Refresh
                </button>
            </header>
            <transition name="fade">
                <div v-if="showRefreshAlert" class="alert alert--success">
                    <span>✓ Data refreshed successfully</span>
                </div>
            </transition>
            <div v-if="groupedCustomers.length" class="country-groups">
                <div v-for="group in groupedCustomers" :key="group.country" class="country-group">
                    <h3 class="country-group__title">{{ group.country }}</h3>
                    <ul class="customer-list">
                        <li v-for="entry in group.customers" :key="entry.customer.id" class="customer-card">
                            <details class="customer-details">
                                <summary class="customer-card__header">
                                    <div class="customer-card__info">
                                        <p class="customer-card__name">
                                            {{ entry.customer.lastName }}, {{ entry.customer.firstName }}
                                        </p>
                                        <span class="customer-card__id">ID: {{ entry.customer.id }}</span>
                                    </div>
                                    <span class="customer-card__count">{{ entry.orders.length }} Orders</span>
                                </summary>
                                <ul v-if="entry.orders.length" class="order-list">
                                    <li v-for="order in entry.orders" :key="order.id" class="order-list__item">
                                        <div class="order-list__item-main">
                                            <p class="order-list__title">order-lg1 {{ order.id }}</p>
                                            <p class="order-list__subtitle">Article {{ order.articleNumber }}</p>
                                        </div>
                                        <time class="order-list__timestamp">{{ formatDate(order.lastChange) }}</time>
                                    </li>
                                </ul>
                                <p v-else class="order-list__empty">No orders available.</p>
                            </details>
                        </li>
                    </ul>
                </div>
            </div>
            <p v-else-if="!isLoading" class="empty">Keine Kunden gefunden.</p>
            <p v-if="error" class="error">{{ error }}</p>
        </section>
    </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import dayjs from 'dayjs';
import { customerService, orderService, type Customer, type OrderSummary } from '../services/api';

interface CountryGroup {
    country: string;
    customers: {
        customer: Customer;
        orders: OrderSummary[];
    }[];
}

const customers = ref<Customer[]>([]);
const orders = ref<OrderSummary[]>([]);
const isLoading = ref(false);
const error = ref('');
const showRefreshAlert = ref(false);

const refresh = async () => {
    try {
        isLoading.value = true;
        error.value = '';
        const [customerResponse, orderResponse] = await Promise.all([
            customerService.listCustomers(),
            orderService.listOrders()
        ]);
        customers.value = customerResponse.data ?? [];
        orders.value = orderResponse.data ?? [];

        // Show success alert
        showRefreshAlert.value = true;
        setTimeout(() => {
            showRefreshAlert.value = false;
        }, 3000);
    } catch (err) {
        error.value = getErrorMessage(err);
    } finally {
        isLoading.value = false;
    }
};

const groupedCustomers = computed<CountryGroup[]>(() => {
    if (!customers.value.length) {
        return [];
    }

    const ordersByCustomer = new Map<string, OrderSummary[]>();

    orders.value.forEach((order) => {
        const embeddedCustomer = order.customer;
        const candidateId =
            (embeddedCustomer && typeof embeddedCustomer === 'object' ? embeddedCustomer.id : undefined) ??
            order.customerId ??
            order.kundeid ??
            null;

        if (!candidateId) {
            return;
        }

        if (!ordersByCustomer.has(candidateId)) {
            ordersByCustomer.set(candidateId, []);
        }

        ordersByCustomer.get(candidateId)!.push(order);
    });

    const groups = new Map<string, CountryGroup>();

    customers.value.forEach((customer) => {
        const countryKey = customer.country.trim() || 'Unbekannt';

        if (!groups.has(countryKey)) {
            groups.set(countryKey, { country: countryKey, customers: [] });
        }

        const customerOrders = (ordersByCustomer.get(customer.id) ?? []).slice().sort((a, b) => {
            const toTime = (value: string | null | undefined) => (value ? new Date(value).getTime() : 0);
            return toTime(b.lastChange ?? null) - toTime(a.lastChange ?? null);
        });

        groups.get(countryKey)!.customers.push({
            customer,
            orders: customerOrders
        });
    });

    return Array.from(groups.values())
        .map((group) => ({
            ...group,
            customers: group.customers.sort((a, b) => {
                const lastNameComparison = a.customer.lastName.localeCompare(b.customer.lastName);
                if (lastNameComparison !== 0) {
                    return lastNameComparison;
                }
                return a.customer.firstName.localeCompare(b.customer.firstName);
            })
        }))
        .sort((a, b) => a.country.localeCompare(b.country));
});

const formatDate = (value: string | null | undefined) => {
    if (!value) return '—';
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
    margin-bottom: 1.25rem;
}

.button {
    border: 1px solid #cbd5f5;
    background: transparent;
    color: #1f2937;
    padding: 0.6rem 1.2rem;
    border-radius: 8px;
    font-weight: 600;
}

.button[disabled] {
    opacity: 0.6;
    cursor: not-allowed;
}

.country-groups {
    display: grid;
    gap: 1.5rem;
}

.country-group__title {
    margin: 0 0 0.75rem;
    font-size: 1.1rem;
    font-weight: 700;
    color: #1f2937;
}

.customer-list {
    list-style: none;
    margin: 0;
    padding: 0;
    display: grid;
    gap: 1rem;
}

.customer-card {
    border: 1px solid #e2e8f0;
    border-radius: 10px;
    background: #f8fafc;
}

.customer-details {
    padding: 1rem 1.25rem;
}

.customer-details[open] {
    padding-bottom: 0.5rem;
}

.customer-card__header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 1rem;
    cursor: pointer;
    list-style: none;
    user-select: none;
}

.customer-card__header::-webkit-details-marker {
    display: none;
}

.customer-card__header::marker {
    display: none;
}

.customer-card__header::after {
    content: '▼';
    font-size: 0.75rem;
    color: #64748b;
    transition: transform 0.2s ease;
    flex-shrink: 0;
}

.customer-details[open] .customer-card__header::after {
    transform: rotate(180deg);
}

.customer-card__info {
    display: flex;
    flex-wrap: wrap;
    gap: 0.5rem;
    align-items: baseline;
    flex: 1;
}

.customer-card__name {
    margin: 0;
    font-weight: 600;
    font-size: 1rem;
}

.customer-card__id {
    font-size: 0.85rem;
    color: #475569;
}

.customer-card__count {
    font-size: 0.85rem;
    color: #64748b;
    font-weight: 500;
    white-space: nowrap;
}

.order-list {
    list-style: none;
    margin: 0.75rem 0 0.5rem 0;
    padding: 0;
    display: grid;
    gap: 0.65rem;
}

.order-list__item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    background: #ffffff;
    border: 1px solid #dbeafe;
    border-radius: 8px;
    padding: 0.75rem 1rem;
}

.order-list__item-main {
    display: grid;
    gap: 0.25rem;
}

.order-list__title {
    margin: 0;
    font-weight: 600;
    color: #1f2937;
}

.order-list__subtitle {
    margin: 0;
    font-size: 0.9rem;
    color: #64748b;
}

.order-list__timestamp {
    font-size: 0.85rem;
    color: #475569;
    white-space: nowrap;
}

.order-list__empty {
    margin: 0;
    font-size: 0.9rem;
    color: #94a3b8;
}

.empty {
    color: #64748b;
}

.error {
    color: #b91c1c;
    font-weight: 600;
    margin-top: 1rem;
}

.alert {
    padding: 0.75rem 1rem;
    border-radius: 8px;
    margin-bottom: 1rem;
    display: flex;
    align-items: center;
    gap: 0.5rem;
    font-weight: 500;
}

.alert--success {
    background: #ecfdf5;
    color: #047857;
    border: 1px solid #a7f3d0;
}

.fade-enter-active,
.fade-leave-active {
    transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
    opacity: 0;
}
@media (max-width: 768px) {
    .order-list__item {
        flex-direction: column;
        align-items: flex-start;
        gap: 0.5rem;
    }

    .order-list__timestamp {
        white-space: normal;
    }
}
</style>
