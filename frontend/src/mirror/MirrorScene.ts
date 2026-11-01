import * as THREE from 'three'
import gsap from 'gsap'

/** 传入场景的单品（字段与后端 ClothingVO 对齐，按需裁剪） */
export interface GarmentInput {
  id: number
  name: string
  category: string
  subCategory?: string
  colorHex?: string | null
}

const BODY_COLOR = 0xd8d2c4
const FLOOR_COLOR = 0x10151f

/**
 * 程序化数字人场景。
 *
 * 不依赖外部 GLB 资源：人体由 Capsule / Sphere 组合，服装是包裹在身体外的
 * 「壳层」几何体。换装 = 替换壳层材质与几何，GSAP 负责弹入过渡。
 * 这样既能跑通「AI 推荐 → 换装渲染」的闭环，也没有模型版权与加载体积问题。
 */
export class MirrorScene {
  private readonly canvas: HTMLCanvasElement
  private readonly renderer: THREE.WebGLRenderer
  private readonly scene: THREE.Scene
  private readonly camera: THREE.PerspectiveCamera
  private readonly stage = new THREE.Group()
  private readonly garments = new THREE.Group()
  private readonly clock = new THREE.Clock()

  private frameId = 0
  private dragging = false
  private lastX = 0
  private lastY = 0
  private readonly spin = 0.0035
  private autoRotate = true

  private readonly onPointerDown = (event: PointerEvent) => {
    this.dragging = true
    this.lastX = event.clientX
    this.lastY = event.clientY
    this.canvas.setPointerCapture(event.pointerId)
  }

  private readonly onPointerMove = (event: PointerEvent) => {
    if (!this.dragging) {
      return
    }
    const deltaX = event.clientX - this.lastX
    const deltaY = event.clientY - this.lastY
    this.lastX = event.clientX
    this.lastY = event.clientY
    this.stage.rotation.y += deltaX * 0.008
    this.stage.rotation.x = THREE.MathUtils.clamp(this.stage.rotation.x + deltaY * 0.002, -0.16, 0.16)
  }

  private readonly onPointerUp = (event: PointerEvent) => {
    this.dragging = false
    if (this.canvas.hasPointerCapture(event.pointerId)) {
      this.canvas.releasePointerCapture(event.pointerId)
    }
  }

  private readonly onWheel = (event: WheelEvent) => {
    event.preventDefault()
    this.camera.position.z = THREE.MathUtils.clamp(this.camera.position.z + event.deltaY * 0.002, 3.0, 5.6)
  }

  private readonly onResize = () => {
    const parent = this.canvas.parentElement
    if (!parent) {
      return
    }
    const width = parent.clientWidth
    const height = parent.clientHeight
    if (width === 0 || height === 0) {
      return
    }
    this.renderer.setSize(width, height, false)
    this.camera.aspect = width / height
    this.camera.updateProjectionMatrix()
  }

  constructor(canvas: HTMLCanvasElement) {
    this.canvas = canvas
    this.renderer = new THREE.WebGLRenderer({ canvas, antialias: true, alpha: true })
    this.renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
    this.renderer.shadowMap.enabled = true
    this.renderer.shadowMap.type = THREE.PCFSoftShadowMap
    this.renderer.outputColorSpace = THREE.SRGBColorSpace

    this.scene = new THREE.Scene()
    this.camera = new THREE.PerspectiveCamera(30, 1, 0.1, 50)
    this.camera.position.set(0, 1.0, 4.5)
    this.camera.lookAt(0, 0.95, 0)

    this.buildLights()
    this.buildFloor()
    this.buildMannequin()

    this.stage.add(this.garments)
    this.scene.add(this.stage)

    canvas.addEventListener('pointerdown', this.onPointerDown)
    canvas.addEventListener('pointermove', this.onPointerMove)
    canvas.addEventListener('pointerup', this.onPointerUp)
    canvas.addEventListener('pointercancel', this.onPointerUp)
    canvas.addEventListener('wheel', this.onWheel, { passive: false })

    this.onResize()
    this.loop()
  }

  /** 应用整套搭配：未传入的品类会被移除 */
  setOutfit(items: GarmentInput[]): void {
    for (const child of [...this.garments.children]) {
      this.garments.remove(child)
      disposeObject(child)
    }
    for (const item of items) {
      for (const mesh of this.buildGarment(item)) {
        mesh.userData.garmentId = item.id
        this.garments.add(mesh)
        mesh.scale.setScalar(0.86)
        gsap.to(mesh.scale, { x: 1, y: 1, z: 1, duration: 0.5, ease: 'back.out(1.8)', delay: 0.04 })
      }
    }
  }

  setAutoRotate(value: boolean): void {
    this.autoRotate = value
  }

  resetView(): void {
    this.stage.rotation.set(0, 0, 0)
    this.camera.position.set(0, 1.0, 4.5)
    this.camera.lookAt(0, 0.95, 0)
  }

  dispose(): void {
    cancelAnimationFrame(this.frameId)
    this.canvas.removeEventListener('pointerdown', this.onPointerDown)
    this.canvas.removeEventListener('pointermove', this.onPointerMove)
    this.canvas.removeEventListener('pointerup', this.onPointerUp)
    this.canvas.removeEventListener('pointercancel', this.onPointerUp)
    this.canvas.removeEventListener('wheel', this.onWheel)
    this.scene.traverse((object) => disposeObject(object))
    this.renderer.dispose()
  }

  private loop = () => {
    this.frameId = requestAnimationFrame(this.loop)
    const delta = this.clock.getDelta()
    if (this.autoRotate && !this.dragging) {
      this.stage.rotation.y += this.spin * Math.min(delta * 60, 2)
    }
    this.renderer.render(this.scene, this.camera)
  }

  private buildLights(): void {
    this.scene.add(new THREE.HemisphereLight(0xd9e6ff, 0x080b12, 0.9))

    const key = new THREE.DirectionalLight(0xfff1d6, 2.2)
    key.position.set(2.2, 3.2, 2.6)
    key.castShadow = true
    key.shadow.mapSize.set(1024, 1024)
    key.shadow.camera.near = 0.5
    key.shadow.camera.far = 12
    key.shadow.camera.left = -2
    key.shadow.camera.right = 2
    key.shadow.camera.top = 3
    key.shadow.camera.bottom = -1
    key.shadow.bias = -0.0016
    key.shadow.normalBias = 0.03
    this.scene.add(key)

    const rim = new THREE.PointLight(0x8fb6ff, 14, 8, 2)
    rim.position.set(-2.2, 2.0, -1.8)
    this.scene.add(rim)
  }

  private buildFloor(): void {
    const floor = new THREE.Mesh(
      new THREE.CircleGeometry(1.75, 64),
      new THREE.MeshStandardMaterial({ color: FLOOR_COLOR, roughness: 0.92, metalness: 0.05 }),
    )
    floor.rotation.x = -Math.PI / 2
    floor.receiveShadow = true
    this.scene.add(floor)

    const ring = new THREE.Mesh(
      new THREE.RingGeometry(1.05, 1.08, 96),
      new THREE.MeshBasicMaterial({ color: 0xd6bd80, transparent: true, opacity: 0.16 }),
    )
    ring.rotation.x = -Math.PI / 2
    ring.position.y = 0.002
    this.scene.add(ring)
  }

  private buildMannequin(): void {
    const skin = new THREE.MeshStandardMaterial({ color: BODY_COLOR, roughness: 0.9, metalness: 0.02 })
    const group = new THREE.Group()

    const add = (mesh: THREE.Mesh) => {
      mesh.castShadow = true
      mesh.receiveShadow = true
      group.add(mesh)
      return mesh
    }

    // 躯干：上宽下窄，比服装壳层略细，避免与壳层穿插
    const torso = new THREE.Mesh(new THREE.CylinderGeometry(0.168, 0.142, 0.5, 28), skin)
    torso.scale.x = 1.06
    torso.position.y = 1.19
    add(torso)

    const chest = new THREE.Mesh(new THREE.SphereGeometry(0.175, 24, 16), skin)
    chest.scale.set(1.05, 0.38, 0.6)
    chest.position.y = 1.41
    add(chest)

    const neck = new THREE.Mesh(new THREE.CylinderGeometry(0.05, 0.062, 0.1, 16), skin)
    neck.position.y = 1.49
    add(neck)

    const head = new THREE.Mesh(new THREE.SphereGeometry(0.1, 32, 24), skin)
    head.scale.set(0.9, 1.08, 0.96)
    head.position.y = 1.6
    add(head)

    const hips = new THREE.Mesh(new THREE.SphereGeometry(0.155, 24, 16), skin)
    hips.scale.set(1.02, 0.66, 0.72)
    hips.position.y = 0.94
    add(hips)

    for (const side of [-1, 1]) {
      const leg = new THREE.Mesh(new THREE.CapsuleGeometry(0.072, 0.58, 8, 16), skin)
      leg.position.set(side * 0.078, 0.49, 0)
      add(leg)

      const foot = new THREE.Mesh(new THREE.CapsuleGeometry(0.042, 0.13, 6, 12), skin)
      foot.rotation.x = Math.PI / 2
      foot.position.set(side * 0.078, 0.055, 0.055)
      add(foot)

      const arm = new THREE.Mesh(new THREE.CapsuleGeometry(0.046, 0.34, 8, 16), skin)
      arm.position.set(side * 0.228, 1.21, 0)
      arm.rotation.z = side * 0.09
      add(arm)

      const hand = new THREE.Mesh(new THREE.SphereGeometry(0.048, 16, 12), skin)
      hand.scale.set(0.9, 1.2, 0.9)
      hand.position.set(side * 0.245, 0.99, 0)
      add(hand)
    }

    this.stage.add(group)
  }

  private buildGarment(item: GarmentInput): THREE.Mesh[] {
    const color = new THREE.Color(item.colorHex || '#9AA5B1')
    const material = new THREE.MeshStandardMaterial({ color, roughness: 0.74, metalness: 0.04 })
    const meshes: THREE.Mesh[] = []

    switch (item.category) {
      case 'TOP': {
        const body = new THREE.Mesh(new THREE.CylinderGeometry(0.202, 0.19, 0.5, 28), material)
        body.position.y = 1.19
        meshes.push(body)
        for (const side of [-1, 1]) {
          const sleeve = new THREE.Mesh(new THREE.CylinderGeometry(0.07, 0.058, 0.26, 18), material)
          sleeve.position.set(side * 0.205, 1.3, 0)
          sleeve.rotation.z = side * 0.13
          meshes.push(sleeve)
        }
        break
      }
      case 'OUTER': {
        const shell = new THREE.Mesh(new THREE.CylinderGeometry(0.222, 0.214, 0.6, 28), material)
        shell.position.y = 1.16
        meshes.push(shell)
        for (const side of [-1, 1]) {
          const sleeve = new THREE.Mesh(new THREE.CylinderGeometry(0.072, 0.062, 0.3, 18), material)
          sleeve.position.set(side * 0.238, 1.18, 0)
          sleeve.rotation.z = side * 0.06
          meshes.push(sleeve)
        }
        break
      }
      case 'BOTTOM': {
        const isSkirt = /裙/.test(item.subCategory ?? '') || /skirt/i.test(item.subCategory ?? '')
        if (isSkirt) {
          const skirt = new THREE.Mesh(new THREE.ConeGeometry(0.3, 0.66, 32), material)
          skirt.position.y = 0.62
          meshes.push(skirt)
        } else {
          for (const side of [-1, 1]) {
            const leg = new THREE.Mesh(new THREE.CylinderGeometry(0.098, 0.086, 0.8, 20), material)
            leg.position.set(side * 0.08, 0.5, 0)
            meshes.push(leg)
          }
          const waist = new THREE.Mesh(new THREE.CylinderGeometry(0.178, 0.174, 0.16, 24), material)
          waist.position.y = 0.91
          meshes.push(waist)
        }
        break
      }
      case 'DRESS': {
        const skirt = new THREE.Mesh(new THREE.ConeGeometry(0.32, 0.86, 32), material)
        skirt.position.y = 0.62
        meshes.push(skirt)
        const bodice = new THREE.Mesh(new THREE.CylinderGeometry(0.192, 0.184, 0.4, 28), material)
        bodice.position.y = 1.19
        meshes.push(bodice)
        break
      }
      case 'SHOES': {
        for (const side of [-1, 1]) {
          const shoe = new THREE.Mesh(new THREE.BoxGeometry(0.125, 0.07, 0.24), material)
          shoe.position.set(side * 0.078, 0.05, 0.05)
          meshes.push(shoe)
        }
        break
      }
      case 'BAG': {
        const bag = new THREE.Mesh(new THREE.BoxGeometry(0.16, 0.19, 0.07), material)
        bag.position.set(0.3, 0.9, 0.02)
        bag.rotation.z = 0.18
        meshes.push(bag)
        break
      }
      case 'ACCESSORY': {
        const necklace = new THREE.Mesh(new THREE.TorusGeometry(0.1, 0.01, 8, 40), material)
        necklace.rotation.x = Math.PI / 2
        necklace.position.y = 1.47
        meshes.push(necklace)
        break
      }
      default:
        break
    }

    for (const mesh of meshes) {
      mesh.castShadow = true
    }
    return meshes
  }
}

function disposeObject(object: THREE.Object3D): void {
  const mesh = object as THREE.Mesh
  if (mesh.geometry) {
    mesh.geometry.dispose()
  }
  const material = mesh.material as THREE.Material | THREE.Material[] | undefined
  if (Array.isArray(material)) {
    material.forEach((item) => item.dispose())
  } else {
    material?.dispose()
  }
}