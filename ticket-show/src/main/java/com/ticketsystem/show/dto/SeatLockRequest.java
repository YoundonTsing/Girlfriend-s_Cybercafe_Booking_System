package com.ticketsystem.show.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 座位锁定请求DTO
 */
@Data
public class SeatLockRequest {

    /**
     * 座位ID列表
     */
    @NotEmpty(message = "座位ID列表不能为空")
    private List<@NotNull(message = "座位ID不能为空") Long> seatIds;
}