package cn.nuaa.jensonxu.fairy.repository.mysql;

import cn.nuaa.jensonxu.fairy.repository.mysql.data.FileUploadRecordDO;
import cn.nuaa.jensonxu.fairy.repository.mysql.mapper.FileUploadRecordMapper;

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
