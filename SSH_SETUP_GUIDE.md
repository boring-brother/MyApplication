# SSH 公钥配置指南

## 已创建的 SSH Key

✅ SSH 公钥已生成，位置：`C:\Users\86151\.ssh\id_ed25519_gitee.pub`

**公钥内容**：
```
ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIMt6hPRZi9vDewL8mqE1xYKtlBhqARpu6StZhvqzChcu lxt_wangyi@163.com
```

## 添加到 Gitee 的步骤

### 1. 复制公钥
打开 PowerShell，执行以下命令复制公钥：
```powershell
Get-Content "$env:USERPROFILE\.ssh\id_ed25519_gitee.pub" | Set-Clipboard
```

或者手动复制上面的公钥内容。

### 2. 登录 Gitee
访问：https://gitee.com/lxt_henan/MyApplication

### 3. 添加 SSH 公钥
1. 点击右上角头像 -> **设置**
2. 左侧菜单选择 **SSH 公钥**
3. 点击 **添加公钥**
4. 填写：
   - **标题**: MyApplication-PC
   - **公钥**: 粘贴刚才复制的公钥内容
5. 点击 **确定**

### 4. 测试连接
在 PowerShell 中执行：
```powershell
ssh -T git@gitee.com
```

如果看到欢迎信息，说明配置成功！

### 5. 推送代码
配置完成后，执行：
```powershell
cd "d:\Program Files\Android\prog\MyApplication"
git push -u origin main
```

## Git 配置信息

✅ 已配置的 Git 用户信息：
- **user.name**: lxt
- **user.email**: lxt_wangyi@163.com

✅ 远程仓库：
- **origin**: git@gitee.com:lxt_henan/MyApplication.git
- **github**: https://github.com/lxt-henan/MyApplication.git

## 完成推送后的仓库地址

- **Gitee**: https://gitee.com/lxt_henan/MyApplication
- **GitHub**: https://github.com/lxt-henan/MyApplication

---

**提示**：完成 SSH 公钥配置后，就可以直接推送代码了！
