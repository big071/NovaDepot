import { describe, expect, it } from "vitest";
import {
  formatAiFailure,
  parseAssistantContent,
  renderInlineMarkdown,
  sourceLabel,
  toolBusinessLabel
} from "@/utils/aiPresentation";

describe("aiPresentation", () => {
  it("parses structured AI answer sections", () => {
    const sections = parseAssistantContent("当前结论\n库存正常\n主要风险\n- 高风险 SKU 缺货");

    expect(sections[0].title).toBe("当前结论");
    expect(sections[1].title).toBe("主要风险");
    expect(sections[1].items[0]).toContain("高风险");
  });

  it("keeps inline markdown and risk highlights", () => {
    const html = renderInlineMarkdown("**结论**：高风险 3 件");

    expect(html).toContain("<strong>结论</strong>");
    expect(html).toContain("nd-ai-risk-high");
    expect(html).toContain("nd-ai-number");
  });

  it("formats DeepSeek failure guidance without exposing raw details", () => {
    const message = formatAiFailure(new Error("network timeout"));

    expect(message).toContain("DeepSeek 调用失败");
    expect(message).toContain("AI_DEEPSEEK_ENABLED");
  });

  it("maps tool and source labels for evidence cards", () => {
    expect(toolBusinessLabel("query_inventory")).toBe("库存查询");
    expect(sourceLabel({ bizNo: "IN-1", status: "POSTED", quantity: 2 })).toBe("IN-1 / POSTED / 2");
  });
});
