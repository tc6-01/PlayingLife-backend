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