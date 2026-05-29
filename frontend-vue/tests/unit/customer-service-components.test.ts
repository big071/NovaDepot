import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import CustomerSessionList from "@/components/cs/CustomerSessionList.vue";

describe("customer service split components", () => {
  it("emits selected session id", async () => {
    const wrapper = mount(CustomerSessionList, {
      props: {
        sessions: [{ id: 7, sessionNo: "CS-7", status: "OPEN", priority: "HIGH" }],
        activeSessionId: null,
        loading: false
      },
      global: {
        stubs: ["NEmpty", "NTag"]
      }
    });

    await wrapper.find("button").trigger("click");

    expect(wrapper.emitted("select-session")?.[0]).toEqual([7]);
  });
});
