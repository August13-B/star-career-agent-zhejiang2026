package org.example.web.controller;

import org.example.web.entity.Result;
import org.example.web.tool.RSA_256;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/new")
public class NewController {

    private final RSA_256 rsa256;

    public NewController(RSA_256 rsa256) {
        this.rsa256 = rsa256;
    }

    // 重点：consumes 明确指定接收表单，绝对不读 JSON
    @PostMapping("/get_info")
    @CrossOrigin
    public Result getInfo(
            @RequestParam String AES,
            @RequestParam String IV,
            @RequestParam String encryptedData
    ) throws Exception {

        String aesKey = rsa256.rsaDecrypt(AES);//用RSA私钥解密前端密钥
        String aesIv = rsa256.rsaDecrypt(IV);//用RSA私钥解密前端偏移值
        String plain = rsa256.aesDecrypt(encryptedData, aesKey, aesIv);//用前端密钥偏移值来阶加密加密数据

        return Result.success(plain);
    }

    @PostMapping("/get_info_miwen")
    @CrossOrigin
    public Result getInfo_miwen(
            @RequestParam String AES,
            @RequestParam String IV,
            @RequestParam String encryptedData
    ) throws Exception {

        String aesKey = rsa256.rsaDecrypt(AES);
        String aesIv = rsa256.rsaDecrypt(IV);
        String plain = rsa256.aesDecrypt(encryptedData, aesKey, aesIv);
        String plain2 = rsa256.aesEncrypt(plain, aesKey, aesIv);
        return Result.success(plain2);
    }
}