<template>
  <section class="space-y-4">
    <header class="nd-hero">
      <div class="nd-hero-header">
        <div>
          <p class="text-xs uppercase tracking-[0.14em] text-text-secondary">Knowledge</p>
          <h1 class="nd-page-title">知识与规则维护</h1>
          <p class="nd-page-subtitle">维护 FAQ、SOP 和最小规则配置。草稿需管理员确认后才会被 AI、客服和 Agent 正式引用。</p>
        </div>
        <n-button class="nd-soft-focus" :loading="loading" @click="loadAll">刷新</n-button>
      </div>
    </header>

    <n-alert v-if="errorText" type="error" :show-icon="false">{{ errorText }}</n-alert>
    <n-alert v-else-if="successText" type="success" :show-icon="false">{{ successText }}</n-alert>

    <div class="grid gap-4 xl:grid-cols-3">
      <article class="nd-table-shell xl:col-span-2">
        <div class="nd-table-head">
          <h3 class="nd-section-title">FAQ 后台维护</h3>
          <n-button size="small" type="primary" @click="createFaq">新增 FAQ 草稿</n-button>
        </div>
        <div class="nd-table-body space-y-3">
          <article v-for="faq in faqs" :key="faq.id" class="rounded-xl border border-border bg-bg/50 p-3">
            <div class="flex flex-wrap items-center justify-between gap-2">
              <p class="font-medium">{{ faq.question }}</p>
              <div class="flex gap-2">
                <n-tag :bordered="false" :type="faq.reviewStatus === 'APPROVED' ? 'success' : 'warning'">{{ faq.reviewStatus }}</n-tag>
                <n-tag :bordered="false" :type="faq.enabled ? 'info' : 'default'">{{ faq.enabled ? '启用' : '停用' }}</n-tag>
              </div>
            </div>
            <p class="mt-1 text-sm text-text-secondary">{{ faq.answer }}</p>
            <p class="mt-2 text-xs text-text-secondary">场景：{{ faq.scene || '-' }} / 标签：{{ faq.tags || '-' }} / 来源：{{ faq.sourceRefId || faq.sourceType || '-' }}</p>
            <div class="mt-3 flex flex-wrap gap-2" v-if="canConfirm">
              <n-button size="small" @click="confirmFaq(faq.id)">确认启用</n-button>
              <n-button size="small" @click="toggleFaq(faq)">{{ faq.enabled ? '停用' : '启用' }}</n-button>
            </div>
          </article>
        </div>
      </article>

      <article class="nd-table-shell">
        <div class="nd-table-head">
          <h3 class="nd-section-title">新 FAQ 草稿</h3>
        </div>
        <div class="nd-table-body space-y-3">
          <n-input v-model:value="faqForm.question" placeholder="问题" />
          <n-input v-model:value="faqForm.answer" type="textarea" :rows="4" placeholder="答案" />
          <n-input v-model:value="faqForm.scene" placeholder="适用场景，如 customer-service" />
          <n-input v-model:value="faqForm.tags" placeholder="标签，用逗号分隔" />
          <n-input-number v-model:value="faqForm.priority" class="w-full" placeholder="优先级" />
        </div>
      </article>
    </div>

    <div class="grid gap-4 xl:grid-cols-3">
      <article class="nd-table-shell xl:col-span-2">
        <div class="nd-table-head">
          <h3 class="nd-section-title">SOP 后台维护</h3>
          <n-button size="small" type="primary" @click="createSop">新增 SOP 草稿</n-button>
        </div>
        <div class="nd-table-body space-y-3">
          <article v-for="sop in sops" :key="sop.id" class="rounded-xl border border-border bg-bg/50 p-3">
            <div class="flex flex-wrap items-center justify-between gap-2">
              <p class="font-medium">{{ sop.title }}</p>
              <div class="flex gap-2">
                <n-tag :bordered="false" :type="sop.reviewStatus === 'APPROVED' ? 'success' : 'warning'">{{ sop.reviewStatus }}</n-tag>
                <n-tag :bordered="false" :type="sop.enabled ? 'info' : 'default'">{{ sop.enabled ? '启用' : '停用' }}</n-tag>
              </div>
            </div>
            <p class="mt-1 text-sm text-text-secondary">{{ sop.steps }}</p>
            <p class="mt-2 text-xs text-text-secondary">风险点：{{ sop.risks || '-' }}</p>
            <p class="mt-1 text-xs text-text-secondary">复核项：{{ sop.reviewChecks || '-' }}</p>
            <div class="mt-3 flex flex-wrap gap-2" v-if="canConfirm">
              <n-button size="small" @click="confirmSop(sop.id)">确认启用</n-button>
              <n-button size="small" @click="toggleSop(sop)">{{ sop.enabled ? '停用' : '启用' }}</n-button>
            </div>
          </article>
        </div>
      </article>

      <article class="nd-table-shell">
        <div class="nd-table-head">
          <h3 class="nd-section-title">新 SOP 草稿</h3>
        </div>
        <div class="nd-table-body space-y-3">
          <n-input v-model:value="sopForm.title" placeholder="SOP 标题" />
          <n-input v-model:value="sopForm.scene" placeholder="适用场景，如 inventory" />
          <n-input v-model:value="sopForm.steps" type="textarea" :rows="4" placeholder="标准处理步骤" />
          <n-input v-model:value="sopForm.risks" type="textarea" :rows="3" placeholder="风险点" />
          <n-input v-model:value="sopForm.reviewChecks" type="textarea" :rows="3" placeholder="复核项" />
          <n-input v-model:value="sopForm.tags" placeholder="标签，用逗号分隔" />
          <n-input-number v-model:value="sopForm.priority" class="w-full" placeholder="优先级" />
        </div>
      </article>
    </div>

    <article class="nd-table-shell">
      <div class="nd-table-head">
        <div>
          <h3 class="nd-section-title">规则配置</h3>
          <p class="nd-section-subtitle">配置变更会写入审计。非管理员只读。</p>
        </div>
      </div>
      <div class="nd-table-body space-y-3">
        <article v-for="rule in rules" :key="rule.configKey" class="grid gap-3 rounded-xl border border-border bg-bg/50 p-3 md:grid-cols-[220px,1fr,160px]">
          <div>
            <p class="font-medium">{{ rule.configName }}</p>
            <p class="mt-1 text-xs text-text-secondary">{{ rule.configKey }}</p>
          </div>
          <n-input v-model:value="ruleDraft[rule.configKey]" :disabled="!canConfirm" />
          <n-button :disabled="!canConfirm" @click="updateRule(rule)">保存配置</n-button>
          <p class="md:col-span-3 text-xs text-text-secondary">{{ rule.remark || '-' }}</p>
        </article>
      </div>
    </article>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { NAlert, NButton, NInput, NInputNumber, NTag, useMessage } from "naive-ui";
import { knowledgeApi, type FaqKnowledge, type RuleConfig, type SopKnowledge } from "@/services/knowledge";
import { useAuthStore } from "@/stores/auth";

const message = useMessage();
const authStore = useAuthStore();
const loading = ref(false);
const errorText = ref("");
const successText = ref("");
const faqs = ref<FaqKnowledge[]>([]);
const sops = ref<SopKnowledge[]>([]);
const rules = ref<RuleConfig[]>([]);
const ruleDraft = reactive<Record<string, string>>({});

const canConfirm = computed(() => authStore.hasPermission("KNOWLEDGE_CONFIRM") || authStore.hasPermission("RULE_CONFIG_UPDATE"));

const faqForm = reactive({ question: "", answer: "", scene: "customer-service", tags: "", priority: 10 });
const sopForm = reactive({
  title: "",
  scene: "customer-service",
  steps: "",
  risks: "",
  reviewChecks: "",
  tags: "",
  priority: 10
});

async function loadAll() {
  loading.value = true;
  errorText.value = "";
  try {
    const [faqData, sopData, ruleData] = await Promise.all([
      knowledgeApi.listFaqs(),
      knowledgeApi.listSops(),
      knowledgeApi.listRules()
    ]);
    faqs.value = faqData;
    sops.value = sopData;
    rules.value = ruleData;
    rules.value.forEach((rule) => {
      ruleDraft[rule.configKey] = rule.configValue;
    });
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "知识数据加载失败";
  } finally {
    loading.value = false;
  }
}

async function createFaq() {
  try {
    await knowledgeApi.createFaq({ ...faqForm });
    successText.value = "FAQ 草稿已创建，等待管理员确认启用";
    Object.assign(faqForm, { question: "", answer: "", scene: "customer-service", tags: "", priority: 10 });
    await loadAll();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "创建 FAQ 失败");
  }
}

async function createSop() {
  try {
    await knowledgeApi.createSop({ ...sopForm });
    successText.value = "SOP 草稿已创建，等待管理员确认启用";
    Object.assign(sopForm, { title: "", scene: "customer-service", steps: "", risks: "", reviewChecks: "", tags: "", priority: 10 });
    await loadAll();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "创建 SOP 失败");
  }
}

async function confirmFaq(id: string) {
  await knowledgeApi.confirmFaq(id);
  successText.value = "FAQ 已确认启用";
  await loadAll();
}

async function confirmSop(id: string) {
  await knowledgeApi.confirmSop(id);
  successText.value = "SOP 已确认启用";
  await loadAll();
}

async function toggleFaq(faq: FaqKnowledge) {
  if (faq.enabled) await knowledgeApi.disableFaq(faq.id);
  else await knowledgeApi.enableFaq(faq.id);
  successText.value = "FAQ 启停状态已更新";
  await loadAll();
}

async function toggleSop(sop: SopKnowledge) {
  if (sop.enabled) await knowledgeApi.disableSop(sop.id);
  else await knowledgeApi.enableSop(sop.id);
  successText.value = "SOP 启停状态已更新";
  await loadAll();
}

async function updateRule(rule: RuleConfig) {
  await knowledgeApi.updateRule(rule.configKey, { ...rule, configValue: ruleDraft[rule.configKey] });
  successText.value = "规则配置已保存并记录审计";
  await loadAll();
}

onMounted(loadAll);
</script>
