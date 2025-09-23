package com.ticketsystem.show.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ticketsystem.show.entity.Seat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 座位Mapper接口
 */
@Mapper
public interface SeatMapper extends BaseMapper<Seat> {

    /**
     * 锁定座位
     */
    @Update("UPDATE t_seat SET lock_status = 1, lock_user_id = #{userId}, " +
            "lock_time = NOW(), lock_expire_time = DATE_ADD(NOW(), INTERVAL 5 MINUTE) " +
            "WHERE id = #{seatId} AND lock_status = 0 AND status = 1 AND is_deleted = 0")
    int lockSeat(@Param("seatId") Long seatId, @Param("userId") Long userId);

    /**
     * 释放座位锁定
     */
    @Update("UPDATE t_seat SET lock_status = 0, lock_user_id = NULL, " +
            "lock_time = NULL, lock_expire_time = NULL " +
            "WHERE id = #{seatId} AND lock_user_id = #{userId} AND is_deleted = 0")
    int releaseSeat(@Param("seatId") Long seatId, @Param("userId") Long userId);

    /**
     * 批量锁定座位
     */
    @Update("<script>" +
            "UPDATE t_seat SET lock_status = 1, lock_user_id = #{userId}, " +
            "lock_time = NOW(), lock_expire_time = DATE_ADD(NOW(), INTERVAL 5 MINUTE) " +
            "WHERE id IN " +
            "<foreach collection='seatIds' item='seatId' open='(' separator=',' close=')'>" +
            "#{seatId}" +
            "</foreach>" +
            " AND lock_status = 0 AND status = 1 AND is_deleted = 0" +
            "</script>")
    int lockSeats(@Param("seatIds") List<Long> seatIds, @Param("userId") Long userId);

    /**
     * 批量释放座位锁定
     */
    @Update("<script>" +
            "UPDATE t_seat SET lock_status = 0, lock_user_id = NULL, " +
            "lock_time = NULL, lock_expire_time = NULL " +
            "WHERE id IN " +
            "<foreach collection='seatIds' item='seatId' open='(' separator=',' close=')'>" +
            "#{seatId}" +
            "</foreach>" +
            " AND lock_user_id = #{userId} AND is_deleted = 0" +
            "</script>")
    int releaseSeats(@Param("seatIds") List<Long> seatIds, @Param("userId") Long userId);

    /**
     * 清理过期锁定
     */
    @Update("UPDATE t_seat SET lock_status = 0, lock_user_id = NULL, " +
            "lock_time = NULL, lock_expire_time = NULL " +
            "WHERE lock_status = 1 AND lock_expire_time < NOW() AND is_deleted = 0")
    int clearExpiredLocks();

    /**
     * 根据区域ID获取座位列表
     */
    @Select("SELECT * FROM t_seat WHERE area_id = #{areaId} AND is_deleted = 0 " +
            "ORDER BY row_num, seat_num")
    List<Seat> selectSeatsByAreaId(@Param("areaId") Long areaId);

    /**
     * 查询区域可用座位数量
     */
    @Select("SELECT COUNT(*) FROM t_seat WHERE area_id = #{areaId} AND status = 1 " +
            "AND lock_status = 0 AND is_deleted = 0")
    int countAvailableSeatsByAreaId(@Param("areaId") Long areaId);

    /**
     * 根据座位ID查询座位详细信息（用于数据一致性验证）
     */
    @Select("SELECT id, area_id, row_num, seat_num, status, lock_status, " +
            "lock_user_id, lock_time, lock_expire_time FROM t_seat " +
            "WHERE id = #{seatId} AND is_deleted = 0")
    Seat selectSeatById(@Param("seatId") Long seatId);

    /**
     * 批量查询座位状态（用于一致性检查）
     */
    @Select("<script>" +
            "SELECT id, area_id, row_num, seat_num, status, lock_status, " +
            "lock_user_id, lock_time, lock_expire_time FROM t_seat " +
            "WHERE id IN " +
            "<foreach collection='seatIds' item='seatId' open='(' separator=',' close=')'>" +
            "#{seatId}" +
            "</foreach>" +
            " AND is_deleted = 0" +
            "</script>")
    List<Seat> selectSeatsByIds(@Param("seatIds") List<Long> seatIds);

    /**
     * 更新座位锁定状态
     */
    @Update("UPDATE t_seat SET lock_status = #{lockStatus}, lock_user_id = #{lockUserId}, " +
            "lock_time = NOW(), lock_expire_time = DATE_ADD(NOW(), INTERVAL 5 MINUTE) " +
            "WHERE id = #{seatId} AND is_deleted = 0")
    int updateLockStatus(@Param("seatId") Long seatId, 
                        @Param("lockStatus") int lockStatus, 
                        @Param("lockUserId") Long lockUserId);
}