import CryptoJS from 'crypto-js'
import JSEncrypt from 'jsencrypt'

let RSA_PUBLIC_KEY = ''

// 初始化获取后端RSA公钥

  RSA_PUBLIC_KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCMgB9bRbJKG2acxLC1S6vCY0kTXZQWAD4VEdyGG9aS0W0z6T/g09/G4lKfyPRGNO1InT0qqph8X0rY38srKj0AhduHFTs2qQSqAvqZy/qp/8tuEiXYQgGXaUhVT0cMUBpb5jCkf8+yBliTX8RJgfUsWAbSYHIOykrbKI//AD03mwIDAQAB"

// RSA加密
export function rsaEncrypt(data) {
  const encrypt = new JSEncrypt()
  encrypt.setPublicKey(RSA_PUBLIC_KEY)
  return encrypt.encrypt(data)
}

// 生成随机 AES KEY(32) + IV(16)
export function generateAesKeyAndIv() {
  return {
    aesKey: CryptoJS.lib.WordArray.random(32).toString().substr(0, 32),
    aesIv: CryptoJS.lib.WordArray.random(16).toString().substr(0, 16)
  }
}

// AES 加密
export function aesEncrypt(data, aesKey, aesIv) {
  const key = CryptoJS.enc.Utf8.parse(aesKey)
  const iv = CryptoJS.enc.Utf8.parse(aesIv)
  return CryptoJS.AES.encrypt(
    CryptoJS.enc.Utf8.parse(data),
    key,
    { iv, mode: CryptoJS.mode.CBC, padding: CryptoJS.pad.Pkcs7 }
  ).toString()
}

// AES 解密
export function aesDecrypt(data, aesKey, aesIv) {
  const key = CryptoJS.enc.Utf8.parse(aesKey)
  const iv = CryptoJS.enc.Utf8.parse(aesIv)
  const decrypted = CryptoJS.AES.decrypt(data, key, { iv, mode: CryptoJS.mode.CBC, padding: CryptoJS.pad.Pkcs7 })
  return decrypted.toString(CryptoJS.enc.Utf8)
}