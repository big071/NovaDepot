import { describe, expect, it } from "vitest";
import { mount } from "@vue/test-utils";
import EmptyState from "@/components/shared/EmptyState.vue";

describe("EmptyState", () => {
  it("renders description and extra slot", () => {
    const wrapper = mount(EmptyState, {
      props: { description: "暂无数据" },
      slots: { extra: "<button>创建</button>" }
    });

    expect(wrapper.text()).toContain("暂无数据");
    expect(wrapper.text()).toContain("创建");
  });
});
