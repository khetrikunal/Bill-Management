const TOKEN_KEY = 'bm_token'

export const getToken = () => localStorage.getItem(TOKEN_KEY)
export const setToken = (t: string | null) => (t ? localStorage.setItem(TOKEN_KEY, t) : localStorage.removeItem(TOKEN_KEY))

export class ApiError extends Error {
  status: number
  fieldErrors: Record<string, string>
  constructor(status: number, message: string, fieldErrors: Record<string, string> = {}) {
    super(message)
    this.status = status
    this.fieldErrors = fieldErrors
  }
}

let onUnauthorized: () => void = () => {}
export const setUnauthorizedHandler = (fn: () => void) => (onUnauthorized = fn)

async function request(method: string, path: string, body?: unknown, isForm = false): Promise<Response> {
  const headers: Record<string, string> = {}
  const t = getToken()
  if (t) headers.Authorization = `Bearer ${t}`
<<<<<<< HEAD
  const requestedCompany = new URLSearchParams(path.split('?')[1] || '').get('companyId')
  headers['X-Company-Id'] = requestedCompany && /^\d+$/.test(requestedCompany)
    ? requestedCompany
    : localStorage.getItem('bm_company_id') || '1'
=======
>>>>>>> origin/main
  if (body !== undefined && !isForm) headers['Content-Type'] = 'application/json'
  let res: Response
  try {
    res = await fetch(`/api${path}`, {
      method, headers,
      body: body === undefined ? undefined : isForm ? (body as FormData) : JSON.stringify(body),
    })
  } catch {
    throw new ApiError(0, 'Cannot reach the server. Check your connection.')
  }
  if (!res.ok) {
    let msg = `Request failed (${res.status})`
    let fields: Record<string, string> = {}
    try {
      const j = await res.json()
      msg = j.message || msg
      fields = j.fieldErrors || {}
    } catch { /* non-JSON error body */ }
    if (res.status === 401 && path !== '/auth/login') onUnauthorized()
    throw new ApiError(res.status, msg, fields)
  }
  return res
}

export const api = {
  get: async <T>(p: string): Promise<T> => (await request('GET', p)).json(),
  post: async <T>(p: string, b?: unknown): Promise<T> => {
    const r = await request('POST', p, b ?? {})
    const text = await r.text()
    return (text ? JSON.parse(text) : undefined) as T
  },
  put: async <T>(p: string, b: unknown): Promise<T> => (await request('PUT', p, b)).json(),
  del: async (p: string): Promise<void> => { await request('DELETE', p) },
  upload: async <T>(p: string, form: FormData): Promise<T> => (await request('POST', p, form, true)).json(),
  blob: async (p: string): Promise<{ blob: Blob; filename: string }> => {
    const r = await request('GET', p)
    const cd = r.headers.get('Content-Disposition') || ''
    const m = /filename\*?=(?:UTF-8'')?"?([^";]+)"?/i.exec(cd)
    return { blob: await r.blob(), filename: m ? decodeURIComponent(m[1]) : 'download' }
  },
}

export function saveBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  a.remove()
  setTimeout(() => URL.revokeObjectURL(url), 10000)
}

/** On phones: open the native share sheet (Print / Save to Files / WhatsApp...). Falls back to a normal download. */
export async function shareOrSave(blob: Blob, filename: string) {
  const file = new File([blob], filename, { type: blob.type })
  const nav = navigator as Navigator & { canShare?: (d: ShareData) => boolean }
  if (nav.canShare && nav.canShare({ files: [file] })) {
    try { await nav.share({ files: [file], title: filename }); return } catch { /* cancelled: fall through */ }
  }
  saveBlob(blob, filename)
}
