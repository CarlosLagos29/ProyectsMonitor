import { createRouter, createWebHistory } from 'vue-router'

import { useSessionStore } from '@/stores/session'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { public: true },
    },
    {
      path: '/',
      name: 'home',
      component: () => import('@/views/HomeView.vue'),
    },
    {
      path: '/monitors/:id',
      name: 'monitor-detail',
      component: () => import('@/views/MonitorDetailView.vue'),
      props: true,
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('@/views/NotFoundView.vue'),
      meta: { public: true },
    },
  ],
})

router.beforeEach((to) => {
  const session = useSessionStore()
  if (to.name === 'login' && session.isActive) {
    return { name: 'home' }
  }
  if (!to.meta.public && !session.isActive) {
    return { name: 'login' }
  }
  return true
})

export default router
