


create table id_segment( 
biz_tag varchar(50) comment '业务标识', 
max_id  bigint(20) comment '分配的id号段的最大值', 
p_step bigint(20) comment '步长' )
engine=InnoDB default charset=utf-8 comment '号段存储表';


insert into id_segment(biz_tag,max_id,p_step)values('ORDER',0,200);

create table id_test(
p_id bigint(20)
)engine=InnoDB default charset=utf-8 comment '用于测试';