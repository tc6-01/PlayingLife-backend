# User-center-Backend 用户中心后端Java实现

## 需求分析

### 库表设计
#### 用户表

- id（主键）bigint

- username 昵称  varchar
- userAccount 登录账号
- avatarUrl 头像 varchar
- gender 性别 tinyint
- userPassword 密码  varchar
- phone 电话 varchar
- email 邮箱 varchar
- userStatus 用户状态 int  0 - 正常
- createTime 创建时间（数据插入时间）datetime
- updateTime 更新时间（数据更新时间）datetime
- isDelete 是否删除 0 1（逻辑删除）tinyint
- role 用户权限 0 普通用户 1 管理员用户

> 是否需要添加索引

#### mybatis自动生成器
MyBatisX 插件，自动根据数据库生成 domain 实体对象、
mapper（操作数据库的对象）、mapper.xml（定义了 mapper对象和数据库的关联，
可以在里面自己写 SQL）、service（包含常用的增删改查）、serviceImpl（具体实现 service）

#### 用户需求 
- 用户注册
- 用户登录

#### 用户管理
- 用户搜素
- 删除用户


# 伙伴匹配系统

## 数据库表设计

标签的分类（要有哪些标签、怎么把标签进行分类）

### 新增标签表（分类表）

建议用标签，不要用分类，更灵活。

性别：男、女

方向：Java、C++、Go、前端

正在学：Spring

目标：考研、春招、秋招、社招、考公、竞赛（蓝桥杯）、转行、跳槽

段位：初级、中级、高级、王者

身份：小学、初中、高中、大一、大二、大三、大四、学生、待业、已就业、研一、研二、研三

状态：乐观、有点丧、一般、单身、已婚、有对象

**【用户自己定义标签】？**



字段：

id int 主键

标签名 varchar 非空（必须唯一，唯一索引）

上传标签的用户 userId int（如果要根据 userId 查已上传标签的话，最好加上，普通索引）

父标签 id ，parentId，int（分类）

是否为父标签 isParent, tinyint（0 不是父标签、1 - 父标签）

创建时间 createTime，datetime

更新时间 updateTime，datetime

是否删除 isDelete， tinyint（0、1）