package cn.nuaa.jensonxu.fairy.integration.chat.advisor;

import cn.nuaa.jensonxu.fairy.common.data.llm.ChatFileDTO;
import cn.nuaa.jensonxu.fairy.common.file.FileProcessResult;
import cn.nuaa.jensonxu.fairy.common.file.FileProcessor;
import cn.nuaa.jensonxu.fairy.common.file.FileType;
import cn.nuaa.jensonxu.fairy.common.repository.minio.MinioProperties;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.jetbrains.annotations.NotNull;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.MimeType;

import reactor.core.publisher.Flux;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * 文件处理 Advisor
 * 在请求发送给模型之前，从 MinIO 下载文件并注入到 UserMessage 中：
 * - 文档类型：提取的文本追加到消息文本
 * - 图片类型：以 Media 对象附加到 UserMessage
 */
@Slf4j
@RequiredArgsConstructor
public class FileAdvisor implements StreamAdvisor, CallAdvisor {

    public static final String FILES_KEY = "files";

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    @NotNull
    @Override
    public String getName() {
        return "FileAdvisor";
    }

    @Override
    public int getOrder() {
        return -10;  // 在 ChatMemoryAdvisor(0) 之前执行
    }

    @NotNull
    @Override
    public ChatClientResponse adviseCall(@NotNull ChatClientRequest request, @NotNull CallAdvisorChain chain) {
        ChatClientRequest enhancedRequest = enhanceWithFiles(request);
        return chain.nextCall(enhancedRequest);
    }

    @NotNull
    @Override
    public Flux<ChatClientResponse> adviseStream(@NotNull ChatClientRequest request, StreamAdvisorChain chain) {
        ChatClientRequest enhancedRequest = enhanceWithFiles(request);
        return chain.nextStream(enhancedRequest);
    }

    /**
     * 从 MinIO 获取文件流
     */
    private InputStream getFileStream(String objectName) throws Exception {
        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(minioProperties.getBucketName())
                        .object(objectName)
                        .build()
        );
    }

    @SuppressWarnings("unchecked")
    private ChatClientRequest enhanceWithFiles(ChatClientRequest request) {
        List<ChatFileDTO> files = (List<ChatFileDTO>) request.context().get(FILES_KEY);
        if (files == null || files.isEmpty()) {
            return request;
        }

        // 下载并处理每个文件
        List<FileProcessResult> documentResults = new ArrayList<>();
        List<FileProcessResult> imageResults = new ArrayList<>();

        for (ChatFileDTO file : files) {
            try {
                InputStream inputStream = getFileStream(file.getFileId());
                FileProcessResult result = FileProcessor.process(inputStream, file.getFileName());

                if (result.isSuccess()) {
                    if (result.getFileType() == FileType.DOCUMENT) {
                        documentResults.add(result);
                    } else if (result.getFileType() == FileType.IMAGE) {
                        imageResults.add(result);
                    }
                    log.info("[file-advisor] 文件处理成功: {}, 类型: {}", file.getFileName(), result.getFileType());
                } else {
                    log.warn("[file-advisor] 文件处理失败: {}, 原因: {}", file.getFileName(), result.getErrorMessage());
                }
            } catch (Exception e) {
                log.error("[file-advisor] 文件下载或处理异常: {}", file.getFileName(), e);
            }
        }

        if (documentResults.isEmpty() && imageResults.isEmpty()) {
            return request;
        }

        // 增强消息列表中的 UserMessage
        List<Message> enhancedMessages = new ArrayList<>();
        for (Message message : request.prompt().getInstructions()) {
            if (message instanceof UserMessage userMessage) {
                enhancedMessages.add(enhanceUserMessage(userMessage, documentResults, imageResults));
            } else {
                enhancedMessages.add(message);
            }
        }

        Prompt enhancedPrompt = new Prompt(enhancedMessages, request.prompt().getOptions());

        return ChatClientRequest.builder()
                .prompt(enhancedPrompt)
                .context(request.context())
                .build();
    }

    private UserMessage enhanceUserMessage(UserMessage original,
                                           List<FileProcessResult> documents,
                                           List<FileProcessResult> images) {
        // 1. 文档内容追加到文本
        String text = original.getText();
        if (!documents.isEmpty()) {
            StringBuilder sb = new StringBuilder(text);
            sb.append("\n\n--- 以下是附件内容 ---");
            for (FileProcessResult doc : documents) {
                sb.append("\n\n[").append(doc.getFileName()).append("]\n");
                sb.append(doc.getTextContent());
            }
            text = sb.toString();
        }

        // 2. 构建增强后的 UserMessage
        UserMessage.Builder builder = UserMessage.builder()
                .text(text)
                .metadata(original.getMetadata());

        // 3. 图片作为 Media 附加
        if (!images.isEmpty()) {
            List<Media> mediaList = new ArrayList<>();
            for (FileProcessResult img : images) {
                byte[] imageBytes = Base64.getDecoder().decode(img.getBase64Content());
                Media media = new Media(MimeType.valueOf(img.getMimeType()), new ByteArrayResource(imageBytes));
                mediaList.add(media);
            }
            builder.media(mediaList);
        }

        return builder.build();
    }
}
