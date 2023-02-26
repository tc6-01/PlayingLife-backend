create table user
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
    comment 'table' charset = utf8;


# 添加修改字符编码
ALTER TABLE user MODIFY  username varchar(256) DEFAULT NULL;
ALTER TABLE user MODIFY _account varchar(256) DEFAULT NULL;
ALTER TABLE user MODIFY avatar_url varchar(1024) DEFAULT NULL;
ALTER TABLE user MODIFY _password varchar(256) DEFAULT NULL;
ALTER TABLE user MODIFY phone varchar(256) DEFAULT NULL;

ALTER TABLE user MODIFY tags varchar(1024) DEFAULT NULL;
ALTER TABLE user MODIFY profile varchar(1024) DEFAULT NULL;
show create table user;

-- auto-generated definition
create table tag
(
    id          bigint auto_increment comment 'id'
        primary key,
    tagName     varchar(256)                       null comment '标签名称',
    userID      varchar(256)                       null comment '用户ID',
    parentID    bigint                             null comment '父标签ID',
    isParent    tinyint                            null comment '是否为父标签 0为是， 1为否',
    create_time datetime default CURRENT_TIMESTAMP null comment '创建时间',
    update_time datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    is_delete   tinyint  default 0                 not null comment '是否删除',
    constraint uinIdx_tag_tagName
        unique (tagName)
)
    comment '标签' charset = utf8;

create index idx_userID
    on tag (userID);


create table team
(
    id           bigint auto_increment comment 'id'
        primary key,
    name   varchar(256)                   not null comment '队伍名称',
    description varchar(1024)                      null comment '描述',
    maxNum    int      default 1                 not null comment '最大人数',
    expireTime    datetime  null comment '过期时间',
    userId            bigint comment '用户id',
    status    int      default 0                 not null comment '0 - 公开，1 - 私有，2 - 加密',
    password varchar(512)                       null comment '密码',

    createTime   datetime default CURRENT_TIMESTAMP null comment '创建时间',
    updateTime   datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    isDelete     tinyint  default 0                 not null comment '是否删除'
)
    comment '队伍' charset = utf8;

create table user_team
(
    id           bigint auto_increment comment 'id'
        primary key,
    userId            bigint comment '用户id',
    teamId            bigint comment '队伍id',
    joinTime datetime  null comment '加入时间',
    createTime   datetime default CURRENT_TIMESTAMP null comment '创建时间',
    updateTime   datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    isDelete     tinyint  default 0                 not null comment '是否删除'
)
    comment '用户队伍关系' charset = utf8;
