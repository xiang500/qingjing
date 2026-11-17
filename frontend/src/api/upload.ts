/** 图片上传：走 multipart/form-data，返回服务端保存后的可访问 URL */
export async function uploadImage(file: File): Promise<string> {
  const token = localStorage.getItem('qj.token')
  const form = new FormData()
  form.append('file', file)
  const response = await fetch('/api/upload', {
    method: 'POST',
    headers: token ? { Authorization: `Bearer ${token}` } : {},
    body: form,
  })
  const body = await response.json()
  if (body.code !== 0) {
    throw new Error(body.msg ?? body.message ?? '上传失败')
  }
  return (body.data as { url: string }).url
}
