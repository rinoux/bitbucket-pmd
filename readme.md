一个用于bitbucket代码检测的插件重构

### 打包方式：
1.安装atlassian sdk；
2.命令行进入插件工程根文件夹，atlas-package打包；

其它命令：
- atlas-run   -- 在localhost安装并启动插件；
- atlas-debug -- 和stlas-run相同，可以在5005端口监听调试；
- atlas-cli   -- after atlas-run or atlas-debug, opens a Maven command line window:
                 - 'pi' reinstalls the plugin into the running product instance
- atlas-help  -- prints description for all commands in the SDK


- 开发文档地址:

https://developer.atlassian.com/display/DOCS/Introduction+to+the+Atlassian+Plugin+SDK
