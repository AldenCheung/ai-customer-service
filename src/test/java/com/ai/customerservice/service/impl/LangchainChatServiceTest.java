package com.ai.customerservice.service.impl;

import com.ai.customerservice.dal.ChatHistory;
import com.ai.customerservice.dal.ChatHistoryDao;
import com.ai.customerservice.model.ChatRequest;
import com.ai.customerservice.model.ChatResponse;
import com.ai.customerservice.service.AuthService;
import com.ai.customerservice.service.CustomerServiceAgent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LangchainChatServiceTest {

    @Mock
    private CustomerServiceAgent customerServiceAgent;

    @Mock
    private ChatHistoryDao chatHistoryDao;

    @Mock
    private AuthService authService;

    private LangchainChatService chatService;

    @BeforeEach
    void setUp() {
        chatService = new LangchainChatService(customerServiceAgent, chatHistoryDao, authService);
    }

    @Test
    void chat_withNewSession_shouldCreateChatHistory() {
        when(chatHistoryDao.findBySessionId("sess-001")).thenReturn(Optional.empty());
        when(chatHistoryDao.insert(any(ChatHistory.class))).thenAnswer(invocation -> {
            ChatHistory ch = invocation.getArgument(0);
            ch.setId(1L);
            return ch;
        });
        when(customerServiceAgent.chat(eq("sess-001"), anyString())).thenReturn("这是AI的回答");

        ChatRequest request = new ChatRequest("你好", "sess-001");
        ChatResponse response = chatService.chat(request);

        assertEquals("这是AI的回答", response.getAnswer());
        assertEquals("sess-001", response.getSessionId());
        assertNotNull(response.getTimestamp());
        verify(chatHistoryDao).insert(any(ChatHistory.class));
    }

    @Test
    void chat_withExistingSession_shouldNotCreateChatHistory() {
        ChatHistory existing = new ChatHistory();
        existing.setId(1L);
        existing.setSessionId("sess-001");
        when(chatHistoryDao.findBySessionId("sess-001")).thenReturn(Optional.of(existing));
        when(customerServiceAgent.chat(eq("sess-001"), anyString())).thenReturn("回答");

        ChatRequest request = new ChatRequest("你好", "sess-001");
        chatService.chat(request);

        verify(chatHistoryDao, never()).insert(any(ChatHistory.class));
    }

    @Test
    void chat_withNullSessionId_shouldGenerateNewSessionId() {
        when(chatHistoryDao.findBySessionId(anyString())).thenReturn(Optional.empty());
        when(chatHistoryDao.insert(any(ChatHistory.class))).thenAnswer(invocation -> {
            ChatHistory ch = invocation.getArgument(0);
            ch.setId(1L);
            return ch;
        });
        when(customerServiceAgent.chat(anyString(), anyString())).thenReturn("回答");

        ChatRequest request = new ChatRequest("你好", null);
        ChatResponse response = chatService.chat(request);

        assertNotNull(response.getSessionId());
        assertFalse(response.getSessionId().isEmpty());
    }

    @Test
    void chat_withBlankSessionId_shouldGenerateNewSessionId() {
        when(chatHistoryDao.findBySessionId(anyString())).thenReturn(Optional.empty());
        when(chatHistoryDao.insert(any(ChatHistory.class))).thenAnswer(invocation -> {
            ChatHistory ch = invocation.getArgument(0);
            ch.setId(1L);
            return ch;
        });
        when(customerServiceAgent.chat(anyString(), anyString())).thenReturn("回答");

        ChatRequest request = new ChatRequest("你好", "   ");
        ChatResponse response = chatService.chat(request);

        assertNotNull(response.getSessionId());
        assertTrue(response.getSessionId().length() > 0);
    }

    @Test
    void chat_whenAnswerContains666666_shouldMarkTransferredToHuman() {
        ChatHistory existing = new ChatHistory();
        existing.setId(1L);
        existing.setSessionId("sess-001");
        existing.setTransferredToHuman(false);
        when(chatHistoryDao.findBySessionId("sess-001")).thenReturn(Optional.of(existing));
        when(customerServiceAgent.chat(eq("sess-001"), anyString()))
                .thenReturn("请联系人工客服，电话666666");

        ChatRequest request = new ChatRequest("转人工", "sess-001");
        chatService.chat(request);

        verify(chatHistoryDao).updateTransferredToHuman(1L, true);
    }

    @Test
    void chat_whenAnswerDoesNotContain666666_shouldNotMarkTransferred() {
        ChatHistory existing = new ChatHistory();
        existing.setId(1L);
        existing.setSessionId("sess-001");
        when(chatHistoryDao.findBySessionId("sess-001")).thenReturn(Optional.of(existing));
        when(customerServiceAgent.chat(eq("sess-001"), anyString())).thenReturn("这是普通回答");

        ChatRequest request = new ChatRequest("你好", "sess-001");
        chatService.chat(request);

        verify(chatHistoryDao, never()).updateTransferredToHuman(anyLong(), anyBoolean());
    }

    @Test
    void chat_whenAlreadyTransferredToHuman_shouldNotUpdateAgain() {
        ChatHistory existing = new ChatHistory();
        existing.setId(1L);
        existing.setSessionId("sess-001");
        existing.setTransferredToHuman(true);
        when(chatHistoryDao.findBySessionId("sess-001")).thenReturn(Optional.of(existing));
        when(customerServiceAgent.chat(eq("sess-001"), anyString()))
                .thenReturn("请联系人工客服，电话666666");

        ChatRequest request = new ChatRequest("转人工", "sess-001");
        chatService.chat(request);

        verify(chatHistoryDao, never()).updateTransferredToHuman(anyLong(), anyBoolean());
    }

    @Test
    void chatStream_shouldReturnSseEmitter() {
        ChatHistory existing = new ChatHistory();
        existing.setId(1L);
        existing.setSessionId("sess-001");
        when(chatHistoryDao.findBySessionId("sess-001")).thenReturn(Optional.of(existing));

        dev.langchain4j.service.TokenStream mockStream = mock(dev.langchain4j.service.TokenStream.class);
        when(customerServiceAgent.chatStream(eq("sess-001"), anyString())).thenReturn(mockStream);
        when(mockStream.onNext(any())).thenReturn(mockStream);
        when(mockStream.onComplete(any())).thenReturn(mockStream);
        when(mockStream.onError(any())).thenReturn(mockStream);
        doNothing().when(mockStream).start();

        ChatRequest request = new ChatRequest("你好", "sess-001");
        SseEmitter emitter = chatService.chatStream(request);

        assertNotNull(emitter);
    }
}
