package com.ssu.aiQuiz.manager;

import com.ssu.aiQuiz.common.ErrorCode;
import com.ssu.aiQuiz.exception.BusinessException;
import com.zhipu.oapi.ClientV4;
import com.zhipu.oapi.Constants;
import com.zhipu.oapi.service.v4.model.*;
import io.reactivex.Flowable;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * Ai调用
 */
@Component
public class AiManager {

    @Resource
    private ClientV4 client;

    // 稳定的随机数
    private static final float STABLE_TEMPERATURE = 0.05f;

    // 默认随机数
    private static final float DEFAULT_TEMPERATURE = 0.95f;

    /**
     * 通用请求
     *
     * @param messages
     * @param isStream
     * @param temperature
     * @return
     */
    public String doRequest(List<ChatMessage> messages, Boolean isStream, Float temperature) {
        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
                .model(Constants.ModelChatGLM4)
                .stream(isStream)
                .invokeMethod(Constants.invokeMethod)
                .temperature(temperature)
                .messages(messages)
                .build();
        try {
            ModelApiResponse invokeModelApiResp = client.invokeModelApi(chatCompletionRequest);
            ChatMessage message = invokeModelApiResp.getData().getChoices().get(0).getMessage();
            return message.getContent().toString();
        } catch (Exception e) {
            e.printStackTrace();
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, e.getMessage());
        }
    }

    /**
     * 简化请求
     *
     * @param message
     * @param isStream
     * @param temperature
     * @return
     */
    public String doRequest(String message, Boolean isStream, Float temperature) {
        List<ChatMessage> messages = new ArrayList<>();
        ChatMessage chatMessage = new ChatMessage(ChatMessageRole.USER.value(), message);
        messages.add(chatMessage);
        return doRequest(messages, isStream, temperature);
    }

    /**
     * 通用请求（含系统prompt和用户prompt）
     *
     * @param userMessage
     * @param systemMessage
     * @param isStream
     * @param temperature
     * @return
     */
    public String doRequest(String userMessage, String systemMessage, Boolean isStream, Float temperature) {
        List<ChatMessage> messages = new ArrayList<>();
        ChatMessage userChatMessage = new ChatMessage(ChatMessageRole.USER.value(), userMessage);
        messages.add(userChatMessage);
        ChatMessage systemChatMessage = new ChatMessage(ChatMessageRole.SYSTEM.value(), systemMessage);
        messages.add(systemChatMessage);
        return doRequest(messages, isStream, temperature);
    }

    /**
     * 稳定的异步请求
     *
     * @param userMessage
     * @param systemMessage
     * @return
     */
    public String doStableRequest(String userMessage, String systemMessage) {
        List<ChatMessage> messages = new ArrayList<>();
        ChatMessage userChatMessage = new ChatMessage(ChatMessageRole.USER.value(), userMessage);
        messages.add(userChatMessage);
        ChatMessage systemChatMessage = new ChatMessage(ChatMessageRole.SYSTEM.value(), systemMessage);
        messages.add(systemChatMessage);
        return doRequest(messages, Boolean.TRUE, STABLE_TEMPERATURE);
    }

    /**
     * 稳定的同步请求
     *
     * @param userMessage
     * @param systemMessage
     * @return
     */
    public String doStableSyncRequest(String userMessage, String systemMessage) {
        List<ChatMessage> messages = new ArrayList<>();
        ChatMessage userChatMessage = new ChatMessage(ChatMessageRole.USER.value(), userMessage);
        messages.add(userChatMessage);
        ChatMessage systemChatMessage = new ChatMessage(ChatMessageRole.SYSTEM.value(), systemMessage);
        messages.add(systemChatMessage);
        return doRequest(messages, Boolean.FALSE, STABLE_TEMPERATURE);
    }

    /**
     * 同步请求
     *
     * @param userMessage
     * @param systemMessage
     * @return
     */
    public String doSyncRequest(String userMessage, String systemMessage) {
        List<ChatMessage> messages = new ArrayList<>();
        ChatMessage systemChatMessage = new ChatMessage(ChatMessageRole.SYSTEM.value(), systemMessage);
        messages.add(systemChatMessage);
        ChatMessage userChatMessage = new ChatMessage(ChatMessageRole.USER.value(), userMessage);
        messages.add(userChatMessage);
        return doRequest(messages, Boolean.FALSE, DEFAULT_TEMPERATURE);
    }

    /**
     * 通用的流式请求
     *
     * @param messages
     * @param temperature
     * @return
     */
    public Flowable<ModelData> doStreamRequest(List<ChatMessage> messages, Float temperature) {
        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
                .model(Constants.ModelChatGLM4)
                .stream(Boolean.TRUE)
                .invokeMethod(Constants.invokeMethod)
                .temperature(temperature)
                .messages(messages)
                .build();
        try {
            ModelApiResponse invokeModelApiResp = client.invokeModelApi(chatCompletionRequest);
            return invokeModelApiResp.getFlowable();
        } catch (Exception e) {
            e.printStackTrace();
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, e.getMessage());
        }
    }

    /**
     * 通用的流式请求（简化消息传递）
     *
     * @param userMessage
     * @param systemMessage
     * @return
     */
    public Flowable<ModelData> doStreamRequest(String userMessage, String systemMessage) {
        List<ChatMessage> messages = new ArrayList<>();
        ChatMessage userChatMessage = new ChatMessage(ChatMessageRole.USER.value(), userMessage);
        messages.add(userChatMessage);
        ChatMessage systemChatMessage = new ChatMessage(ChatMessageRole.SYSTEM.value(), systemMessage);
        messages.add(systemChatMessage);
        return doStreamRequest(messages, DEFAULT_TEMPERATURE);
    }

    /**
     * 通用的稳定的流式请求（简化消息传递）
     *
     * @param userMessage
     * @param systemMessage
     * @return
     */
    public Flowable<ModelData> doStreamStableRequest(String userMessage, String systemMessage) {
        List<ChatMessage> messages = new ArrayList<>();
        ChatMessage userChatMessage = new ChatMessage(ChatMessageRole.USER.value(), userMessage);
        messages.add(userChatMessage);
        ChatMessage systemChatMessage = new ChatMessage(ChatMessageRole.SYSTEM.value(), systemMessage);
        messages.add(systemChatMessage);
        return doStreamRequest(messages, STABLE_TEMPERATURE);
    }

}
