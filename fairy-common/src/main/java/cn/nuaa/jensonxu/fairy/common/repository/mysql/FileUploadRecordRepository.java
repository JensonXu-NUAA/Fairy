package cn.nuaa.jensonxu.fairy.common.repository.mysql;

import cn.nuaa.jensonxu.fairy.common.repository.mysql.data.FileUploadRecordDO;
import cn.nuaa.jensonxu.fairy.common.repository.mysql.mapper.FileUploadRecordMapper;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class FileUploadRecordRepository {

    private final FileUploadRecordMapper mapper;

    public void insert(FileUploadRecordDO fileUploadRecordDO){
        mapper.insert(fileUploadRecordDO);
    }
}
