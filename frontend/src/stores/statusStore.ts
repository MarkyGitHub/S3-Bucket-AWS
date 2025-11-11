import { reactive } from "vue";

export interface LastUpdateStatus {
  updatedRows: number;
  appliedTimestamp: string;
}

const state = reactive<{
  lastUpdate: LastUpdateStatus | null;
}>({
  lastUpdate: null,
});

export const useStatusStore = () => state;

export const setLastUpdateStatus = (status: LastUpdateStatus | null) => {
  state.lastUpdate = status;
};
