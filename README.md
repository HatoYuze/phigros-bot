# Phigros-Bot

*一个基于 `mirai` 的 `phigros` QQ 机器人插件*

> 该项目仍在**快速开发**中，可能会出现 功能缺陷等问题，您可以通过 [issue](https://github.com/HatoYuze/phigros-bot/issues) 向我们反馈

---

## 如何使用？
- 请确认您的 `mirai` 属于可用状态 _(可以正常收发信息)_
> 如果在您尝试登录时遇到了困难 
> - 可以尝试使用 [MrXiaoM/Overflow](https://github.com/MrXiaoM/Overflow) 所提供的方案对接 `onebot` 协议
> - 或者寻找可用的签名服务器并使用可用的 `qsign` 端
- 您可从 [release](https://github.com/HatoYuze/phigros-bot/releases) 下载后缀为 `mirai2.jar` 的文件, 
随后丢进`plugins`即可

> 为了保证本项目的存档数据为最新的，还请您即时更新本包!

---

## 指令列表
- `bind` (`/phi bind <sessionToken>`)
> 绑定账号
- `b19` (`/phi b19`)
> 根据所绑定的账号获取 `bests` 记录
- `info` (`/phi info <曲名>`)
> 获取所绑定账号指定歌曲的游玩记录
- `rank` (`/phi rank`)
> 获取用户列表排行榜
- ...

---

## 待办列表
- [ ] 对各部分功能图形化
> - [ ] b19
> - [ ] 个人数据
> - [ ] 排行榜
> - [ ] 曲 info
- [x] 别名查曲 & 添加别名
- [x] 获取曲绘
- [ ] 能够使用 `Taptap` 扫描二维码绑定 `sessionToken`

---

_特别感谢 [Rosemoe](https://github.com/Rosemoe) 为本项目提供了算法支持_