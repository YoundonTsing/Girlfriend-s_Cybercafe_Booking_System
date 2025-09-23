package com.ticketsystem.show.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ticketsystem.show.entity.Show;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ShowMapper extends BaseMapper<Show> {
}