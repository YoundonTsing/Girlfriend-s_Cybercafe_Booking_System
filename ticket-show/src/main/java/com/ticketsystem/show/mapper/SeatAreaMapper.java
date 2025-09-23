package com.ticketsystem.show.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ticketsystem.show.entity.SeatArea;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 座位区域Mapper接口
 */
@Mapper
public interface SeatAreaMapper extends BaseMapper<SeatArea> {

    /**
     * 根据机位类型获取可访问的座位区域
     */
    @Select("SELECT * FROM t_seat_area WHERE is_deleted = 0 " +
            "AND (show_type_access IS NULL OR FIND_IN_SET(#{showType}, show_type_access) > 0) " +
            "ORDER BY floor_level, area_type, id")
    List<SeatArea> selectAreasByShowType(@Param("showType") Integer showType);

    /**
     * 根据楼层获取座位区域
     */
    @Select("SELECT * FROM t_seat_area WHERE is_deleted = 0 AND floor_level = #{floorLevel} " +
            "ORDER BY area_type, id")
    List<SeatArea> selectAreasByFloor(@Param("floorLevel") Integer floorLevel);
}