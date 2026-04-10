import { createRouter, createWebHistory } from "vue-router";
import { useAuthStore } from "@/stores/auth";
import LoginPage from "@/pages/LoginPage.vue";
import DashboardPage from "@/pages/DashboardPage.vue";
import WorkspaceLayout from "@/layouts/WorkspaceLayout.vue";
import ProductsPage from "@/pages/wms/ProductsPage.vue";
import WarehousesPage from "@/pages/wms/WarehousesPage.vue";
import LocationsPage from "@/pages/wms/LocationsPage.vue";
import InventoryPage from "@/pages/wms/InventoryPage.vue";
import InboundPage from "@/pages/wms/InboundPage.vue";
import OutboundPage from "@/pages/wms/OutboundPage.vue";
import AiAssistantPage from "@/pages/ai/AiAssistantPage.vue";
import CustomerServicePage from "@/pages/cs/CustomerServicePage.vue";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/", redirect: "/dashboard" },
    { path: "/login", component: LoginPage, meta: { public: true } },
    {
      path: "/",
      component: WorkspaceLayout,
      children: [
        { path: "dashboard", component: DashboardPage },
        { path: "wms/products", component: ProductsPage },
        { path: "wms/warehouses", component: WarehousesPage },
        { path: "wms/locations", component: LocationsPage },
        { path: "wms/inventory", component: InventoryPage },
        { path: "wms/inbound", component: InboundPage },
        { path: "wms/outbound", component: OutboundPage },
        { path: "ai/enterprise", component: AiAssistantPage },
        { path: "cs/workspace", component: CustomerServicePage }
      ]
    }
  ]
});

router.beforeEach((to) => {
  const authStore = useAuthStore();
  if (to.meta.public) return true;
  if (!authStore.isLoggedIn) {
    return "/login";
  }
  return true;
});

export default router;
