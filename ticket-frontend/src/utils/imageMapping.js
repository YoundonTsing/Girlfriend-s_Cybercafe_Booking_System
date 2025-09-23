/**
 * 订单图片映射工具
 * 根据订单/演出名称返回对应的本地SVG图片路径
 */

/**
 * 获取演出/订单对应的图片
 * @param {string} showName - 演出或机位名称
 * @returns {string} SVG图片路径
 */
export function getShowPoster(showName) {
  if (!showName) {
    return '/images/seat_intermediate.svg'
  }
  
  const name = showName.toLowerCase()
  
  // 网咖机位类型匹配（优先级高）
  if (name.includes('svip') || name.includes('超级vip') || name.includes('至尊')) {
    return '/images/seat_svip.svg'
  } else if (name.includes('vip') || name.includes('包间') || name.includes('豪华')) {
    return '/images/seat_vip_room.svg'
  } else if (name.includes('高级') || name.includes('advanced') || name.includes('高端')) {
    return '/images/seat_advanced.svg'
  } else if (name.includes('新手') || name.includes('新客') || name.includes('newbie') || name.includes('入门')) {
    return '/images/seat_newbie.svg'
  } else if (name.includes('中级') || name.includes('intermediate') || name.includes('标准')) {
    return '/images/seat_intermediate.svg'
  }
  
  // 传统演出类型匹配（也使用座位图标）
  if (name.includes('音乐') || name.includes('演唱会') || name.includes('concert')) {
    return '/images/seat_svip.svg' // 音乐会使用最高级座位
  } else if (name.includes('话剧') || name.includes('戏剧') || name.includes('drama')) {
    return '/images/seat_vip_room.svg' // 话剧使用VIP包间
  } else if (name.includes('体育') || name.includes('nba') || name.includes('sport')) {
    return '/images/seat_advanced.svg' // 体育赛事使用高级座位
  } else if (name.includes('电影') || name.includes('movie') || name.includes('cinema')) {
    return '/images/seat_intermediate.svg' // 电影使用标准座位
  }
  
  // 默认返回中级座位
  return '/images/seat_intermediate.svg'
}

/**
 * 图片映射表
 * 用于文档说明和调试
 */
export const IMAGE_MAPPING = {
  // 网咖机位类型
  'SVIP机位': '/images/seat_svip.svg',
  'VIP包间': '/images/seat_vip_room.svg', 
  '高级机位': '/images/seat_advanced.svg',
  '中级机位': '/images/seat_intermediate.svg',
  '新手机位': '/images/seat_newbie.svg',
  
  // 演出类型映射
  '音乐会': '/images/seat_svip.svg',
  '话剧': '/images/seat_vip_room.svg',
  '体育赛事': '/images/seat_advanced.svg',
  '电影': '/images/seat_intermediate.svg',
  
  // 默认
  '默认': '/images/seat_intermediate.svg'
}

/**
 * 获取所有可用的SVG图片列表
 */
export const AVAILABLE_IMAGES = [
  '/images/seat_svip.svg',
  '/images/seat_vip_room.svg',
  '/images/seat_advanced.svg',
  '/images/seat_intermediate.svg',
  '/images/seat_newbie.svg'
]

/**
 * 图片描述信息
 */
export const IMAGE_DESCRIPTIONS = {
  '/images/seat_svip.svg': 'SVIP座位 - 至尊体验，顶级配置',
  '/images/seat_vip_room.svg': 'VIP包间 - 私密空间，尊享服务',
  '/images/seat_advanced.svg': '高级座位 - 优质体验，高端配置',
  '/images/seat_intermediate.svg': '标准座位 - 舒适体验，均衡配置',
  '/images/seat_newbie.svg': '新手座位 - 入门首选，性价比高'
}