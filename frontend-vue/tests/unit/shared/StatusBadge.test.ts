import { describe, expect, it } from "vitest";
import { mount } from "@vue/test-utils";
import { NTag } from "naive-ui";
import StatusBadge from "@/components/shared/StatusBadge.vue";

describe("StatusBadge", () => {
  it("maps completed-like statuses to success tags", () => {
    const wrapper = mount(StatusBadge, { props: { status: "COMPLETED" } });

    expect(wrapper.text()).toContain("COMPLETED");
    expect(wrapper.findComponent(NTag).props("type")).toBe("success");
  });

  it("maps cancelled status to error tags", () => {
    const wrapper = mount(StatusBadge, { props: { status: "CANCELLED" } });

    expect(wrapper.findComponent(NTag).props("type")).toBe("error");
  });
});
