import { createApp } from "vue";
import { createPinia } from "pinia";
import { createDiscreteApi } from "naive-ui";
import App from "./App.vue";
import router from "./router";
import "./styles/main.css";
import { useThemeStore } from "./stores/theme";

const app = createApp(App);
const pinia = createPinia();

app.use(pinia);
app.use(router);

const themeStore = useThemeStore();
themeStore.initTheme();

const { message } = createDiscreteApi(["message"]);
app.provide("$message", message);

app.mount("#app");
