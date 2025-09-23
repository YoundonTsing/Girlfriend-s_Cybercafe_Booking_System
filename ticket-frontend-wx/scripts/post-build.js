/**
 * 微信小程序构建后处理脚本
 * 解决微信开发者工具期望在 dist/dev 根目录找到 app.json 的问题
 */

import { readFileSync, writeFileSync, copyFileSync, existsSync, mkdirSync } from 'fs'
import { join, dirname } from 'path'
import { fileURLToPath } from 'url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = dirname(__filename)

const PROJECT_ROOT = join(__dirname, '..')
const DEV_DIR = join(PROJECT_ROOT, 'dist/dev')
const MP_WEIXIN_DIR = join(DEV_DIR, 'mp-weixin')

console.log('🔧 开始微信小程序构建后处理...')

try {
  // 确保目录存在
  if (!existsSync(DEV_DIR)) {
    mkdirSync(DEV_DIR, { recursive: true })
  }

  // 复制必要的配置文件到 dev 根目录
  const filesToCopy = [
    'app.json',
    'project.config.json', 
    'project.private.config.json'
  ]

  filesToCopy.forEach(file => {
    const sourcePath = join(MP_WEIXIN_DIR, file)
    const targetPath = join(DEV_DIR, file)
    
    if (existsSync(sourcePath)) {
      if (file === 'app.json') {
        // 特殊处理 app.json，修改页面路径前缀
        const appConfig = JSON.parse(readFileSync(sourcePath, 'utf8'))
        
        // 为所有页面路径添加 mp-weixin/ 前缀
        if (appConfig.pages) {
          appConfig.pages = appConfig.pages.map(page => `mp-weixin/${page}`)
        }
        
        // 为 tabBar 页面路径添加前缀
        if (appConfig.tabBar && appConfig.tabBar.list) {
          appConfig.tabBar.list = appConfig.tabBar.list.map(tab => ({
            ...tab,
            pagePath: `mp-weixin/${tab.pagePath}`
          }))
        }
        
        writeFileSync(targetPath, JSON.stringify(appConfig, null, 2))
        console.log(`✅ 已处理并复制 ${file}`)
      } else {
        copyFileSync(sourcePath, targetPath)
        console.log(`✅ 已复制 ${file}`)
      }
    } else {
      console.log(`⚠️  文件不存在: ${file}`)
    }
  })

  console.log('🎉 微信小程序构建后处理完成！')
  console.log('📝 现在可以在微信开发者工具中导入以下目录：')
  console.log(`   ${DEV_DIR}`)

} catch (error) {
  console.error('❌ 构建后处理失败:', error)
  process.exit(1)
}