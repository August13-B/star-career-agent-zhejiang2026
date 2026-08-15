package org.example.web.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.example.web.entity.Result;
import org.example.web.entity.User;
import org.example.web.service.UserService;
import org.example.web.tool.JwtUtil;
import org.example.web.tool.RSA_256;
import org.example.web.tool.SHA_256;
import org.example.web.tool.Version;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private RSA_256 rsa256;

    /**
     * 将对象转换为字符串，支持 Integer、Long、Double 等类型
     */
    private String convertToString(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof String) {
            return (String) obj;
        } else {
            return obj.toString();
        }
    }

    /**
     * 注册用户（支持加密邮箱、加密手机号、账号注册，支持邀请码验证）
     * 请求参数：nickname, userPassword, encryptedEmail, encryptedPhone, AES, IV, userAccount（可选），userRole（可选，整数），invitationCode（可选，字符串）
     * 注意：至少需要提供一种联系方式（加密邮箱或加密手机号）
     * 加密流程参考NewController：前端使用RSA-256加密临时AES密钥和IV，再用AES加密业务数据
     * 用户角色：1-学生（默认），2-管理员，3-企业端，4-导师
     * 非学生角色必须提供正确的邀请码
     */
    @PostMapping(value = "/register", produces = "application/json")
    @CrossOrigin
    public Result<Map<String, Object>> register(
            @RequestBody Map<String, Object> request,  // 改为 Object 以支持整数类型
            HttpSession session) throws Exception {

        // 从 Map 中获取参数，使用类型安全转换
        String nickname = convertToString(request.get("nickname"));
        String userPassword = convertToString(request.get("userPassword"));
        String encryptedEmail = convertToString(request.get("encryptedEmail"));
        String encryptedPhone = convertToString(request.get("encryptedPhone"));
        String AES = convertToString(request.get("AES"));
        String IV = convertToString(request.get("IV"));
        // userAccount 可选，如果未提供则由系统自动生成
        String userAccount = convertToString(request.get("userAccount"));
        
        // 获取用户角色：支持 userRole 和 userRoleStr 两种参数名
        Integer userRole = null;
        Object roleObj = request.get("userRole");
        if (roleObj == null) {
            // 如果 userRole 不存在，尝试 userRoleStr
            roleObj = request.get("userRoleStr");
        }
        if (roleObj != null) {
            if (roleObj instanceof Integer) {
                userRole = (Integer) roleObj;
            } else if (roleObj instanceof String) {
                try {
                    userRole = Integer.parseInt((String) roleObj);
                } catch (NumberFormatException e) {
                    return Result.<Map<String, Object>>error("用户角色格式错误，应为数字", null);
                }
            } else if (roleObj instanceof Number) {
                userRole = ((Number) roleObj).intValue();
            } else {
                return Result.<Map<String, Object>>error("用户角色格式错误", null);
            }
        }
        String invitationCode = convertToString(request.get("invitationCode")); // 邀请码保持字符串

        // 1. 参数验证
        if (nickname == null || nickname.length() > 32 || nickname.length() < 2) {
            return Result.error("昵称长度应在2-32位之间");
        }
        if (userPassword == null || userPassword.length() < 6 || userPassword.length() > 20) {
            return Result.error("密码长度应在6-20位之间");
        }
        
        // 2. 检查至少提供一种加密联系方式
        if ((encryptedEmail == null || encryptedEmail.isEmpty()) && 
            (encryptedPhone == null || encryptedPhone.isEmpty())) {
            return Result.error("请提供邮箱或手机号");
        }
        
        // 3. 检查AES和IV参数
        if (AES == null || AES.isEmpty() || IV == null || IV.isEmpty()) {
            return Result.error("加密参数缺失");
        }
        
        // 4. 解密邮箱和手机号
        String email = null;
        String phone = null;
        
        // 处理可能的空格替换（前端可能将+替换为空格）
        if (encryptedEmail != null && encryptedEmail.contains(" ")) {
            encryptedEmail = encryptedEmail.replace(' ', '+');
        }
        if (encryptedPhone != null && encryptedPhone.contains(" ")) {
            encryptedPhone = encryptedPhone.replace(' ', '+');
        }
        if (AES != null && AES.contains(" ")) {
            AES = AES.replace(' ', '+');
        }
        if (IV != null && IV.contains(" ")) {
            IV = IV.replace(' ', '+');
        }
        
        // 解密AES密钥和IV
        String aesKey = rsa256.rsaDecrypt(AES);
        String aesIv = rsa256.rsaDecrypt(IV);
        
        // 解密邮箱（如果提供）
        if (encryptedEmail != null && !encryptedEmail.isEmpty()) {
            try {
                email = rsa256.aesDecrypt(encryptedEmail, aesKey, aesIv);
                System.out.println("解密后的邮箱：" + email);
                
                // 检查邮箱格式
                if (!email.contains("@") || !email.contains(".")) {
                    return Result.<Map<String, Object>>error("邮箱格式不正确", null);
                }
                
                // 检查邮箱是否已被注册
                if (userService.isEmailExist(email)) {
                    return Result.<Map<String, Object>>error("该邮箱已被注册", null);
                }
            } catch (Exception e) {
                return Result.<Map<String, Object>>error("邮箱解密失败", null);
            }
        }
        
        // 解密手机号（如果提供）
        if (encryptedPhone != null && !encryptedPhone.isEmpty()) {
            try {
                phone = rsa256.aesDecrypt(encryptedPhone, aesKey, aesIv);
                System.out.println("解密后的手机号：" + phone);
                
                // 检查手机号格式（11位数字）
                if (!phone.matches("\\d{11}")) {
                    return Result.<Map<String, Object>>error("手机号格式不正确，应为11位数字", null);
                }
                
                // 检查手机号是否已被注册
                if (userService.isPhoneExist(phone)) {
                    return Result.<Map<String, Object>>error("该手机号已被注册", null);
                }
            } catch (Exception e) {
                return Result.<Map<String, Object>>error("手机号解密失败", null);
            }
        }
        
        // 5. 检查用户账号是否已被使用（如果提供）
        if (userAccount != null && !userAccount.isEmpty()) {
            if (userService.isUserAccountExist(userAccount)) {
                return Result.<Map<String, Object>>error("该账号已被使用", null);
            }
        }
        
        // 6. 处理用户角色和邀请码（明文）
        // userRole 变量已在上方定义，如果为null则使用默认值1（学生角色）
        if (userRole == null) {
            userRole = 1; // 默认学生角色
        }
        
        System.out.println("用户角色：" + userRole);
        
        // 验证用户角色是否合法（1-4）
        if (userRole < 1 || userRole > 4) {
            return Result.<Map<String, Object>>error("用户角色不合法，应为1-4", null);
        }
        
        // 如果是非学生角色，必须提供邀请码
        if (userRole != 1) {
            if (invitationCode == null || invitationCode.isEmpty()) {
                return Result.<Map<String, Object>>error("非学生角色必须提供邀请码", null);
            }
            
            System.out.println("用户角色：" + userRole + "，邀请码：" + invitationCode);
        }
        
        // 7. 创建用户对象
        User user = new User();
        user.setNickname(nickname);
        user.setUserPassword(userPassword); // Service层会进行SHA256加密
        
        // 设置联系方式
        user.setEmail(email);
        user.setPhone(phone);
        
        // 设置用户账号（如果未提供，Service层会自动生成）
        if (userAccount != null && !userAccount.isEmpty()) {
            user.setUserAccount(userAccount);
        }
        
        // 确定注册类型
        if (email != null && !email.isEmpty()) {
            user.setRegisterType(2); // 邮箱注册
        } else if (phone != null && !phone.isEmpty()) {
            user.setRegisterType(1); // 手机号注册
        } else {
            user.setRegisterType(5); // 学号/自定义账号注册
        }
        
        user.setUserRole(userRole); // 设置用户角色（可能是默认1，也可能是解密得到的）
        user.setUserStatus(1); // 正常状态
        
        // 8. 注册用户（传递邀请码）
        try {
            userService.register(user, invitationCode);
        } catch (RuntimeException e) {
            // 捕获注册过程中的异常（如邀请码无效）
            return Result.<Map<String, Object>>error(e.getMessage(), null);
        } catch (Exception e) {
            return Result.<Map<String, Object>>error("注册失败：" + e.getMessage(), null);
        }
        
        // 8. 返回生成的用户账号
        // 由于Service层会自动生成userAccount，需要根据注册方式查询用户
        User registeredUser = null;
        if (email != null && !email.isEmpty()) {
            registeredUser = userService.findByEmail(email);
        } else if (phone != null && !phone.isEmpty()) {
            registeredUser = userService.findByPhone(phone);
        }
        
        if (registeredUser == null) {
            return Result.<Map<String, Object>>error("注册失败，无法找到新注册的用户", null);
        }
        
        // 9. 对返回的敏感信息进行加密（使用前端提供的临时AES密钥和IV）
        Map<String, Object> data = new HashMap<>();
        data.put("message", "注册成功");
        data.put("userAccount", registeredUser.getUserAccount());
        data.put("nickname", registeredUser.getNickname());
        
        // 加密返回的邮箱和手机号（如果存在）使用用户输入的明文
        if (email != null && !email.isEmpty()) {
            String encryptedReturnEmail = rsa256.aesEncrypt(email, aesKey, aesIv);
            data.put("encryptedEmail", encryptedReturnEmail);
        }
        if (phone != null && !phone.isEmpty()) {
            String encryptedReturnPhone = rsa256.aesEncrypt(phone, aesKey, aesIv);
            data.put("encryptedPhone", encryptedReturnPhone);
        }
        
        return Result.success(data);
    }

    /**
     * 用户登录（支持加密账号、邮箱、手机号登录）
     * 请求参数：login_way (userAccount/email/phone), encryptedLoginValue, encryptedPassword, AES, IV
     * 加密流程：前端使用RSA公钥加密临时AES密钥和IV，再用AES加密登录凭证和密码
     */
    @PostMapping("/login")
    @CrossOrigin
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> login_data) throws Exception {
        String login_way = login_data.get("login_way");
        String encryptedLoginValue = login_data.get("encryptedLoginValue");
        String encryptedPassword = login_data.get("encryptedPassword");
        String AES = login_data.get("AES");
        String IV = login_data.get("IV");
        
        // 1. 参数验证
        if (login_way == null || encryptedLoginValue == null || encryptedPassword == null || 
            AES == null || IV == null) {
            return Result.error("参数不能为空");
        }
        
        // 2. 处理空格替换（前端可能将+替换为空格）
        if (encryptedLoginValue.contains(" ")) {
            encryptedLoginValue = encryptedLoginValue.replace(' ', '+');
        }
        if (encryptedPassword.contains(" ")) {
            encryptedPassword = encryptedPassword.replace(' ', '+');
        }
        if (AES.contains(" ")) {
            AES = AES.replace(' ', '+');
        }
        if (IV.contains(" ")) {
            IV = IV.replace(' ', '+');
        }
        
        // 3. 解密AES密钥和IV
        String aesKey = rsa256.rsaDecrypt(AES);
        String aesIv = rsa256.rsaDecrypt(IV);
        
        // 4. 解密登录凭证和密码
        String login_value;
        String user_password;
        try {
            login_value = rsa256.aesDecrypt(encryptedLoginValue, aesKey, aesIv);
            user_password = rsa256.aesDecrypt(encryptedPassword, aesKey, aesIv);
            System.out.println("解密后的登录凭证：" + login_value);
        } catch (Exception e) {
            return Result.error("登录凭证或密码解密失败");
        }
        
        if (login_value.isEmpty()) {
            return Result.error("登录凭证不能为空");
        }
        if (user_password.isEmpty()) {
            return Result.error("密码不能为空");
        }
        
        // 5. 根据登录方式查找用户
        User user = null;
        switch (login_way) {
            case "userAccount":
                user = userService.findByUserAccount(login_value);
                break;
            case "email":
                user = userService.findByEmail(login_value);
                break;
            case "phone":
                user = userService.findByPhone(login_value);
                break;
            default:
                return Result.error("登录方式填写有误，应为 userAccount/email/phone");
        }
        
        // 6. 检查用户是否存在
        if (user == null) {
            return Result.error("用户不存在");
        }
        
        // 7. 验证密码（数据库存储的是SHA256加密后的密码）
        String encryptedPasswordHash = SHA_256.sha256(user_password);
        if (!encryptedPasswordHash.equals(user.getUserPassword())) {
            return Result.error("密码错误");
        }
        
        // 8. 检查用户状态
        if (user.getUserStatus() != 1) {
            return Result.error("账号状态异常，请联系管理员");
        }
        
        // 9. 生成JWT token
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", user.getId());
        claims.put("userAccount", user.getUserAccount());
        claims.put("userRole", user.getUserRole());
        String jwtToken = JwtUtil.genToken(claims);

        // 10. 构造返回数据
        Map<String, Object> loginData = new HashMap<>();
        loginData.put("id", user.getId());
        loginData.put("userAccount", user.getUserAccount());
        loginData.put("nickname", user.getNickname());
        loginData.put("userRole", user.getUserRole());
        loginData.put("userStatus", user.getUserStatus());
        // 返回的邮箱和手机号需要加密（使用相同的临时AES密钥）
        if (user.getEmail() != null && !user.getEmail().isEmpty()) {
            String encryptedReturnEmail = rsa256.aesEncrypt(user.getEmail(), aesKey, aesIv);
            loginData.put("encryptedEmail", encryptedReturnEmail);
        }
        if (user.getPhone() != null && !user.getPhone().isEmpty()) {
            String encryptedReturnPhone = rsa256.aesEncrypt(user.getPhone(), aesKey, aesIv);
            loginData.put("encryptedPhone", encryptedReturnPhone);
        }
        loginData.put("token", jwtToken);

        return Result.success(loginData);
    }

    /**
     * 根据ID删除用户（范围删除）
     */
    @PostMapping("/deleteById")
    @CrossOrigin
    public Result<Void> deleteById(@RequestParam Integer start, @RequestParam Integer end) {
        userService.deleteById(start, end);
        return Result.success();
    }

    /**
     * 获取用户信息（明文版，需要token验证）
     */
    @GetMapping("/getUserInfo")
    @CrossOrigin
    public Result<Map<String, Object>> getUserInfo(@RequestHeader("Authorization") String token,@RequestParam String IV,@RequestParam String AES) {
        if (AES != null && AES.contains(" ")) {
            AES = AES.replace(' ', '+');
        }
        if (IV != null && IV.contains(" ")) {
            IV = IV.replace(' ', '+');
        }
        if (token == null || token.isEmpty()) {
            return Result.<Map<String, Object>>error("token不能为空", null);
        }
        
        try {
            Map<String, Object> claims = JwtUtil.parseToken(token);
            Long userId = Long.parseLong(claims.get("id").toString());
            User user = userService.findById(userId);
            
            if (user == null) {
                return Result.<Map<String, Object>>error("用户不存在", null);
            }
            String aesKey = rsa256.rsaDecrypt(AES);
            String aesIv = rsa256.rsaDecrypt(IV);
            String phone=null;
            String email=null;
            if(user.getPhone() != null && !user.getPhone().isEmpty()){
                phone = rsa256.decryptFromDB(user.getPhone());
            }
            if (user.getEmail() != null && !user.getEmail().isEmpty()){
                email = rsa256.decryptFromDB(user.getEmail());
            }



            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", user.getId());
            userInfo.put("userAccount", user.getUserAccount());
            userInfo.put("userRole", user.getUserRole());
            userInfo.put("userStatus", user.getUserStatus());
            userInfo.put("createTime", user.getCreateTime());
            userInfo.put("nickname", user.getNickname());
            if(phone!=null && !phone.isEmpty()){
                userInfo.put("phone", rsa256.aesEncrypt(phone, aesKey, aesIv));
            }
            if(email!=null && !email.isEmpty()){
                userInfo.put("email", rsa256.aesEncrypt(email, aesKey, aesIv));
            }
            return Result.success(userInfo);
        } catch (Exception e) {
            return Result.<Map<String, Object>>error("token无效或已过期", null);
        }
    }

    /**
     * 发送验证码邮件（用于邮箱验证，如注册时验证邮箱是否可用）
     */
    @GetMapping("/sendmail")
    @CrossOrigin
    public Result<String> sendmail(@RequestParam String encryptedEmail, String AES,String IV,HttpSession session) throws Exception {
        // 解密邮箱
        if (encryptedEmail != null && encryptedEmail.contains(" ")) {
            encryptedEmail = encryptedEmail.replace(' ', '+');}
        if (AES != null && AES.contains(" ")) {
            AES = AES.replace(' ', '+');}
        if (IV != null && IV.contains(" ")) {
            IV = IV.replace(' ', '+');}
        String aesKey = rsa256.rsaDecrypt(AES);
        String aesIv = rsa256.rsaDecrypt(IV);
        String email = rsa256.aesDecrypt(encryptedEmail, aesKey, aesIv);
        System.out.println("待验证邮箱：" + email);
        
        // 检查邮箱是否已被注册（使用新的email字段）
        if (userService.isEmailExist(email)) {
            return Result.error("邮箱已被占用");
        }
        
        // 检查发送频率
        Long lasttime = userService.getcodetime(session);
        if (lasttime==null || System.currentTimeMillis()-lasttime>60*1000){
            userService.sendmail(email,session);
            return Result.success("验证码已发送，请注意查收！！");
        } else{
            Integer time = Math.toIntExact((60000-(System.currentTimeMillis()-lasttime)) / 1000);
            return Result.error("请于"+time+"秒之后再试");
        }
    }

    /**
     * 验证邮箱验证码（暂时保留原逻辑）
     */
    @PostMapping("/verify_code")
    @CrossOrigin
    public Result<String> verifyCode(@RequestBody Map<String, String> request, HttpSession session) throws Exception {

        // 从 Map 中获取所有参数
        String input_code = request.get("input_code");
        String encryptedEmail = request.get("encryptedEmail");
        String IV = request.get("IV");
        String AES = request.get("AES");
        String aesKey = rsa256.rsaDecrypt(AES);
        String aesIv = rsa256.rsaDecrypt(IV);
        String email = rsa256.aesDecrypt(encryptedEmail, aesKey, aesIv);

        Integer savedCode = (Integer) session.getAttribute("emailcode");
        Long codeTime = (Long) session.getAttribute("emailcodetime");
        String emailO = (String) session.getAttribute("email");

        
        if (savedCode != null && codeTime != null) {
            if (System.currentTimeMillis() - codeTime <= 3 * 60 * 1000) {
                if (savedCode.toString().equals(input_code) && email.equals(emailO)) {
                    session.removeAttribute("emailcode");
                    session.removeAttribute("emailcodetime");
                    session.setAttribute("new_email", email);
                    return Result.success("邮箱验证成功");
                } else {
                    return Result.error("验证码输入错误！");
                }
            } else {
                return Result.error("验证码已过期！");
            }
        } else {
            return Result.error("验证码失效！");
        }
    }

    /**
     * 获取版本号
     */
    @GetMapping("/get_version")
    @CrossOrigin
    public Result<String> getVersion() {
        Version version = new Version();
        String versionNumber = version.getVersion();
        return Result.success(versionNumber);
    }

    /**
     * 忘记密码 - 发送邮件（支持加密邮箱）
     */
    @GetMapping("/forget_password_sendmail")
    @CrossOrigin
    public Result<String> forgetPasswordSendmail(@RequestParam String encryptedEmail, 
                                                 @RequestParam String AES, 
                                                 @RequestParam String IV, 
                                                 HttpSession session) throws Exception {
        // 处理可能的空格替换
        if (encryptedEmail != null && encryptedEmail.contains(" ")) {
            encryptedEmail = encryptedEmail.replace(' ', '+');
        }
        if (AES != null && AES.contains(" ")) {
            AES = AES.replace(' ', '+');
        }
        if (IV != null && IV.contains(" ")) {
            IV = IV.replace(' ', '+');
        }
        
        // 解密邮箱
        String aesKey = rsa256.rsaDecrypt(AES);
        String aesIv = rsa256.rsaDecrypt(IV);
        String email = rsa256.aesDecrypt(encryptedEmail, aesKey, aesIv);
        System.out.println("忘记密码 - 解密后的邮箱：" + email);
        
        // 检查邮箱是否存在
        User userEmail = userService.findByEmail(email);
        if (userEmail != null){
            Long lasttime = userService.getcodetime(session);
            if (lasttime==null || System.currentTimeMillis()-lasttime>60*1000){
                userService.forget_password_sendmail(email,session);
                return Result.success("验证码已发送，请注意查收！！");
            } else{
                Integer time = Math.toIntExact((60000-(System.currentTimeMillis()-lasttime)) / 1000);
                return Result.error("请于"+time+"秒之后再试");
            }
        } else{
            return Result.error("未查询到相关邮箱！");
        }
    }

    /**
     * 忘记密码 - 重置密码（支持加密邮箱）
     */
    @PostMapping("/forget_password")
    @CrossOrigin
    public Result<String> forgetPassword(@RequestParam String encryptedEmail, 
                                         @RequestParam String encryptedNewPassword,
                                         @RequestParam String AES,
                                         @RequestParam String IV,
                                         HttpSession session) throws Exception {
        // 处理可能的空格替换
        if (encryptedEmail != null && encryptedEmail.contains(" ")) {
            encryptedEmail = encryptedEmail.replace(' ', '+');
        }
        if (encryptedNewPassword != null && encryptedNewPassword.contains(" ")) {
            encryptedNewPassword = encryptedNewPassword.replace(' ', '+');
        }
        if (AES != null && AES.contains(" ")) {
            AES = AES.replace(' ', '+');
        }
        if (IV != null && IV.contains(" ")) {
            IV = IV.replace(' ', '+');
        }
        
        // 解密参数
        String aesKey = rsa256.rsaDecrypt(AES);
        String aesIv = rsa256.rsaDecrypt(IV);
        String email = rsa256.aesDecrypt(encryptedEmail, aesKey, aesIv);
        String newPassword = rsa256.aesDecrypt(encryptedNewPassword, aesKey, aesIv);
        
        System.out.println("忘记密码 - 解密后的邮箱：" + email);
        
        User userinfo = userService.findByEmail(email);
        if (userinfo == null) {
            return Result.error("用户不存在");
        }
        
        // 修复：检查会话中的邮箱，使用正确的会话属性名
        String email0 = session.getAttribute("email") != null ? session.getAttribute("email").toString() : "";
        System.out.println("session中的邮箱: [" + email0 + "]");
        System.out.println("前端传来的邮箱: [" + email + "]");

        if (!email0.trim().equals(email.trim())) {
            return Result.error("前后邮箱不一致");
        }
        
        String password = SHA_256.sha256(newPassword);
        if (userinfo.getUserPassword().equals(password)){
            return Result.error("新密码要跟之前密码不一致！");
        }
        
        userService.updatePasswordByEmail(email, password);
        return Result.success("密码重置成功");
    }

    /**
     * 修改密码（需要旧密码验证）
     * 请求参数：userId, encryptedOldPassword, encryptedNewPassword, AES, IV
     * 流程：解密旧密码和新密码，验证旧密码是否正确，正确则更新为新密码
     */
    @PutMapping("/change_password")
    @CrossOrigin
    public Result<String> changePassword(@RequestBody Map<String, String> request) throws Exception {
        // 1. 获取参数
        String userIdStr = request.get("userId");
        String encryptedOldPassword = request.get("encryptedOldPassword");
        String encryptedNewPassword = request.get("encryptedNewPassword");
        String encryptedNewPassword_again = request.get("encryptedNewPassword_again");
        String AES = request.get("AES");
        String IV = request.get("IV");
        
        // 2. 参数验证
        if (userIdStr == null || userIdStr.isEmpty()) {
            return Result.error("用户ID不能为空");
        }
        if (encryptedOldPassword == null || encryptedOldPassword.isEmpty()) {
            return Result.error("旧密码不能为空");
        }
        if (encryptedNewPassword == null || encryptedNewPassword.isEmpty()) {
            return Result.error("新密码不能为空");
        }
        if (AES == null || AES.isEmpty() || IV == null || IV.isEmpty()) {
            return Result.error("加密参数缺失");
        }
        if (!Objects.equals(encryptedNewPassword_again, encryptedNewPassword)) {
            return Result.error("两次密码输入不一致");
        }
        
        // 3. 处理可能的空格替换（前端可能将+替换为空格）
        if (encryptedOldPassword.contains(" ")) {
            encryptedOldPassword = encryptedOldPassword.replace(' ', '+');
        }
        if (encryptedNewPassword.contains(" ")) {
            encryptedNewPassword = encryptedNewPassword.replace(' ', '+');
        }
        if (AES.contains(" ")) {
            AES = AES.replace(' ', '+');
        }
        if (IV.contains(" ")) {
            IV = IV.replace(' ', '+');
        }
        
        // 4. 解密AES密钥和IV
        String aesKey = rsa256.rsaDecrypt(AES);
        String aesIv = rsa256.rsaDecrypt(IV);
        
        // 5. 解密旧密码和新密码
        String oldPassword;
        String newPassword;
        try {
            oldPassword = rsa256.aesDecrypt(encryptedOldPassword, aesKey, aesIv);
            newPassword = rsa256.aesDecrypt(encryptedNewPassword, aesKey, aesIv);
        } catch (Exception e) {
            return Result.error("密码解密失败");
        }
        
        // 6. 验证密码长度
        if (newPassword.length() < 6 || newPassword.length() > 20) {
            return Result.error("新密码长度应在6-20位之间");
        }
        
        // 7. 转换用户ID
        Long userId;
        try {
            userId = Long.parseLong(userIdStr);
        } catch (NumberFormatException e) {
            return Result.error("用户ID格式错误");
        }
        
        // 8. 根据用户ID查找用户
        User user = userService.findById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        
        // 9. 验证旧密码是否正确
        String oldPasswordHash = SHA_256.sha256(oldPassword);
        if (!oldPasswordHash.equals(user.getUserPassword())) {
            return Result.error("旧密码错误");
        }
        
        // 10. 检查新密码是否与旧密码相同
        String newPasswordHash = SHA_256.sha256(newPassword);
        if (oldPasswordHash.equals(newPasswordHash)) {
            return Result.error("新密码不能与旧密码相同");
        }
        
        // 11. 更新密码
        try {
            userService.updatePasswordById(userId, newPasswordHash);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
        
        return Result.success("密码修改成功");
    }
}
