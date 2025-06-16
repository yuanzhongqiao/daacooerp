import { defineStore } from 'pinia'

export const useAppStore = defineStore('app', {
  state: () => ({
    sidebar: {
      opened: true
    },
    device: 'desktop'
  }),
  actions: {
    closeSideBar({ withoutAnimation }) {
      this.sidebar.opened = false
    },
    openSideBar({ withoutAnimation }) {
      this.sidebar.opened = true
    },
    toggleSideBar() {
      this.sidebar.opened = !this.sidebar.opened
    },
    toggleDevice(device) {
      this.device = device
    }
  }
})