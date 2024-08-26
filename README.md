# Phigros-Bot

*一个基于 `mirai` 的 `phigros` QQ 机器人插件*

> 该项目仍在**快速开发**中，可能会出现 功能缺陷等问题，您可以通过 [issue](https://github.com/HatoYuze/phigros-bot/issues) 向我们反馈

> 为了保证本项目的存档数据为最新的，还请您即时更新本包!

#### 我们建议您，在其他库转载本库内容时 _(例如曲绘，谱面等数据)_，标注素材的来源

---

## 如何使用？
- 请确认您的 `mirai` 属于可用状态 _(可以正常收发信息)_
> 如果在您尝试登录时遇到了困难 
> - 可以尝试使用 [MrXiaoM/Overflow](https://github.com/MrXiaoM/Overflow) 所提供的方案对接 `onebot` 协议
> - 或者寻找可用的签名服务器并使用可用的 `qsign` 端
- 您可从 [release](https://github.com/HatoYuze/phigros-bot/releases) 下载后缀为 `mirai2.jar` 的文件, 
随后丢进`plugins`即可

在您**首次启动**本插件时
插件会尝试从 [/phigros-res/Illustration/](https://github.com/HatoYuze/phigros-bot/tree/main/phigros-res/Illustration) 下载**所有**的 `png` 图片到插件的数据目录中

这个过程可能将**消耗 1 到 2 分钟**的时间，成功更新后会在控制台提示 `更新曲绘成功`, 随后的**每一次启动都会**检测曲绘文件是否是 最新的 并更新

> 下载过程中将默认使用 [https://gh.api.99988866.xyz/](https://github.com/hunshcn/gh-proxy) 提供的加速镜像，您也可以从设置中更改所配置的值


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
- `alias` (`/phi alias`)
> 获取别名对应的歌曲
- `addAlias` (`/phi alias`)
> 增加别名
- ...
---

## 配置文件说明

> 本插件的配置文件在 `./config/com.github.hatoyuze.phigros-bot` 目录下

### aliases.yml
*别名及模糊匹配相关设置*

- `isNormalUserAllowed` 是否允许普通用户添加别名 (默认为 `true`)
> 该项针对 `/phi aa` 指令，
>   如果为 `true` 则代表所有人都可使用该指令添加别名, 为`false`则代表只有被授予权限 `*:*` 的用户才可以添加别名

- `alias` 用于存放别名数据库 (默认将由 `jar` 包内部别名库初始化)
> 格式如下(仅供参考，实际使用请保证空格的对齐):
> ```yaml
> alias:
>  ‘曲目 sid’:
>   - '别名1'
>   - '...'
> ```

- `sensitivity` 模糊匹配歌曲曲名的灵敏度 (默认为 `0.4`)
> 模糊匹配使用了 `Levenshtein` 距离算法获取值， `不同值 / 字符长度` 若小于 该值 则判定为匹配成功
>  #### 例如:
>   要使 `fog` 匹配 `foge` 成功， 该值至少 `0.75`
>    一般地 该值越少越灵敏，需要相同的字符也越多

### github_proxy_url_setting.yml
*更新曲绘时使用的 `github` 代理网站设置*

~~该项一般用户无需修改，请确保您所提供的代理源是有效的~~

- `enable` 是否启用代理功能 (默认为 `true`)
> - 当为 `false` 时，直接请求 `raw.githubusercontent.com` 的 原网址
> - 为 `true` 时，将选择访问**设定的代理网站**而不是 原网址

- `method` 启用的方式 (默认为 `PREFIX`)
> 该项仅可选择下列值
> - `PREFIX` 在原网址的基础上添加前缀
> 
> > 例如对于 https://raw.githubusercontent.com/HatoYuze/phigros-bot/main/phigros-res/Illustration/BreakOver.Kforest.png ,  **启用**该项后的目标网站为
> > `$url/https://raw.githubusercontent.com/HatoYuze/phigros-bot/main/phigros-res/Illustration/BreakOver.Kforest.png`
> 
> - `REPLACE` 替换 `raw.githubusercontent.com` 为指定网站
> > 例如对于 https://raw.githubusercontent.com/HatoYuze/phigros-bot/main/phigros-res/Illustration/BreakOver.Kforest.png ,  **启用**该项后的目标网站为
> > `https://$url/HatoYuze/phigros-bot/main/phigros-res/Illustration/BreakOver.Kforest.png`
> 
>***上文中的 `$url` 皆代表配置中的 `url` 项***

- `url` (默认为 `https://gh.api.99988866.xyz/`)
> 若 `method` 为 `REPLACE` 时 **请勿** 添加`https://` 头
> 
> 若 `method` 为 `PREFIX` 时 **请**保证包含 `https://` 头

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

_感谢 [Rosemoe](https://github.com/Rosemoe) 为本项目提供了协议算法支持_

_特别感谢库 [3035936740/Phigros_Resource](https://github.com/3035936740/Phigros_Resource) 所提供的 `Phigros` 解包工具_
