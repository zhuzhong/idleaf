


create table id_segment( 
biz_tag varchar(50) comment '业务标识', 
max_id  bigint(20) comment '分配的id号段的最大值', 
p_step bigint(20) comment '步长' )
engine=InnoDB default charset=utf8 comment '号段存储表';


insert into id_segment(biz_tag,max_id,p_step)values('ORDER',0,20);


alter table id_segment add last_update_time datetime;
alter table id_segment add current_update_time datetime;

update id_segment set last_update_time=now(),current_update_time=now();


create table id_test(
p_id bigint(20)
)engine=InnoDB default charset=utf8 comment '用于测试';