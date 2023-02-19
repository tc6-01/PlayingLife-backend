create table beeran.user
(
    id          bigint auto_increment comment 'id
'
        primary key,
    username    varchar(256)                       null comment '用户昵称',
    _account    varchar(256)                       null comment '账号',
    avatar_url  varchar(1024)                      null comment '头像地址',
    gender      tinyint                            null comment '性别',
    _password   varchar(256)                       not null comment '密码',
    phone       varchar(256)                       null comment '电话',
    email       varchar(512)                       null comment '邮箱',
    create_time datetime default CURRENT_TIMESTAMP null comment '创建时间',
    update_time datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    _status     int                                null comment '是否有效',
    is_delete   tinyint  default 0                 not null comment '是否删除',
    role        int      default 0                 not null comment '用户权限
0 普通用户
1 管理员',
    planetCode  varchar(512)                       null comment '星球编号',
    tags        varchar(1024)                      null comment '用户json标签',
    profile        varchar(1024)                      null comment '用户json标签'
)
    comment 'table';


# 添加修改字符编码
ALTER TABLE user MODIFY  username varchar(256) DEFAULT NULL;
ALTER TABLE user MODIFY _account varchar(256) DEFAULT NULL;
ALTER TABLE user MODIFY avatar_url varchar(1024) DEFAULT NULL;
ALTER TABLE user MODIFY _password varchar(256) DEFAULT NULL;
ALTER TABLE user MODIFY phone varchar(256) DEFAULT NULL;

ALTER TABLE user MODIFY tags varchar(1024) DEFAULT NULL;
ALTER TABLE user MODIFY profile varchar(1024) DEFAULT NULL;
show create table user;
