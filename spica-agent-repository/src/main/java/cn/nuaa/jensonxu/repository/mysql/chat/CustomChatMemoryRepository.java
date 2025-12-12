package cn.nuaa.jensonxu.repository.mysql.chat;

import cn.nuaa.jensonxu.repository.mysql.data.CustomChatMessageDO;
import cn.nuaa.jensonxu.repository.mysql.mapper.CustomChatMessageMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class CustomChatMemoryRepository {

    private final CustomChatMessageMapper customChatMessageMapper;

    @Autowired
    public CustomChatMemoryRepository(CustomChatMessageMapper customChatMessageMapper) {
        this.customChatMessageMapper = customChatMessageMapper;
    }

    public void insert(CustomChatMessageDO customChatMessageDO) {
        customChatMessageMapper.insert(customChatMessageDO);
    }

    public List<CustomChatMessageDO> selectByConversationId(String conversationId) {
        return customChatMessageMapper.selectList(new LambdaQueryWrapper<CustomChatMessageDO>()
                .eq(CustomChatMessageDO::getConversationId, conversationId));
    }
}
