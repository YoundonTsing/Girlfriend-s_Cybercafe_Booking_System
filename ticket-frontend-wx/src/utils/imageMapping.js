/**
 * 订单图片映射工具
 * 根据订单/演出名称返回对应的本地SVG图片路径
 */

/**
 * 获取演出/订单对应的图片
 * @param {string} showName - 演出或机位名称
 * @returns {string} 图片路径
 */
export function getShowPoster(showName) {
  if (!showName) {
    return '/static/images/5.png' // 使用5号图片作为默认
  }
  
  const name = showName.toLowerCase()
  
  // 网咖机位类型匹配（优先级高）
  if (name.includes('svip') || name.includes('超级vip') || name.includes('至尊')) {
    return '/static/images/1.png' // SVIP使用1号图片
  } else if (name.includes('vip') || name.includes('包间') || name.includes('豪华')) {
    return '/static/images/2.png' // VIP使用2号图片
  } else if (name.includes('高级') || name.includes('advanced') || name.includes('高端')) {
    return '/static/images/3.png' // 高级使用3号图片
  } else if (name.includes('新手') || name.includes('新客') || name.includes('newbie') || name.includes('入门')) {
    return '/static/images/4.png' // 新手使用4号图片
  } else if (name.includes('中级') || name.includes('intermediate') || name.includes('标准')) {
    return '/static/images/5.png' // 中级使用5号图片
  }
  
  // 传统演出类型匹配（轮播图使用）
  if (name.includes('音乐') || name.includes('演唱会') || name.includes('concert') || name.includes('热门音乐会')) {
    return '/static/images/1.png' // 音乐会使用1号图片
  } else if (name.includes('话剧') || name.includes('戏剧') || name.includes('drama') || name.includes('经典话剧')) {
    return '/static/images/2.png' // 话剧使用2号图片
  } else if (name.includes('舞蹈') || name.includes('dance') || name.includes('舞蹈演出')) {
    return '/static/images/3.png' // 舞蹈使用3号图片
  } else if (name.includes('体育') || name.includes('nba') || name.includes('sport')) {
    return '/static/images/4.png' // 体育赛事使用4号图片
  } else if (name.includes('电影') || name.includes('movie') || name.includes('cinema')) {
    return '/static/images/5.png' // 电影使用5号图片
  }
  
  // 默认返回5号图片
  return '/static/images/5.png'
}

/**
 * 图片映射表
 * 用于文档说明和调试
 */
export const IMAGE_MAPPING = {
  // 网咖机位类型
  'SVIP机位': '/static/images/seat_svip.svg',
  'VIP包间': '/static/images/seat_vip_room.svg',
  '高级机位': '/static/images/seat_advanced.svg',
  '中级机位': '/static/images/seat_intermediate.svg',
  '新手机位': '/static/images/seat_newbie.svg',
  
  // 演出类型映射
  '音乐会': '/static/images/seat_svip.svg',
  '话剧': '/static/images/seat_vip_room.svg',
  '体育赛事': '/static/images/seat_advanced.svg',
  '电影': '/static/images/seat_intermediate.svg',
  
  // 默认
  '默认': '/static/images/seat_intermediate.svg'
}

/**
 * 获取所有可用的SVG图片列表
 */
export const SEAT_ICONS = [
  '/static/images/seat_svip.svg',
  '/static/images/seat_vip_room.svg',
  '/static/images/seat_advanced.svg',
  '/static/images/seat_intermediate.svg',
  '/static/images/seat_newbie.svg'
]

/**
 * 图片描述信息
 */
export const SEAT_DESCRIPTIONS = {
  '/static/images/seat_svip.svg': 'SVIP座位 - 至尊体验，顶级配置',
  '/static/images/seat_vip_room.svg': 'VIP包间 - 私密空间，尊享服务',
  '/static/images/seat_advanced.svg': '高级座位 - 优质体验，高端配置',
  '/static/images/seat_intermediate.svg': '标准座位 - 舒适体验，均衡配置',
  '/static/images/seat_newbie.svg': '新手座位 - 入门首选，性价比高'
}