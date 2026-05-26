export type ApiOptions = RequestInit & { token?: string }

export async function apiClient<T>(input: string, options: ApiOptions = {}): Promise<T> {
  const response = await fetch(input, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(options.token ? { Authorization: ['Bearer', options.token].join(' ') } : {}),
      ...(options.headers ?? {}),
    },
  })

  if (!response.ok) {
    throw new Error(`API request failed with status ${response.status}`)
  }

  return (await response.json()) as T
}
