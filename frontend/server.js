import http from 'node:http'
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)

const port = Number(process.env.PORT || 8080)
const backendBaseUrl = (process.env.BACKEND_BASE_URL || 'http://api-gateway').replace(/\/$/, '')
const distDir = path.join(__dirname, 'dist')

const mimeTypes = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.svg': 'image/svg+xml',
  '.json': 'application/json; charset=utf-8',
  '.ico': 'image/x-icon',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.png': 'image/png',
  '.webp': 'image/webp',
}

function send(res, statusCode, body, headers = {}) {
  res.writeHead(statusCode, {
    'Access-Control-Allow-Origin': '*',
    ...headers,
  })
  res.end(body)
}

function readStaticFile(filePath) {
  const ext = path.extname(filePath).toLowerCase()
  const contentType = mimeTypes[ext] || 'application/octet-stream'
  return { contentType, stream: fs.createReadStream(filePath) }
}

async function proxyToBackend(req, res) {
  const targetUrl = new URL(req.url || '/', backendBaseUrl)
  const headers = { ...req.headers }
  headers.host = targetUrl.host
  delete headers.origin
  delete headers.referer
  delete headers['sec-fetch-site']
  delete headers['sec-fetch-mode']
  delete headers['sec-fetch-dest']
  delete headers['sec-fetch-user']

  const proxyResponse = await fetch(targetUrl, {
    method: req.method,
    headers,
    body: req.method === 'GET' || req.method === 'HEAD' ? undefined : req,
    duplex: 'half',
  })

  const responseHeaders = {}
  proxyResponse.headers.forEach((value, key) => {
    if (key.toLowerCase() !== 'transfer-encoding') {
      responseHeaders[key] = value
    }
  })

  res.writeHead(proxyResponse.status, responseHeaders)
  if (req.method === 'HEAD') {
    res.end()
    return
  }

  const body = Buffer.from(await proxyResponse.arrayBuffer())
  res.end(body)
}

const server = http.createServer(async (req, res) => {
  try {
    const urlPath = decodeURIComponent((req.url || '/').split('?')[0])

    if (urlPath.startsWith('/api/') || urlPath === '/api') {
      await proxyToBackend(req, res)
      return
    }

    if (urlPath.startsWith('/actuator/')) {
      await proxyToBackend(req, res)
      return
    }

    const normalizedPath = urlPath === '/' ? '/index.html' : urlPath
    let filePath = path.join(distDir, normalizedPath)

    if (!filePath.startsWith(distDir)) {
      send(res, 403, 'Forbidden')
      return
    }

    if (!fs.existsSync(filePath) || fs.statSync(filePath).isDirectory()) {
      filePath = path.join(distDir, 'index.html')
    }

    const { contentType, stream } = readStaticFile(filePath)
    res.writeHead(200, { 'Content-Type': contentType })
    stream.pipe(res)
  } catch (error) {
    send(res, 500, `Server error: ${error instanceof Error ? error.message : String(error)}`)
  }
})

server.listen(port, () => {
  console.log(`Frontend server listening on port ${port}`)
  console.log(`Proxying API routes to ${backendBaseUrl}`)
})