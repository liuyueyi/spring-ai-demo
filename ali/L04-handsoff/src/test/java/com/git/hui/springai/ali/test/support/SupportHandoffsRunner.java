/*
 * Copyright 2025-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.git.hui.springai.ali.test.support;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.git.hui.springai.ali.test.support.tools.SupportTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

import java.util.List;

/**
 * Runs the customer support handoffs demo when {@code handoffs.run-examples=true}.
 * Executes four turns in sequence (same thread_id) to demonstrate warranty collection,
 * issue classification, and resolution.
 */
@Slf4j
//@Component
public class SupportHandoffsRunner implements ApplicationRunner {

	private static final String THREAD_ID = "handoffs-demo-thread";

	@Autowired
	private ChatModel chatModel;
	@Autowired
	private MemorySaver memorySaver;
	@Override
	public void run(ApplicationArguments args) throws Exception {
		RunnableConfig config = RunnableConfig.builder().threadId(THREAD_ID).build();

		List<ToolCallback> allTools = List.of(
				SupportTools.recordWarrantyStatusTool(),
				SupportTools.recordIssueTypeTool(),
				SupportTools.provideSolutionTool(),
				SupportTools.escalateToHumanTool());

		ReactAgent supportAgent = ReactAgent.builder()
				.name("support_agent")
				.model(chatModel)
				.tools(allTools)
				.hooks(new HandoffsSupportHook())
				.saver(memorySaver)
				.build();

		// Turn 1: Warranty collection
		log.info("=== Turn 1: Warranty Collection ===");
		AssistantMessage r1 = supportAgent.call(new UserMessage("我的手机屏幕碎了"), config);
		log.info("Assistant: {}", r1.getText());

		// Turn 2: User responds about warranty
		log.info("\n=== Turn 2: Warranty Response ===");
		AssistantMessage r2 = supportAgent.call(new UserMessage("是的，它仍在保修期内"), config);
		log.info("Assistant: {}", r2.getText());

		// Turn 3: User describes the issue
		log.info("\n=== Turn 3: Issue Description ===");
		AssistantMessage r3 = supportAgent.call(new UserMessage("屏幕因为衰落到地上，导致碎了"), config);
		log.info("Assistant: {}", r3.getText());

		// Turn 4: Resolution
		log.info("\n=== Turn 4: Resolution ===");
		AssistantMessage r4 = supportAgent.call(new UserMessage("我应该怎么办?"), config);
		log.info("Assistant: {}", r4.getText());
	}
}
