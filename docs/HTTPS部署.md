# 后端 HTTPS 部署 — 自签证书方案

> 把后端从 `http://10.0.0.1:3000` 升级为 `https://10.0.0.1:3443`,配置 SSL 证书。
> 内网 IP 无公网 CA 证书, 采用**自签根 CA + 签发 leaf 证书 + 手机信任 CA** 方案 (mkcert 风格)。

## 架构

```
后端机 (10.0.0.1)                        手机
┌────────────────────────────┐          ┌──────────────────────┐
│ backend/gen-cert.sh        │          │ 装根 CA: rootCA.crt   │
│  → cert/                   │          │  (系统凭据→CA证书)     │
│     rootCA.crt (CA)        │──传输──→ │                      │
│     cert.pem   (leaf)      │          │ network_security_config│
│     key.pem    (leaf key)  │          │  src="user" 信任用户CA │
│                            │          │                      │
│ server.js (https :3443)    │←─HTTPS──│ ApiClient →           │
│   https.createServer(cert) │          │  https://10.0.0.1:3443│
└────────────────────────────┘          └──────────────────────┘
```

## 步骤 (后端机)

```bash
cd backend
bash gen-cert.sh              # 默认签 10.0.0.1; 改 IP: bash gen-cert.sh 192.168.1.5
node server.js                # 输出 🔒 HTTPS: https://0.0.0.0:3443
```
- 有 cert/ → 自动 HTTPS(:3443) + HTTP→HTTPS 重定向(:3000)
- 无 cert → 回退 HTTP(:3000) 并警告

## 步骤 (手机)

1. 把 `backend/cert/rootCA.crt` 传到手机(如 Download 目录)
2. 设置 → 密码与安全 → 系统与凭据 → 加密与凭据 → **安装证书** → **CA 证书** → 选 rootCA.crt
3. (需锁屏密码验证) 命名 "Xinan CA" → 确定
4. 心安 app 自动信任(network_security_config 已配 `src="user"`)

## 验证

- app 登录/上报走 `https://10.0.0.1:3443`,SSL 握手成功(因手机信了自签 CA)
- `curl https://10.0.0.1:3443/api/analysis/trends`(后端机本机,带 --cacert rootCA.crt 验证)

## 改动清单

| 文件 | 改动 |
|------|------|
| backend/server.js | CFG 加 httpsPort(3443)+证书路径; 启动段 https 优先 + HTTP 重定向 |
| backend/gen-cert.sh | ★ 新增 openssl 生成 CA + leaf(含 SAN IP) |
| data/ApiClient.kt | baseUrl → `https://10.0.0.1:3443` |
| res/xml/network_security_config.xml | 信 system + user CA(移除明文) |
| AndroidManifest | 移除 usesCleartextTraffic(纯 HTTPS) |

## 若有公网域名

把自签换 Let's Encrypt 更佳:
- `npm i greenlock` 或 Caddy 反代 `domain.com { reverse_proxy localhost:3000 }`
- ApiClient baseUrl 改 `https://domain.com`
- 手机无需装 CA(LE 证书受系统信任)
