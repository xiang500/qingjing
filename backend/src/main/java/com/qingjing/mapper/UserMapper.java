package com.qingjing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qingjing.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}