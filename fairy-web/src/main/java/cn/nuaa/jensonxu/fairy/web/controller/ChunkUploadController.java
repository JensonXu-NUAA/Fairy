package cn.nuaa.jensonxu.fairy.web.controller;

import cn.nuaa.jensonxu.fairy.service.data.request.ChunkMergeDTO;
import cn.nuaa.jensonxu.fairy.service.data.request.QueryChunkStatusDTO;
import cn.nuaa.jensonxu.fairy.service.data.response.vo.ChunkMergeResultVO;
import cn.nuaa.jensonxu.fairy.service.data.response.vo.ChunkStatusVO;
import cn.nuaa.jensonxu.fairy.service.data.response.vo.ChunkUploadResultVO;
import cn.nuaa.jensonxu.fairy.service.file.ChunkUploadService;
import cn.nuaa.jensonxu.fairy.service.data.request.ChunkInitDTO;
import cn.nuaa.jensonxu.fairy.service.data.response.CustomResponse;
import cn.nuaa.jensonxu.fairy.service.data.response.vo.ChunkUploadInitVO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 分片上传
 */
@Slf4j
@RestController
@RequestMapping("/api/chunk")
@RequiredArgsConstructor
public class ChunkUploadController {

    private final ChunkUploadService chunkUploadService;

    /**
     * 分片上传初始化
     * @param request 初始化请求
     * @return 初始化响应
     */
    @PostMapping("/init")
    public CustomResponse<ChunkUploadInitVO> initChunkUpload(@Validated @RequestBody ChunkInitDTO request) {
        try {
            log.info("[chunk] 接收到初始化请求, 文件名: {}, MD5: {}, 大小: {}字节", request.getFileName(), request.getFileMd5(), request.getFileSize());
            ChunkUploadInitVO vo = chunkUploadService.initChunkUpload(request);
            log.info("[chunk] 初始化成功, MD5: {}, 已上传: {}/{}, 进度: {}%", vo.getFileMd5(), vo.getUploadedChunks(), vo.getTotalChunks(), vo.getUploadProgress());
            return CustomResponse.success(vo);

        } catch (Exception e) {
            log.error("[chunk] 初始化失败, 文件MD5: {}", request.getFileMd5(), e);
            return CustomResponse.error("初始化失败: " + e.getMessage());
        }
    }

    /**
     * 分片上传
     * @param fileMd5 文件MD5
     * @param chunkIndex 分片索引
     * @param file 分片文件
     * @return 上传结果
     */
    @PostMapping("/upload")
    public CustomResponse<ChunkUploadResultVO> uploadChunk(@RequestParam("fileMd5") String fileMd5,
                                                           @RequestParam("chunkIndex") Integer chunkIndex,
                                                           @RequestParam("file") MultipartFile file) {
        try {
            // 参数校验
            if (fileMd5 == null || fileMd5.trim().isEmpty()) {
                return CustomResponse.error("文件MD5不能为空");
            }
            if (chunkIndex == null || chunkIndex < 0) {
                return CustomResponse.error("分片索引不合法");
            }
            if (file.isEmpty()) {
                return CustomResponse.error("分片文件不能为空");
            }

            log.info("[chunk] 接收到上传请求, MD5: {}, 分片索引: {}, 文件大小: {}字节", fileMd5, chunkIndex, file.getSize());
            ChunkUploadResultVO result = chunkUploadService.uploadChunk(fileMd5, chunkIndex, file);  // 执行上传
            log.info("[chunk] 上传成功, MD5: {}, 分片: {}, 进度: {}%, 是否完成: {}", result.getFileMd5(), result.getChunkIndex(), result.getUploadProgress(), result.getIsComplete());

            if (result.getIsComplete()) {
                return CustomResponse.build(200, "分片上传完成，所有分片已就绪，可调用合并接口", result);  // 如果上传完成，提示用户可以调用合并接口
            }
            return CustomResponse.success(result);

        } catch (Exception e) {
            log.error("[chunk] 上传失败, MD5: {}, 分片索引: {}", fileMd5, chunkIndex, e);
            return CustomResponse.error("上传失败: " + e.getMessage());
        }
    }

    /**
     * 查询分片上传状态
     * @param fileMd5 文件MD5
     * @return 分片状态详情
     */
    @GetMapping("/status")
    public CustomResponse<ChunkStatusVO> queryChunkStatus(@Validated @RequestBody QueryChunkStatusDTO fileMd5) {
        try {
            log.info("[chunk] 分片状态查询, 接收到查询请求, MD5: {}", fileMd5.getFileMd5());

            // 参数校验
            if (fileMd5.getFileMd5().trim().isEmpty()) {
                return CustomResponse.error("文件MD5不能为空");
            }
            ChunkStatusVO statusVO = chunkUploadService.queryChunkStatus(fileMd5.getFileMd5());  // 查询状态
            log.info("[chunk] 分片状态查询, 查询成功, MD5: {}, 已上传: {}/{}, 进度: {}%", statusVO.getFileMd5(), statusVO.getUploadedCount(), statusVO.getTotalChunks(), statusVO.getUploadProgress());

            // 根据完成状态返回不同提示信息
            if (statusVO.getIsComplete()) {
                return CustomResponse.build(200, "所有分片已上传完成, 可调用合并接口", statusVO);
            } else if (statusVO.getUploadedCount() > 0) {
                return CustomResponse.build(200,
                        String.format("已上传 %d/%d 个分片, 可继续上传",
                                statusVO.getUploadedCount(),
                                statusVO.getTotalChunks()),
                        statusVO);
            } else {
                return CustomResponse.build(200, "尚未上传任何分片", statusVO);
            }

        } catch (Exception e) {
            log.error("[chunk] 分片状态查询, 查询失败, MD5: {}", fileMd5.getFileMd5(), e);
            return CustomResponse.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 合并分片文件
     * @param request 合并请求
     * @return 合并结果
     */
    @PostMapping("/merge")
    public CustomResponse<ChunkMergeResultVO> mergeChunks(@Validated @RequestBody ChunkMergeDTO request) {
        try {
            log.info("[chunk] 接收到合并请求, MD5: {}", request.getFileMd5());

            // 参数校验
            if (request.getFileMd5() == null || request.getFileMd5().trim().isEmpty()) {
                return CustomResponse.error("文件MD5不能为空");
            }

            ChunkMergeResultVO result = chunkUploadService.mergeChunks(request.getFileMd5());  // 执行合并
            log.info("[chunk] 文件合并成功, MD5: {}, 最终路径: {}, 耗时: {}ms", result.getFileMd5(), result.getFinalFilePath(), result.getMergeDuration());

            return CustomResponse.success("文件合并成功", result);

        } catch (Exception e) {
            log.error("[chunk] 文件合并失败, MD5: {}", request.getFileMd5(), e);
            return CustomResponse.error("合并失败: " + e.getMessage());
        }
    }
}
