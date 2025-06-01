// stores/useBoardStore.js
import { defineStore } from 'pinia'

export const useTripMapStore = defineStore('tripMap', {
  state: () => ({
    isLine: false,
  }),
  actions: {
    setPage(isLine) {
      this.isLine = isLine
    },
  },
})
