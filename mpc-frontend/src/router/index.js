import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginView.vue'),
    meta: { guest: true }
  },
  {
    path: '/',
    name: 'Main',
    component: () => import('@/views/MainLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        redirect: '/friends'
      },
      {
        path: 'friends',
        name: 'Friends',
        component: () => import('@/views/FriendsView.vue')
      },
      {
        path: 'friends/:friendId',
        name: 'PrivateChat',
        component: () => import('@/views/FriendsView.vue')
      },
      {
        path: 'groups',
        redirect: '/friends'
      },
      {
        path: 'groups/:groupId',
        name: 'GroupChat',
        component: () => import('@/views/GroupPage.vue')
      },
      {
        path: 'notifications',
        name: 'Notifications',
        component: () => import('@/views/NotificationsView.vue')
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.requiresAuth && !auth.token) return '/login'
  if (to.meta.guest && auth.token) return '/'
})

export default router
