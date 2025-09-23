package com.ticketsystem.show.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 票档库存实体类
 * 用于高并发场景下的库存管理，支持乐观锁机制
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("t_ticket_stock")
public class TicketStock implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 票档ID
     */
    @TableField("ticket_id")
    private Long ticketId;

    /**
     * 总库存
     */
    @TableField("total_stock")
    private Integer totalStock;

    /**
     * 锁定库存
     */
    @TableField("locked_stock")
    private Integer lockedStock;

    /**
     * 已售库存
     */
    @TableField("sold_stock")
    private Integer soldStock;

    /**
     * 乐观锁版本号
     */
    @Version
    @TableField("version")
    private Integer version;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 可用库存（计算字段，用于前端显示）
     */
    @TableField(exist = false)
    private Integer availableStock;

    /**
     * 获取可用库存
     * @return 可用库存数量
     */
    public Integer getAvailableStock() {
        if (availableStock != null) {
            return availableStock;
        }
        return totalStock - lockedStock - soldStock;
    }

    /**
     * 设置可用库存（用于计算后设置）
     */
    public void setAvailableStock(Integer availableStock) {
        this.availableStock = availableStock;
    }

    /**
     * 检查是否有足够的可用库存
     * @param quantity 需要的数量
     * @return 是否有足够库存
     */
    public boolean hasEnoughStock(Integer quantity) {
        return getAvailableStock() >= quantity;
    }
}