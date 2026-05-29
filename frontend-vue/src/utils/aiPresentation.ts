import type { RenderedSection, ToolCallMessage } from "@/types/aiView";

export function summarizeObject(value: unknown) {
  if (!value || typeof value !== "object") return "";
  return Object.entries(value as Record<string, unknown>)
    .slice(0, 3)
    .map(([key, val]) => `${resultFieldLabel(key)}：${formatCell(val)}`)
    .join("；");
}

export function summarizeActions(result: Record<string, unknown>) {
  const rows = extractRows(String(result.taskCode ?? ""), result);
  if (rows.length === 0) return "";
  const sample = rows[0] as Record<string, unknown>;
  return formatCell(sample.reason ?? sample.replySuggestion ?? sample.sopSuggestion ?? sample.action ?? sample.productName ?? sample.section);
}

export function extractRows(taskCode: string, result: Record<string, unknown>) {
  if (taskCode === "DAILY_OPERATIONS_REPORT") {
    const sections = (result.sections as Record<string, Record<string, unknown>> | undefined) ?? {};
    return Object.entries(sections).map(([key, value]) => ({ section: resultFieldLabel(key), ...value }));
  }
  if (taskCode === "SOP_RECOMMENDATION") {
    const steps = Array.isArray(result.steps) ? result.steps : [];
    return steps.map((step, index) => ({ step: `步骤 ${index + 1}`, action: formatCell(step) }));
  }
  const primaryKeys = ["suggestions", "analysisList", "topRiskItems", "anomalies", "items", "list"];
  for (const key of primaryKeys) {
    const candidate = result[key];
    if (Array.isArray(candidate)) {
      return candidate.map((item) => (typeof item === "object" && item !== null ? item as Record<string, unknown> : { value: item }));
    }
  }
  return [];
}

export function resultFieldLabel(key: string) {
  const map: Record<string, string> = {
    productCode: "商品编码",
    productName: "商品名称",
    availableQty: "可用库存",
    currentQty: "当前库存",
    safetyStock: "安全库存",
    suggestReplenishQty: "建议补货量",
    reason: "原因",
    categorySuggestion: "分类建议",
    prioritySuggestion: "优先级建议",
    replySuggestion: "候选回复",
    sopSuggestion: "SOP 建议",
    faqHitSuggestion: "FAQ 命中",
    reportDate: "日期",
    reportText: "日报摘要",
    section: "模块",
    inbound: "入库",
    outbound: "出库",
    tickets: "工单",
    lowStock: "低库存",
    action: "建议动作",
    step: "步骤"
  };
  return map[key] || key;
}

export function formatCell(value: unknown) {
  if (value === null || value === undefined || value === "") return "-";
  if (typeof value === "object") return JSON.stringify(value);
  return String(value);
}

export function parseAssistantContent(content: string): RenderedSection[] {
  const cleaned = (content || "").replace(/\r/g, "").trim();
  if (!cleaned) {
    return [{ title: "生成中", content: ["正在整理业务回答..."], items: [], badge: "AI", badgeType: "info" }];
  }
  const knownTitles = ["当前结论", "主要风险", "建议动作", "数据依据", "下一步", "下一步可执行操作"];
  const sections: RenderedSection[] = [];
  let current: RenderedSection = { title: "当前结论", content: [], items: [], badge: "结论", badgeType: "success" };

  for (const rawLine of cleaned.split("\n")) {
    const line = rawLine.trim();
    if (!line) continue;
    const normalized = line.replace(/^#{1,6}\s*/, "").replace(/^\*\*(.+)\*\*$/, "$1").replace(/[:：]$/, "");
    const matchedTitle = knownTitles.find((title) => normalized === title || normalized.startsWith(title));
    if (matchedTitle) {
      if (current.content.length || current.items.length) sections.push(current);
      current = {
        title: matchedTitle === "下一步" ? "下一步可执行操作" : matchedTitle,
        content: [],
        items: [],
        ...sectionBadge(matchedTitle)
      };
      continue;
    }

    const bullet = line.match(/^[-*]\s+(.+)$/) || line.match(/^\d+[.)、]\s+(.+)$/);
    if (bullet) {
      current.items.push(cleanToolNames(bullet[1]));
    } else {
      splitLongParagraph(cleanToolNames(line)).forEach((paragraph) => current.content.push(paragraph));
    }
  }
  if (current.content.length || current.items.length) sections.push(current);
  return sections.length ? sections : [{ title: "当前结论", content: splitLongParagraph(cleanToolNames(cleaned)), items: [], badge: "结论", badgeType: "success" }];
}

export function sectionBadge(title: string): Pick<RenderedSection, "badge" | "badgeType"> {
  if (title.includes("风险")) return { badge: "风险", badgeType: "warning" };
  if (title.includes("动作") || title.includes("下一步")) return { badge: "行动", badgeType: "info" };
  if (title.includes("依据")) return { badge: "依据", badgeType: "default" };
  return { badge: "结论", badgeType: "success" };
}

export function isBusinessCardSection(section: RenderedSection) {
  return section.title.includes("风险") || section.title.includes("建议动作") || section.title.includes("下一步");
}

export function priorityLabel(text: string) {
  if (/高优先级|高风险|失败|异常|超时|缺货|未查询到相关数据/.test(text)) return "高";
  if (/中优先级|中风险|待审核|待处理|未关闭|复核/.test(text)) return "中";
  return "低";
}

export function priorityType(text: string): "default" | "error" | "info" | "success" | "warning" {
  const label = priorityLabel(text);
  if (label === "高") return "error";
  if (label === "中") return "warning";
  return "info";
}

export function splitLongParagraph(text: string) {
  if (text.length <= 120) return [text];
  return text.split(/(?<=[。！？；])/).map((item) => item.trim()).filter(Boolean);
}

export function renderInlineMarkdown(text: string) {
  const escaped = escapeHtml(text);
  return escaped
    .replace(/\*\*(.+?)\*\*/g, "<strong>$1</strong>")
    .replace(/`([^`]+)`/g, "<code>$1</code>")
    .replace(/(高优先级|高风险|失败|异常|超时|未查询到相关数据)/g, "<span class=\"nd-ai-risk-high\">$1</span>")
    .replace(/(中优先级|中风险|待审核|待处理|未关闭)/g, "<span class=\"nd-ai-risk-medium\">$1</span>")
    .replace(/(\d+(?:\.\d+)?\s*(?:件|条|个|元|天|小时|%|SKU|单)?)/g, "<span class=\"nd-ai-number\">$1</span>");
}

export function escapeHtml(text: string) {
  return text
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

export function cleanToolNames(text: string) {
  return text
    .replace(/\bquery_inventory\b/g, "库存查询")
    .replace(/\bquery_purchase\b/g, "采购查询")
    .replace(/\bquery_outbound\b/g, "出库查询")
    .replace(/\bquery_tickets\b/g, "工单查询")
    .replace(/\bquery_inbound\b/g, "入库查询")
    .replace(/\bquery_sale\b/g, "销售查询")
    .replace(/\bget_daily_report\b/g, "运营日报查询")
    .replace(/\bget_inventory_stats\b/g, "库存统计查询");
}

export function toolBusinessLabel(toolName?: string) {
  if (!toolName) return "业务查询";
  const map: Record<string, string> = {
    query_inventory: "库存查询",
    query_purchase: "采购查询",
    query_outbound: "出库查询",
    query_tickets: "工单查询",
    query_inbound: "入库查询",
    query_sale: "销售查询",
    query_product: "商品查询",
    query_partner: "往来单位查询",
    get_daily_report: "运营日报查询",
    get_inventory_stats: "库存统计查询"
  };
  return map[toolName] || "业务查询";
}

export function friendlyArguments(argumentsSummary: string) {
  return cleanToolNames(argumentsSummary)
    .replace(/lowStock/g, "低库存")
    .replace(/limit/g, "条数")
    .replace(/status/g, "状态")
    .replace(/orderNo/g, "单号")
    .replace(/dateFrom/g, "开始日期")
    .replace(/dateTo/g, "结束日期");
}

export function formatAiFailure(error: unknown) {
  const message = error instanceof Error
    ? error.message
    : typeof error === "object" && error !== null && "message" in error
      ? String((error as { message?: unknown }).message ?? "")
      : "";
  if (message.includes("DeepSeek 调用失败")) {
    return message;
  }
  return [
    "DeepSeek 调用失败，请检查：",
    "1. API Key 是否正确",
    "2. AI_DEEPSEEK_ENABLED 是否为 true",
    "3. AI_PROVIDER 是否为 deepseek-chat",
    "4. 网络是否能访问 DeepSeek",
    "5. 账户额度是否充足",
    "6. 模型名称是否正确"
  ].join("\n");
}

export function toolStatusLabel(tool: ToolCallMessage) {
  if (tool.status === "CALLING") return "调用中";
  if (tool.permissionResult === "DENIED" || tool.status === "DENIED") return "无权限";
  if (tool.status === "FAILED" || tool.success === false) return "失败";
  if (tool.empty || tool.status === "EMPTY") return "无结果";
  return "成功";
}

export function toolStatusType(tool: ToolCallMessage) {
  if (tool.status === "CALLING") return "info";
  if (tool.permissionResult === "DENIED" || tool.status === "DENIED") return "warning";
  if (tool.status === "FAILED" || tool.success === false) return "error";
  if (tool.empty || tool.status === "EMPTY") return "default";
  return "success";
}

export function sourceLabel(source: Record<string, unknown>) {
  const main = source.bizNo ?? source.name ?? source.productName ?? source.sourceId ?? "-";
  const status = source.status ? ` / ${source.status}` : "";
  const qty = source.quantity ?? source.availableQty;
  return `${main}${status}${qty === undefined ? "" : ` / ${qty}`}`;
}

export function statusLabel(status: string) {
  const map: Record<string, string> = {
    PENDING: "等待生成",
    STREAMING: "生成中...",
    COMPLETED: "已完成",
    FAILED: "生成失败",
    STOPPED: "已停止"
  };
  return map[status] || status;
}

export function normalizeMessageForAgent(content: string) {
  const text = content.trim();
  if (["今天最需要处理什么", "今天先处理什么", "优先处理什么", "今日最该处理什么"].includes(text)) {
    return "生成今日运营日报，并告诉我今天最需要优先处理的事项";
  }
  return text;
}
