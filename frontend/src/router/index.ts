import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: () => import('@/views/Login.vue') },
    {
      path: '/',
      component: () => import('@/views/ChatLayout.vue'),
      children: [
        { path: '', name: 'chat', component: () => import('@/views/ChatMain.vue') },
        { path: 'conversation/:id', name: 'conversation', component: () => import('@/views/ChatMain.vue') }
      ]
    }
  ]
})

router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token')
  if (!token && to.name !== 'login') {
    next({ name: 'login' })
  } else if (token && to.name === 'login') {
    next({ name: 'chat' })
  } else {
    next()
  }
})

export default router