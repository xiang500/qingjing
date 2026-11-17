package com.czit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.czit.entity.Clothing;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ClothingMapper extends BaseMapper<Clothing> {
}