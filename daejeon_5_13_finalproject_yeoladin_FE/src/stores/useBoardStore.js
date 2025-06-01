// stores/useBoardStore.js
import { defineStore } from 'pinia'

export const BoardStateEnum = {
  BOARD: 'board',
  FILTER: 'filter',
}
export const BoardCategoryEnum = {
  AUTHOR: '작성자',
  TITLE: '제목',
}

export const useBoardStore = defineStore('board', {
  state: () => ({
    currentPage: 1,
    boardState: BoardStateEnum.BOARD,
    initialize:false,
    categoryState: BoardCategoryEnum.AUTHOR,
    keyword:"",
  }),
  actions: {
    setPage(page) {
      this.currentPage = page
    },
    setBoardState(state) {
      this.boardState = state
    },
    setInitialize(initialize) {
      this.initialize = initialize
    },
    setCategory(category) {
      this.categoryState = category
    },
    setKeyword(keyword) {
      this.keyword = keyword
    },
  },
  persist: {
    key: 'board-page',
    storage: localStorage,
    paths: ['currentPage', 'boardState'],
  },
})
