package cn.yiliedu.servlet;
import cn.yiliedu.util.JDBCUtils;
import com.mysql.cj.util.StringUtils;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


@WebServlet("/loginServlet")
public class LoginServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        req.setCharacterEncoding("utf-8");
        PrintWriter printWriter=resp.getWriter();

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (StringUtils.isNullOrEmpty(username) || StringUtils.isNullOrEmpty(password)) {
            req.getRequestDispatcher("/failServlet").forward(req, resp);
        }

        String sql = "select * from customers where username = ? and password = ?";

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet resultSet = null;
        try {
            conn = JDBCUtils.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            resultSet = pstmt.executeQuery();
            if (!resultSet.next()) {
                //login failed
                req.getRequestDispatcher("/failServlet").forward(req,resp);
            } else {
                //login success,get customer id，save in cookie
                int id = resultSet.getInt(1);
                String customerId=Integer.toString(id);
                Cookie login=new Cookie("customerId",customerId);
                login.setMaxAge(600);
                resp.addCookie(login);
                resp.sendRedirect("http://localhost:8080/pay.html");
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } finally {
            JDBCUtils.close(resultSet, pstmt, conn);
        }

    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doGet(req,resp);
    }

}


