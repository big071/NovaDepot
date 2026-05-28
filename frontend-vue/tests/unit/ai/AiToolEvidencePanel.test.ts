import { describe, expect, it } from "vitest";
import { mount } from "@vue/test-utils";
import AiToolEvidencePanel from "@/components/ai/AiToolEvidencePanel.vue";
import type { ToolCallMessage } from "@/types/aiView";

describe("AiToolEvidencePanel", () => {
  it("renders business labels and tool statuses", () => {
    const toolCalls: ToolCallMessage[] = [
      { toolName: "query_inventory", displayName: "库存查询", status: "SUCCESS", success: true, summary: "命中 2 条" },
      { toolName: "query_purchase", status: "FAILED", success: false, summary: "调用失败" },
      { toolName: "query_tickets", status: "DENIED", permissionResult: "DENIED", summary: "无权限" },
      { toolName: "query_sale", status: "EMPTY", empty: true, summary: "无数据" }
    ];

    const wrapper = mount(AiToolEvidencePanel, { props: { toolCalls, messageIndex: 0 } });

    expect(wrapper.text()).toContain("库存查询");
    expect(wrapper.text()).toContain("成功");
    expect(wrapper.text()).toContain("失败");
    expect(wrapper.text()).toContain("无权限");
    expect(wrapper.text()).toContain("无结果");
  });
});
