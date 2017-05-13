package com.zhuzhong.idleaf;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MysqlDemo {

    public Idsegment getNextIdsegment() {
        try {
            return updateId();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    public void insertIdTest(Connection conn, Long newId) {
        String sql = "insert into id_test(p_id) values(?)";
        PreparedStatement stmt = null;
        try {
            conn.setAutoCommit(false);
            stmt = conn.prepareStatement(sql);
            stmt.setLong(1, newId);
            stmt.execute();
            conn.commit();
        } catch (SQLException e) {
       
            e.printStackTrace();
        } finally {
            try {
                stmt.close();
            } catch (SQLException e) {
              
                e.printStackTrace();
            }
        }

    }

    private Idsegment updateId() throws Exception {
        String querySql = "select p_step ,max_id  from id_worker where biz_tag=?";
        String updateSql = "update id_worker set max_id=? where biz_tag=? and max_id=?";
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            stmt = conn.prepareStatement(querySql);

            stmt.setString(1, "ORDER");

            ResultSet rs = stmt.executeQuery();
            Long step = null;
            Long currentMaxId = null;
            while (rs.next()) {
                step = rs.getLong("p_step");
                currentMaxId = rs.getLong("max_id");
            }

            Long nextMaxId = currentMaxId + step;
            stmt.close();
            stmt = conn.prepareStatement(updateSql);
            stmt.setLong(1, nextMaxId);
            stmt.setString(2, "ORDER");
            stmt.setLong(3, currentMaxId);
            int result = stmt.executeUpdate();

            if (result == 1) {

                return new Idsegment(step, nextMaxId);
            } else {
                return updateId(); //并发原子更新，如果没有成功，则继续，直到成功为止
            }
        } finally {
            conn.commit();
            stmt.close();
            //conn.close();
        }
    }

    private Connection connection = null;

    public synchronized Connection getConnection() throws ClassNotFoundException, SQLException {
        if (connection == null) {
            // Connection conn = null;
            String sql;
            // MySQL的JDBC URL编写方式：jdbc:mysql://主机名称：连接端口/数据库的名称?参数=值
            // 避免中文乱码要指定useUnicode和characterEncoding
            // 执行数据库操作之前要在数据库管理系统上创建一个数据库，名字自己定，
            // 下面语句之前就要先创建javademo数据库
            String url = "jdbc:mysql://localhost:3306/test?"
                    + "user=root&password=mysql&useUnicode=true&characterEncoding=UTF8";

            // url="jdbc:h2:tcp://localhost/~/test";

            Class.forName("com.mysql.jdbc.Driver");// 动态加载mysql驱动

            System.out.println("成功加载驱动程序");
            // 一个Connection代表一个数据库连接
            this.connection = DriverManager.getConnection(url);
            return this.connection;
        } else {
            return connection;
        }
    }
}