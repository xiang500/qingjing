// 拉取运行时大资源：MediaPipe 姿态模型 + wasm 运行时。
// 这些文件不进版本库（体积大），首次启动前端前执行一次：npm run fetch:assets
import fs from 'node:fs'
import path from 'node:path'
import https from 'node:https'
import { fileURLToPath } from 'node:url'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const modelsDir = path.join(root, 'public', 'models')
const wasmDir = path.join(root, 'public', 'mediapipe-wasm')

fs.mkdirSync(modelsDir, { recursive: true })
fs.mkdirSync(wasmDir, { recursive: true })

const models = [
  {
    file: 'pose_landmarker_lite.task',
    url: 'https://storage.googleapis.com/mediapipe-models/pose_landmarker/pose_landmarker_lite/float16/1/pose_landmarker_lite.task',
  },
  {
    file: 'selfie_segmenter.tflite',
    url: 'https://storage.googleapis.com/mediapipe-models/image_segmenter/selfie_segmenter/float16/latest/selfie_segmenter.tflite',
  },
]

function download(url, dest) {
  return new Promise((resolve, reject) => {
    const request = https.get(url, (response) => {
      if (response.statusCode && response.statusCode >= 300 && response.statusCode < 400 && response.headers.location) {
        response.resume()
        download(response.headers.location, dest).then(resolve, reject)
        return
      }
      if (response.statusCode !== 200) {
        response.resume()
        reject(new Error(`HTTP ${response.statusCode} ${url}`))
        return
      }
      const file = fs.createWriteStream(dest)
      response.pipe(file)
      file.on('finish', () => file.close(() => resolve(dest)))
      file.on('error', reject)
    })
    request.on('error', reject)
  })
}

function copyWasm() {
  const source = path.join(root, 'node_modules', '@mediapipe', 'tasks-vision', 'wasm')
  if (!fs.existsSync(source)) {
    console.warn('[assets] 未找到 @mediapipe/tasks-vision，请先 npm install')
    return
  }
  for (const name of fs.readdirSync(source)) {
    if (!/\.(wasm|js)$/.test(name)) continue
    // 只保留 SIMD 与 no-SIMD 两套运行时，模块版本仅在特殊场景使用
    if (name.includes('vision_wasm_module')) continue
    fs.copyFileSync(path.join(source, name), path.join(wasmDir, name))
  }
}

async function main() {
  for (const model of models) {
    const dest = path.join(modelsDir, model.file)
    if (fs.existsSync(dest) && fs.statSync(dest).size > 1024) {
      console.log(`[assets] 已存在 ${model.file}`)
      continue
    }
    try {
      await download(model.url, dest)
      console.log(`[assets] 下载完成 ${model.file} ${(fs.statSync(dest).size / 1024 / 1024).toFixed(1)}MB`)
    } catch (error) {
      console.warn(`[assets] 下载失败 ${model.file}: ${error.message}（姿态识别将自动降级）`)
    }
  }
  copyWasm()
  console.log('[assets] 完成')
}

main()