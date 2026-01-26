package cn.nuaa.jensonxu.fairy.repository.mysql;

import cn.nuaa.jensonxu.fairy.repository.mysql.data.CustomChatMessageDO;
import cn.nuaa.jensonxu.fairy.repository.mysql.mapper.CustomChatMessageMapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class CustomChatMemoryRepository {

    private final CustomChatMessageMapper customChatMessageMapper;

    public void insert(CustomChatMessageDO customChatMessageDO) {
        customChatMessageMapper.insert(customChatMessageDO);
    }

    public List<CustomChatMessageDO> selectByConversationId(String conversationId) {
        return customChatMessageMapper.selectList(new LambdaQueryWrapper<CustomChatMessageDO>()
                .eq(CustomChatMessageDO::getConversationId, conversationId));
    }
}
