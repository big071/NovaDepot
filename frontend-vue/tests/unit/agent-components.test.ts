import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import AgentTaskList from "@/components/agent/AgentTaskList.vue";

describe("agent non-AI components", () => {
  it("emits selected task without changing task payloads", async () => {
    const wrapper = mount(AgentTaskList, {
      props: {
        tasks: [{ taskCode: "LOW_STOCK_ANALYSIS", taskName: "低库存分析", description: "desc", params: [] }],
        selectedTaskCode: "",
        selectedTask: null,
        selectedTaskParams: [],
        taskForm: {},
        executing: false,
        canExecute: true,
        displayTaskName: (task) => task.taskName,
        displayTaskDescription: (task) => task.description,
        displayTaskIntro: (task) => task.description,
        toNumber: () => 0,
        toText: () => ""
      },
      global: {
        stubs: ["NButton", "NEmpty", "NInput", "NInputNumber"]
      }
    });

    await wrapper.find("button").trigger("click");

    expect(wrapper.emitted("select-task")?.[0]).toEqual(["LOW_STOCK_ANALYSIS"]);
  });
});
