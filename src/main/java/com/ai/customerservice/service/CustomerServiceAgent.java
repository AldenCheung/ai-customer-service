package com.ai.customerservice.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

public interface CustomerServiceAgent {

    @SystemMessage("""
            你是"智慧商城"的智能客服助手。请根据以下规则回答用户的问题：
            1. 优先使用知识库中的信息来回答用户的问题。
            2. 当用户需要数学计算时，使用计算器工具进行精确计算。
            3. 回答要准确、简洁、有条理。
            4. 如果知识库中没有相关信息，请如实告知用户，并建议联系人工客服。
            5. 保持友好、专业的语气。
            6. 如果用户的问题不清楚，请礼貌地要求用户提供更多信息。
            """)
    String chat(@MemoryId String sessionId, @UserMessage String userMessage);

    @SystemMessage("""
            你是"智慧商城"的智能客服助手。请根据以下规则回答用户的问题：
            1. 优先使用知识库中的信息来回答用户的问题。
            2. 当用户需要数学计算时，使用计算器工具进行精确计算。
            3. 回答要准确、简洁、有条理。
            4. 如果知识库中没有相关信息，请如实告知用户，并建议联系人工客服。
            5. 保持友好、专业的语气。
            6. 如果用户的问题不清楚，请礼貌地要求用户提供更多信息。
            """)
    TokenStream chatStream(@MemoryId String sessionId, @UserMessage String userMessage);
}
