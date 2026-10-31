import { onBeforeUnmount, ref, type Ref } from 'vue'

/**
 * 浏览器端姿态识别：MediaPipe PoseLandmarker 跑在 main thread，
 * 视频帧不出本机。模型与 wasm 均由本地 public 目录提供（npm run fetch:assets）。
 *
 * 摄像头/模型不可用时只降级提示，不阻塞 3D 试衣。
 */
const CONNECTIONS: Array<[number, number]> = [
  [11, 12], [11, 13], [13, 15], [12, 14], [14, 16],
  [11, 23], [12, 24], [23, 24], [23, 25], [25, 27], [24, 26], [26, 28],
]

export type TrackerStatus = 'idle' | 'loading' | 'ready' | 'error'

export function usePoseTracker(video: Ref<HTMLVideoElement | null>, overlay: Ref<HTMLCanvasElement | null>) {
  const status = ref<TrackerStatus>('idle')
  const message = ref('')
  const fps = ref(0)

  let landmarker: {
    detectForVideo: (video: HTMLVideoElement, ts: number) => { landmarks: Array<Array<{ x: number; y: number; visibility?: number }>> }
    close: () => void
  } | null = null
  let stream: MediaStream | null = null
  let frameId = 0
  let lastVideoTime = -1
  let frames = 0
  let lastFpsAt = 0

  async function start() {
    if (status.value === 'ready' || status.value === 'loading') {
      return
    }
    status.value = 'loading'
    message.value = '正在加载 MediaPipe 姿态模型…'
    try {
      const vision = await import('@mediapipe/tasks-vision')
      const fileset = await vision.FilesetResolver.forVisionTasks('/mediapipe-wasm')
      landmarker = (await vision.PoseLandmarker.createFromOptions(fileset, {
        baseOptions: {
          modelAssetPath: '/models/pose_landmarker_lite.task',
          delegate: 'GPU',
        },
        runningMode: 'VIDEO',
        numPoses: 1,
        minPoseDetectionConfidence: 0.5,
        minTrackingConfidence: 0.5,
      })) as unknown as typeof landmarker

      stream = await navigator.mediaDevices.getUserMedia({
        video: { width: { ideal: 640 }, height: { ideal: 480 }, facingMode: 'user' },
        audio: false,
      })
      const element = video.value
      if (!element) {
        throw new Error('视频元素未就绪')
      }
      element.srcObject = stream
      await element.play()

      status.value = 'ready'
      message.value = '端侧推理运行中 · 视频帧不会离开本机'
      loop()
    } catch (error) {
      status.value = 'error'
      message.value = error instanceof Error
        ? `姿态识别不可用：${error.message}（可继续使用 3D 试衣）`
        : '姿态识别不可用，可继续使用 3D 试衣'
      stopStream()
    }
  }

  function loop() {
    const element = video.value
    const canvas = overlay.value
    if (!element || !canvas || !landmarker) {
      return
    }
    if (element.readyState >= 2 && element.currentTime !== lastVideoTime) {
      lastVideoTime = element.currentTime
      const result = landmarker.detectForVideo(element, performance.now())
      draw(canvas, element, result.landmarks?.[0])
      frames += 1
      const now = performance.now()
      if (now - lastFpsAt > 1000) {
        fps.value = frames
        frames = 0
        lastFpsAt = now
      }
    }
    frameId = requestAnimationFrame(loop)
  }

  function draw(
    canvas: HTMLCanvasElement,
    videoElement: HTMLVideoElement,
    landmarks: Array<{ x: number; y: number; visibility?: number }> | undefined,
  ) {
    const width = videoElement.clientWidth
    const height = videoElement.clientHeight
    if (canvas.width !== width || canvas.height !== height) {
      canvas.width = width
      canvas.height = height
    }
    const ctx = canvas.getContext('2d')
    if (!ctx) {
      return
    }
    ctx.clearRect(0, 0, width, height)
    if (!landmarks) {
      return
    }
    const point = (index: number) => ({
      x: (1 - landmarks[index].x) * width,
      y: landmarks[index].y * height,
    })
    ctx.lineWidth = 2
    ctx.strokeStyle = 'rgba(214, 189, 128, 0.85)'
    for (const [from, to] of CONNECTIONS) {
      const a = point(from)
      const b = point(to)
      ctx.beginPath()
      ctx.moveTo(a.x, a.y)
      ctx.lineTo(b.x, b.y)
      ctx.stroke()
    }
    ctx.fillStyle = 'rgba(240, 226, 191, 0.95)'
    for (const item of landmarks) {
      ctx.beginPath()
      ctx.arc((1 - item.x) * width, item.y * height, 2.6, 0, Math.PI * 2)
      ctx.fill()
    }
  }

  function stopStream() {
    stream?.getTracks().forEach((track) => track.stop())
    stream = null
  }

  function stop() {
    cancelAnimationFrame(frameId)
    landmarker?.close()
    landmarker = null
    stopStream()
    if (video.value) {
      video.value.srcObject = null
    }
    status.value = 'idle'
    message.value = ''
    fps.value = 0
  }

  onBeforeUnmount(stop)

  return { status, message, fps, start, stop }
}