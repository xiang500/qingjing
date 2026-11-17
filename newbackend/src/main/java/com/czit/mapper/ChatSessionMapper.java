package com.czit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.czit.entity.ChatSession;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSession> {
}